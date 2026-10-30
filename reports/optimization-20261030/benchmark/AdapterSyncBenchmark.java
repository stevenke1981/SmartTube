import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Standalone algorithm benchmark; models Video.equals and Video.sync without Android. */
public class AdapterSyncBenchmark {
    private static volatile long checksum;
    private static final int REPETITIONS = 50;
    private static final int SAMPLES = 15;
    private static boolean faithfulHash;
    private static boolean guarded;

    private static final class Card {
        final int key;
        final boolean mix;
        int watched;
        final String videoId;
        final String playlistId = null;
        final String reloadPageKey = null;
        final String playlistParams = null;
        final String channelId = null;
        final int sectionId = -1;
        final String channelGroupId = null;
        final Object mediaItem = null;

        Card(int key, boolean mix, int watched) {
            this.key = key;
            this.mix = mix;
            this.watched = watched;
            videoId = "video_" + key;
        }

        @Override public int hashCode() {
            if (!faithfulHash) return key;
            int result = hashCodeAny(videoId, playlistId, reloadPageKey, playlistParams,
                    channelId, sectionId, channelGroupId, mediaItem);
            return result != -1 ? result : super.hashCode();
        }
        @Override public boolean equals(Object other) {
            return other instanceof Card && hashCode() == other.hashCode() && mix == ((Card) other).mix;
        }
    }

    // Same algorithm and eight-field call shape as Video.hashCode / Helpers.hashCodeAny.
    private static int hashCodeAny(Object... items) {
        if (items == null || items.length == 0) return -1;
        int hash = -1;
        for (Object item : items) {
            if (item != null && item.hashCode() != -1) {
                hash = 31 * hash + item.hashCode();
                break;
            }
        }
        return hash != -1 ? Math.abs(hash) : -1;
    }

    private static void baseline(List<Card> displayed, List<Card> updates, List<Integer> notifications) {
        for (Card update : updates) {
            for (int i = 0; i < displayed.size(); i++) {
                Card origin = displayed.get(i);
                if (origin.equals(update)) {
                    origin.watched = update.watched;
                    if (notifications != null) notifications.add(i);
                }
            }
        }
    }

    private static void indexed(List<Card> displayed, List<Card> updates, List<Integer> notifications) {
        if (updates.size() <= 1 || (guarded && (updates.size() < 20 || displayed.size() < 256))) {
            baseline(displayed, updates, notifications);
            return;
        }
        Map<Card, List<Integer>> positions = new HashMap<>();
        for (int i = 0; i < displayed.size(); i++) {
            positions.computeIfAbsent(displayed.get(i), key -> new ArrayList<>()).add(i);
        }
        for (Card update : updates) {
            List<Integer> matches = positions.get(update);
            if (matches != null) {
                for (int index : matches) {
                    displayed.get(index).watched = update.watched;
                    if (notifications != null) notifications.add(index);
                }
            }
        }
    }

    private static void assertSemantics() {
        List<Card> original = List.of(new Card(7, false, 0), new Card(7, false, 1),
                new Card(7, true, 2), new Card(99, false, 3));
        List<Card> updates = List.of(new Card(7, false, 80), new Card(7, true, 50),
                new Card(8, false, 30), new Card(7, false, 90));
        for (List<Card> input : List.of(updates, updates.subList(0, 1), List.<Card>of())) {
            List<Card> a = new ArrayList<>();
            List<Card> b = new ArrayList<>();
            for (Card card : original) {
                a.add(new Card(card.key, card.mix, card.watched));
                b.add(new Card(card.key, card.mix, card.watched));
            }
            List<Integer> expectedEvents = new ArrayList<>();
            List<Integer> actualEvents = new ArrayList<>();
            baseline(a, input, expectedEvents);
            indexed(b, input, actualEvents);
            if (!actualEvents.equals(expectedEvents)) throw new AssertionError("Notification order differs");
            for (int i = 0; i < a.size(); i++) {
                if (a.get(i).watched != b.get(i).watched) throw new AssertionError("State differs");
            }
            if (input == updates && (!actualEvents.equals(List.of(0, 1, 2, 0, 1)) ||
                    b.get(0).watched != 90 || b.get(1).watched != 90 ||
                    b.get(2).watched != 50 || b.get(3).watched != 3)) {
                throw new AssertionError("Duplicate/mix/missing/repeated update semantics differ");
            }
        }
        indexed(new ArrayList<>(), updates, new ArrayList<>());
    }

    private static long measure(boolean candidate, List<Card> displayed, List<Card> updates) {
        long begin = System.nanoTime();
        for (int repetition = 0; repetition < REPETITIONS; repetition++) {
            if (candidate) indexed(displayed, updates, null);
            else baseline(displayed, updates, null);
        }
        long elapsed = System.nanoTime() - begin;
        long sum = 0;
        for (Card card : displayed) sum += card.watched;
        checksum = sum;
        return elapsed / REPETITIONS;
    }

    public static void main(String[] args) {
        faithfulHash = args.length > 0 && args[0].equals("video-hash");
        assertSemantics();
        guarded = args.length > 1 && args[1].equals("guarded");
        System.err.println("PASS: duplicate, mix distinction, missing, repeated, empty, single, notifications");
        System.err.println("JVM: " + System.getProperty("java.runtime.version") + " " + System.getProperty("java.vm.name"));
        System.err.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        System.err.println("Model: " + (faithfulHash ? "Video hashCodeAny eight-field varargs; isMix simplified boolean" : "cheap integer equality"));
        System.err.println("Guard: " + guarded);
        List<Card> warmCards = new ArrayList<>();
        List<Card> warmUpdates = new ArrayList<>();
        for (int i = 0; i < 1000; i++) warmCards.add(new Card(i, false, 0));
        for (int i = 0; i < 60; i++) warmUpdates.add(new Card(i * 13, false, i));
        for (int i = 0; i < 2000; i++) {
            baseline(warmCards, warmUpdates, null);
            indexed(warmCards, warmUpdates, null);
        }
        System.out.println("cards,updates,sample,baseline_ns,indexed_ns");
        for (int count : new int[]{100, 1000, 5000}) {
            for (int batch : new int[]{1, 2, 8, 20, 60}) {
                List<Card> displayed = new ArrayList<>();
                List<Card> updates = new ArrayList<>();
                for (int i = 0; i < count; i++) displayed.add(new Card(i % 17 == 0 ? 7 : i, false, 0));
                for (int i = 0; i < batch; i++) updates.add(new Card(i % 4 == 0 ? count + i : (i * 31) % count, false, i));
                for (int warmup = 0; warmup < 10; warmup++) {
                    measure(false, displayed, updates);
                    measure(true, displayed, updates);
                }
                long[] before = new long[SAMPLES];
                long[] after = new long[SAMPLES];
                for (int sample = 0; sample < SAMPLES; sample++) {
                    if (sample % 2 == 0) {
                        before[sample] = measure(false, displayed, updates);
                        after[sample] = measure(true, displayed, updates);
                    } else {
                        after[sample] = measure(true, displayed, updates);
                        before[sample] = measure(false, displayed, updates);
                    }
                    System.out.printf("%d,%d,%d,%d,%d%n", count, batch, sample, before[sample], after[sample]);
                }
                Arrays.sort(before);
                Arrays.sort(after);
                System.err.printf("cards=%d updates=%d baseline_median=%d p95=%d indexed_median=%d p95=%d ratio=%.2fx%n",
                        count, batch, before[SAMPLES / 2], before[(int) Math.ceil(SAMPLES * .95) - 1],
                        after[SAMPLES / 2], after[(int) Math.ceil(SAMPLES * .95) - 1],
                        (double) before[SAMPLES / 2] / after[SAMPLES / 2]);
            }
        }
        System.err.println("checksum=" + checksum);
    }
}

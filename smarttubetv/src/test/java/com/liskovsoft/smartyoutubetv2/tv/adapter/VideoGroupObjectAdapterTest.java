package com.liskovsoft.smartyoutubetv2.tv.adapter;

import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class VideoGroupObjectAdapterTest {
    @Test
    public void batchSyncPreservesDuplicatesOrderAndUnmatchedCards() {
        Video first = video("same", 0);
        Video duplicate = video("same", 1);
        Video other = video("other", 2);
        VideoGroupObjectAdapter adapter = new VideoGroupObjectAdapter((Presenter) null);
        List<Video> displayed = new ArrayList<>(Arrays.asList(first, duplicate, other));
        for (int i = 0; i < 300; i++) {
            displayed.add(video("filler-" + i, 0));
        }
        adapter.add(VideoGroup.from(displayed));
        List<Integer> notifications = new ArrayList<>();
        adapter.registerObserver(new ObjectAdapter.DataObserver() {
            @Override public void onItemRangeChanged(int start, int count) {
                notifications.add(start);
            }
        });
        List<Video> updates = new ArrayList<>(Arrays.asList(video("same", 80), video("missing", 30), video("same", 90)));
        for (int i = 0; i < 20; i++) {
            updates.add(video("unmatched-" + i, 10));
        }
        adapter.sync(VideoGroup.from(updates));
        assertEquals(90, first.percentWatched, 0);
        assertEquals(90, duplicate.percentWatched, 0);
        assertEquals(2, other.percentWatched, 0);
        assertEquals(Arrays.asList(0, 1, 0, 1), notifications);
        adapter.sync(VideoGroup.from(Collections.singletonList(video("same", 40))));
        assertEquals(40, first.percentWatched, 0);
        assertEquals(40, duplicate.percentWatched, 0);
        assertEquals(Arrays.asList(0, 1, 0, 1, 0, 1), notifications);
        adapter.sync(VideoGroup.from(Collections.emptyList()));
        assertEquals(6, notifications.size());
    }

    private static Video video(String id, float watched) {
        Video video = new Video();
        video.videoId = id;
        video.percentWatched = watched;
        return video;
    }
}

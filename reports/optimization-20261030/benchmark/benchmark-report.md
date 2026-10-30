# Adapter synchronization benchmark

Task PERF-01, actor performance, receiver root. Base SHA `4b3f333e540a1403429d1a9d86dec758e59288f9`. This isolated worktree changes only benchmark evidence; the application implementation belongs to the main agent.

## Scope and method

The existing `VideoGroupObjectAdapter.sync` compares every update against every displayed card. The candidate builds a per-call `HashMap<Card,List<Integer>>`, preserving duplicate matches and notification order. A guarded variant retains the nested loop when updates < 20 or displayed cards < 256 to avoid allocation regressions in small workloads.

The standalone program models the app algorithm, rather than loading Android classes. The `video-hash` mode copies the exact eight-field varargs call shape of `Video.hashCode` and the body of `Helpers.hashCodeAny`; its `isMix` is a fixed boolean instead of app metadata inspection. The cheap integer-hash mode intentionally shows the counterexample where indexing costs more than comparisons.

Same machine: AMD Ryzen 9 3900X, 12 cores / 24 logical processors, Windows 10 Pro 10.0.19045, Microsoft OpenJDK 17.0.20.1+1-LTS 64-bit Server VM. No device or Android VM was used. Each process runs 2,000 global warmup pairs, 10 per-shape warmups of 50 repetitions, then 15 samples of 50 calls. Baseline/candidate sample order alternates. Raw samples are elapsed nanoseconds divided by 50. p95 is nearest rank (the maximum of 15 samples). No forced GC or CPU affinity; allocation/GC and system noise remain visible.

## Commands and results

From the repository root in PowerShell (all exited 0):

```powershell
$java = 'D:\cleanyourtv\work\toolchains\jdk17\jdk-17.0.20.1+1\bin\java.exe'
& $java reports/optimization-20261030/benchmark/AdapterSyncBenchmark.java > reports/optimization-20261030/benchmark/adapter-sync-raw.csv 2> reports/optimization-20261030/benchmark/adapter-sync-summary.txt
& $java reports/optimization-20261030/benchmark/AdapterSyncBenchmark.java video-hash > reports/optimization-20261030/benchmark/adapter-sync-video-hash-raw.csv 2> reports/optimization-20261030/benchmark/adapter-sync-video-hash-summary.txt
& $java reports/optimization-20261030/benchmark/AdapterSyncBenchmark.java video-hash guarded > reports/optimization-20261030/benchmark/adapter-sync-guarded-raw.csv 2> reports/optimization-20261030/benchmark/adapter-sync-guarded-summary.txt
```

PASS: both duplicate equal cards update; mixes stay distinct despite identical hashes; unmatched cards remain unchanged; repeated updates preserve final value and event order; empty/single batches and empty displays are accepted. These assertions exercise the unguarded indexed algorithm. Guarding only selects the original nested algorithm for small inputs; app-level regression tests must separately exercise the actual adapter.

Guarded faithful-hash model, median / p95 microseconds per synchronization:

| Cards | Updates | Baseline median / p95 | Guarded median / p95 | Median ratio |
| ---: | ---: | ---: | ---: | ---: |
| 100 | 1 | 0.410 / 0.684 | 0.424 / 0.622 | 0.97x |
| 100 | 2 | 0.800 / 0.804 | 0.820 / 0.850 | 0.98x |
| 100 | 8 | 3.092 / 3.202 | 3.102 / 3.564 | 1.00x |
| 100 | 20 | 7.714 / 8.018 | 7.718 / 7.908 | 1.00x |
| 100 | 60 | 23.168 / 23.294 | 23.224 / 23.940 | 1.00x |
| 1000 | 1 | 3.686 / 3.882 | 3.676 / 3.840 | 1.00x |
| 1000 | 2 | 7.522 / 9.078 | 7.340 / 8.162 | 1.02x |
| 1000 | 8 | 29.876 / 31.030 | 29.930 / 30.974 | 1.00x |
| 1000 | 20 | 76.258 / 78.954 | 38.548 / 75.932 | 1.98x |
| 1000 | 60 | 220.828 / 231.578 | 36.266 / 57.298 | 6.09x |
| 5000 | 1 | 18.926 / 19.404 | 18.938 / 19.692 | 1.00x |
| 5000 | 2 | 38.742 / 40.754 | 38.936 / 39.776 | 1.00x |
| 5000 | 8 | 154.580 / 161.680 | 153.634 / 159.806 | 1.01x |
| 5000 | 20 | 443.770 / 468.102 | 242.240 / 311.600 | 1.83x |
| 5000 | 60 | 1339.846 / 1682.762 | 254.654 / 308.514 | 5.26x |

Do not omit the failed initial hypothesis: indexing every batch larger than one is slower for small batches. The faithful-hash unguarded run at 1,000 cards / 2 updates regressed from 7.874 to 41.244 microseconds median (0.19x). Even at 20 updates, the cheap integer-equality model regressed at 1,000 and 5,000 cards. The proposed guard is supported for tested video-hash workloads, not a universal threshold for all devices or data.

NOT_RUN: Android ART timing, UI frame times, startup, playback, actual target-TV memory/GC. These modeled JVM speedups cannot be advertised as whole-app speedups. Cold compilation, network work and view notifications are outside the timed section. The source harness SHA256 is `E9E4E7AEDCF3CC0E6C1584B5991B37DD8D5A224A7B1EC63755E66753997F8704`.

## Resource inventory (read-only)

An in-memory Pillow lossless-WebP scan of all 361 tracked PNG files measured 15,696,113 original bytes and 9,399,812 encoded bytes: a potential 6,296,301-byte (40.1%) saving before changing references. This is not an APK measurement. No files were changed by the scan; hidden RGB in transparent pixels may differ unless `exact=True` is used and verified.

Largest useful documentation candidates: `images/screen.png` 1,650,517 → 988,564; `browse_home.png` 1,145,695 → 500,682; `browse_history.png` 1,463,695 → 937,422; `browse_subscriptions.png` 1,135,324 → 658,994; `video_related.png` 1,403,189 → 940,428; `browse.png` 1,102,322 → 706,738; `new/compatibility.png` 780,183 → 418,518; `browse_music.png` 880,136 → 521,266; `search.png` 724,350 → 447,098; `search_keyboard.png` 575,749 → 350,632; `video.png` 445,118 → 266,644. Update documentation references when changing extensions.

Packaged raster candidates: `drawable-xhdpi/grid_bg.png` 400,386 → 334,172; `mipmap-nodpi/app_logo_semi_red.png` 13,180 → 8,840; `app_logo.png` 13,026 → 8,770; `drawable/movie.png` 13,007 → 7,884; `app_logo_semi_grey.png` 12,544 → 7,590; `app_icon.png` 9,678 → 5,434; `app_icon_alt.png` 9,278 → 5,166; `app_banner.png` 7,766 → 4,720. Resource basenames can stay the same. Verify decoded pixels and supported Android API level.

Preserve ExoPlayer test fixtures and Fastlane/store PNG screenshots. Large media test fixtures are not APK payload. Do not remove locales or enable global shrinking without reflection/resource-name validation. Git history size does not decrease when replacing tracked files in a new commit.

Next owner: root copies this evidence into the main worktree, implements the guarded algorithm and actual adapter tests, verifies APK size, and records final application SHA. No commit or push was performed by this actor.

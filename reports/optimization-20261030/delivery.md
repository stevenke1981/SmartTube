# SmartTube optimization delivery

Base: `4b3f333e540a1403429d1a9d86dec758e59288f9`. Branch: `perf/lean-tv-experience`. Upstream remains yuliskov/SmartTube; authorized push destination is stevenke1981/SmartTube fork.

Changes:
- Retain the requested drama/music/kids/Shorts/news-live navigation and settings access to remaining sections; fix duplicate Music category on cold launcher entry.
- Index card positions only for >=20 updates and >=256 displayed cards; preserve direct scans for small batches, duplicate matches, notification order and final watched values.
- Re-encode 40 existing assets with identical decoded pixels, saving 4,859,271 source bytes; remove the unused 400,386-byte grid_bg resource. Total existing-asset reduction: 5,259,657 bytes, before adding the new banner and reports. Git history/build cache sizes are unaffected.
- Package a generated Codex cinematic SmartTube TV banner (320×180, 8,490 bytes). Details and exact prompt in appearance.md.
- Fix host-test environment: app-only Robolectric 4.11.1 ([release source](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.11.1)); exclude Android Conscrypt JNI only from UnitTestRuntimeClasspath. Initial failures were unsupported class version61, then conscrypt_jni; final test passes.

## Evidence

PASS: Windows / Microsoft JDK17.0.20.1 / Android SDK34. Command (exit0):

```powershell
$env:JAVA_HOME='D:\cleanyourtv\work\toolchains\jdk17\jdk-17.0.20.1+1'
$env:ANDROID_HOME='D:\cleanyourtv\work\toolchains\android-sdk'
.\gradlew.bat :smarttubetv:testStstableDebugUnitTest :smarttubetv:assembleStstableRelease --console=plain
```

BUILD SUCCESSFUL, 2m40s, 1204 tasks executed. Actual adapter regression: 1 test, 0 failures, 0 errors, 0 skipped. Baseline pre-asset-optimization release build also passed. APK hashes and exact byte counts are in apk-before.json/apk-after.json. ARM64 28,944,958→28,553,935 bytes; ARMv7 27,164,190→26,773,167; universal39,673,701→39,282,678. Packaged res compressed bytes1,832,219→1,441,011. The new banner's bytes were found unchanged inside every APK.

PASS: same-machine modeled JVM benchmark, raw samples and command in benchmark/benchmark-report.md. 1000 cards/60 updates median220.828→36.266µs. This model isolates matching work and is not actual Android adapter/TV timing. Small-update regressions in the initial unguarded idea are retained in the report.

PASS: independent scoped review, findings resolved (review.md). PASS: `git -c core.whitespace=cr-at-eol diff --check`. Existing CRLF file conventions preserved.

NOT_RUN: actual TV focus/navigation, startup, frame time, playback, Android ART timing, hosted CI, beta/F-Droid builds, full-project lint and unrelated test suites. `adb devices` returned no connected devices. Existing dependency deprecation/resource-format warnings remain. Release APKs are local build outputs, not a published/signed distribution or device-install acceptance.

The encoder script replays encoding from the original source tree; its JSON log is the measured original conversion, not a claim that rerunning on already optimized files saves the same bytes again.

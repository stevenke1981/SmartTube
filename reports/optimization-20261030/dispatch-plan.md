# SmartTube optimization delivery

Base: `4b3f333e5`; includes the previously requested five-category navigation changes.

Owner: root implements and integrates; PERF-01 investigates and benchmarks in isolated `D:/SmartTube-perf-evidence`; REVIEW-01 independently reviews the final patch.

Acceptance:
- Reduce checked-out asset bytes without dropping languages, codecs, test fixtures or platform support. Measure APK bytes on the same release variant before/after.
- Reduce batch card sync work while preserving duplicate updates, order and single-update behavior; run regression tests and a same-machine JVM microbenchmark.
- Generate a real launcher/banner asset with built-in Codex image generation, wire into Manifest, verify packaged resource.
- Build, review, commit and push to the user's fork; preserve upstream and unrelated `.sites-runtime` files.

Runtime TV FPS, startup and playback are NOT_RUN without a connected device. A microbenchmark is not TV latency evidence. No R8/reflection changes, ABI removal, locale removal, or test-fixture deletion are planned.

"""Reproduce asset encodings; each existing image is verified pixel-for-pixel."""
import io
import json
import pathlib
import subprocess
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[2]
REPORT = pathlib.Path(__file__).parent
changes = []
tracked = subprocess.check_output(["git", "ls-files", "*.png"], cwd=ROOT, text=True).splitlines()
for name in tracked:
    path = ROOT / name
    # Store uploads and codec fixtures retain their original file formats.
    if not (name.startswith("images/") or name.startswith("smarttubetv/src/")):
        continue
    original = Image.open(path)
    rgba = original.convert("RGBA").tobytes()
    encoded = io.BytesIO()
    docs = name.startswith("images/")
    target = path.with_suffix(".webp") if docs else path
    if docs:
        original.save(encoded, "WEBP", lossless=True, exact=True, method=6)
    else:
        original.save(encoded, "PNG", optimize=True, compress_level=9)
    data = encoded.getvalue()
    if len(data) >= path.stat().st_size:
        continue
    assert Image.open(io.BytesIO(data)).convert("RGBA").tobytes() == rgba, name
    changes.append({"old": name, "new": target.relative_to(ROOT).as_posix(),
                    "before": path.stat().st_size, "after": len(data), "rgba_equal": True})
    target.write_bytes(data)
    if docs:
        path.unlink()

for name in subprocess.check_output(["git", "ls-files", "*.md", "*.html", "*.yml", "*.xml"], cwd=ROOT, text=True).splitlines():
    path = ROOT / name
    raw = path.read_bytes()
    updated = raw
    for item in changes:
        if item["old"] != item["new"]:
            updated = updated.replace(item["old"].encode(), item["new"].encode())
    if updated != raw:
        path.write_bytes(updated)

(REPORT / "asset-savings.json").write_text(json.dumps(changes, indent=2), encoding="utf-8")
print(json.dumps({"files": len(changes), "saved_bytes": sum(c["before"]-c["after"] for c in changes)}))

#!/usr/bin/env python3
"""Export only the Emissary hitbox locator tracks from the read-only source pack."""

from __future__ import annotations

import gzip
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / "_nyctocosm_source" / "datapack" / "data"
ANIMATIONS = SOURCE / "animated_java" / "functions" / "ajemissary" / "zzzzzzzz" / "animations"
HITBOX_FUNCTIONS = SOURCE / "emissary" / "functions" / "ai" / "hitbox"
OUTPUT = ROOT / "src" / "main" / "resources" / "assets" / "nyctocosm" / "native_rigs" / "emissary_damage_locators.json.gz"

LOCATORS = (
    "center", "leftmiddlewingtip0", "rightmiddlewingtip0",
    "rightupperwingtip1", "leftupperwingtip1", "rightmiddlewingtip1",
    "leftmiddlewingtip1", "rightlowerwingtip1", "leftlowerwingtip1",
    "righthand", "lefthand", "rightmiddlewingtip2", "rightupperwingtip2",
    "leftmiddlewingtip2", "leftupperwingtip2", "rightmiddlewingtip3", "leftupperwingtip3", "rightupperwingtip3",
)
NUMBER = r"[+-]?(?:\d+(?:\.\d*)?|\.\d*)"
COORDINATE = rf"(?:\^(?:{NUMBER})?|~(?:{NUMBER})?)"
PATTERN = re.compile(
    rf"execute if entity @s\[tag=aj\.ajemissary\.locator_origin\."
    rf"(?P<locator>{'|'.join(LOCATORS)})\] at @s on origin run tp @s "
    rf"(?P<x>{COORDINATE}) (?P<y>{COORDINATE}) (?P<z>{COORDINATE}) "
    rf"(?P<yaw>{COORDINATE}) (?P<pitch>{COORDINATE})"
)


def value(token: str) -> float:
    return round(float(token[1:] or "0"), 8)


def main() -> None:
    if not ANIMATIONS.is_dir() or not HITBOX_FUNCTIONS.is_dir():
        raise SystemExit(f"Missing source animation or hitbox functions under {SOURCE}")

    animation_data: dict[str, dict[str, object]] = {}
    source_files: set[Path] = set(HITBOX_FUNCTIONS.rglob("*.mcfunction"))
    for folder in sorted(path for path in ANIMATIONS.iterdir() if path.is_dir()):
        play = folder / "play_as_root.mcfunction"
        tick = folder / "tick_animation.mcfunction"
        if not play.is_file() or not tick.is_file():
            continue
        loop_match = re.search(r"loop_mode (\d+)", play.read_text(encoding="utf-8-sig"))
        duration_match = re.search(r"matches (\d+)\.\.", tick.read_text(encoding="utf-8-sig"))
        if not loop_match or not duration_match:
            continue

        frames: list[dict[str, object]] = []
        leaves = sorted(folder.rglob("leaf_*_as_bone.mcfunction"), key=lambda p: int(re.search(r"leaf_(\d+)", p.name)[1]))
        for leaf in leaves:
            source_files.add(leaf)
            frame_number = int(re.search(r"leaf_(\d+)", leaf.name)[1])
            locators: dict[str, list[float]] = {}
            for line in leaf.read_text(encoding="utf-8-sig").splitlines():
                match = PATTERN.search(line)
                if match:
                    locators[match["locator"]] = [value(match[key]) for key in ("x", "y", "z", "yaw", "pitch")]
            if not locators:
                continue
            if set(locators) != set(LOCATORS):
                raise SystemExit(f"Incomplete damage locators in {leaf}: {sorted(locators)}")
            frames.append({"tick": frame_number, "locators": locators})

        if not frames:
            continue
        ticks = [frame["tick"] for frame in frames]
        if ticks != list(range(ticks[0], ticks[-1] + 1)) or ticks[0] != 0:
            raise SystemExit(f"Non-contiguous Emissary animation frames in {folder.name}: {ticks[:3]}..{ticks[-3:]}")
        animation_data[folder.name] = {
            "duration": int(duration_match[1]),
            "loop": int(loop_match[1]) == 0,
            "frames": frames,
        }

    if len(animation_data) < 40:
        raise SystemExit(f"Expected the complete Emissary animation set, found {len(animation_data)} clips")

    digest = hashlib.sha256()
    normalized_files = sorted(source_files, key=lambda path: path.as_posix().lower())
    for path in normalized_files:
        relative = path.relative_to(SOURCE).as_posix().encode("utf-8")
        digest.update(relative + b"\0" + path.read_bytes() + b"\0")

    payload = {
        "schema": 1,
        "sourceSha256": digest.hexdigest().upper(),
        "sourceFileCount": len(normalized_files),
        "locators": list(LOCATORS),
        "animations": animation_data,
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    encoded = json.dumps(payload, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    with OUTPUT.open("wb") as raw:
        with gzip.GzipFile(filename="", fileobj=raw, mode="wb", mtime=0) as compressed:
            compressed.write(encoded)

    print(
        f"Exported {len(animation_data)} animations, "
        f"{sum(len(item['frames']) for item in animation_data.values())} frames, "
        f"{len(normalized_files)} source files; sha256={payload['sourceSha256']}"
    )
    print(f"Wrote {OUTPUT.relative_to(ROOT)} ({OUTPUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()

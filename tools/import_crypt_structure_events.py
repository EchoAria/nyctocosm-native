"""Convert only the two audited crypt/Slatehenge marker entities."""
from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / "_nyctocosm_source" / "datapack" / "data"
sys.path.insert(0, str(Path(__file__).resolve().parent))
from nbt_assets import sanitize  # noqa: E402

STRUCTURES = (
    Path("structures/structures/crypt/hallways/generic/end.nbt"),
    Path("nyctocosm/structures/slatehenge.nbt"),
)

def main():
    report_path = ROOT / "docs/structure-import.json"
    report = json.loads(report_path.read_text(encoding="utf-8"))
    results = {}
    for relative in STRUCTURES:
        source = SOURCE / relative
        destination = ROOT / "src/main/resources/data" / relative
        result = sanitize(source, destination)
        if sum(result.get("converted_entities", {}).values()) != 1:
            raise SystemExit(f"Expected exactly one audited native event in {source}: {result}")
        report_key = next((key for key in report if key.replace("\\", "/") == relative.as_posix()), None)
        if report_key is None:
            raise SystemExit(f"Missing existing import report entry for {relative}")
        report[report_key] = result
        results[relative.as_posix()] = result
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(results, ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""List curated BIOQRA challenges. Uses only Python's standard library; no network."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "data" / "challenges.json"
LIVE_CATALOG = ROOT / "data" / "live_challenges.json"

def filter_entries(entries, source=None, kind=None, search=None):
    return [
        item for item in entries
        if (not source or item["source"].casefold() == source.casefold())
        and (not kind or item["kind"] == kind)
        and (not search or search.casefold() in
             (item["title"] + " " + item["category"]).casefold())
    ]

def main():
    parser = argparse.ArgumentParser(description="Explore curated BIOQRA challenges offline.")
    parser.add_argument("--source", choices=["Rosalind", "Open Problems"])
    parser.add_argument("--kind", choices=["educational", "benchmark"])
    parser.add_argument("--search", help="Search challenge titles and categories")
    parser.add_argument("--live", action="store_true", help="Use latest downloaded Rosalind cache")
    parser.add_argument("--json", action="store_true", help="Print JSON instead of a table")
    args = parser.parse_args()
    path = LIVE_CATALOG if args.live else CATALOG
    if not path.exists():
        parser.error("No live cache found; first run: python tools/refresh_challenges.py")
    catalog = json.loads(path.read_text(encoding="utf-8"))
    entries = filter_entries(catalog["entries"], args.source, args.kind, args.search)
    if args.json:
        print(json.dumps(entries, indent=2, ensure_ascii=False))
    else:
        updated = catalog.get("fetched_at", catalog.get("reviewed_on", "unknown"))
        print(f"BIOQRA catalog: {len(entries)} results (updated {updated})")
        for challenge in entries:
            print(f"{challenge['source']:<14} {challenge['id']:<43} {challenge['title']}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

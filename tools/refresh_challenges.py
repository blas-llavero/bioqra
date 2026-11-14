#!/usr/bin/env python3
"""Refresh all five official Rosalind tracks; retain previous cache on failures.

Public challenge data only. Standard-library networking; no AI key or patient data.
"""
import argparse
import json
import re
import sys
import tempfile
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.error import URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
SOURCE_URL = "https://rosalind.info/problems/list-view/"
CACHE_PATH = ROOT / "data" / "live_challenges.json"
MAX_BYTES = 2_000_000
TRACKS = {
    "bioinformatics-stronghold": None,
    "python-village": "python-village",
    "bioinformatics-armory": "bioinformatics-armory",
    "bioinformatics-textbook-track": "bioinformatics-textbook-track",
    "algorithmic-heights": "algorithmic-heights",
}
PROBLEM_RE = re.compile(r"^/problems/([a-z0-9]+)/?$")
BLOCKED_PATHS = {"list-view", "tree-view", "topics", "locations"}

class RosalindParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.problem_id = None
        self.link_text = []
        self.entries = {}

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            href = dict(attrs).get("href", "")
            match = PROBLEM_RE.fullmatch(href)
            self.problem_id = match.group(1) if match and match.group(1) not in BLOCKED_PATHS else None
            self.link_text = []

    def handle_data(self, data):
        if self.problem_id is not None:
            self.link_text.append(data)

    def handle_endtag(self, tag):
        if tag == "a" and self.problem_id is not None:
            title = " ".join(" ".join(self.link_text).split())
            if 4 <= len(title) <= 160 and any(c.isalpha() for c in title):
                pid = self.problem_id
                self.entries[pid] = {
                    "id": "rosalind-" + pid,
                    "title": title,
                    "source": "Rosalind",
                    "source_url": "https://rosalind.info/problems/" + pid + "/",
                    "category": "unclassified",
                    "difficulty": "unknown",
                    "kind": "educational",
                    "status": "not_attempted",
                }
            self.problem_id = None
            self.link_text = []

def parse_rosalind(html, minimum=50):
    parser = RosalindParser()
    parser.feed(html)
    entries = sorted(parser.entries.values(), key=lambda x: x["id"])
    if len(entries) < minimum:
        raise ValueError("Only %d problems parsed (minimum %d)" % (len(entries), minimum))
    return entries

def fetch_index(location=None):
    url = SOURCE_URL + ("?location=" + location if location else "")
    request = Request(url, headers={"User-Agent": "BIOQRA/0.2 (public scientific challenge discovery)"})
    with urlopen(request, timeout=20) as response:
        final = urlparse(response.geturl())
        if final.scheme != "https" or final.hostname != "rosalind.info" or final.path.rstrip("/") != "/problems/list-view":
            raise ValueError("Unexpected redirect away from official Rosalind index")
        content = response.read(MAX_BYTES + 1)
    if len(content) > MAX_BYTES:
        raise ValueError("Official index exceeds safety size limit")
    return content.decode("utf-8", errors="replace")

def refresh(cache_path=CACHE_PATH, fetcher=fetch_index):
    by_id = {}
    counts = {}
    title_conflicts = []
    for name, location in TRACKS.items():
        entries = parse_rosalind(fetcher(location), minimum=1)
        counts[name] = len(entries)
        for entry in entries:
            if entry["id"] in by_id:
                if by_id[entry["id"]]["title"] != entry["title"]:
                    # The same official problem may be labelled differently across tracks.
                    # Keep the first label; retain evidence without duplicating the problem.
                    title_conflicts.append({"id": entry["id"], "track": name,
                                            "alternate_title": entry["title"]})
                continue
            entry["track"] = name
            by_id[entry["id"]] = entry
    if len(by_id) < 200:
        raise ValueError("Only %d unique problems across tracks; keeping previous cache" % len(by_id))
    result = {
        "schema_version": 2,
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "source_url": SOURCE_URL,
        "status": "verified_source_fetch",
        "track_counts": counts,
        "alternate_title_count": len(title_conflicts),
        "alternate_titles": title_conflicts,
        "entries": sorted(by_id.values(), key=lambda x: x["id"]),
    }
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=cache_path.parent, delete=False) as handle:
        tmp_path = Path(handle.name)
        json.dump(result, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    tmp_path.replace(cache_path)
    return result

def main():
    parser = argparse.ArgumentParser(description="Refresh all five official Rosalind tracks")
    parser.add_argument("--cache", type=Path, default=CACHE_PATH)
    args = parser.parse_args()
    try:
        result = refresh(args.cache)
        print("Updated %d unique Rosalind challenges" % len(result["entries"]))
        print("Alternate labels for duplicated problem IDs: %d" % result["alternate_title_count"])
        for track, count in result["track_counts"].items():
            print("  %s: %d" % (track, count))
    except (URLError, TimeoutError, OSError, ValueError) as exc:
        print("Refresh unavailable; existing cache retained: %s" % exc, file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

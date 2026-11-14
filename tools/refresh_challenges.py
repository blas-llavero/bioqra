#!/usr/bin/env python3
"""Refresh a public BIOQRA challenge cache from an official source.

No patient data, AI API, credentials, third-party dependencies, or scraping
of arbitrary URLs. Only the explicit trusted official Rosalind index is fetched.
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
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
SOURCE_URL = "https://rosalind.info/problems/list-view/"
CACHE_PATH = ROOT / "data" / "live_challenges.json"
MAX_BYTES = 2_000_000
PROBLEM_RE = re.compile(r"^/problems/([a-z0-9]+)/?$")
BLOCKED_PATHS = {"list-view", "tree-view", "topics", "locations", "bioinformatics-stronghold"}

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
        if self.problem_id:
            self.link_text.append(data)

    def handle_endtag(self, tag):
        if tag == "a" and self.problem_id:
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

def parse_rosalind(html):
    parser = RosalindParser()
    parser.feed(html)
    entries = sorted(parser.entries.values(), key=lambda x: x["id"])
    # A drastic drop often indicates a site redesign or access error.
    if len(entries) < 50:
        raise ValueError("Only %d problems parsed; refusing to overwrite cache" % len(entries))
    return entries

def fetch_index():
    request = Request(SOURCE_URL, headers={"User-Agent": "BIOQRA/0.1 (research challenge catalog)"})
    with urlopen(request, timeout=15) as response:
        if response.geturl().split("?")[0].rstrip("/") != SOURCE_URL.rstrip("/"):
            raise ValueError("Unexpected redirect away from trusted source")
        content = response.read(MAX_BYTES + 1)
    if len(content) > MAX_BYTES:
        raise ValueError("Official index exceeds safety size limit")
    return content.decode("utf-8", errors="replace")

def refresh(cache_path=CACHE_PATH, fetcher=fetch_index):
    entries = parse_rosalind(fetcher())
    result = {
        "schema_version": 1,
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "source_url": SOURCE_URL,
        "status": "verified_source_fetch",
        "entries": entries,
    }
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    # Never leave a partially written cache.
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=cache_path.parent,
                                     delete=False) as handle:
        tmp_path = Path(handle.name)
        json.dump(result, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    tmp_path.replace(cache_path)
    return result

def main():
    parser = argparse.ArgumentParser(description="Fetch the official Rosalind catalog and cache it")
    parser.add_argument("--cache", type=Path, default=CACHE_PATH, help="Local JSON cache path")
    args = parser.parse_args()
    try:
        result = refresh(args.cache)
        print("Updated %d Rosalind challenges at %s" % (len(result["entries"]), result["fetched_at"]))
    except (URLError, TimeoutError, OSError, ValueError) as exc:
        print("Refresh unavailable; existing cache retained: %s" % exc, file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

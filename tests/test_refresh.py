import json
import tempfile
import unittest
from pathlib import Path
from tools.refresh_challenges import TRACKS, parse_rosalind, refresh

def sample(start=0, n=55):
    return "<html><body>" + "".join(
        '<a href="/problems/p%03d/">Problem number %03d</a>' % (i, i)
        for i in range(start, start+n)
    ) + "</body></html>"

def fake_fetch(location=None):
    positions = list(TRACKS.values())
    return sample(positions.index(location) * 55)

class RefreshTests(unittest.TestCase):
    def test_extracts_problem_links_and_deduplicates(self):
        entries = parse_rosalind(sample())
        self.assertEqual(len(entries), 55)
        self.assertEqual(entries[0]["id"], "rosalind-p000")
        self.assertEqual(entries[0]["status"], "not_attempted")

    def test_empty_or_changed_index_rejected(self):
        with self.assertRaises(ValueError):
            parse_rosalind("<html><body>blocked</body></html>")

    def test_success_writes_cache(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "catalog.json"
            result = refresh(path, fetcher=fake_fetch)
            self.assertEqual(len(result["entries"]), 275)
            self.assertEqual(len(result["track_counts"]), 5)
            self.assertEqual(len(json.loads(path.read_text())["entries"]), 275)

    def test_failed_fetch_does_not_replace_cache(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "catalog.json"
            path.write_text('{"saved": true}')
            with self.assertRaises(ValueError):
                refresh(path, fetcher=lambda location: "<html>broken</html>")
            self.assertEqual(path.read_text(), '{"saved": true}')

    def test_missing_track_preserves_cache(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "catalog.json"
            path.write_text('{"previous": true}')
            def failed_last(location=None):
                if location == "algorithmic-heights":
                    return "<html>error</html>"
                return fake_fetch(location)
            with self.assertRaises(ValueError):
                refresh(path, fetcher=failed_last)
            self.assertEqual(path.read_text(), '{"previous": true}')

if __name__ == "__main__":
    unittest.main()

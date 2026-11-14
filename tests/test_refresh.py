import json
import tempfile
import unittest
from pathlib import Path
from tools.refresh_challenges import parse_rosalind, refresh

def sample(n=55):
    return "<html><body>" + "".join(
        '<a href="/problems/p%03d/">Problem number %03d</a>' % (i, i)
        for i in range(n)
    ) + "</body></html>"

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
            result = refresh(path, fetcher=lambda: sample())
            self.assertEqual(len(result["entries"]), 55)
            self.assertEqual(len(json.loads(path.read_text())["entries"]), 55)

    def test_failed_fetch_does_not_replace_cache(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "catalog.json"
            path.write_text('{"saved": true}')
            with self.assertRaises(ValueError):
                refresh(path, fetcher=lambda: "<html>broken</html>")
            self.assertEqual(path.read_text(), '{"saved": true}')

if __name__ == "__main__":
    unittest.main()

# BIOQRA PDF preview — security design

The **View PDF** button is shown only after a successful manual or automatic export and successful read-access verification. PDF rendering takes place within the challenge screen using the Android framework **PdfRenderer**; no network service or proprietary PDF viewer is required.

## Authorization and data boundaries

- Android 10+: the URI must identify a numeric item from the primary-volume MediaStore Downloads collection, whose name, MIME type, relative path `Download/Bioqra/YYMMDD-Project/`, size, and **owner package** match the saved BIOQRA result.
- Optional user-selected SAF folder: a currently persisted **read grant** for the exact document tree is required. The viewer traverses `Bioqra/YYMMDD-Project/Project.pdf` using DocumentsContract, comparing the final document ID to the requested URI and validating document metadata.
- Android 8–9: only the exact canonical app-specific external Documents file is allowed, under `Bioqra/YYMMDD-Project/`. File URIs are rejected on Android 10+.
- The file must be 5 bytes–10 MB and begin with the PDF signature. A fresh read-only descriptor is opened for each render. Parsing and rendering is performed by Android PdfRenderer.
- Renderer accepts up to 150 pages and renders at most two million pixels for an individual page, off the main UI thread. Previous bitmap resources and the descriptor are released. Navigation cannot access pages outside the document.
- The PDF URI is kept only in activity memory after saving; it is not taken from an arbitrary external intent or inferred from an untrusted path.

## Tests and limitations

`SecurePdfAccessTest` checks exact filename matching, incorrect extensions and traversal-like names. Android GitHub Actions verifies that the code compiles and runs unit tests.

**Device verification remains necessary:** test MediaStore owner metadata, SAF read-grant expiry, providers returning non-seekable descriptors, unusual PDFs, large page sizes, low-memory tablets, UI lifecycle dismissal and document replacement between validation and rendering. The checks reduce URI substitution risks but cannot eliminate malformed-PDF renderer vulnerabilities or TOCTOU changes to mutable files. The PDF viewer does not execute scripts or external links.

PDF preview changes are independent of the scientific results and do not establish medical or biological validity.

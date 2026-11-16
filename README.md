# BIOQRA — Open Bioinformatics Research & Patient Assistance

![BIOQRA — Official full logo](docs/images/bioqra_full_logo.png)

BIOQRA is an open-source project for Android and the web. It aims to help researchers discover bioinformatics challenges, develop reproducible computational approaches, and verify results with scientific software. A separate, privacy-first patient research module is planned to help people document their experiences, securely manage health records, and optionally contribute de-identified information to ethically reviewed research. BIOQRA is not a diagnostic, treatment, vaccine-testing, or medication-safety determination tool.

## Android APK: download, install, and use

**Development status: an experimental Android source build is now configured.** Debug APKs are produced by GitHub Actions when the build succeeds; there is not yet a reviewed official GitHub Release. Check [Android Debug APK Actions](https://github.com/blas-llavero/bioqra/actions/workflows/android.yml).

To install the current experimental build on Android **if GitHub Actions shows a successful build**:

1. Open [Android Debug APK Actions](https://github.com/blas-llavero/bioqra/actions/workflows/android.yml) in your Android browser while signed into GitHub.
2. Open the latest **successful** run of **Android Debug APK**.
3. At the bottom under **Artifacts**, download **bioqra-debug-apk** (a ZIP archive). Extract the ZIP with Android's Files app.
4. Locate **app-debug.apk** in the extracted files. Open it. If prompted, allow installation from that source for this operation; revoke the permission afterward.
5. Launch **BIOQRA**. The home screen places **Cancer Research** top left and **Refresh** top right. **Challenges** now includes a source selector for Rosalind, Open Problems, Cancer Grand Challenges, DREAM, hackathons and ML Contests. **Hackathons** and **Cancer Research** contain curated, sourced listings with clear participation labels; they are not automatically certified as open competitions. **Help** explains the pilot. **App Description** opens an in-app video player for the approved English introduction. If the MP4 is not bundled with the APK, select **Select approved English video (MP4)** and choose the approved file from the Android Files app; the selected file is read locally and its access is remembered. **How to Use** still shows a placeholder until its separate tutorial is reviewed.
6. On launch the app attempts to update the five official Rosalind tracks and checks the Open Problems benchmark index. If Open Problems cannot be verified, a reviewed directory remains visible; Rosalind retains its own local cache or a six-item starter list. No medical or patient records are collected.

The debug APK is unsigned for store distribution and signed with a development key by Android tooling; it is only for testing and is not a vetted production release. The approved English introduction is not yet bundled into the GitHub build; the player displays a clear message if the file is absent. The How to Use tutorial has not yet been approved. Do not download APKs from unofficial mirrors.

## Author and purpose

**Conceived and programmed by Blas Llavero.** More information: https://blasllavero.com

BIOQRA is intended as open-source software that everyone can study, modify, and use for lawful, charitable and public-benefit purposes, subject to the project's eventual license and applicable medical-data protections. The project aims to support equitable access to scientific knowledge, health research and technology across countries. It does not promise cures, clinical benefits, or access to particular vaccines or medicines.

## Planned capabilities

- Find bioinformatics challenges from established public research and educational platforms.
- Run transparent, reproducible analysis pipelines using open-source tools; distinguish computational validation from experimental or clinical validation.
- Prioritize local and self-hosted processing to minimize use of paid AI services.
- Provide an Android interface and web application.
- Support ten interface languages, including English, Catalan and Spanish, selected from device language where supported.
- Respect Android gesture/navigation bars and system insets.
- Provide accessible descriptions, screen-reader support, and optional natural-sounding open-source narration.
- Include **About** and **Help** videos, each up to 30 seconds per supported language, **only after the project owner has reviewed and approved every image, video and audio asset**.
- Develop the patient module with explicit informed consent, granular sharing controls, data minimization, encryption, deletion/export options, access controls and appropriate GDPR compliance.
- Avoid interpreting patient anecdotes as proof of causation, treatment efficacy or vaccine safety. Encourage formal reporting of suspected adverse reactions to healthcare professionals and competent pharmacovigilance authorities.

**Media approval rule:** Do not commit or upload images, video or audio files to this repository until Blas Llavero has explicitly reviewed and approved them.

## Approved introductory video (English)

The English 30-second Piper-narrated introduction has been reviewed and approved by the project owner. The Android player code is implemented in `VideoActivity.java`; the actual MP4 must be placed in:

`app/src/main/res/raw/bioqra_app_description_en.mp4`

Android packaging uses the resource name without the file extension (`R.raw.bioqra_app_description_en`). Use exactly that filename (lowercase letters and underscores) and create the `raw` directory if necessary. **The MP4 is not yet committed to this repository**; do not assume GitHub Actions artifacts include it. Until it is added, the Android video player provides a local MP4 document picker and remembers read access on compatible Android versions. The file remains on the user's device; BIOQRA does not upload the video. When it is added, subsequent APK builds will contain the offline video. The distinct `How to Use` video will use real screenshots after that tutorial has been approved.

## Live discovery across official biomedical challenge pages

BIOQRA 0.3 adds **Grand Challenge index discovery** on app startup and when the user presses **Refresh**. It scans each page of the official [medical-image analysis challenge index](https://grand-challenge.org/challenges/), not only a short manually written list. The index contains hundreds of historical challenges, but BIOQRA only treats a listing as open/upcoming if the index explicitly states **Accepting submissions** or **Opening submissions**, and rejects known expired deadlines. It keeps an offline cache after a successful scan. The scan may fail if the site's HTML changes, access is blocked or the network is unavailable; in this case the previous entries remain and their verification dates expire rather than silently claiming availability.

**Scope and limitations:** Neither the internet nor Grand Challenge offers a universal registry of every bioinformatics competition worldwide. This iteration significantly broadens discovery through Grand Challenge and keeps the Rosalind and Open Problems integrations, but it does **not** guarantee worldwide exhaustive coverage, automatically verify eligibility, or guarantee that the submission page remains open. Some events allow ongoing post-challenge algorithm submissions rather than new competition entries; always confirm with the organizer. Further official-platform adapters (DREAM, CodaBench and other registries) are planned. No ChatGPT API, AI credits or patient information is required for this public index scan.

## Bundling the approved App Description video

The owner-approved **30-second English Piper video** can be packaged directly into the Android APK by copying the exact approved `bioqra_with_piper_english_v3.mp4` to `app/src/main/res/raw/bioqra_app_description_en.mp4`, then committing that file. The in-app player automatically chooses this bundled resource and plays it offline when **App Description** is tapped. The video is **not yet in GitHub** until this binary commit has been made. The local-video picker remains available for experimental builds without the asset.

## Competition status and complete pagination (November 2026)

The **Challenges** screen now defaults to **Open / Upcoming** and prioritizes competitions or funding calls with a concrete research task. Use the separate **source** and **availability** selectors to switch between:

- **Open / Upcoming**: verified research competitions and upcoming submission phases. Each status is based on an official source review dated 16 November 2026. Seven days after that date, records without an authoritative live status check are automatically downgraded to **Needs verification** rather than falsely marked open.
- **Research benchmarks**: Open Problems benchmarks for reproducible research, not time-limited contests.
- **Educational exercises**: Rosalind exercises.
- **Needs verification**: directories, uncertain or stale opportunities.
- **All records**: every entry, including funded research, historical events and restricted competitions. These entries are clearly labeled and are **not** presented as open.

Use **Previous** and **Next** below the list. The interface displays 25 records per page, with a correct result count and page number; there is no 100-result truncation. Searches and source/filter changes reset to page one.

Examples of actual research competitions include OWL (prostate cancer recurrence explanation), Light My Cells 2 (fluorescence microscopy prediction), COBRA (radiotherapy CT reconstruction; validation begins 1 December 2026), and TopBrain V2 (brain vessel segmentation; final-test phase opens 11 December 2026). Lung Nodule Volumetry is restricted to vendors with CE-certified products and is **not** included in the general open-competition view. Grand Challenge organizers must be consulted for eligibility and submission rules.

**Important:** As of this milestone, live automatic refresh covers Rosalind and checks the Open Problems index; the other competitions use a dated official-source review and conservative staleness logic, not an automated live registration check. The application cannot guarantee a place remains open after the review date.

## Multi-source challenge directory (Android prototype)

The Android challenge source filter now includes:

- **Rosalind:** 284 educational exercises, automatically refreshed from five official tracks.
- **Open Problems:** 14 single-cell computational benchmarks. BIOQRA checks the official index on opening the app, falling back to the reviewed directory if the page format changes or connectivity fails. These benchmarks have scientific evaluation metrics; a benchmark is not a treatment.
- **Cancer Grand Challenges:** research topics and selected funding calls, including gastroesophageal cancer prevention research. The **24 November 2026** application deadline is displayed with a status derived from the device date; submissions are intended for eligible international research teams, not necessarily individuals.
- **DREAM:** a link to official open biomedical competitions; each challenge's eligibility and dates must be checked with the organizer.
- **Hackathons:** organizer pages for BioHackathon Europe and GENEDIS. Directory listings may refer to concluded events; do not assume registration is open.
- **ML Contests:** a competition aggregator. BIOQRA has not individually verified its third-party listings.

This is a curated public research directory, not an autonomous across-the-web crawler. The Open Problems source is checked dynamically; the other non-Rosalind sources still require periodic editorial review. The built-in list stores no patient information. Scientific solutions and their validation remain future milestones.

## First functionality: offline challenge catalog

The initial catalog contains 12 selected Rosalind exercises and 14 Open Problems benchmarks. It can be searched offline without AI calls or paid APIs. See [catalog instructions](docs/CHALLENGE_CATALOG.md). No challenge has been solved or validated by BIOQRA yet.

## Challenge catalog refresh (experimental)

BIOQRA includes a Python refresh utility for the five official Rosalind problem tracks. The original one-track downloader retrieved only part of the public catalog; the revised synchronizer fetches and merges all five sections. It downloads public problem titles and URLs from one trusted source, validates a minimum count, and replaces the local JSON cache atomically; on a failed refresh the previously saved cache is preserved. It uses no ChatGPT API and does not send patient data. Other sources (hackathons, DREAM, Open Problems, oncology) are not yet synchronized automatically. **The Android app is buildable and attempts Rosalind and Open Problems refresh on launch; other competition sources still require a dedicated live-status synchronizer.**

Run after cloning:

```bash
python tools/refresh_challenges.py
python tools/list_challenges.py --live
python tools/list_challenges.py --live --search cancer
python -m unittest discover -s tests -v
```

The refresh command requires Internet access, while the resulting JSON can be read offline using --live. The catalog count is not guaranteed to equal the website's displayed total if the site changes. A refresh is rejected if any track is empty or fewer than 200 unique problems are gathered. An initial cache is created only after a successful fetch.

## Current status

Project setup / requirements definition. The Android prototype and limited challenge index integration are implemented and buildable. The full website, automatic competition status sync, scientific solver, and patient module are not yet implemented.

## Build from source

The native Android prototype uses Java 17, Android Gradle Plugin 8.9.2, SDK 35 and Gradle 8.11.1. Build it with a compatible Android SDK and Gradle installation:

```bash
gradle :app:assembleDebug
```

Expected debug APK output (after a successful build): `app/build/outputs/apk/debug/app-debug.apk`.

The Python catalog utility can also be run from a local clone (requires Python 3.10+):

```bash
git clone https://github.com/blas-llavero/bioqra.git
cd bioqra
python tools/list_challenges.py
python tools/list_challenges.py --source Rosalind
python tools/list_challenges.py --search DNA
python -m unittest discover -s tests -v
```

These Python catalog commands are offline after cloning and use only the Python standard library. The refresh script requires Internet access. The website, clinical patient module, scientific solving engine, localized UI, reviewed logo and videos remain future milestones.

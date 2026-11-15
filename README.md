# BIOQRA — Open Bioinformatics Research & Patient Assistance

BIOQRA is an open-source project for Android and the web. It aims to help researchers discover bioinformatics challenges, develop reproducible computational approaches, and verify results with scientific software. A separate, privacy-first patient research module is planned to help people document their experiences, securely manage health records, and optionally contribute de-identified information to ethically reviewed research. BIOQRA is not a diagnostic, treatment, vaccine-testing, or medication-safety determination tool.

## Android APK: download, install, and use

**Development status: an experimental Android source build is now configured.** Debug APKs are produced by GitHub Actions when the build succeeds; there is not yet a reviewed official GitHub Release. Check [Android Debug APK Actions](https://github.com/blas-llavero/bioqra/actions/workflows/android.yml).

To install the current experimental build on Android **if GitHub Actions shows a successful build**:

1. Open [Android Debug APK Actions](https://github.com/blas-llavero/bioqra/actions/workflows/android.yml) in your Android browser while signed into GitHub.
2. Open the latest **successful** run of **Android Debug APK**.
3. At the bottom under **Artifacts**, download **bioqra-debug-apk** (a ZIP archive). Extract the ZIP with Android's Files app.
4. Locate **app-debug.apk** in the extracted files. Open it. If prompted, allow installation from that source for this operation; revoke the permission afterward.
5. Launch **BIOQRA**. The home screen places **Cancer Research** top left and **Refresh** top right. **Challenges** searches the Rosalind index; **Hackathons** and **Cancer Research** currently provide clearly identified official links, not automated event feeds. **Help** explains the pilot. **App Description** opens an in-app video player for the approved English introduction once the approved MP4 is bundled with the APK. **How to Use** still shows a placeholder until its separate tutorial is reviewed.
6. On launch the app attempts to update the five official Rosalind tracks; without connectivity it uses its locally saved catalog or a six-item starter list. No medical or patient records are collected.

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

Android packaging uses the resource name without the file extension (`R.raw.bioqra_app_description_en`). Use exactly that filename (lowercase letters and underscores) and create the `raw` directory if necessary. **The MP4 is not yet committed to this repository**; do not assume GitHub Actions artifacts include it. When it is added, subsequent APK builds will contain the offline video. The distinct `How to Use` video will use real screenshots after that tutorial has been approved.

## First functionality: offline challenge catalog

The initial catalog contains 12 selected Rosalind exercises and 14 Open Problems benchmarks. It can be searched offline without AI calls or paid APIs. See [catalog instructions](docs/CHALLENGE_CATALOG.md). No challenge has been solved or validated by BIOQRA yet.

## Challenge catalog refresh (experimental)

BIOQRA includes a Python refresh utility for the five official Rosalind problem tracks. The original one-track downloader retrieved only part of the public catalog; the revised synchronizer fetches and merges all five sections. It downloads public problem titles and URLs from one trusted source, validates a minimum count, and replaces the local JSON cache atomically; on a failed refresh the previously saved cache is preserved. It uses no ChatGPT API and does not send patient data. Other sources (hackathons, DREAM, Open Problems, oncology) are not yet synchronized automatically. **The Android app has not been built; automatic refresh on app launch is a planned integration, not an active feature.**

Run after cloning:

```bash
python tools/refresh_challenges.py
python tools/list_challenges.py --live
python tools/list_challenges.py --live --search cancer
python -m unittest discover -s tests -v
```

The refresh command requires Internet access, while the resulting JSON can be read offline using --live. The catalog count is not guaranteed to equal the website's displayed total if the site changes. A refresh is rejected if any track is empty or fewer than 200 unique problems are gathered. An initial cache is created only after a successful fetch.

## Current status

Project setup / requirements definition. The Android application, website, scientific challenge integration, and patient module have **not yet been implemented**.

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

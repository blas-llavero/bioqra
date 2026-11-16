# BIOQRA — User Guide

![BIOQRA full logo](images/bioqra_full_logo.png)

BIOQRA is an experimental open-source Android project to discover and explore bioinformatics challenges. Its goal is to support scientific research for public benefit. It does **not** diagnose conditions, recommend treatments, establish drug causality, or demonstrate cures.

## Download the Android APK

See the latest successful run of [Android Debug APK on GitHub Actions](https://github.com/blas-llavero/bioqra/actions/workflows/android.yml). Download the artifact `bioqra-debug-apk`, extract its ZIP file, and open `app-debug.apk` on your Android device. The APK must be built successfully before it is available.

## How to use the current Android prototype

1. Open **BIOQRA**; the app tries to refresh its official Rosalind challenge listing at launch. If the connection fails, it can show its last saved catalog.
2. Search by problem title or identifier in **Search challenges**.
3. Open **Challenges** to browse Rosalind exercises.
4. Open **Cancer Research** or **Hackathons** for initial research-source links; these sections are not yet continuously updated competition databases.
5. Tap **Refresh** to retry the Rosalind update.
6. Tap **App Description** to play the approved English introduction. If the MP4 is not bundled in the APK, select the approved video from device storage.
7. **How to Use** video, multi-source automated challenge aggregation, reproducible solvers, and a secure patient-data module remain planned features.

Only public bioinformatics challenge data is loaded by the current prototype; do not enter or upload patient health records.

Conceived and programmed by Blas Llavero. For more information: https://blasllavero.com

# BIOQRA visual identity and file mapping

This package uses the **two user-approved image uploads without any content retouching or cropping**. Dark corners, luminous edges, and typography are preserved exactly as supplied. Only proportional resizing is used for derivative icons; the Android adaptive foreground adds transparent safe-area padding so the full icon remains visible within system masks.

## Original artwork

- `docs/images/bioqra_full_logo.png`: **first** uploaded image, intended for GitHub README, manual covers and full-size presentation.
- `assets/branding/bioqra_compact_logo_master.png`: **second** uploaded image, intended for smaller app and web icons. It has **no long subtitle**.

## Android

- `app/src/main/res/mipmap-*/ic_launcher.png`: legacy launch icons (48, 72, 96, 144, 192 pixels), matching Android density buckets.
- `app/src/main/res/mipmap-*/ic_launcher_round.png`: full artwork in each bitmap; Android launcher may apply its own round mask.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`: adaptive icon configuration for Android 8+.
- `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`: safely padded full compact artwork for adaptive masking.
- `app/src/main/res/values/bioqra_launcher_colors.xml`: icon background color.
- `app/src/main/AndroidManifest.xml`: add `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` to the `<application>` tag. `install_into_bioqra.py` can patch this automatically.

## Website (future)

Place `web/public` files in the future web app's public/static root. The site itself has **not** been implemented yet. In the HTML head (adjust URL base path if deployed under a subdirectory):

```html
<link rel="icon" href="favicon.ico" sizes="any">
<link rel="apple-touch-icon" href="apple-touch-icon.png">
<link rel="manifest" href="site.webmanifest">
```

For a presentation, use `bioqra-full-logo.webp` (optimized) or the original image at `docs/images/bioqra_full_logo.png`.

## README and manual

The setup script inserts the full logo **between the README title and first project description**, leaving the Android download section immediately after the description. The manual `docs/USER_GUIDE.md` also contains the full logo.

## Media and license

Only the user-approved logos are included; no audio, videos, private data, or medical records are in this package. Before distributing assets as open content, specify a visual asset license compatible with the project's open-source goals.

# Zenith Anime

<div align="center">

**Free anime streaming app for Android and the web with watch tracking and multi-server playback.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web-green.svg)](https://gokuthug1.github.io/zenith-anime/)
[![GitHub Pages](https://img.shields.io/badge/Web%20Stream-Live-10b981.svg)](https://gokuthug1.github.io/zenith-anime/)
[![Deploy Zenith Anime Web to GitHub Pages](https://github.com/gokuthug1/zenith-anime/actions/workflows/deploy-pages.yml/badge.svg)](https://github.com/gokuthug1/zenith-anime/actions/workflows/deploy-pages.yml)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/G9vh5kVcvC)

<br>

[**Launch Web Stream**](https://gokuthug1.github.io/zenith-anime/) &bull; [**Download Android APK**](APK/zenith-anime-latest.apk) &bull; [**Showcase & Docs**](https://gokuthug1.github.io/zenith-anime/landing.html) &bull; [**Discord Server**](https://discord.gg/G9vh5kVcvC)

</div>

---

## 📖 Overview

Zenith Anime is a free, local-first anime streaming application available as a **native Android app** and as a **responsive web app**. It focuses on fast playback, continuous watch tracking, zero advertisements, and complete privacy with local-first storage.

* **Web Edition**: Stream directly in your browser on PC, Laptop, Tablet, or Phone with no install required.
* **Android Native App**: Built with Jetpack Compose & ExoPlayer for Android 8.0+.

---

## 🌐 Web Streaming Edition

The web version brings the complete Zenith experience to modern desktop and mobile browsers.

* **Native 16:9 Video Streaming**: Clean, un-hijacked iframe player with direct access to resolution, audio, subtitle, and fullscreen controls.
* **Instant Multi-Server Switching**: Switch between AniEmbed, VidSrc, Viduki, and MegaPlay with one click without resetting progress.
* **Audio Track Switching**: Easily toggle between Subtitled and English Dubbed streams.
* **Continuous Watch Tracking & History**:
  * Tracks watch progress automatically in the background.
  * Resumes from where you left off via the **Continue Watching** shelf on the Home screen.
  * Automatically marks episodes completed (`✓`) once watched.
* **Responsive Layouts for All Screens**:
  * **Desktop / Laptop**: Full top navigation bar with quick search, theme picker, and catalog rows.
  * **Tablet / Phone**: Optimized touch interface with a floating island bottom navigation dock.
* **Local-First Library**:
  * Organize your collection into *Watching*, *Completed*, *On Hold*, *Dropped*, and *Plan to Watch*.
  * Export and import complete JSON backups directly from your browser.
* **8 Dynamic Accent Themes**: Emerald, Cyan, Sakura, Violet, Amber, Crimson, Indigo, and Gold.
* **Weekly Airing Schedule**: Live release countdowns for upcoming episodes airing in Japan.

---

## 📱 Android Native Client

The Android edition is a full-featured native client designed for Android phones, tablets, and emulators.

* **Media3 / ExoPlayer Engine**: Native hardware-accelerated playback with gesture controls (brightness, volume, double-tap seek).
* **Local Room Database**: Complete offline-first storage with zero tracking or telemetry.
* **Offline Downloads**: Batch episode downloader with auto-delete options for watched files.
* **Tracker Integration**: Optional 2-way scrobbling with AniList and MyAnimeList.
* **AnimeThemes Jukebox**: Watch clean openings and endings in 1080p/720p via AnimeThemes.moe.
* **UI Customization Playground**: Customize player layouts (Center, Bottom, Split), seekbar styles, and accent colors.
* **Smart TV & Cast Support**: Stream to Chromecast and Google Cast-enabled devices.
* **Discord Rich Presence (RPC)**: Share your current episode and watch status via webhook.

---

## 🚀 Quick Links

| Platform | Link | Details |
|---|---|---|
| **Web Streaming App** | [Launch Web Stream](https://gokuthug1.github.io/zenith-anime/) | Runs in any browser (Chrome, Firefox, Safari, Edge) |
| **Landing & Showcase** | [Showcase Page](https://gokuthug1.github.io/zenith-anime/landing.html) | Interactive UI playground & download hub |
| **Android APK** | [Download Latest APK](APK/zenith-anime-latest.apk) | Direct APK download (Android 8.0+) |
| **Community Discord** | [Join Discord](https://discord.gg/G9vh5kVcvC) | Discussion, feedback, and updates |

---

## 🛠️ Deploying Your Own Web Instance

### Option 1: GitHub Pages (Recommended)
1. Fork or clone this repository.
2. In your repo, go to **Settings** &rarr; **Pages**.
3. Under **Build and deployment** &rarr; **Source**, select **GitHub Actions**.
4. Push any change to `master` and the included `.github/workflows/deploy-pages.yml` workflow will automatically build and deploy the `docs/` folder.

### Option 2: Vercel
1. Fork or clone this repository.
2. Go to [Vercel](https://vercel.com/new) and import the repository.
3. Vercel automatically detects `vercel.json` (configured for `/docs` with SPA routing).
4. Click **Deploy**.

---

## 🔨 Building the Android App from Source

### Prerequisites
* [Android Studio Ladybug (2024.2+)](https://developer.android.com/studio) or newer
* JDK 17+
* Android SDK 35+ (API 35/36)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/gokuthug1/zenith-anime.git
cd zenith-anime

# Assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🎨 UI/UX Inspiration

The visual design language and interaction patterns are inspired by [Th3-Anime](https://github.com/Th3-Anime/Th3-Anime). Appreciation goes to their creators for setting a high bar for anime interfaces.

---

## ⚖️ Disclaimer / DMCA

Zenith Anime does not host, upload, or store any media files on its servers. The application functions solely as an indexing client that connects to publicly available third-party video hosts. For copyright issues or content removal, please contact the respective media hosting services directly.

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

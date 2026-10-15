# Zenith Anime

<div align="center">

**A modern, local-first, highly customizable anime streaming and organization client for Android.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0+-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg)](https://kotlinlang.org)
[![Build APK](https://github.com/gokuthug1/zenith-anime/actions/workflows/build-apk.yml/badge.svg)](https://github.com/gokuthug1/zenith-anime/actions/workflows/build-apk.yml)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/G9vh5kVcvC)
[![Website](https://img.shields.io/badge/Website-zenith--anime-10b981.svg)](https://gokuthug1.github.io/zenith-anime/)

</div>

---

> [!IMPORTANT]
> ### ⚡ Development Note: 100% Vibecoded in One Month
> **This entire application was vibecoded by Gemini 3.8 Flash in one month.**
> Every single feature, architecture layer, Jetpack Compose screen, ExoPlayer integration, custom Room database schema, and live UI customization playground was generated and refined through autonomous agentic pair programming.

---

> [!WARNING]
> ### ⚖️ Legal Disclaimer / DMCA
> **Zenith Anime does not host, store, upload, or distribute any media files on any server.** This project functions solely as a client interface that indexes and provides access to content from publicly available third-party sources. Zenith Anime does not own or control any third-party content, and availability may vary depending on external providers. For copyright or legal concerns regarding specific media, please contact the respective hosting providers or rights holders directly.

---

> [!NOTE]
> ### 🎨 UI/UX Credits & Inspiration
> The user interface design, aesthetic, and interaction patterns are heavily inspired by [Th3-Anime](https://github.com/Th3-Anime/Th3-Anime). Sincere appreciation goes to the creators and designers of Th3-Anime for setting a high standard for anime streaming interfaces.

---

## 💬 Community & Discord

Join our official community Discord for announcements, feedback, feature voting, bug reports, and discussion:

**[Join the Zenith Anime Discord Server](https://discord.gg/G9vh5kVcvC)** *(Official Community)*

---

## ✨ Features

- **Full Video Player Suite (ExoPlayer Engine)**
  - Seamless subtitle tracks (English, Japanese, Spanish, etc.) & real dub audio switching.
  - Granular playback speed control (0.25x – 3.0x) and fast hold-to-speed gestures.
  - Video aspect ratio modes: Fit, Fill, Zoom, 16:9, and 4:3.
  - Customizable auto-hide controls, sleep timer with countdown, and auto-skip intro/outro.
  - Unobtrusive letterbox screen-lock button with user-selected left or right placement.
  - Continuous 1-second watch progress persistence for jump-free progress bars.

- **Interactive Customization Playground**
  - Reorder, show, or hide individual player controls.
  - Adjust transport button positioning (Center, Bottom, Split).
  - Switch timeline seekbar styles (Modern, Minimal, Thick).
  - Toggle between Modern and Legacy video player layouts.
  - Choose accent color overrides (Emerald, Cyan, Sakura, Violet, Amber, Crimson).
  - Live real-time preview of player and UI changes before applying.

- **Remastered Continue Watching**
  - Rich 16:9 widescreen thumbnails with dark gradient scrims and progress percentage badges.
  - Multi-line titles and formatted remaining duration indicators.
  - Dedicated **Info** action button to navigate directly to the anime details page.
  - "Clear All" with confirmation safeguard.

- **Anime Organization & MyAnimeList Integration**
  - Clear watch statuses: Watching, Completed, On Hold, Dropped, Plan to Watch.
  - Authentic MyAnimeList (MAL) reviews integrated directly into details pages.
  - Local-first architecture: Room database + Moshi serialization. Zero telemetry or tracking.
  - Full backup & restore for offline data portability.

- **Profile Customization**
  - Support for custom avatar URLs, SVG vectors, and animated GIFs.
  - Safe image validation and responsive UI rendering.

---

## 📥 Download & Installation

Pre-built APKs and distribution portals:

- **Latest Release APK**: [`APK/zenith-anime-latest.apk`](APK/zenith-anime-latest.apk)
- **Debug APK**: [`APK/app-debug.apk`](APK/app-debug.apk)
- **GitHub Releases**: [Latest Release (v1.2.0)](https://github.com/gokuthug1/zenith-anime/releases/latest)
- **Web App / Showcase**: [https://gokuthug1.github.io/zenith-anime/](https://gokuthug1.github.io/zenith-anime/)

### Requirements
- Android 8.0 (API level 26) or higher.
- ARM64 or x86_64 device / emulator.

---

## 🛠️ Building from Source

### Prerequisites
- [Android Studio Ladybug (2024.2+) or newer](https://developer.android.com/studio)
- JDK 17 or higher
- Android SDK 35+ (API 35/36)

### Clone and Compile

```bash
git clone https://github.com/gokuthug1/zenith-anime.git
cd zenith-anime
./gradlew assembleDebug
```

The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Unit Tests

```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for complete details.

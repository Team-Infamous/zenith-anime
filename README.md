# Zenith Anime

<div align="center">

**A modern, local-first, highly customizable anime streaming and organization client for Android & Web (PC, Laptops, Tablets, Phones).**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Web%20%7C%20Android-green.svg)](https://gokuthug1.github.io/zenith-anime/)
[![Deploy with Vercel](https://vercel.com/button)](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2Fgokuthug1%2Fzenith-anime)
[![GitHub Pages](https://img.shields.io/badge/GitHub%20Pages-Live%20Stream-10b981.svg)](https://gokuthug1.github.io/zenith-anime/)
[![Build APK](https://github.com/gokuthug1/zenith-anime/actions/workflows/build-apk.yml/badge.svg)](https://github.com/gokuthug1/zenith-anime/actions/workflows/build-apk.yml)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/G9vh5kVcvC)

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

- **Flagship UI/UX & Navigation Overhaul**
  - Dynamic 5-card auto-scrolling spotlight hero carousel with animated pill indicators.
  - Sticky glassmorphic top bar with collapsing frosted blur navbar and random anime picker.
  - 2-line smart typography wrapping and frosted pill badges across catalog cards.
  - Modern floating island dock bottom navigation with active luminous glow.
  - Segmented anime details screen (Episodes default, Overview, Characters & VAs, Reviews, Related).
  - Detailed 16:9 thumbnail list vs compact numbered grid switcher and episode range selector.

- **Full Video Player Suite & Automation (ExoPlayer Engine)**
  - YouTube-style double-tap seeking ripples with stacking +10s / +20s / +30s badges.
  - Sleek vertical swipe HUDs for brightness (left) and volume (right) with central pill indicator.
  - Auto-skip opening (OP) and ending (ED) with instant 85s jump button.
  - In-player quick episode drawer strip and "Next Episode" 5s countdown overlay during final 30s.
  - Seamless subtitle styling engine with numerical sizes (14-26sp), backgrounds (Semi, Solid, Grey, None), multi-track dub audio switching, and screen lock.
  - Versatile video aspect ratio modes: Fit, Stretch, Fill, Zoom, 16:9, and 4:3.
  - Decoupled modular transport controls across Center, Bottom, and Split player layout modes.

- **Anime Themes & Music Jukebox (AnimeThemes.moe)**
  - Dedicated "Themes" tab in Anime Details with creditless Openings and Endings.
  - Watch 1080p/720p clean anime themes directly in-app with embedded Media3 video player.

- **2-Way Tracker Sync & Auto-Scrobbler (AniList & MyAnimeList)**
  - Fully opt-in (disabled by default, user preference in Settings).
  - Automatically marks episodes watched and status completed at configurable threshold (default 85%).

- **Smart TV & Google Cast Streaming**
  - Completely disabled by default.
  - Toggleable in Player Settings with in-player cast action icon and device picker.

- **Batch Downloader & Auto-Delete Storage Manager**
  - Download "Next 1", "Next 5", or "All Available" episodes in a single tap.
  - Optional auto-delete watched downloads to free up device storage.

- **Seasonal Release Schedule & Live Countdowns**
  - Weekly broadcast grid with real-time live countdown badges ("Airs in Xh Ym").
  - Airing reminder notification toggle for favorite upcoming series.

- **Discord Rich Presence (RPC) & Background Audio**
  - Broadcast current anime, episode, and remaining time to Discord via webhook.
  - Background audio playback for listening with screen turned off.

- **Title Language Preference (English vs. Japanese)**
  - Toggle between English and Japanese (Romaji) titles globally across Home, Details, Player, Library, and Continue Watching.

- **Mega Server as Default & Recommended**
  - High-speed direct HLS video streaming out of the box with download and audio track switching support.

- **Refined UI/UX, Sleek Seekbar & Device Inset Support**
  - Modernized 3.5dp slim seekbar with responsive 14dp thumb, frosted continue watching timestamp badges, and full system navigation bar padding across all device screens.

- **Local Backup File Export & Clipboard Copy**
  - Direct JSON backup export to the device's public `Download/` folder via Android MediaStore alongside instant clipboard copy.

- **Interactive Customization Playground**
  - Reorder, show, or hide individual player controls.
  - Adjust transport button positioning (Center, Bottom, Split).
  - Switch timeline seekbar styles (Modern, Minimal, Thick).
  - Toggle between Modern and Legacy video player layouts.
  - Choose accent color overrides (Emerald, Cyan, Sakura, Violet, Amber, Crimson, Indigo, Gold).
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

## 🌐 Web Streaming Edition (Browser, PC, Mac, Tablet, Phone)

Zenith Anime Web brings the complete Zenith streaming experience to all web browsers with zero installation:

- **Live Web Stream**: [https://gokuthug1.github.io/zenith-anime/](https://gokuthug1.github.io/zenith-anime/)
- **Android Showcase & Landing**: [`landing.html`](https://gokuthug1.github.io/zenith-anime/landing.html)

### 🚀 Deploying Your Own Instance (Vercel or GitHub Pages)

#### Option 1: Deploy with Vercel (1-Click)
1. Fork or clone this repository.
2. Go to [Vercel](https://vercel.com/new) and click **Import Project**.
3. Select your repository. Vercel automatically detects `vercel.json` (configured for `docs/` with clean URLs and SPA routing).
4. Click **Deploy**. Your Zenith Anime web streaming app is live in seconds!

#### Option 2: Deploy with GitHub Pages
1. Go to your repository on GitHub.
2. Navigate to **Settings** &rarr; **Pages**.
3. Under **Build and deployment** &rarr; **Source**, select:
   - **Deploy from a branch** &rarr; Branch: `master` (or `main`) &rarr; Folder: `/docs` &rarr; **Save**, OR
   - **GitHub Actions** (the included `.github/workflows/deploy-pages.yml` workflow will automatically deploy on every push).
4. Visit your GitHub Pages URL (e.g. `https://<username>.github.io/<repo>/`).

---

## 📥 Android Native APK (Android 8.0+)

Pre-built native APKs with ExoPlayer & local Room database:

- **Latest Release APK**: [`APK/zenith-anime-latest.apk`](APK/zenith-anime-latest.apk)
- **Debug APK**: [`APK/app-debug.apk`](APK/app-debug.apk)
- **GitHub Releases**: [Latest Release (v1.7.0)](https://github.com/gokuthug1/zenith-anime/releases/latest)

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

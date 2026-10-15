# Zenith Anime v1.5.0

> **Notice:** The entire Zenith Anime application was vibecoded by **Gemini 3.8 Flash** in **one month**.

Zenith Anime is a modern, local-first, highly customizable anime streaming and organization client for Android. Built with Jetpack Compose, Material Design 3, and Media3 ExoPlayer.

---

## 🚀 What's New in v1.5.0

### 🌐 English vs. Japanese Anime Titles (User Preference)
- **Universal Title Language Preference**: New setting in Library & Behavior settings allowing users to choose between **English** or **Japanese (Romaji)** anime titles across the entire app.
- **Full App Consistency**: Applies seamlessly across the Home screen (Hero, Trending, Top Rated, Popular), Library, Continue Watching rows/sheets, Search results, Anime Details, and the Video Player.
- **Database Schema Migration (Room v4)**: Added migration `MIGRATION_3_4` storing both `englishTitle` and `romajiTitle` locally with backward compatibility and zero data loss.
- **AniList & MAL Sync Integration**: Both AniList and MalSync metadata providers now populate both title variants during live catalog sync.

### ⚡ Mega Server as Default & Recommended
- **Mega (Recommended)**: Set as the default primary streaming server across fresh installs and reset states.
- **Direct High-Speed HLS Streaming**: Optimized direct video resolution with download and audio track switching support.
- **Cleaner Pill Displays**: Server chips in the player format cleanly as `Mega` instead of truncating or displaying unformatted provider tags.

### 🎨 Design Modernization, Sleek Animations & UI Bloat Cleanup
- **Hero Carousel Improvements**:
  - Fixed the `+ + My List` double-plus icon/text bug.
  - Repositioned carousel indicator dots to the bottom edge so they never overlap with the `(i)` info action button.
- **Continue Watching Card & Sheets**:
  - Continue watching timestamp wrapped in a sleek frosted translucent pill badge.
  - Removed circular cutout background clutter from sheet action buttons ((X), (i), and delete).
  - Deduplicated episode subtitles (e.g., displaying `Episode 1` instead of `Episode 1 • Episode 1`).
- **Input Box Layout Polish**:
  - Replaced clipped `OutlinedTextField` with vertically centered `BasicTextField` rows in `AnimeDetailsScreen`, `PlayerScreen`, and bottom sheets for flawless text rendering on all screen sizes and densities.
- **Player Episode Tab Bloat Reduction**:
  - Removed redundant `Sheet` button since episodes are already conveniently browsable directly within the tab list.
  - Landscape top bar episode navigation icon updated to `VideoLibrary` for visual clarity.
- **Sleek Video Seekbar**:
  - Replaced chunky 16dp Material 3 track with a refined 3.5dp slim track and responsive 14dp thumb for a modern, sleek streaming interface.
- **Full Device Responsiveness**:
  - Added `navigationBarsPadding()` to the portrait tab player section and bottom sheets to prevent system bar overlap across 3-button and gesture navigation modes on modern devices.
- **Details Screen Status Consolidation**:
  - Consolidated separate bookmark toggle and status dropdown into a unified interactive status badge with one-tap removal.

### 💾 Enhanced Local Backup & Export
- **Dual Export Options**:
  - **Download File**: Direct JSON file export directly into the device's public `Download/` directory via Android MediaStore (`zenith_anime_backup_<timestamp>.json`).
  - **Copy JSON**: Instant clipboard copy with notification toast.

---

## 📦 Download & Verification

| Asset | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| `zenith-anime-latest.apk` | 28.6 MB | `0819911A6F74FF6C90FEABFB2D7FBEDCD4D90A02096EA0B0AFE116D01D0D5B73` |
| `app-debug.apk` | 28.6 MB | `0819911A6F74FF6C90FEABFB2D7FBEDCD4D90A02096EA0B0AFE116D01D0D5B73` |

### Installation Instructions
1. Download `zenith-anime-latest.apk` to your Android device.
2. If prompted, enable "Install unknown apps" for your browser or file manager.
3. Open the APK and tap **Install**.
4. (Optional) Transfer via Wireless Debugging using `adb install -r APK/zenith-anime-latest.apk`.

---

## ⚖️ Legal Disclaimer / DMCA Notice

> **Zenith Anime does not host, store, upload, or distribute any media files on any server.** This project functions solely as a client interface that indexes and provides access to content from publicly available third-party sources. Zenith Anime does not own or control any third-party content, and availability may vary depending on external providers. For copyright or legal concerns regarding specific media, please contact the respective hosting providers or rights holders directly.

---

## 🎨 Acknowledgements & Credits

- Visual aesthetics, navigation paradigm, and player design heavily inspired by [Th3-Anime](https://github.com/Th3-Anime/Th3-Anime).
- Theme data and creditless animations provided by [AnimeThemes.moe](https://animethemes.moe).
- Licensed under the permissive [MIT License](LICENSE).

---

## 💬 Community & Discussion

- **Official Discord Server:** [https://discord.gg/G9vh5kVcvC](https://discord.gg/G9vh5kVcvC)

# Zenith Anime v1.2.0

> **Notice:** The entire Zenith Anime application was vibecoded by **Gemini 3.8 Flash** and **DeepSeek V4.1 Flash** in **one month**.

Zenith Anime is a modern, local-first, highly customizable anime streaming and organization client for Android. Built with Jetpack Compose, Material Design 3, and Media3 ExoPlayer.

---

## 🚀 What's New in v1.2.0

### 📺 Video Player Remaster & UI/UX Polish
- **Letterbox Screen Lock**: Redesigned lock control placed unobtrusively inside the black letterbox area in Fit mode with display cutout support. Configurable left or right screen placement.
- **Orientation Lock Stability**: Fixed bug where locking the player forced exit from fullscreen into portrait mode; orientation lock now reliably holds landscape.
- **Independent Menu Scrolling**: Playback speed, aspect ratio, audio dub, and sleep timer menus now feature independent scroll containers, eliminating clipping and touch conflicts.
- **Dub Audio Engine**: Fully overhauled multi-track audio discovery and selection. Users can switch to English or other dubbed audio streams seamlessly.
- **Control Streamlining**: Removed redundant controls and unwanted brightness sliders from the player overlay for an unobstructed view.

### ⏱️ Watch Progress Persistence
- **1-Second Interval Saving**: Added granular progress saving setting allowing state synchronization down to every second, eliminating progress jumps on resume.
- **Continuous Timeline Tracking**: Improved playback position tracking on app minimize, pause, and exit.

### 📋 Continue Watching & Home Remaster
- **Widescreen Episode Cards**: Upgraded continue watching cards to an elegant 16:9 widescreen layout with thumbnail artwork and high-contrast progress bars.
- **Press-and-Hold Context Menu**: Long-press any Continue Watching card to quickly remove individual episodes, clear an entire series, or navigate directly to anime details.
- **Direct Info Navigation**: Top-corner info badge navigates directly to the anime details page.
- **Home Cleanup**: Removed non-functional category chips and redundant refresh buttons for a streamlined browsing experience.

### 🌟 Details Page & Reviews
- **MyAnimeList (MAL) Reviews**: Integrated official MAL community reviews into anime details with pagination and reviewer ratings.
- **Watchlist Persistence Bug Fix**: Fixed critical bug where opening details before watching caused the anime to disappear from the watchlist.
- **Share Notice**: Friendly popup indicating sharing functionality is coming soon.

### 👤 Profile & Customization Playground
- **Profile Avatar**: Cleaned up the avatar display by removing the obstructing edit pen icon and its overlay trigger.
- **Responsive UI Playground**: Viewport auto-fitting ensures all controls and preview frames fit user screens without horizontal overflow.

---

## 📦 Download & Verification

| Asset | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| `zenith-anime-latest.apk` | 30.2 MB | `aad0a4c36090ecef90c74685708a556df8304ddbe776df356d1eba4f2ff6a416` |
| `app-debug.apk` | 30.2 MB | `aad0a4c36090ecef90c74685708a556df8304ddbe776df356d1eba4f2ff6a416` |

### Installation Instructions
1. Download `zenith-anime-latest.apk` to your Android device.
2. If prompted, enable "Install unknown apps" for your browser or file manager.
3. Open the APK and tap **Install**.
4. (Optional) Transfer via Wireless Debugging using `adb install -r APK/zenith-anime-latest.apk`.

---

## ⚖️ Legal Disclaimer / DMCA Notice

> **Zenith Anime does not host, store, upload, or distribute any media files on its servers.** This project functions solely as a client interface that indexes and provides access to content from publicly available third-party sources. Zenith Anime does not own or control any third-party content, and availability may vary depending on external providers. For copyright or legal concerns regarding specific media, please contact the respective hosting providers or rights holders directly.

---

## 🎨 Acknowledgements & Credits

- Visual aesthetics, navigation paradigm, and player design heavily inspired by [Th3-Anime](https://github.com/Th3-Anime/Th3-Anime).
- Licensed under the permissive [MIT License](LICENSE).

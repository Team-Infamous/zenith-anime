# Zenith Anime v1.3.0

> **Notice:** The entire Zenith Anime application was vibecoded by **Gemini 3.8 Flash** in **one month**.

Zenith Anime is a modern, local-first, highly customizable anime streaming and organization client for Android. Built with Jetpack Compose, Material Design 3, and Media3 ExoPlayer.

---

## 🚀 What's New in v1.3.0

### 🎨 Flagship UI/UX Overhaul
- **Auto-Advancing Hero Carousel**: Replaced static spotlight banner on the Home Screen with a dynamic 5-card auto-scrolling spotlight carousel with animated pill indicators and vibrant backdrop mesh gradient tinting.
- **Sticky Glassmorphic Top Bar**: Smoothly collapses into a frosted-glass blurred navbar (`Color(0xEE121622)` with `1px` translucent border) as you scroll, keeping Search, Genre Filter, and the new **Random Anime** picker accessible at all times.
- **2-Line Smart Typography & Frosted Badges**: Fixed aggressive single-line title truncation across catalog grids and cards by enabling 2-line auto-wrapping with tight line height (`lineHeight = 16.sp`, `maxLines = 2`), frosted glass pill badges (`Color(0x66000000)`), and ambient poster elevation shadows.
- **Segmented Pager Tabs for Anime Details**: Organized series details into 5 dedicated horizontal tabs:
  - **Episodes** *(Default)*: Rapid binge navigation.
  - **Overview**: Synopsis, genres, format, airing status, studios, and staff metadata.
  - **Characters & Voice Actors**: Comprehensive character cards paired with Japanese and English VAs powered by AniList GraphQL.
  - **Reviews**: Community reviews and score distributions.
  - **Related**: Sequels, prequels, spin-offs, and franchise recommendations.
- **Episode View Switcher & Range Carousel**:
  - **Detailed List Mode**: Rich 16:9 thumbnails, episode titles, duration, watched checkmarks, and progress indicators.
  - **Compact Grid Mode**: Clean numbered squares (`[ 1 ]`, `[ 2 ]`, ...) for quick episode picking.
  - **Episode Range Selector Carousel**: Fast range jumping (`1–25`, `26–50`, etc.) for long-running series.
- **Floating Island Dock Bottom Navigation**: Modern floating pill dock with 24dp rounded corners, frosted backdrop blur, and an active luminous indicator beneath selected tabs.

### ⚡ Player Experience & Automation
- **YouTube-Style Double-Tap Seek Ripples**: Double-tapping the left or right side of the screen triggers dynamic curved ripples with animated stacking badges (`+10s`, `+20s`, `+30s`, etc.).
- **Vertical Swipe HUDs**: Sliding vertically on the left side controls screen brightness and sliding on the right side controls volume, indicated by a sleek minimalist floating pill HUD in the center.
- **Auto-Skip OP/ED Button**: Floating quick chip that instantly skips 85 seconds forward without touching the seekbar.
- **"Next Episode" Countdown Overlay**: Automatically slides up in the bottom-right corner during the last 30 seconds of an episode with a 5-second auto-play timer and "Watch Next" / "Dismiss" actions.
- **In-Player Quick Episode Drawer Strip**: Bottom drawer strip inside the player allowing users to switch episodes on the fly without exiting fullscreen.
- **Subtitle Styling Enhancements**: Added outline thickness controls and styling options in the subtitle dialog.

---

## 📦 Download & Verification

| Asset | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| `zenith-anime-latest.apk` | 28.5 MB | `150c3f719c350ddd8bb6f94f877178f20e6335bcbae93140504b0824eae03424` |
| `app-debug.apk` | 28.5 MB | `150c3f719c350ddd8bb6f94f877178f20e6335bcbae93140504b0824eae03424` |

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
- Licensed under the permissive [MIT License](LICENSE).

---

## 💬 Community & Discussion

- **Official Discord Server:** [https://discord.gg/G9vh5kVcvC](https://discord.gg/G9vh5kVcvC)

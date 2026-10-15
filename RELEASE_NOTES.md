# Zenith Anime v1.4.0

> **Notice:** The entire Zenith Anime application was vibecoded by **Gemini 3.8 Flash** in **one month**.

Zenith Anime is a modern, local-first, highly customizable anime streaming and organization client for Android. Built with Jetpack Compose, Material Design 3, and Media3 ExoPlayer.

---

## 🚀 What's New in v1.4.0

### 🎵 Anime Themes & Music Jukebox (AnimeThemes.moe)
- **Dedicated "Themes" Tab**: Explore creditless Openings (OP) and Endings (ED) directly on the Anime Details screen powered by the AnimeThemes.moe API.
- **Embedded In-App Theme Player**: Watch full 1080p/720p clean anime openings and endings without leaving the app using the high-performance Media3 video player dialog.
- **Theme Details**: Song titles, performing artists, sequence numbers, and resolution badges.

### 🔄 2-Way Tracker Sync & Auto-Scrobbler (AniList & MyAnimeList)
- **User Preference (Disabled by Default)**: Tracker sync stays completely offline unless explicitly enabled by the user in Settings.
- **Auto-Scrobble Engine**: Automatically updates your AniList or MyAnimeList watch list when an episode reaches completion threshold (default 85%).
- **Customizable Threshold**: Adjust scrobble completion percentage from 50% to 100% in Settings.
- **Direct Auth Support**: Secure personal access token configuration for AniList and MAL.

### 📺 Google Cast & Smart TV Streaming
- **Disabled by Default**: Completely dormant by default. When enabled in Player Settings, adds cast options and device pickers to both Modern and Legacy video player top bars.
- **Streamlined Casting**: Seamlessly route streams to compatible Google Cast / Chromecast and Android TV devices.

### 💾 Batch Downloader & Auto-Delete Storage Manager
- **Batch Download Modal**: Queue "Next 1", "Next 5", or "All Available" episodes for background download in a single tap.
- **Auto-Delete Watched Episodes**: Automatically removes downloaded video files and sidecar subtitle assets from internal storage once marked as watched (90%+ completion).

### ⏰ Seasonal Release Schedule with Live Countdowns & Airing Reminders
- **Live Airing Countdown Clock**: Badges dynamically calculate real-time release status (`Airs in 2h 15m`, `Airs in 3d 4h`, `Aired 1h ago`) with pulsing status indicators.
- **One-Tap Airing Reminders**: Tap the notification bell icon to schedule an episode reminder directly from the weekly schedule.

### 🎮 Discord Rich Presence (RPC) & Background Audio
- **Discord Status Updates**: Optional webhook presence dispatch broadcasting currently playing anime, episode number, and remaining duration.
- **Background Audio Playback**: Keep listening to anime audio tracks even when your phone screen is locked or while multitasking in other apps.

---

## 📦 Download & Verification

| Asset | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| `zenith-anime-latest.apk` | 28.6 MB | `f064f072bbe3aad6790bf6d18906ae7d0354158b6c7042040373243bb0d93e66` |
| `app-debug.apk` | 28.6 MB | `f064f072bbe3aad6790bf6d18906ae7d0354158b6c7042040373243bb0d93e66` |

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

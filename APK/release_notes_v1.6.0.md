# Zenith Anime v1.6.0

> **Notice:** The entire Zenith Anime application was vibecoded by **Gemini 3.8 Flash** in **one month**.

Zenith Anime is a modern, local-first, highly customizable anime streaming and organization client for Android. Built with Jetpack Compose, Material Design 3, Room Database, and Media3 ExoPlayer.

---

## 🚀 What's New in v1.6.0

This release delivers **16 major features and enhancements**, a comprehensive bug fix sweep across data, network, and player engines, and a complete UI/UX remaster across all screens.

### 🌟 16 Highlights & Enhancements

1. **System-Wide True AMOLED Black Engine & Unified 8-Accent Theme Palette**
   - Replaced hardcoded dark background and surface colors across all screens with dynamic `MaterialTheme.colorScheme.background` and `surface`.
   - Activating AMOLED mode renders true pitch-black (`#000000`) backgrounds for maximum battery efficiency.
   - Synchronized all 8 theme accents (`Emerald`, `Cyan`, `Sakura`, `Violet`, `Amber`, `Crimson`, `Indigo`, and `Gold`) across the app and web documentation.

2. **Adaptive Navigation Bar Dock Architecture (`Standard`, `Compact`, `Floating`)**
   - Added selectable navigation dock styles in Settings and Playground:
     - **Standard**: Sleek 64dp dock with indicator pills and labeled tabs.
     - **Compact**: Slim 52dp minimalist icon-first bar.
     - **Floating**: Floating glassmorphic island capsule with 32dp corner radii and horizontal floating margins.

3. **Dynamic Font Scaling & System Density Injection**
   - Live font scale multiplier (`0.85x` to `1.25x`) dynamically injected into Compose's `LocalDensity` without requiring an app restart.

4. **Bi-Directional Tracker Scrobbler & Token Integration**
   - Added a dedicated "Tracker Scrobbler" tab in MalSync settings with credentials management for AniList and MyAnimeList auth tokens.
   - Fixed scrobbler mapping so MyAnimeList IDs are never passed as AniList IDs, and completed status is prevented on Episode 1.

5. **Airing Timeline Notifications & Persistent Reminder Subscriptions**
   - Integrated persistent notification subscriptions in the Schedule screen. Tapping "Remind Me" stores reminder IDs, records an `AppNotification` in Room, and updates the timeline UI state across app restarts.

6. **Interactive Privacy Suite & Anti-Snoop Protection**
   - Interactive privacy toggles: `blockScreenshots` (Android `FLAG_SECURE`), `hideFromRecents` (`setExcludeFromRecents`), and `incognitoMode` (preventing search history persistence).

7. **Storage Access Framework (SAF) Custom Download Directory Picker**
   - Connected download folder settings to Android's native file picker (`ActivityResultContracts.OpenDocumentTree`) for saving to any external storage or SD card folder.

8. **Supercharged Anime Title Localization Engine**
   - Enhanced `Anime.displayTitle(preference)` with intelligent fallbacks across Native, Romaji/Japanese, and English titles across all rails, cards, and player headers.

9. **Multi-Layout Episode View Selector & Spoiler Blur Protection**
   - Three selectable episode layout modes in Details: Detailed Cards, Thumbnail Grid, and Compact Numbers.
   - Automatic spoiler blur (`Modifier.blur(14.dp)`) hides unreleased/unwatched episode thumbnails when enabled.

10. **Smart Search Multi-Factor Filtering & Real-Time Status Sorting**
    - Search queries match across English, Romaji, and Native titles. Added instant local filtering by status (Airing, Finished, Upcoming), release season, and sort orders.

11. **ExoPlayer Hardware Loudness Normalization & Crash Shielding**
    - Integrated `android.media.audiofx.LoudnessEnhancer` to normalize audio levels. Added unique UUID tokens (`zenith_player_${UUID.randomUUID()}`) to prevent duplicate session ID crashes.

12. **Cancelable Auto-Next Countdown & Direction-Aware Skip OP/ED**
    - Redesigned auto-next countdown banner with an explicit cancel guard to prevent reappearing after dismissal.
    - Direction-aware intro and outro skipping buttons.

13. **Dynamic Responsive Player Top-Bar with Horizontal Actions Dock**
    - Horizontally scrollable action icon container with guaranteed title width, preventing title text ellipsis truncation on narrow portrait screens.
    - Swipe gestures dynamically adjust device screen brightness attributes.

14. **Customization Playground Preview Dashboard**
    - Live interactive preview controls for navigation styles, card corner radius (4–24dp), grid columns (2 or 3), font scale, and card badges.

15. **Full Native Android Share Chooser Integration**
    - Replaced placeholder share toasts with native Android `Intent.ACTION_SEND` share sheets.

16. **Responsive Glassmorphism Documentation & Mobile Navigation**
    - Mobile hamburger navigation menu toggles a frosted glassmorphism dropdown sheet.
    - Added Indigo and Gold theme previews, and fixed DMCA modal dismiss handlers.

---

### 🐛 Bug Fix Sweep

- **Room Database Migration (Room v5)**: Bumped database version to 5 and added `MIGRATION_4_5` for `AppNotification.isUnread`, preventing `IllegalStateException` crashes on app startup.
- **Watchlist Data Loss Fix**: Fixed `AnimeDao.clearWatchlist()` to update `isInWatchlist = 0` rather than deleting rows from the database.
- **Airing Anime Premature Completion**: Fixed `AnimeRepository.saveWatchProgress` so completing the latest released episode of an ongoing series does not mark the entire anime as `COMPLETED`.
- **Download Cancellation & Ghost Files**: Handled `CancellationException` in `AnimeDownloadManager` to prevent canceled downloads from being marked failed or stuck in notifications.
- **HLS Regex & Dub Stream Resolution**: Fixed resolution regex parsing (`1920x1080`) and added fallback stream directory resolution for English Dub streams.
- **Embed WebView Audio Leak**: Ensured `EmbedPlayer` halts playback, navigates to `about:blank`, and invokes `destroy()` on disposal.
- **Unreleased Anime Playback Guard**: Play clicks on upcoming anime with 0 released episodes gracefully route to the Details screen.

---

## 📦 Download & Verification

| Asset | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| `zenith-anime-latest.apk` | 28.7 MB | `C680B543271793AA323834A793A19F19BA00A0B89EF32CBE96FBB51CFF347D60` |
| `app-debug.apk` | 28.7 MB | `C680B543271793AA323834A793A19F19BA00A0B89EF32CBE96FBB51CFF347D60` |
| `checksums.txt` | 170 B | SHA-256 checksum manifest |

### Installation Instructions
1. Download `zenith-anime-latest.apk` to your Android device.
2. If prompted, enable "Install unknown apps" for your browser or file manager.
3. Open the APK and tap **Install**.
4. (Optional) Transfer via Wireless Debugging using `adb install -r APK/zenith-anime-latest.apk`.

---

## ⚖️ Legal Disclaimer / DMCA Notice

> **Zenith Anime does not host, store, upload, or distribute any media files on any server.** This project functions solely as a client interface that indexes and provides access to content from publicly available third-party sources. Zenith Anime does not own or control any third-party content, and availability may vary depending on external providers. For copyright or legal concerns regarding specific media, please contact the respective hosting providers or rights holders directly.

---

## 💬 Community & Discussion

- **Official Discord Server:** [https://discord.gg/G9vh5kVcvC](https://discord.gg/G9vh5kVcvC)

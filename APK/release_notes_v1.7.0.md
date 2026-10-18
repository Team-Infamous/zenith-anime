# Zenith Anime v1.7.0

> **Notice:** The entire Zenith Anime application was vibecoded by **Gemini 3.8 Flash** in **one month**.

Zenith Anime is a modern, local-first, highly customizable anime streaming and organization client for Android. Built with Jetpack Compose, Material Design 3, Room Database, and Media3 ExoPlayer.

---

## 🚀 What's New in v1.7.0

This release delivers major player customizations, video aspect ratio modes, resilient multi-title provider searching and decoding, coroutine cancellation safety, title-aware library sorting, and comprehensive UI/UX refinements.

### 🌟 Release Highlights & Enhancements

1. **Enhanced Video Aspect Ratio Engine**
   - Added new aspect ratio modes: **Fill**, **16:9** (widescreen), and **4:3** (classic retro aspect ratio) alongside Fit, Stretch, and Zoom.
   - Dynamic player surface resizing that respects system window insets and notch cutouts.

2. **Refined Subtitle Typography & Background Customization**
   - Clean numerical subtitle font size steps: `14sp`, `16sp`, `18sp`, `22sp`, and `26sp`.
   - Expanded subtitle background options: `Semi` (BlackSemi), `Solid` (BlackSolid), `Grey`, and `None`, with seamless backward compatibility for existing user configurations.
   - Theme accent propagation to all player settings sheets.

3. **Universal Modular Playback Transport Controls**
   - Extracted and decoupled `transportPlayPauseButton` across Center, Bottom, and Split transport modes in both portrait and landscape orientations.
   - Fluid spring micro-interactions and haptic feedback on play/pause toggles.

4. **Dynamic Episode Spoiler Blur Protection**
   - Configurable thumbnail spoiler blur (12dp) applied to unreleased or unwatched episodes in both the anime details episode lists and the in-player episode drawer.

5. **Resilient Multi-Title Provider Search Engine**
   - `AllAnimeResolver` now searches across English, Romaji, and native Japanese titles simultaneously, drastically improving search accuracy for anime with alternate romanizations or licensor titles.

6. **Hardened Stream Decryption & API Clock URL Normalization**
   - Robust byte-level XOR hex decoding with UTF-8 byte reconstruction.
   - Automatic API URL normalization for clock endpoints (`/apivtwo/clock?id=` -> `https://api.allanime.day/apivtwo/clock.json?id=`).

7. **Guaranteed Cyclic Provider Fallback**
   - Refactored `ProviderManager.getFallbackProvider` with cyclic iteration (`(index + 1) % size`), ensuring every registered provider is tried without getting stuck.

8. **Network Resource Hygiene & Coroutine Cancellation Guarding**
   - Enforced OkHttp response closure via `.use { ... }` blocks across network calls to prevent connection leaks.
   - Strict `CancellationException` re-throwing across `AniListService`, `AnimeThemesService`, `MalSyncService`, `DiscordRpcService`, and `TrackerSyncManager` to preserve coroutine lifecycle semantics.

9. **AniList & MyAnimeList Sync Hardening**
   - Extracted native Japanese titles and genre lists during AniList profile imports.
   - Resilient episode count handling for unreleased media (`NOT_YET_RELEASED` -> 0).
   - Validated MAL scrobbling IDs (`> 0`) preventing invalid requests.

10. **Title-Aware Library Sorting**
    - Library alphabetical sorting now honors the user's active title language preference (`English`, `Romaji`, or `Native`).

11. **Expanded Roman Numeral Season Detection**
    - Enhanced `SeasonLabeler` regex patterns to parse Roman numeral titles (e.g. `Mushoku Tensei II Part` -> Season 2) while safeguarding against false positives.

12. **Destructive Action Confirmation Safeguards**
    - Confirmation dialogs protect against accidental removal of anime from Watchlist or Favorites when `confirmDestructiveActions` is enabled.
    - Confirmation prompt before clearing all notifications.

13. **Notification Center Improvements**
    - Tapping notifications automatically marks them as read.
    - Visual surface highlighting for unread notification items.

14. **System Surface Theme Synchronization**
    - Replaced hardcoded surfaces across dialogs, sheets, and cards with `MaterialTheme.colorScheme.surface` and `surfaceVariant` for complete AMOLED and accent theme fidelity.
    - Splash screen respects `settings.reduceAnimations`.

15. **Web Showcase & Documentation Sync**
    - Updated web documentation app logic with `ACCENT_MAP` validation and ARIA state management.

16. **Comprehensive Local Test Suite**
    - Added `StreamResolverLocalTest` covering XOR decryption, provider cycling, and title/episode rules.
    - Extended tests for HLS parser resolution parsing and tie-breaking, library sorting with language preferences, and season labeling.

---

## 📦 Package Information & Checksums

| Asset | File Name | Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| **Release APK** | `zenith-anime-latest.apk` | ~30.1 MB | `9EF0BEE46C0C7B4ED42D9FB7EA9D3FE76AB394DE720144CA461884F38BC11A25` |
| **Debug APK** | `app-debug.apk` | ~30.1 MB | `9EF0BEE46C0C7B4ED42D9FB7EA9D3FE76AB394DE720144CA461884F38BC11A25` |

---

## 📱 Installation

1. Download `zenith-anime-latest.apk` on your Android device (Android 8.0+).
2. Allow installation from unknown sources if prompted.
3. Open Zenith Anime and enjoy ad-free streaming!

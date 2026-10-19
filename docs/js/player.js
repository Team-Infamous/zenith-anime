/**
 * Zenith Anime Web — Clean Native Video Player & Watch Tracking Engine
 * Direct un-hijacked iframe video stream embedding with instant multi-server switching,
 * Sub/Dub audio toggle, episode navigation, and continuous watch history & progress tracking.
 */

(function (window) {
  'use strict';

  const Player = {
    currentAnime: null,
    currentEpisode: 1,
    currentServer: 'aniembed',
    currentLanguage: 'sub',
    timer: null,
    playbackSeconds: 0,
    estimatedDuration: 1440, // Standard anime broadcast: ~24 minutes in seconds

    init() {
      // 100% native interaction — zero global keyboard or click intercepting.
    },

    /**
     * Load and mount an anime episode
     */
    load(anime, episodeNumber = 1, options = {}) {
      this.currentAnime = anime;
      this.currentEpisode = parseInt(episodeNumber, 10) || 1;

      const settings = window.ZenithStorage?.getSettings() || {};
      this.currentServer = options.server || settings.defaultServer || 'aniembed';
      this.currentLanguage = options.language || settings.defaultAudio || 'sub';

      // Restore saved progress for this episode if previously watched
      const saved = window.ZenithStorage?.getProgress(anime.id, this.currentEpisode);
      this.playbackSeconds = saved ? (saved.currentTimeSec || 0) : 0;
      this.estimatedDuration = saved && saved.durationSec ? saved.durationSec : 1440;

      this.renderPlayer();
      this.startProgressTracking();
    },

    /**
     * Render the clean native video viewport and stream control bar
     */
    renderPlayer() {
      const container = document.getElementById('player-viewport-container');
      if (!container) return;

      const anime = this.currentAnime;
      const ep = this.currentEpisode;
      const server = this.currentServer;
      const lang = this.currentLanguage;

      const embedUrl = window.ZenithAPI.getStreamUrl(server, anime, ep, lang);
      const titleLang = window.ZenithStorage?.getSettings().titleLanguage || 'english';
      const displayTitle = (titleLang === 'romaji' ? anime.romajiTitle : anime.englishTitle) || anime.title;
      const totalEpisodes = anime.totalEpisodes || 24;

      container.className = 'player-viewport-container';
      container.innerHTML = `
        <div class="player-wrapper" id="player-wrapper">
          <!-- 100% Native Clean Iframe Embed (Zero Overlays, Direct Native Player Interaction) -->
          <div class="player-embed-box">
            <iframe 
              id="zenith-video-iframe"
              class="player-iframe"
              src="${embedUrl}"
              allow="autoplay; fullscreen; encrypted-media; picture-in-picture; screen-wake-lock"
              referrerpolicy="no-referrer"
              title="${displayTitle} - Episode ${ep}"
            ></iframe>
          </div>

          <!-- Non-Intrusive Server & Stream Control Bar (Directly Below Video) -->
          <div class="player-stream-bar" id="player-stream-bar">
            <!-- Server Switcher Pills -->
            <div class="stream-bar-group stream-servers-group">
              <span class="stream-bar-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                  <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                  <line x1="6" y1="6" x2="6.01" y2="6"></line>
                  <line x1="6" y1="18" x2="6.01" y2="18"></line>
                </svg>
                <span>Server</span>
              </span>
              <div class="stream-servers-list">
                ${window.ZenithAPI.getAvailableServers().map(s => `
                  <button type="button" class="server-pill-btn ${s.id === server ? 'active' : ''}" data-server="${s.id}" title="${s.note}">
                    <span class="server-dot"></span>
                    <span class="server-name">${s.name.replace(/ \(.*\)/, '')}</span>
                  </button>
                `).join('')}
              </div>
            </div>

            <!-- Audio & Transport Actions -->
            <div class="stream-bar-group stream-controls-group">
              <!-- Audio (Sub / Dub) Toggle -->
              <button type="button" class="stream-pill-btn audio-pill ${lang === 'dub' ? 'dub' : 'sub'}" id="player-btn-audio" title="Switch Audio Track (Sub / Dub)">
                <span class="audio-badge">${lang.toUpperCase()}</span>
                <span>Audio</span>
              </button>

              <!-- Previous & Next Episode Navigation -->
              <div class="ep-nav-group">
                <button type="button" class="stream-ctrl-btn" id="ctrl-prev" ${ep <= 1 ? 'disabled' : ''} title="Previous Episode">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
                  </svg>
                  <span>Prev</span>
                </button>

                <button type="button" class="stream-ctrl-btn" id="ctrl-next" ${ep >= (totalEpisodes > 0 ? totalEpisodes : 999) ? 'disabled' : ''} title="Next Episode">
                  <span>Next</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/>
                  </svg>
                </button>
              </div>

              <!-- Reload Stream Button -->
              <button type="button" class="stream-ctrl-btn" id="player-btn-reload" title="Reload Video Stream">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 4 23 10 17 10"></polyline>
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                </svg>
                <span>Reload</span>
              </button>
            </div>
          </div>
        </div>
      `;

      this.bindPlayerEvents();
    },

    getServerDisplayName(serverId) {
      const match = window.ZenithAPI.getAvailableServers().find(s => s.id === serverId);
      return match ? match.name.split(' ')[0] : 'Server';
    },

    /**
     * Wire up server switching, audio toggle, and episode controls
     */
    bindPlayerEvents() {
      // 1. Server switcher pills
      document.querySelectorAll('.server-pill-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const s = btn.getAttribute('data-server');
          if (s && s !== this.currentServer) {
            this.currentServer = s;
            window.ZenithStorage?.updateSettings({ defaultServer: s });
            const iframe = document.getElementById('zenith-video-iframe');
            if (iframe) {
              iframe.src = window.ZenithAPI.getStreamUrl(s, this.currentAnime, this.currentEpisode, this.currentLanguage);
            }
            document.querySelectorAll('.server-pill-btn').forEach(b => {
              b.classList.toggle('active', b.getAttribute('data-server') === s);
            });
            const displayName = this.getServerDisplayName(s);
            window.ZenithUI?.showToast(`Switched server to ${displayName}`);
          }
        });
      });

      // 2. Sub / Dub audio toggle
      document.getElementById('player-btn-audio')?.addEventListener('click', () => {
        this.currentLanguage = this.currentLanguage === 'sub' ? 'dub' : 'sub';
        window.ZenithStorage?.updateSettings({ defaultAudio: this.currentLanguage });
        const iframe = document.getElementById('zenith-video-iframe');
        if (iframe) {
          iframe.src = window.ZenithAPI.getStreamUrl(this.currentServer, this.currentAnime, this.currentEpisode, this.currentLanguage);
        }
        const btn = document.getElementById('player-btn-audio');
        if (btn) {
          btn.className = `stream-pill-btn audio-pill ${this.currentLanguage === 'dub' ? 'dub' : 'sub'}`;
          const badge = btn.querySelector('.audio-badge');
          if (badge) badge.textContent = this.currentLanguage.toUpperCase();
        }
        window.ZenithUI?.showToast(`Switched audio to ${this.currentLanguage.toUpperCase()}`);
      });

      // 3. Reload stream button
      document.getElementById('player-btn-reload')?.addEventListener('click', () => {
        const iframe = document.getElementById('zenith-video-iframe');
        if (iframe) {
          const currentSrc = iframe.src;
          iframe.src = '';
          setTimeout(() => {
            iframe.src = currentSrc;
          }, 80);
          window.ZenithUI?.showToast('Reloading video stream...');
        }
      });

      // 4. Previous / Next episode navigation via hash change
      document.getElementById('ctrl-prev')?.addEventListener('click', () => {
        if (this.currentEpisode > 1) {
          window.location.hash = `#watch/${this.currentAnime.id}/${this.currentEpisode - 1}`;
        }
      });

      document.getElementById('ctrl-next')?.addEventListener('click', () => {
        const total = this.currentAnime?.totalEpisodes || 999;
        if (this.currentEpisode < total) {
          window.location.hash = `#watch/${this.currentAnime.id}/${this.currentEpisode + 1}`;
        }
      });
    },

    // =========================================================================
    // CONTINUOUS WATCH TIME, HISTORY & PROGRESS TRACKING
    // =========================================================================

    /**
     * Start background progress tracking for watch history and elapsed time
     */
    startProgressTracking() {
      this.stopProgressTracking();
      // Record initial visit immediately into storage (History & Continue Watching)
      this.saveCurrentProgress();

      // Silent background ticker: increments watch time and persists progress
      this.timer = setInterval(() => {
        this.playbackSeconds += 1;

        // Auto-persist every 5 seconds
        if (this.playbackSeconds % 5 === 0) {
          this.saveCurrentProgress();
        }
      }, 1000);
    },

    /**
     * Stop tracking interval and persist final watch progress
     */
    stopProgressTracking() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
      this.saveCurrentProgress();
    },

    /**
     * Persist current watch progress to ZenithStorage
     */
    saveCurrentProgress() {
      if (!this.currentAnime || !this.currentEpisode) return;

      const duration = this.estimatedDuration || 1440;
      const isCompleted = duration > 0 && (this.playbackSeconds / duration) >= 0.88;

      window.ZenithStorage?.saveProgress({
        animeId: this.currentAnime.id,
        animeTitle: this.currentAnime.title,
        episodeNumber: this.currentEpisode,
        coverImage: this.currentAnime.coverImage,
        bannerImage: this.currentAnime.bannerImage,
        currentTimeSec: this.playbackSeconds,
        durationSec: duration,
        completed: isCompleted,
        language: this.currentLanguage
      });
    }
  };

  window.ZenithPlayer = Player;
})(window);

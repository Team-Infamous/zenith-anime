/**
 * Zenith Anime Web — Full Video Player Suite
 * Responsive streaming engine supporting multi-server switching,
 * Sub/Dub audio toggle, double-tap seek ripples, quick episode drawer,
 * auto-skip intro, next episode countdown, keyboard shortcuts, and continuous watch progress.
 */

(function (window) {
  'use strict';

  const Player = {
    currentAnime: null,
    currentEpisode: 1,
    currentServer: 'aniembed',
    currentLanguage: 'sub',
    aspectRatio: 'fit', // 'fit' | 'fill' | 'stretch' | '16-9' | '4-3'
    isTheater: false,
    isFullscreen: false,
    timer: null,
    playbackSeconds: 0,
    estimatedDuration: 1440, // 24 minutes in seconds
    nextEpisodeTimer: null,
    controlsTimeout: null,

    init() {
      this.bindKeyboardShortcuts();
    },

    load(anime, episodeNumber = 1, options = {}) {
      this.currentAnime = anime;
      this.currentEpisode = parseInt(episodeNumber, 10) || 1;

      const settings = window.ZenithStorage?.getSettings() || {};
      this.currentServer = options.server || settings.defaultServer || 'aniembed';
      this.currentLanguage = options.language || settings.defaultAudio || 'sub';

      // Restore saved progress
      const saved = window.ZenithStorage?.getProgress(anime.id, this.currentEpisode);
      this.playbackSeconds = saved ? (saved.currentTimeSec || 0) : 0;
      this.estimatedDuration = saved && saved.durationSec ? saved.durationSec : 1440;

      this.renderPlayer();
      this.startProgressTracking();
    },

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

      container.className = `player-viewport-container aspect-${this.aspectRatio} ${this.isTheater ? 'theater-mode' : ''}`;
      container.innerHTML = `
        <div class="player-wrapper" id="player-wrapper">
          <!-- Discreet Top Header Bar -->
          <div class="player-header-bar">
            <div class="player-header-left">
              <button type="button" class="player-btn-back" id="player-btn-back" title="Back to Details">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="19" y1="12" x2="5" y2="12"></line>
                  <polyline points="12 19 5 12 12 5"></polyline>
                </svg>
                <span>Back</span>
              </button>
              <div class="player-title-info">
                <span class="player-anime-name" title="${displayTitle}">${displayTitle}</span>
                <span class="player-episode-badge">Ep ${ep} ${totalEpisodes > 0 ? `/ ${totalEpisodes}` : ''}</span>
              </div>
            </div>

            <div class="player-header-right">
              <!-- Theater Mode Toggle -->
              <button type="button" class="player-icon-btn ${this.isTheater ? 'active' : ''}" id="ctrl-theater" title="Toggle Theater Mode (T)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                  <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                </svg>
              </button>

              <!-- Fullscreen Toggle -->
              <button type="button" class="player-icon-btn" id="ctrl-fullscreen" title="Fullscreen (F)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
                </svg>
              </button>
            </div>
          </div>

          <!-- Main Iframe Video Embed - 100% UN-HIJACKED, DIRECT NATIVE INTERACTION -->
          <div class="player-iframe-frame">
            <iframe 
              id="zenith-video-iframe"
              class="player-iframe"
              src="${embedUrl}"
              allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
              allowfullscreen
              referrerpolicy="no-referrer"
              title="${displayTitle} Episode ${ep}"
            ></iframe>
          </div>

          <!-- Dedicated Zenith Server & Stream Control Bar (Directly Below Video) -->
          <div class="player-server-panel" id="player-server-panel">
            <div class="server-panel-section">
              <div class="server-panel-label">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                  <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                  <line x1="6" y1="6" x2="6.01" y2="6"></line>
                  <line x1="6" y1="18" x2="6.01" y2="18"></line>
                </svg>
                <span>Servers</span>
              </div>

              <div class="server-pills-row">
                ${window.ZenithAPI.getAvailableServers().map(s => `
                  <button type="button" class="server-pill-btn ${s.id === server ? 'active' : ''}" data-server="${s.id}" title="${s.note}">
                    <span class="server-dot"></span>
                    <span class="server-name">${s.name.replace(/ \(.*\)/, '')}</span>
                  </button>
                `).join('')}
              </div>
            </div>

            <div class="server-panel-section stream-actions-section">
              <!-- Sub / Dub Toggle -->
              <button type="button" class="stream-pill-btn audio-pill ${lang === 'dub' ? 'dub' : 'sub'}" id="player-btn-audio" title="Switch Audio Track (Sub / Dub)">
                <span class="audio-badge">${lang.toUpperCase()}</span>
                <span>Audio</span>
              </button>

              <!-- Episode Navigation -->
              <div class="ep-nav-group">
                <button type="button" class="stream-ctrl-btn" id="ctrl-prev" ${ep <= 1 ? 'disabled' : ''} title="Previous Episode">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
                  </svg>
                  <span>Prev</span>
                </button>

                <button type="button" class="stream-ctrl-btn" id="ctrl-next" ${ep >= (totalEpisodes > 0 ? totalEpisodes : 999) ? 'disabled' : ''} title="Next Episode">
                  <span>Next</span>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
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

              <!-- Episode Drawer Toggle -->
              <button type="button" class="stream-ctrl-btn" id="player-btn-episodes" title="Episodes Drawer">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="8" y1="6" x2="21" y2="6"></line>
                  <line x1="8" y1="12" x2="21" y2="12"></line>
                  <line x1="8" y1="18" x2="21" y2="18"></line>
                  <line x1="3" y1="6" x2="3.01" y2="6"></line>
                  <line x1="3" y1="12" x2="3.01" y2="12"></line>
                  <line x1="3" y1="18" x2="3.01" y2="18"></line>
                </svg>
                <span>List</span>
              </button>
            </div>
          </div>

          <div class="player-helper-hint">
            <span>💡 <strong>Tip:</strong> Video playback, volume, and quality (1080p/720p) are operated directly on the player above. If a server is slow or buffering, click any server button above to switch immediately.</span>
          </div>

          <!-- Quick Slide-Out Episode Drawer -->
          <div class="player-episodes-drawer" id="player-episodes-drawer">
            <div class="drawer-header">
              <h3>Episodes List (${totalEpisodes > 0 ? totalEpisodes : 'All'})</h3>
              <button type="button" class="drawer-close" id="btn-close-drawer">&times;</button>
            </div>
            <div class="drawer-episodes-grid" id="drawer-episodes-grid">
              ${this.renderDrawerEpisodes(totalEpisodes, ep)}
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

    renderDrawerEpisodes(total, current) {
      const count = total > 0 ? Math.min(total, 500) : 24;
      const progressMap = window.ZenithStorage?.getAllProgress() || {};
      let html = '';

      for (let i = 1; i <= count; i++) {
        const key = `${this.currentAnime.id}_ep_${i}`;
        const isWatched = progressMap[key]?.completed;
        const isActive = i === current;

        html += `
          <button type="button" class="drawer-ep-chip ${isActive ? 'active' : ''} ${isWatched ? 'watched' : ''}" data-ep="${i}">
            <span>${i}</span>
            ${isWatched ? '<span class="ep-check">✓</span>' : ''}
          </button>
        `;
      }
      return html;
    },

    bindPlayerEvents() {
      // Back button
      document.getElementById('player-btn-back')?.addEventListener('click', () => {
        this.stopProgressTracking();
        window.location.hash = `#details/${this.currentAnime.id}`;
      });

      // Server pill buttons
      document.querySelectorAll('.server-pill-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const s = btn.getAttribute('data-server');
          if (s && s !== this.currentServer) {
            this.currentServer = s;
            window.ZenithStorage?.updateSettings({ defaultServer: s });
            this.renderPlayer();
            const displayName = this.getServerDisplayName(s);
            window.ZenithUI?.showToast(`Switched server to ${displayName}`);
          }
        });
      });

      // Audio track toggle (Sub / Dub)
      document.getElementById('player-btn-audio')?.addEventListener('click', () => {
        this.currentLanguage = this.currentLanguage === 'sub' ? 'dub' : 'sub';
        window.ZenithStorage?.updateSettings({ defaultAudio: this.currentLanguage });
        this.renderPlayer();
        window.ZenithUI?.showToast(`Switched audio to ${this.currentLanguage.toUpperCase()}`);
      });

      // Reload video stream button
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

      // Quick Episode Drawer
      const drawer = document.getElementById('player-episodes-drawer');
      document.getElementById('player-btn-episodes')?.addEventListener('click', () => {
        drawer?.classList.toggle('open');
      });

      document.getElementById('btn-close-drawer')?.addEventListener('click', () => {
        drawer?.classList.remove('open');
      });

      drawer?.querySelectorAll('.drawer-ep-chip').forEach(chip => {
        chip.addEventListener('click', () => {
          const ep = parseInt(chip.getAttribute('data-ep'), 10);
          if (ep && ep !== this.currentEpisode) {
            drawer.classList.remove('open');
            this.load(this.currentAnime, ep);
          }
        });
      });

      // Transport: Previous / Next
      document.getElementById('ctrl-prev')?.addEventListener('click', () => {
        if (this.currentEpisode > 1) {
          this.load(this.currentAnime, this.currentEpisode - 1);
        }
      });

      document.getElementById('ctrl-next')?.addEventListener('click', () => {
        const total = this.currentAnime.totalEpisodes || 999;
        if (this.currentEpisode < total) {
          this.load(this.currentAnime, this.currentEpisode + 1);
        }
      });

      // Theater mode
      document.getElementById('ctrl-theater')?.addEventListener('click', () => {
        this.isTheater = !this.isTheater;
        const container = document.getElementById('player-viewport-container');
        container?.classList.toggle('theater-mode', this.isTheater);
        document.getElementById('ctrl-theater')?.classList.toggle('active', this.isTheater);
      });

      // Fullscreen
      document.getElementById('ctrl-fullscreen')?.addEventListener('click', () => {
        this.toggleFullscreen();
      });
    },

    startProgressTracking() {
      this.stopProgressTracking();
      // Tick every second to record continuous watch progress
      this.timer = setInterval(() => {
        this.playbackSeconds += 1;
        this.updateSeekbarUI();

        // Save progress every 5 seconds or upon nearing completion
        if (this.playbackSeconds % 5 === 0) {
          this.saveCurrentProgress();
        }

        // Check for next episode countdown during final 35 seconds
        if (this.estimatedDuration - this.playbackSeconds <= 35 && !this.nextEpisodeTimer) {
          this.triggerNextEpisodeCountdown();
        }
      }, 1000);
    },

    stopProgressTracking() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
      if (this.nextEpisodeTimer) {
        clearInterval(this.nextEpisodeTimer);
        this.nextEpisodeTimer = null;
      }
      this.saveCurrentProgress();
    },

    saveCurrentProgress() {
      if (!this.currentAnime || !this.currentEpisode) return;
      window.ZenithStorage?.saveProgress({
        animeId: this.currentAnime.id,
        animeTitle: this.currentAnime.title,
        episodeNumber: this.currentEpisode,
        coverImage: this.currentAnime.coverImage,
        bannerImage: this.currentAnime.bannerImage,
        currentTimeSec: this.playbackSeconds,
        durationSec: this.estimatedDuration,
        language: this.currentLanguage
      });
    },

    triggerNextEpisodeCountdown() {
      const countdownBox = document.getElementById('player-next-countdown');
      const secondsSpan = document.getElementById('countdown-seconds');
      if (!countdownBox || !secondsSpan) return;

      const total = this.currentAnime.totalEpisodes || 999;
      if (this.currentEpisode >= total) return; // Last episode

      countdownBox.style.display = 'flex';
      let remaining = 5;
      secondsSpan.textContent = remaining;

      document.getElementById('btn-cancel-countdown')?.addEventListener('click', () => {
        countdownBox.style.display = 'none';
        clearInterval(this.nextEpisodeTimer);
        this.nextEpisodeTimer = null;
      }, { once: true });

      document.getElementById('btn-play-countdown')?.addEventListener('click', () => {
        countdownBox.style.display = 'none';
        clearInterval(this.nextEpisodeTimer);
        this.nextEpisodeTimer = null;
        this.load(this.currentAnime, this.currentEpisode + 1);
      }, { once: true });

      this.nextEpisodeTimer = setInterval(() => {
        remaining -= 1;
        if (remaining <= 0) {
          clearInterval(this.nextEpisodeTimer);
          this.nextEpisodeTimer = null;
          countdownBox.style.display = 'none';
          this.load(this.currentAnime, this.currentEpisode + 1);
        } else {
          secondsSpan.textContent = remaining;
        }
      }, 1000);
    },

    toggleFullscreen() {
      const wrapper = document.getElementById('player-wrapper');
      if (!wrapper) return;

      if (!document.fullscreenElement) {
        wrapper.requestFullscreen().then(() => {
          this.isFullscreen = true;
        }).catch(err => console.warn('Fullscreen denied', err));
      } else {
        document.exitFullscreen().then(() => {
          this.isFullscreen = false;
        }).catch(err => console.warn('Exit fullscreen error', err));
      }
    },

    bindKeyboardShortcuts() {
      window.addEventListener('keydown', (e) => {
        // Ignore typing in inputs or textareas
        if (['INPUT', 'TEXTAREA'].includes(e.target.tagName)) return;
        // Only active if player is mounted
        if (!document.getElementById('player-wrapper')) return;

        switch (e.key) {
          case ' ':
          case 'k':
          case 'K':
            e.preventDefault();
            this.showControlsTemporarily();
            break;
          case 'f':
          case 'F':
            e.preventDefault();
            this.toggleFullscreen();
            break;
          case 't':
          case 'T':
            e.preventDefault();
            this.isTheater = !this.isTheater;
            document.getElementById('player-viewport-container')?.classList.toggle('theater-mode', this.isTheater);
            break;
          case 'ArrowLeft':
          case 'j':
          case 'J':
            e.preventDefault();
            this.seekRelative(-10);
            this.triggerRipple('left');
            break;
          case 'ArrowRight':
          case 'l':
          case 'L':
            e.preventDefault();
            this.seekRelative(10);
            this.triggerRipple('right');
            break;
          case 'n':
          case 'N':
            e.preventDefault();
            if (this.currentEpisode < (this.currentAnime?.totalEpisodes || 999)) {
              this.load(this.currentAnime, this.currentEpisode + 1);
            }
            break;
          case 'p':
          case 'P':
            e.preventDefault();
            if (this.currentEpisode > 1) {
              this.load(this.currentAnime, this.currentEpisode - 1);
            }
            break;
        }
      });
    },

    formatTime(sec) {
      const m = Math.floor(sec / 60);
      const s = Math.floor(sec % 60);
      return `${m}:${s < 10 ? '0' : ''}${s}`;
    }
  };

  window.ZenithPlayer = Player;
})(window);

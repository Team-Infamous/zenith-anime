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
          <!-- Main Iframe Video Embed -->
          <iframe 
            id="zenith-video-iframe"
            class="player-iframe"
            src="${embedUrl}"
            allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
            allowfullscreen
            referrerpolicy="no-referrer"
            title="${displayTitle} Episode ${ep}"
          ></iframe>

          <!-- Touch Gesture Seeking Tap Zones -->
          <div class="player-touch-zone left" id="touch-seek-left" aria-label="Rewind 10 seconds">
            <div class="seek-ripple-badge" id="ripple-left">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z"/>
              </svg>
              <span>-10s</span>
            </div>
          </div>
          <div class="player-touch-zone right" id="touch-seek-right" aria-label="Fast forward 10 seconds">
            <div class="seek-ripple-badge" id="ripple-right">
              <span>+10s</span>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z"/>
              </svg>
            </div>
          </div>

          <!-- In-Player Quick HUD Overlay (Top & Bottom) -->
          <div class="player-hud-overlay" id="player-hud-overlay">
            <!-- Top Bar -->
            <div class="player-hud-top">
              <button type="button" class="player-btn-back" id="player-btn-back" title="Back to Details">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="19" y1="12" x2="5" y2="12"></line>
                  <polyline points="12 19 5 12 12 5"></polyline>
                </svg>
              </button>
              <div class="player-title-info">
                <span class="player-anime-name">${displayTitle}</span>
                <span class="player-episode-badge">Episode ${ep} of ${totalEpisodes > 0 ? totalEpisodes : '—'}</span>
              </div>
              <div class="player-top-actions">
                <!-- Server Selector -->
                <div class="player-dropdown-wrap">
                  <button type="button" class="player-pill-btn" id="player-btn-servers">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                      <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                      <line x1="6" y1="6" x2="6.01" y2="6"></line>
                      <line x1="6" y1="18" x2="6.01" y2="18"></line>
                    </svg>
                    <span>${this.getServerDisplayName(server)}</span>
                  </button>
                  <div class="player-dropdown-menu" id="player-server-menu">
                    ${window.ZenithAPI.getAvailableServers().map(s => `
                      <button type="button" class="player-menu-item ${s.id === server ? 'active' : ''}" data-server="${s.id}">
                        <strong>${s.name}</strong>
                        <small>${s.note}</small>
                      </button>
                    `).join('')}
                  </div>
                </div>

                <!-- Sub / Dub Toggle -->
                <button type="button" class="player-pill-btn ${lang === 'dub' ? 'dub-active' : ''}" id="player-btn-audio" title="Switch Audio Track">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                    <line x1="12" y1="19" x2="12" y2="23"></line>
                    <line x1="8" y1="23" x2="16" y2="23"></line>
                  </svg>
                  <span>${lang.toUpperCase()}</span>
                </button>

                <!-- Episode Drawer Toggle -->
                <button type="button" class="player-pill-btn" id="player-btn-episodes" title="Episodes Drawer">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="8" y1="6" x2="21" y2="6"></line>
                    <line x1="8" y1="12" x2="21" y2="12"></line>
                    <line x1="8" y1="18" x2="21" y2="18"></line>
                    <line x1="3" y1="6" x2="3.01" y2="6"></line>
                    <line x1="3" y1="12" x2="3.01" y2="12"></line>
                    <line x1="3" y1="18" x2="3.01" y2="18"></line>
                  </svg>
                  <span>Episodes</span>
                </button>
              </div>
            </div>

            <!-- Auto-Skip Opening Banner (Instant 85s Jump) -->
            <button type="button" class="player-skip-op-btn" id="player-btn-skip-op" title="Skip Intro (+85s)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z"/>
              </svg>
              <span>Skip Opening (+85s)</span>
            </button>

            <!-- Bottom Floating Transport Strip -->
            <div class="player-hud-bottom">
              <!-- Seekbar Slider & Timers -->
              <div class="player-seekbar-row">
                <span class="player-time-text" id="player-current-time">${this.formatTime(this.playbackSeconds)}</span>
                <div class="player-seekbar-track" id="player-seekbar-track">
                  <div class="player-seekbar-fill" id="player-seekbar-fill" style="width: ${(this.playbackSeconds / this.estimatedDuration) * 100}%"></div>
                  <div class="player-seekbar-thumb" id="player-seekbar-thumb" style="left: ${(this.playbackSeconds / this.estimatedDuration) * 100}%"></div>
                </div>
                <span class="player-time-text" id="player-duration-time">${this.formatTime(this.estimatedDuration)}</span>
              </div>

              <!-- Action Buttons Row -->
              <div class="player-controls-row">
                <div class="player-controls-left">
                  <button type="button" class="player-ctrl-btn" id="ctrl-prev" ${ep <= 1 ? 'disabled' : ''} title="Previous Episode (P)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
                    </svg>
                  </button>
                  <button type="button" class="player-ctrl-btn" id="ctrl-rewind" title="Rewind 10s (Left Arrow)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z"/>
                    </svg>
                  </button>
                  <button type="button" class="player-ctrl-btn" id="ctrl-forward" title="Fast Forward 10s (Right Arrow)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z"/>
                    </svg>
                  </button>
                  <button type="button" class="player-ctrl-btn" id="ctrl-next" ${totalEpisodes > 0 && ep >= totalEpisodes ? 'disabled' : ''} title="Next Episode (N)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/>
                    </svg>
                  </button>
                </div>

                <div class="player-controls-right">
                  <!-- Aspect Ratio Selector -->
                  <button type="button" class="player-ctrl-btn" id="ctrl-aspect" title="Aspect Ratio (${this.aspectRatio.toUpperCase()})">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                      <line x1="8" y1="4" x2="8" y2="8"></line>
                      <line x1="16" y1="4" x2="16" y2="8"></line>
                    </svg>
                  </button>

                  <!-- Theater Mode Toggle -->
                  <button type="button" class="player-ctrl-btn" id="ctrl-theater" title="Toggle Theater Mode (T)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                      <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                    </svg>
                  </button>

                  <!-- Fullscreen Toggle -->
                  <button type="button" class="player-ctrl-btn" id="ctrl-fullscreen" title="Fullscreen (F)">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Next Episode Countdown Banner Overlay -->
          <div class="player-next-countdown" id="player-next-countdown" style="display: none;">
            <div class="countdown-card">
              <div class="countdown-text">
                <strong>Next Episode Starting Soon</strong>
                <span>Episode ${ep + 1} airs in <span id="countdown-seconds">5</span>s</span>
              </div>
              <div class="countdown-actions">
                <button type="button" class="btn btn-secondary btn-sm" id="btn-cancel-countdown">Cancel</button>
                <button type="button" class="btn btn-primary btn-sm" id="btn-play-countdown">Play Now</button>
              </div>
            </div>
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
      const wrapper = document.getElementById('player-wrapper');
      const hud = document.getElementById('player-hud-overlay');
      const serverMenu = document.getElementById('player-server-menu');

      // Back button
      document.getElementById('player-btn-back')?.addEventListener('click', () => {
        this.stopProgressTracking();
        window.location.hash = `#details/${this.currentAnime.id}`;
      });

      // Server toggle & selection
      document.getElementById('player-btn-servers')?.addEventListener('click', (e) => {
        e.stopPropagation();
        serverMenu?.classList.toggle('show');
      });

      serverMenu?.querySelectorAll('.player-menu-item').forEach(item => {
        item.addEventListener('click', () => {
          const s = item.getAttribute('data-server');
          if (s) {
            this.currentServer = s;
            window.ZenithStorage?.updateSettings({ defaultServer: s });
            this.renderPlayer();
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

      // Skip Opening (+85 seconds)
      document.getElementById('player-btn-skip-op')?.addEventListener('click', () => {
        this.seekRelative(85);
        window.ZenithUI?.showToast('Skipped opening (+85s)');
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

      // Rewind / Forward 10s
      document.getElementById('ctrl-rewind')?.addEventListener('click', () => this.seekRelative(-10));
      document.getElementById('ctrl-forward')?.addEventListener('click', () => this.seekRelative(10));

      // Touch zone double-taps
      let lastTapLeft = 0;
      document.getElementById('touch-seek-left')?.addEventListener('click', () => {
        const now = Date.now();
        if (now - lastTapLeft < 350) {
          this.seekRelative(-10);
          this.triggerRipple('left');
        }
        lastTapLeft = now;
      });

      let lastTapRight = 0;
      document.getElementById('touch-seek-right')?.addEventListener('click', () => {
        const now = Date.now();
        if (now - lastTapRight < 350) {
          this.seekRelative(10);
          this.triggerRipple('right');
        }
        lastTapRight = now;
      });

      // Aspect Ratio mode switcher
      document.getElementById('ctrl-aspect')?.addEventListener('click', () => {
        const modes = ['fit', 'fill', 'stretch', '16-9', '4-3'];
        const nextIdx = (modes.indexOf(this.aspectRatio) + 1) % modes.length;
        this.aspectRatio = modes[nextIdx];
        const container = document.getElementById('player-viewport-container');
        if (container) {
          container.className = `player-viewport-container aspect-${this.aspectRatio} ${this.isTheater ? 'theater-mode' : ''}`;
        }
        window.ZenithUI?.showToast(`Aspect: ${this.aspectRatio.toUpperCase()}`);
      });

      // Theater mode
      document.getElementById('ctrl-theater')?.addEventListener('click', () => {
        this.isTheater = !this.isTheater;
        const container = document.getElementById('player-viewport-container');
        container?.classList.toggle('theater-mode', this.isTheater);
      });

      // Fullscreen
      document.getElementById('ctrl-fullscreen')?.addEventListener('click', () => {
        this.toggleFullscreen();
      });

      // Seekbar click / drag
      const seekbarTrack = document.getElementById('player-seekbar-track');
      seekbarTrack?.addEventListener('click', (e) => {
        const rect = seekbarTrack.getBoundingClientRect();
        const percent = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
        this.playbackSeconds = Math.floor(percent * this.estimatedDuration);
        this.updateSeekbarUI();
        this.saveCurrentProgress();
      });

      // Autohide controls on mouse idle
      wrapper?.addEventListener('mousemove', () => this.showControlsTemporarily());
      wrapper?.addEventListener('click', () => this.showControlsTemporarily());

      // Close popups on outside click
      document.addEventListener('click', (e) => {
        if (!e.target.closest('#player-btn-servers')) {
          serverMenu?.classList.remove('show');
        }
      });
    },

    showControlsTemporarily() {
      const hud = document.getElementById('player-hud-overlay');
      if (!hud) return;
      hud.classList.add('visible');
      clearTimeout(this.controlsTimeout);
      this.controlsTimeout = setTimeout(() => {
        hud.classList.remove('visible');
      }, 3500);
    },

    triggerRipple(side) {
      const el = document.getElementById(`ripple-${side}`);
      if (!el) return;
      el.classList.add('active');
      setTimeout(() => el.classList.remove('active'), 600);
    },

    seekRelative(seconds) {
      this.playbackSeconds = Math.max(0, Math.min(this.estimatedDuration, this.playbackSeconds + seconds));
      this.updateSeekbarUI();
      this.saveCurrentProgress();
    },

    updateSeekbarUI() {
      const fill = document.getElementById('player-seekbar-fill');
      const thumb = document.getElementById('player-seekbar-thumb');
      const timeText = document.getElementById('player-current-time');
      const percent = (this.playbackSeconds / this.estimatedDuration) * 100;

      if (fill) fill.style.width = `${percent}%`;
      if (thumb) thumb.style.left = `${percent}%`;
      if (timeText) timeText.textContent = this.formatTime(this.playbackSeconds);
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

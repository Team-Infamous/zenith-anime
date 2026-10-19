/**
 * Zenith Anime Web — Main Application Controller & Router
 * SPA router, responsive bottom navigation dock, theme manager,
 * and keyboard navigation listeners.
 */

(function (window) {
  'use strict';

  const App = {
    currentRoute: 'home',

    init() {
      // 1. Apply user's saved accent theme
      const settings = window.ZenithStorage.getSettings();
      document.documentElement.setAttribute('data-accent', settings.accent || 'emerald');

      // 2. Initialize Player
      window.ZenithPlayer.init();

      // 3. Bind Global Navigation Events
      this.bindGlobalNavigation();

      // 4. Listen for URL Hash Changes
      window.addEventListener('hashchange', () => this.handleRoute());

      // 5. Initial Route Dispatch
      this.handleRoute();
    },

    bindGlobalNavigation() {
      // Header quick search
      const headerSearchInput = document.getElementById('header-search-input');
      const headerSearchForm = document.getElementById('header-search-form');

      headerSearchForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        const q = headerSearchInput?.value.trim();
        if (q) {
          window.location.hash = `#search?q=${encodeURIComponent(q)}`;
          if (headerSearchInput) headerSearchInput.value = '';
        }
      });

      // Header random anime roulette button
      document.getElementById('header-btn-random')?.addEventListener('click', async () => {
        window.ZenithUI?.showToast('Rolling random anime roulette...');
        const random = await window.ZenithAPI.getRandomAnime();
        if (random) {
          window.location.hash = `#details/${random.id}`;
        }
      });

      // Mobile Menu Drawer Toggle
      const mobileToggle = document.getElementById('mobile-menu-toggle');
      const mobileNav = document.getElementById('nav-links');
      mobileToggle?.addEventListener('click', () => {
        mobileNav?.classList.toggle('open');
      });

      mobileNav?.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', () => {
          mobileNav.classList.remove('open');
        });
      });

      // Bottom Floating Dock Nav Items
      document.querySelectorAll('.dock-nav-item').forEach(item => {
        item.addEventListener('click', () => {
          document.querySelectorAll('.dock-nav-item').forEach(i => i.classList.remove('active'));
          item.classList.add('active');
        });
      });
    },

    handleRoute() {
      const hash = window.location.hash || '#home';
      const cleanHash = hash.replace(/^#\/?/, '');
      const mainContainer = document.getElementById('app-main-content');
      if (!mainContainer) return;

      // Smooth view enter animation
      mainContainer.classList.remove('route-view-enter');
      void mainContainer.offsetWidth; // trigger reflow for smooth re-trigger
      mainContainer.classList.add('route-view-enter');

      // Scroll to top
      window.scrollTo({ top: 0, behavior: 'instant' });

      // Parse route & parameters
      const [routePart, queryPart] = cleanHash.split('?');
      const segments = routePart.split('/').filter(Boolean);
      const rootRoute = segments[0] || 'home';
      this.currentRoute = rootRoute;

      // Parse query params
      const queryParams = {};
      if (queryPart) {
        queryPart.split('&').forEach(param => {
          const [k, v] = param.split('=');
          if (k) queryParams[decodeURIComponent(k)] = decodeURIComponent(v || '');
        });
      }

      // Update Navigation Active States (Header & Bottom Dock)
      this.updateActiveNavIndicators(rootRoute);

      // Route Dispatcher
      switch (rootRoute) {
        case 'home':
          window.ZenithUI.renderHomeScreen(mainContainer);
          break;

        case 'search':
          window.ZenithUI.renderSearchScreen(mainContainer, queryParams);
          break;

        case 'details':
          const animeId = segments[1];
          if (animeId) {
            window.ZenithUI.renderDetailsScreen(mainContainer, animeId);
          } else {
            window.location.hash = '#home';
          }
          break;

        case 'watch':
          const watchAnimeId = segments[1];
          const watchEp = parseInt(segments[2], 10) || 1;
          if (watchAnimeId) {
            window.ZenithUI.renderWatchScreen(mainContainer, watchAnimeId, watchEp);
          } else {
            window.location.hash = '#home';
          }
          break;

        case 'schedule':
          window.ZenithUI.renderScheduleScreen(mainContainer);
          break;

        case 'mylist':
          window.ZenithUI.renderMyListScreen(mainContainer);
          break;

        case 'settings':
          window.ZenithUI.renderSettingsScreen(mainContainer);
          break;

        default:
          window.ZenithUI.renderHomeScreen(mainContainer);
          break;
      }
    },

    updateActiveNavIndicators(route) {
      // Top header nav links
      document.querySelectorAll('.nav-link').forEach(link => {
        const href = link.getAttribute('href') || '';
        const isMatch = href === `#${route}`;
        link.classList.toggle('active', isMatch);
      });

      // Bottom floating island dock
      document.querySelectorAll('.dock-nav-item').forEach(item => {
        const target = item.getAttribute('data-route');
        item.classList.toggle('active', target === route);
      });

      // Toggle floating dock visibility (hide in full watch mode if desired, or keep minimized)
      const dock = document.getElementById('mobile-dock-nav');
      if (dock) {
        dock.style.display = route === 'watch' ? 'none' : 'flex';
      }
    }
  };

  window.addEventListener('DOMContentLoaded', () => {
    App.init();
  });

  window.ZenithApp = App;
})(window);

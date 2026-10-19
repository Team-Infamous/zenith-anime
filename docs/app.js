/**
 * Zenith Anime — Interactive Web Client Script
 * Built with vanilla JavaScript. Modular, defensive, and accessible.
 */

(function () {
  "use strict";

  const ACCENT_MAP = {
    emerald: "#06c149",
    cyan: "#06b6d4",
    sakura: "#f43f5e",
    violet: "#8b5cf6",
    amber: "#f59e0b",
    crimson: "#ef4444",
    indigo: "#536dfe",
    gold: "#ffd54f"
  };

  // State Management
  const state = {
    accent: localStorage.getItem("zenith_accent") || "emerald",
    playground: {
      isLegacy: false,
      transportPosition: "center", // center, bottom, split
      seekbarStyle: "modern",     // modern, minimal, thick
      lockSide: "left",           // left, right
      overlayDim: 0.65,
      hiddenControls: new Set()
    }
  };

  // DOM Elements cache
  const elements = {
    html: document.documentElement,
    themeChips: document.querySelectorAll(".color-chip"),
    segButtons: document.querySelectorAll(".seg-btn"),
    toggles: document.querySelectorAll(".playground-toggle"),
    simulatorBox: document.getElementById("sim-screen"),
    simulatorLock: document.getElementById("sim-lock-icon"),
    simulatorTransportCenter: document.getElementById("sim-transport-center"),
    simulatorTransportBottom: document.getElementById("sim-transport-bottom"),
    simulatorSeekbar: document.getElementById("sim-seekbar"),
    simulatorTopIcons: document.querySelectorAll(".sim-top-icon"),
    mobileToggle: document.getElementById("mobile-menu-toggle"),
    navLinks: document.getElementById("nav-links"),
    modals: document.querySelectorAll(".modal-overlay"),
    toastContainer: document.getElementById("toast-container")
  };

  /** Initialize Application */
  function init() {
    applyTheme(state.accent);
    bindThemeEvents();
    bindPlaygroundEvents();
    bindModalEvents();
    bindMobileNav();
    updateSimulator();
  }

  /** Apply Theme Accent */
  function applyTheme(accentName) {
    const normalized = ACCENT_MAP[accentName] ? accentName : "emerald";
    state.accent = normalized;
    elements.html.setAttribute("data-accent", normalized);
    localStorage.setItem("zenith_accent", normalized);

    const dot = document.getElementById("theme-current-dot");
    if (dot) dot.style.backgroundColor = ACCENT_MAP[normalized] || '#10b981';

    // Update active chip state
    elements.themeChips.forEach((chip) => {
      const isTarget = chip.getAttribute("data-color") === normalized;
      chip.classList.toggle("active", isTarget);
      chip.setAttribute("aria-selected", isTarget ? "true" : "false");
      chip.setAttribute("aria-pressed", isTarget ? "true" : "false");
    });
  }

  /** Bind Theme Switcher Events */
  function bindThemeEvents() {
    const toggleBtn = document.getElementById("theme-dropdown-toggle");
    const dropdown = document.getElementById("theme-palette-dropdown");

    toggleBtn?.addEventListener("click", (e) => {
      e.stopPropagation();
      dropdown?.classList.toggle("show");
    });

    document.addEventListener("click", (e) => {
      if (!e.target.closest("#theme-dropdown-wrap")) {
        dropdown?.classList.remove("show");
      }
    });

    elements.themeChips.forEach((chip) => {
      chip.addEventListener("click", () => {
        const color = chip.getAttribute("data-color");
        if (color) {
          applyTheme(color);
          dropdown?.classList.remove("show");
        }
      });
    });
  }

  /** Bind Interactive Playground Controls */
  function bindPlaygroundEvents() {
    // Segmented Buttons (Transport position, Seekbar style, Lock side)
    elements.segButtons.forEach((btn) => {
      btn.addEventListener("click", () => {
        const group = btn.getAttribute("data-group");
        const val = btn.getAttribute("data-value");
        if (!group || !val) return;

        // Toggle active class inside the group
        const siblings = btn.parentElement?.querySelectorAll(".seg-btn");
        siblings?.forEach((sib) => sib.classList.remove("active"));
        btn.classList.add("active");

        if (group === "transport") {
          state.playground.transportPosition = val;
        } else if (group === "seekbar") {
          state.playground.seekbarStyle = val;
        } else if (group === "lockside") {
          state.playground.lockSide = val;
        } else if (group === "layout") {
          state.playground.isLegacy = val === "legacy";
        }

        updateSimulator();
      });
    });

    // Control Visibility Toggles
    elements.toggles.forEach((toggle) => {
      toggle.addEventListener("change", (e) => {
        const target = e.target;
        const controlId = target?.getAttribute("data-control");
        if (!controlId) return;

        if (target.checked) {
          state.playground.hiddenControls.delete(controlId);
        } else {
          state.playground.hiddenControls.add(controlId);
        }
        updateSimulator();
      });
    });
  }

  /** Update Simulator Mockup DOM based on state */
  function updateSimulator() {
    const p = state.playground;

    // Lock icon visibility & side
    if (elements.simulatorLock) {
      if (p.isLegacy || p.hiddenControls.has("lock")) {
        elements.simulatorLock.style.display = "none";
      } else {
        elements.simulatorLock.style.display = "flex";
        if (p.lockSide === "right") {
          elements.simulatorLock.style.left = "auto";
          elements.simulatorLock.style.right = "10px";
        } else {
          elements.simulatorLock.style.left = "10px";
          elements.simulatorLock.style.right = "auto";
        }
      }
    }

    // Transport position
    if (elements.simulatorTransportCenter && elements.simulatorTransportBottom) {
      if (p.transportPosition === "bottom") {
        elements.simulatorTransportCenter.style.display = "none";
        elements.simulatorTransportBottom.style.display = "flex";
      } else if (p.transportPosition === "split") {
        elements.simulatorTransportCenter.style.display = "flex";
        elements.simulatorTransportBottom.style.display = "flex";
      } else {
        // center
        elements.simulatorTransportCenter.style.display = "flex";
        elements.simulatorTransportBottom.style.display = "none";
      }
    }

    // Seekbar thickness
    if (elements.simulatorSeekbar) {
      let height = "4px";
      if (p.seekbarStyle === "minimal") height = "2px";
      if (p.seekbarStyle === "thick") height = "6px";
      elements.simulatorSeekbar.style.height = height;
    }

    // Top control icons visibility
    elements.simulatorTopIcons.forEach((icon) => {
      const id = icon.getAttribute("data-id");
      if (id && p.hiddenControls.has(id)) {
        icon.style.display = "none";
      } else {
        icon.style.display = "inline-flex";
      }
    });
  }

  /** Modal Dialog Management */
  function bindModalEvents() {
    // Open trigger
    document.querySelectorAll("[data-open-modal]").forEach((trigger) => {
      trigger.addEventListener("click", (e) => {
        e.preventDefault();
        const modalId = trigger.getAttribute("data-open-modal");
        if (modalId) openModal(modalId);
      });
    });

    // Close buttons & backdrop click
    elements.modals.forEach((modal) => {
      modal.addEventListener("click", (e) => {
        if (e.target === modal) closeModal(modal);
      });

      modal.querySelectorAll(".modal-close-btn").forEach((btn) => {
        btn.addEventListener("click", () => closeModal(modal));
      });
    });

    // Escape key listener
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        elements.modals.forEach((m) => closeModal(m));
      }
    });
  }

  function openModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    const closeBtn = modal.querySelector(".modal-close-btn");
    closeBtn?.focus();
  }

  function closeModal(modal) {
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
  }

  /** Mobile Navigation Drawer */
  function bindMobileNav() {
    if (!elements.mobileToggle || !elements.navLinks) return;

    elements.mobileToggle.addEventListener("click", () => {
      const isExpanded = elements.mobileToggle.getAttribute("aria-expanded") === "true";
      elements.mobileToggle.setAttribute("aria-expanded", isExpanded ? "false" : "true");
      elements.navLinks.classList.toggle("mobile-open");
    });
  }

  /** Show Toast Feedback */
  window.showToast = function (message) {
    let container = document.getElementById("toast-container");
    if (!container) {
      container = document.createElement("div");
      container.id = "toast-container";
      container.style.cssText = "position:fixed;bottom:24px;right:24px;z-index:9999;display:flex;flex-direction:column;gap:8px;";
      document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.style.cssText = "background:#121722;color:#f0f3f8;border:1px solid #222d42;padding:10px 16px;border-radius:8px;font-size:0.88rem;box-shadow:0 4px 12px rgba(0,0,0,0.4);display:flex;align-items:center;gap:8px;";
    toast.textContent = message;

    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transition = "opacity 200ms ease";
      setTimeout(() => toast.remove(), 200);
    }, 2800);
  };

  /** Copy text helper */
  window.copyToClipboard = function (text, message) {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(() => {
        window.showToast(message || "Copied to clipboard!");
      });
    }
  };

  // Run on DOM Ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();

package com.example.ui.player

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

object EmbedHijack {

    private const val TAG = "ZenithHijack"
    private const val MAX_HTML_CHARS = 6 * 1024 * 1024
    private const val EMBED_MARKER = "__zenith_embed=1"
    private const val FALLBACK_UA =
        "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val forwardedHeaders = setOf(
        "accept",
        "accept-language",
        "user-agent",
        "referer",
        "origin",
        "cookie",
        "sec-fetch-dest",
        "sec-fetch-mode",
        "sec-fetch-site",
        "sec-fetch-user",
        "sec-ch-ua",
        "sec-ch-ua-mobile",
        "sec-ch-ua-platform",
        "priority",
        "dnt"
    )

    private val headTag = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun looksLikeDocument(request: WebResourceRequest): Boolean {
        val headers = request.requestHeaders
        val destination = headers.entries
            .firstOrNull { it.key.equals("Sec-Fetch-Dest", ignoreCase = true) }
            ?.value
            ?.lowercase()
        when (destination) {
            "document", "iframe", "frame" -> return true
            "script", "style", "image", "video", "audio", "font", "empty", "object",
            "embed", "manifest", "worker", "sharedworker", "track" -> return false
        }
        val accept = headers.entries
            .firstOrNull { it.key.equals("Accept", ignoreCase = true) }
            ?.value
            ?.lowercase()
            ?: ""
        if (accept.contains("text/html")) return true
        return request.isForMainFrame
    }

    fun rewrite(
        url: String,
        requestHeaders: Map<String, String>,
        isMainFrame: Boolean,
        wrapMainFrame: Boolean
    ): WebResourceResponse? {
        if (url.contains(EMBED_MARKER)) {
            val inner = stripMarker(url)
            Log.i(TAG, "marker inner=" + inner)
            return serveDocument(inner, requestHeaders)
        }
        if (isMainFrame && wrapMainFrame) {
            return serveWrapper(url)
        }
        return serveDocument(url, requestHeaders)
    }

    private fun serveWrapper(embedUrl: String): WebResourceResponse? {
        return try {
            val wrapper = wrapperHtml(embedUrl)
            Log.i(TAG, "wrap url=" + embedUrl + " bytes=" + wrapper.length)
            htmlResponse(wrapper, 200, "OK", Charsets.UTF_8)
        } catch (t: Throwable) {
            Log.i(TAG, "wrap failed url=" + embedUrl + " " + t.javaClass.simpleName + ": " + t.message)
            null
        }
    }

    private fun serveDocument(url: String, requestHeaders: Map<String, String>): WebResourceResponse? {
        return try {
            val builder = Request.Builder().url(url).get()
            var hasAgent = false
            for ((key, value) in requestHeaders) {
                val name = key.lowercase()
                if (name !in forwardedHeaders) continue
                if (name == "user-agent") hasAgent = true
                try {
                    builder.header(key, value)
                } catch (t: Throwable) {
                    Log.i(TAG, "skip header " + key + " " + t.javaClass.simpleName)
                }
            }
            if (!hasAgent) builder.header("User-Agent", FALLBACK_UA)

            client.newCall(builder.build()).execute().use { response ->
                val body = response.body
                if (body == null) {
                    Log.i(TAG, "no body url=" + url + " code=" + response.code)
                    return null
                }
                if (!response.isSuccessful) {
                    Log.i(TAG, "not ok url=" + url + " code=" + response.code + " " + response.message)
                    return null
                }
                val contentType = body.contentType()
                val type = contentType?.type?.lowercase() ?: ""
                val subtype = contentType?.subtype?.lowercase() ?: ""
                val isHtml = (type == "text" && subtype == "html") ||
                    (type == "application" && subtype == "xhtml+xml")
                if (!isHtml) {
                    Log.i(TAG, "not html url=" + url + " mime=" + type + "/" + subtype)
                    return null
                }
                if (body.contentLength() > MAX_HTML_CHARS) {
                    Log.i(TAG, "too large url=" + url + " length=" + body.contentLength())
                    return null
                }
                val charset = contentType?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val html = body.string()
                if (html.length > MAX_HTML_CHARS) {
                    Log.i(TAG, "too large body url=" + url + " chars=" + html.length)
                    return null
                }
                for (cookie in response.headers("Set-Cookie")) {
                    try {
                        CookieManager.getInstance().setCookie(url, cookie)
                    } catch (_: Throwable) {
                    }
                }
                val finalUrl = response.request.url.toString()
                val injected = inject(html, if (finalUrl == url) null else finalUrl)
                val reason = if (response.message.isEmpty()) "OK" else response.message
                Log.i(TAG, "served url=" + url + " bytes=" + injected.length + " code=" + response.code)
                htmlResponse(injected, response.code, reason, charset)
            }
        } catch (t: Throwable) {
            Log.i(TAG, "fetch failed url=" + url + " " + t.javaClass.name + ": " + t.message)
            null
        }
    }

    private fun htmlResponse(
        html: String,
        code: Int,
        message: String,
        charset: Charset
    ): WebResourceResponse {
        return WebResourceResponse(
            "text/html",
            charset.name(),
            code,
            message,
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-store"
            ),
            ByteArrayInputStream(html.toByteArray(charset))
        )
    }

    private fun wrapperHtml(embedUrl: String): String {
        val inner = markedUrl(embedUrl)
        return "<!DOCTYPE html><html><head>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}" +
            "iframe{position:fixed;top:0;left:0;width:100%;height:100%;border:0}</style></head><body>" +
            "<iframe src=\"" + inner + "\" allow=\"autoplay; fullscreen; encrypted-media; picture-in-picture\" allowfullscreen></iframe>" +
            "<script>" + controllerScript + "</script></body></html>"
    }

    private fun markedUrl(url: String): String =
        url + (if (url.contains("?")) "&" else "?") + EMBED_MARKER

    private fun stripMarker(url: String): String {
        val cleaned = url.replace(EMBED_MARKER, "").replace("?&", "?").replace("&&", "&")
        return if (cleaned.endsWith("?") || cleaned.endsWith("&")) cleaned.dropLast(1) else cleaned
    }

    private fun inject(html: String, baseUrl: String?): String {
        var output = html
        if (baseUrl != null) {
            val head = headTag.find(output)
            output = if (head != null) {
                output.substring(0, head.range.last + 1) + "<base href=\"" + baseUrl + "\">" +
                    output.substring(head.range.last + 1)
            } else {
                "<base href=\"" + baseUrl + "\">" + output
            }
        }
        val payload = "<script>" + controllerScript + "</script>"
        val bodyEnd = output.lastIndexOf("</body>", ignoreCase = true)
        return if (bodyEnd >= 0) {
            output.substring(0, bodyEnd) + payload + output.substring(bodyEnd)
        } else {
            output + payload
        }
    }

    val controllerScript: String = """
(function () {
  if (window.__zenithInstalled) { return; }
  window.__zenithInstalled = true;

  var AD_CSS = 'div[id*="ad"],div[class*="ad-"],div[class*="banner"],div[id*="pop"],iframe[src*="ad"]{display:none!important;opacity:0!important;pointer-events:none!important;}';
  var MEDIA_CSS = 'video::-webkit-media-controls{display:none!important;}video::-webkit-media-controls-enclosure{display:none!important;}';
  var CHROME_SELECTORS = [
    '.jw-controls', '.jw-overlays', '.jw-display-icon-container', '.vjs-control-bar', '.vjs-big-play-button',
    '.plyr__controls', '.plyr__control', '.plyr__overlay',
    '[class*="control-bar"]', '[class*="controlbar"]', '[class*="controls"]', '[id*="control-bar"]', '[id*="controls"]',
    '[class*="player-bar"]', '[class*="bottom-bar"]', '[class*="top-bar"]',
    '[class*="player-title"]', '[class*="video-title"]', '[class*="overlay-title"]', '[class*="episode-title"]',
    '[class*="watermark"]', '[class*="logo"]', '[class*="brand"]',
    '[class*="next-episode"]', '[class*="nextep"]', '[class*="recommend"]', '[class*="notice"]',
    '[class*="comment"]', '[class*="chat"]', '[class*="discuss"]',
    '[aria-label*="omment"]', '[aria-label*="hat"]', '[title*="omment"]', '[title*="hat"]',
    '[class*="popup"]', '[class*="ad-"]', '[id*="ad-"]'
  ];

  var video = null;
  var hadVideo = false;
  var currentFit = 'contain';
  var scanPending = false;

  function pageRoot() { return document.head || document.documentElement; }

  function ensureStyle(id, css) {
    var node = document.getElementById(id);
    if (!node) {
      node = document.createElement('style');
      node.id = id;
      if (pageRoot()) { pageRoot().appendChild(node); }
    }
    if (node.textContent !== css) { node.textContent = css; }
  }

  function collectVideos(scope, out) {
    try {
      var found = scope.querySelectorAll('video');
      for (var i = 0; i < found.length; i++) { out.push(found[i]); }
    } catch (e) {}
    try {
      var all = scope.querySelectorAll('*');
      for (var j = 0; j < all.length; j++) {
        if (all[j].shadowRoot) { collectVideos(all[j].shadowRoot, out); }
      }
    } catch (e) {}
    return out;
  }

  function pickVideo() {
    var list = collectVideos(document, []);
    var best = null;
    var bestScore = -1;
    for (var i = 0; i < list.length; i++) {
      var candidate = list[i];
      if (!candidate.isConnected) { continue; }
      var score = 0;
      if (candidate.currentSrc) { score += 2; }
      if (candidate.videoWidth) { score += 2; }
      if (candidate.duration && isFinite(candidate.duration)) { score += 1; }
      if (!candidate.paused) { score += 1; }
      if (score > bestScore) { bestScore = score; best = candidate; }
    }
    return best;
  }

  function hideChrome() {
    if (!video || !video.isConnected) { return; }
    for (var i = 0; i < CHROME_SELECTORS.length; i++) {
      var nodes;
      try { nodes = document.querySelectorAll(CHROME_SELECTORS[i]); } catch (e) { continue; }
      for (var j = 0; j < nodes.length; j++) {
        var node = nodes[j];
        if (!node || node === document.body || node === document.documentElement) { continue; }
        if (node.tagName === 'VIDEO' || node.tagName === 'SCRIPT' || node.tagName === 'STYLE') { continue; }
        if (video.contains(node) || node.contains(video)) { continue; }
        node.style.setProperty('display', 'none', 'important');
      }
    }
  }

  function report() {
    if (!video || !video.isConnected) {
      if (hadVideo) { post({ ready: false }); }
      return;
    }
    var total = video.duration;
    var src = video.currentSrc || video.src || '';
    if (!src) {
      try {
        var sources = video.querySelectorAll('source');
        if (sources.length > 0) { src = sources[sources.length - 1].src || ''; }
      } catch (e) {}
    }
    post({
      ready: true,
      t: video.currentTime || 0,
      d: (total && isFinite(total)) ? total : 0,
      playing: !video.paused && !video.ended,
      waiting: video.readyState < 3 && !video.paused,
      volume: (typeof video.volume === 'number') ? video.volume : 1,
      muted: !!video.muted,
      rate: video.playbackRate || 1,
      ended: !!video.ended,
      src: src,
      drm: !!(video.mediaKeys || (window.MediaSource && video.currentSrc && video.currentSrc.indexOf('blob:') === 0))
    });
  }

  function post(payload) {
    var raw = JSON.stringify(payload);
    try {
      if (window.AndroidPlayer && window.AndroidPlayer.postState) {
        window.AndroidPlayer.postState(raw);
        return;
      }
    } catch (e) {}
    try {
      if (window.parent && window.parent !== window) {
        window.parent.postMessage(JSON.stringify({ __zenith: payload }), '*');
      }
    } catch (e) {}
  }

  function broadcastCommand(name, arg) {
    var frames = document.querySelectorAll('iframe');
    for (var i = 0; i < frames.length; i++) {
      try {
        frames[i].contentWindow.postMessage(JSON.stringify({ __zenithCmd: name, arg: arg }), '*');
      } catch (e) {}
    }
  }

  function applyCommand(name, arg) {
    if (!video || !video.isConnected) { return; }
    try {
      if (name === 'toggle') {
        if (video.paused) {
          var started = video.play();
          if (started && started.catch) { started.catch(function () {}); }
        } else {
          video.pause();
        }
      } else if (name === 'play') {
        var resumed = video.play();
        if (resumed && resumed.catch) { resumed.catch(function () {}); }
      } else if (name === 'pause') {
        video.pause();
      } else if (name === 'seek') {
        video.currentTime = Math.max(0, Number(arg) || 0);
      } else if (name === 'seekBy') {
        var limit = (video.duration && isFinite(video.duration)) ? video.duration : 1e9;
        video.currentTime = Math.max(0, Math.min(limit, (video.currentTime || 0) + (Number(arg) || 0)));
      } else if (name === 'setVolume') {
        video.volume = Math.max(0, Math.min(1, Number(arg)));
      } else if (name === 'setMuted') {
        video.muted = (arg === true || arg === 'true');
      } else if (name === 'setRate') {
        video.playbackRate = Math.max(0.25, Math.min(4, Number(arg) || 1));
      } else if (name === 'setFit') {
        var f = String(arg || 'fit').toLowerCase();
        if (f === 'stretch' || f === 'fill') { currentFit = 'fill'; }
        else if (f === 'cover') { currentFit = 'cover'; }
        else if (f === 'zoom') { currentFit = 'zoom'; }
        else { currentFit = 'contain'; }
        video.style.setProperty('object-fit', (currentFit === 'zoom') ? 'contain' : currentFit, 'important');
        video.style.setProperty('width', '100%', 'important');
        video.style.setProperty('height', '100%', 'important');
        video.style.setProperty('transform', (currentFit === 'zoom') ? 'scale(1.14)' : 'none', 'important');
        video.style.setProperty('transform-origin', 'center center', 'important');
        if (f === '16:9') { video.style.setProperty('aspect-ratio', '16 / 9', 'important'); }
        else if (f === '4:3') { video.style.setProperty('aspect-ratio', '4 / 3', 'important'); }
        else { video.style.removeProperty('aspect-ratio'); }
      }
    } catch (e) {}
    var hasVideo = !!(video && video.isConnected);
    post({
      diag: true,
      cmd: name,
      applied: hasVideo,
      playing: hasVideo ? !video.paused : false,
      t: hasVideo ? (video.currentTime || 0) : 0
    });
    report();
  }

  function bind(target) {
    var changed = (video !== target);
    video = target;
    if (!target.__zenithBound) {
      target.__zenithBound = true;
      var events = [
        'loadedmetadata', 'durationchange', 'timeupdate', 'play', 'pause', 'playing', 'waiting',
        'canplay', 'canplaythrough', 'seeking', 'seeked', 'volumechange', 'ratechange', 'ended',
        'error', 'emptied'
      ];
      for (var i = 0; i < events.length; i++) {
        target.addEventListener(events[i], report, true);
      }
    }
    hadVideo = true;
    ensureStyle('__zenith_hide', MEDIA_CSS);
    hideChrome();
    if (changed) { applyCommand('setFit', currentFit); }
    report();
  }

  function scan() {
    var candidate = pickVideo();
    if (candidate) { bind(candidate); }
  }

  function scheduleScan() {
    if (scanPending) { return; }
    scanPending = true;
    window.setTimeout(function () {
      scanPending = false;
      scan();
      hideChrome();
    }, 250);
  }

  function onMessage(event) {
    var data = null;
    try {
      data = (typeof event.data === 'string') ? JSON.parse(event.data) : event.data;
    } catch (e) { return; }
    if (!data) { return; }
    if (data.__zenith) {
      try {
        if (window.AndroidPlayer && window.AndroidPlayer.postState) {
          window.AndroidPlayer.postState(JSON.stringify(data.__zenith));
        }
      } catch (e) {}
      return;
    }
    if (data.__zenithCmd) {
      if (video && video.isConnected) { applyCommand(data.__zenithCmd, data.arg); }
      broadcastCommand(data.__zenithCmd, data.arg);
    }
  }

  function diagnostics() {
    var videos = collectVideos(document, []);
    post({
      diag: true,
      url: String(location.href).substring(0, 140),
      isTop: (window.parent === window),
      frames: document.querySelectorAll('iframe').length,
      videos: videos.length,
      hasBridge: !!(window.AndroidPlayer && window.AndroidPlayer.postState),
      title: String(document.title).substring(0, 90)
    });
  }

  function start() {
    ensureStyle('__zenith_ads', AD_CSS);
    scan();
    window.setTimeout(diagnostics, 2500);
    window.setTimeout(diagnostics, 9000);
    var target = document.body || document.documentElement;
    if (target && window.MutationObserver) {
      var observer = new MutationObserver(function () { scheduleScan(); });
      try {
        observer.observe(target, {
          childList: true,
          subtree: true,
          attributes: true,
          attributeFilter: ['class', 'style']
        });
      } catch (e) {}
    }
    window.setInterval(function () {
      scan();
      if (video && video.isConnected) {
        hideChrome();
        report();
      }
    }, 1500);
    window.addEventListener('message', onMessage);
  }

  window.__zenith = {
    cmd: function (name, arg) {
      if (video && video.isConnected) { applyCommand(name, arg); }
      broadcastCommand(name, arg);
    },
    refresh: function () {
      scan();
      hideChrome();
      report();
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start);
  } else {
    start();
  }
})();
""".trimIndent()
}

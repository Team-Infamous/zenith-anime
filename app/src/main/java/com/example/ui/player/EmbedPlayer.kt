package com.example.ui.player

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/** State reported back by the embedded player's hijacked <video> element. */
data class EmbedPlayerState(
    val ready: Boolean = false,
    val playing: Boolean = false,
    val buffering: Boolean = true,
    val positionSec: Long = 0L,
    val durationSec: Long = 0L,
    val muted: Boolean = false,
    val volume: Float = 1f,
    /** The real media URL the embed is playing, when it exposes one. */
    val detectedSource: String? = null,
    /** True when the embed uses MediaSource (blob:) so the URL cannot be downloaded directly. */
    val drmOrMse: Boolean = false
)

/** Imperative handle the player UI uses to drive the embedded WebView player. */
class EmbedPlayerController {
    internal var webView: WebView? = null

    fun send(name: String, arg: Any? = null) {
        val formattedArg = when (arg) {
            null -> ""
            is Number, is Boolean -> ",$arg"
            is String -> {
                if (arg.isEmpty()) ""
                else if (
                    arg.toDoubleOrNull() != null ||
                    arg == "true" ||
                    arg == "false" ||
                    (arg.startsWith("\"") && arg.endsWith("\"")) ||
                    (arg.startsWith("'") && arg.endsWith("'"))
                ) {
                    ",$arg"
                } else {
                    ",${JSONObject.quote(arg)}"
                }
            }
            else -> ",${JSONObject.quote(arg.toString())}"
        }
        val call = "window.__zenith&&window.__zenith.cmd('$name'$formattedArg)"
        webView?.evaluateJavascript(call, null)
    }
}

/**
 * Fallback playback path: loads a third-party embed inside a WebView and hijacks its video element.
 * Used when no direct stream can be resolved, or when the user explicitly picks the built-in player.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedPlayerView(
    embedUrl: String,
    wrapEmbeds: Boolean,
    state: MutableState<EmbedPlayerState>,
    controller: EmbedPlayerController,
    modifier: Modifier = Modifier,
    adBlockEnabled: Boolean = true
) {
    val wrap by rememberUpdatedState(wrapEmbeds)
    val currentAdBlock by rememberUpdatedState(adBlockEnabled)
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    allowFileAccess = true
                    allowContentAccess = true
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val intercepted = request ?: return null
                        if (intercepted.method != "GET") return null
                        if (!intercepted.url.toString().startsWith("http")) return null
                        if (!EmbedHijack.looksLikeDocument(intercepted)) return null
                        return EmbedHijack.rewrite(
                            intercepted.url.toString(),
                            intercepted.requestHeaders,
                            intercepted.isForMainFrame,
                            wrap
                        )
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val scheme = request?.url?.scheme?.lowercase() ?: ""
                        if (scheme != "http" && scheme != "https") return true
                        if (currentAdBlock && request?.isForMainFrame == true) {
                            val host = request.url?.host?.lowercase() ?: ""
                            val trusted = listOf(
                                "vidlink", "vidsrc", "tryembed", "megaplay", "kaido", "megacloud",
                                "rabbitstream", "rapid-cloud", "streamwish", "filemoon", "jwplayer",
                                "anikoto", "aniembed", "viduki", "animegg", "anineko", "allanime",
                                "gogoanime", "hianime", "zoro"
                            )
                            if (trusted.none { host.contains(it) }) return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(EmbedHijack.controllerScript, null)
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postState(json: String) {
                        Handler(Looper.getMainLooper()).post {
                            try {
                                val payload = JSONObject(json)
                                if (payload.optBoolean("diag", false)) return@post
                                val current = state.value
                                state.value = current.copy(
                                    ready = payload.optBoolean("ready", false),
                                    playing = payload.optBoolean("playing", current.playing),
                                    buffering = payload.optBoolean("waiting", false),
                                    positionSec = payload.optDouble("t", current.positionSec.toDouble()).toLong(),
                                    durationSec = payload.optDouble("d", current.durationSec.toDouble()).toLong(),
                                    muted = payload.optBoolean("muted", current.muted),
                                    volume = payload.optDouble("volume", current.volume.toDouble()).toFloat(),
                                    detectedSource = payload.optString("src", "").takeIf { it.startsWith("http") }
                                        ?: current.detectedSource,
                                    drmOrMse = payload.optBoolean("drm", current.drmOrMse)
                                )
                            } catch (_: Throwable) {
                            }
                        }
                    }
                }, "AndroidPlayer")

                if (embedUrl.isNotBlank()) {
                    tag = embedUrl
                    loadUrl(embedUrl)
                }
                controller.webView = this
            }
        },
        update = { webView ->
            if (embedUrl.isNotBlank() && webView.tag != embedUrl) {
                webView.tag = embedUrl
                webView.loadUrl(embedUrl)
            }
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(controller) {
        onDispose {
            controller.webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
            controller.webView = null
        }
    }
}

@Composable
fun rememberEmbedPlayerController(): EmbedPlayerController = remember { EmbedPlayerController() }

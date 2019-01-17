package com.devfatani.webvuw

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.View
import android.webkit.WebView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.platform.PlatformView
import android.support.v4.widget.SwipeRefreshLayout
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.LinearLayout
import io.flutter.plugin.common.EventChannel


enum class FlutterMethodName {
    loadUrl,
    canGoBack,
    canGoForward,
    goBack,
    goForward,
    stopLoading,
}

class WebVuw internal constructor(
        context: Context,
        messenger: BinaryMessenger,
        id: Int,
        params: Map<*, *>) :
        PlatformView,
        MethodCallHandler,
        SwipeRefreshLayout.OnRefreshListener,
        EventChannel.StreamHandler {

    companion object {
        const val TAG = "WebVuw"

        //PRAMS NAME
        const val INITIAL_URL = "initialUrl"
        const val HEADER = "header"
        const val USER_AGENT = "userAgent"
        const val CHANNEL_NAME = "plugins.devfatani.com/web_vuw_"
        const val WEB_VUW_EVENT = "web_vuw_events_"
        const val EVENT = "event"
        const val URL = "url"
        const val ENABLE_JAVA_SCRIPT = "enableJavascript"
        const val ENABLE_LOCAL_STORAGE = "enableLocalStorage"
    }

    private val linearLay = LinearLayout(context)

    private val swipeRefresh: SwipeRefreshLayout = SwipeRefreshLayout(context)
    private val webVuw: WebView = WebView(context)
    private val methodChannel: MethodChannel

    private var eventSinkNavigation: EventChannel.EventSink? = null

    init {
        if (params.containsKey(INITIAL_URL)) {
            val initialURL = params[INITIAL_URL] as String


            if (params[HEADER] != null) {
                val header = params[HEADER] as Map<String, String>
                webVuw.loadUrl(initialURL, header)
            } else webVuw.loadUrl(initialURL)

            if (params[USER_AGENT] != null) {
                val userAgent = params[USER_AGENT] as String
                webVuw.settings.userAgentString = userAgent
            }


            val isJavaScriptEnabled = params[ENABLE_JAVA_SCRIPT] as Boolean
            val isLocalStorageEnabled = params[ENABLE_LOCAL_STORAGE] as Boolean
            webVuw.settings.javaScriptEnabled = isJavaScriptEnabled
            webVuw.settings.domStorageEnabled = isLocalStorageEnabled

            val self = this@WebVuw
            webVuw.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (self.eventSinkNavigation != null && url != null) {
                        val result = HashMap<String, String>().apply {
                            put(EVENT, "onPageStarted")
                            put(URL, url)
                        }
                        self.eventSinkNavigation!!.success(result)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (self.eventSinkNavigation != null && url != null) {
                        val result = HashMap<String, String>().apply {
                            put(EVENT, "onPageFinished")
                            put(URL, url)
                        }
                        self.eventSinkNavigation!!.success(result)
                    }
                }

                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (request != null && self.eventSinkNavigation != null) {
                        val nxtUrl = request.url.toString()
                        val result = HashMap<String, String>().apply {
                            put(EVENT, "shouldOverrideUrlLoading")
                            put(URL, nxtUrl)
                        }
                        self.eventSinkNavigation!!.success(result)
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }

            linearLay.orientation = LinearLayout.VERTICAL
            swipeRefresh.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            webVuw.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            linearLay.addView(swipeRefresh)
            swipeRefresh.addView(webVuw)
            swipeRefresh.setOnRefreshListener(this@WebVuw)

            EventChannel(messenger, "$WEB_VUW_EVENT$id").setStreamHandler(this@WebVuw)
        }

        methodChannel = MethodChannel(messenger, "$CHANNEL_NAME$id")
        methodChannel.setMethodCallHandler(this@WebVuw)
    }

    override fun getView(): View {
        return linearLay
    }

    override fun onMethodCall(methodCall: MethodCall, result: Result) {
        when (FlutterMethodName.valueOf(methodCall.method)) {
            FlutterMethodName.loadUrl -> loadUrl(methodCall, result)
            FlutterMethodName.canGoBack -> canGoBack(methodCall, result)
            FlutterMethodName.canGoForward -> canGoForward(methodCall, result)
            FlutterMethodName.goBack -> goBack(methodCall, result)
            FlutterMethodName.goForward -> goForward(methodCall, result)
            FlutterMethodName.stopLoading -> stopLoading(methodCall, result)
        }
    }

    private fun stopLoading(methodCall: MethodCall, result: Result) {
        webVuw.stopLoading()
        result.success(null)
    }

    private fun loadUrl(methodCall: MethodCall, result: Result) {
        val url = methodCall.arguments as String
        webVuw.loadUrl(url)
        result.success(null)
    }

    private fun canGoBack(methodCall: MethodCall, result: Result) {
        result.success(webVuw.canGoBack())
    }

    private fun canGoForward(methodCall: MethodCall, result: Result) {
        result.success(webVuw.canGoForward())
    }

    private fun goBack(methodCall: MethodCall, result: Result) {
        if (webVuw.canGoBack()) {
            webVuw.goBack()
        }
        result.success(null)
    }

    private fun goForward(methodCall: MethodCall, result: Result) {
        if (webVuw.canGoForward()) {
            webVuw.goForward()
        }
        result.success(null)
    }

    override fun onRefresh() {
        webVuw.reload()
        swipeRefresh.isRefreshing = false
    }

    override fun dispose() {}

    override fun onListen(args: Any?, event: EventChannel.EventSink?) {
        eventSinkNavigation = event
    }

    override fun onCancel(p0: Any?) {

    }
}
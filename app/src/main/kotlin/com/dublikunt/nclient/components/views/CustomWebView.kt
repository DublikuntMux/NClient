package com.dublikunt.nclient.components.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.utility.LogUtility.download

open class CustomWebView : WebView {
    private val javaScriptInterface: MyJavaScriptInterface

    constructor(context: Context) : super(context.applicationContext) {
        javaScriptInterface = MyJavaScriptInterface(context.applicationContext)
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        javaScriptInterface = MyJavaScriptInterface(context.applicationContext)
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        javaScriptInterface = MyJavaScriptInterface(context.applicationContext)
        initialize()
    }

    override fun loadUrl(url: String) {
        download("Loading url: $url")
        super.loadUrl(url)
    }

    @SuppressLint(
        "SetJavaScriptEnabled",
        "AddJavascriptInterface"
    ) //it only uses showHtml and Nhentai should be trusted if you use this app (I think)
    private fun initialize() {
        settings.javaScriptEnabled = true
        settings.userAgentString = Global.userAgent
        addJavascriptInterface(javaScriptInterface, "HtmlViewer")
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
                download("Started url: $url")
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView, url: String) {
                val html = "javascript:window.HtmlViewer.showHTML" +
                    "('" + url + "','<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');"
                loadUrl(html)
            }
        }
        addFetcher(object : HtmlFetcher {
            override fun fetchUrl(url: String?, html: String?) {
                download("Fetch for url $url: $html")
            }
        })
    }

    fun addFetcher(fetcher: HtmlFetcher?) {
        if (fetcher == null) return
        javaScriptInterface.addFetcher(fetcher)
    }

    fun removeFetcher(fetcher: HtmlFetcher?) {
        if (fetcher == null) return
        javaScriptInterface.removeFetcher(fetcher)
    }

    interface HtmlFetcher {
        fun fetchUrl(url: String?, html: String?)
    }

    internal class MyJavaScriptInterface(var ctx: Context) {
        var fetchers: MutableList<HtmlFetcher> = ArrayList(5)
        fun addFetcher(fetcher: HtmlFetcher) {
            fetchers.add(fetcher)
        }

        fun removeFetcher(fetcher: HtmlFetcher) {
            fetchers.remove(fetcher)
        }

        @JavascriptInterface
        fun showHTML(url: String?, html: String?) {
            for (fetcher in fetchers) fetcher.fetchUrl(url, html)
        }
    }
}

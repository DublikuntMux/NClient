package com.dublikunt.nclientv2.components.views

import android.annotation.SuppressLint
import android.content.*
import android.util.AttributeSet
import android.util.Base64
import android.webkit.JavascriptInterface
import com.dublikunt.nclientv2.utility.LogUtility.download
import com.dublikunt.nclientv2.utility.Utility
import org.jsoup.Jsoup

class LoginWebView : CustomWebView {
    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private val fetcher: HtmlFetcher = object : HtmlFetcher {
        override fun fetchUrl(url: String?, html: String?) {
            val jsoup = Jsoup.parse(html)
            val body = jsoup.body()
            val form = body.getElementsByTag("form").first()
            body.getElementsByClass("lead").first()!!.text("Tested")
            form!!.tagName("div")
            form.before(
                """<script>
document.getElementsByClassName('lead')[0].innerHTML='test';
alert('test');
function intercept(){
    password=document.getElementById('id_password').value;
    email=document.getElementById('id_username_or_email').value;
    token=document.getElementsByName('csrfmiddlewaretoken')[0].value;
    captcha=document.getElementById('g-recaptcha-response').value;
     Interceptor.intercept(email,password,token,captcha);
}
</script>"""
            )
            form.getElementsByAttributeValue("type", "submit").first()!!
                .attr("onclick", "intercept()")
            removeFetcher(this)
            val encodedHtml =
                Base64.encodeToString(jsoup.outerHtml().toByteArray(), Base64.NO_PADDING)
            loadDataWithBaseURL(Utility.getBaseUrl(), encodedHtml, "text/html", "base64", null)
        }
    }

    @SuppressLint("AddJavascriptInterface")
    private fun init(context: Context) {
        addJavascriptInterface(JSInterceptor(), "Interceptor")
        addFetcher(fetcher)
        loadUrl(Utility.getBaseUrl() + "login/")
    }

    internal class JSInterceptor {
        @JavascriptInterface
        fun intercept(email: String?, password: String?, token: String?, captcha: String?) {
            download(String.format("e:'%s',p:'%s',t:'%s',c:'%s'", email, password, token, captcha))
        }
    }
}

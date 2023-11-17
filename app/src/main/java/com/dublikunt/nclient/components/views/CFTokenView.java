package com.dublikunt.nclient.components.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dublikunt.nclient.R;
import com.dublikunt.nclient.components.CookieInterceptor;
import com.dublikunt.nclient.settings.Global;

public class CFTokenView {
    private final ViewGroup masterLayout;
    private final CFTokenWebView webView;
    private final Button button;

    public CFTokenView(@NonNull ViewGroup masterLayout) {
        this.masterLayout = masterLayout;
        webView = masterLayout.findViewById(R.id.webView);
        button = masterLayout.findViewById(R.id.hideWebView);
        button.setOnClickListener(v -> CookieInterceptor.hideWebView());
    }

    public Button getButton() {
        return button;
    }

    public CFTokenWebView getWebView() {
        return webView;
    }

    public void setVisibility(int visible) {
        masterLayout.setVisibility(visible);
    }

    public void post(Runnable o) {
        masterLayout.post(o);
    }

    public static class CFTokenWebView extends WebView {
        public CFTokenWebView(@NonNull Context context) {
            super(context);
            init();
        }

        public CFTokenWebView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public CFTokenWebView(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            forceAcceptCookies();
            applyWebViewSettings();
        }

        private void forceAcceptCookies() {
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        }

        private void applyWebViewSettings() {
            setWebChromeClient(new WebChromeClient());
            setWebViewClient(new WebViewClient());
            WebSettings webSettings = getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setUserAgentString(Global.getUserAgent());
            webSettings.setAllowContentAccess(true);
        }
    }
}

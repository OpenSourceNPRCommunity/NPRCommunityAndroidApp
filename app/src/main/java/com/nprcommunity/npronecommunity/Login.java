package com.nprcommunity.npronecommunity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nprcommunity.npronecommunity.Background.BackgroundAudioService;

public class Login extends AppCompatActivity {

    private String TAG = "LOGIN",
                    URL = Config.OATH_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(hasValidToken()) {
            Intent i = new Intent(Login.this, Navigate.class);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

//        Start Login WebView
        final WebView myWebView = findViewById(R.id.webview_login);
//        Enable javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(request != null && request.getRequestHeaders() != null && request.getRequestHeaders().containsKey("X-Forwarded-Access-Token")) {
                    Intent i = new Intent(Login.this, Navigate.class);
                    startActivity(i);
                    finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if(errorResponse.getStatusCode() == 404 && request.getUrl().toString().equals(Config.OATH_URL)) {
                    TokenManager tokenManager = new TokenManager(Login.this);
                    tokenManager.SetToken(errorResponse.getResponseHeaders().get("x-forwarded-access-token"));
                    Intent i = new Intent(Login.this, Navigate.class);
                    startActivity(i);
                    finish();
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }
        });
        myWebView.loadUrl("https://npr.jessesaran.com");
    }

    private boolean hasValidToken() {
        TokenManager tokenManager = new TokenManager(this);
//            WebView Handles removing expired cookies automatically API level 21, so just check if cookie exists
        if(tokenManager.GetToken() != null && CookieManager.getInstance().getCookie(URL) != null) {
            return true;
        }
        return false;
//        CookieManager
//        String cookieString = CookieManager.getInstance().getCookie("https://npr.jessesaran.com");
//        Log.d(TAG, "COOKIES FROM npr.jessesaran.com: " + cookieString);
//        CookieManager cookieManager = CookieManager.getInstance();
//        cookieManager.setCookie("https://npr.jessesaran.com", "_oauth2_proxy=");
//        cookieManager.flush();
//        cookieString = CookieManager.getInstance().getCookie("https://npr.jessesaran.com");
//        Log.d(TAG, "COOKIES FROM npr.jessesaran.com: " + cookieString);
    }
}

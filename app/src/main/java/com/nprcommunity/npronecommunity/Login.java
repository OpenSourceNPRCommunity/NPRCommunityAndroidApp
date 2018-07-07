package com.nprcommunity.npronecommunity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

public class Login extends AppCompatActivity {

    private String TAG = "LOGIN",
                    URL = Config.OATH_URL;

    public static final int REQUEST_CODE = 0,
                            RESULT_CODE_OK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionDialog(this));

        setContentView(R.layout.activity_login);

        if(hasValidToken()) {
            Intent i = new Intent(Login.this, Navigate.class);
            startActivityForResult(i, REQUEST_CODE);
            return;
        }

        setUpWebLogin();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_CODE_OK) {
                setUpWebLogin();
            }
        }
    }

    private void setUpWebLogin() {
        //Start Login WebView
        final WebView myWebView = findViewById(R.id.webview_login);
        //Enable javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if(request != null && request.getRequestHeaders() != null && request.getRequestHeaders().containsKey("X-Forwarded-Access-Token")) {
                    Intent i = new Intent(Login.this, Navigate.class);
                    startActivityForResult(i, REQUEST_CODE);
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if(errorResponse.getStatusCode() == 404 && request.getUrl().toString().equals(Config.OATH_URL)) {
                    SettingsAndTokenManager tokenManager = new SettingsAndTokenManager(Login.this);
                    tokenManager.setToken(errorResponse.getResponseHeaders().get("x-forwarded-access-token"));
                    Intent i = new Intent(Login.this, Navigate.class);
                    startActivityForResult(i, REQUEST_CODE);
                    return;
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }
        });
        myWebView.loadUrl(Config.LOGIN_URL);
    }

    private boolean hasValidToken() {
        SettingsAndTokenManager tokenManager = new SettingsAndTokenManager(this);
//            WebView Handles removing expired cookies automatically API level 21, so just check if cookie exists
        if(tokenManager.getToken() != null && CookieManager.getInstance().getCookie(URL) != null) {
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

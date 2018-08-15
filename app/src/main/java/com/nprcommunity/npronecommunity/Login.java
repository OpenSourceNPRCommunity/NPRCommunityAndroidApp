package com.nprcommunity.npronecommunity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.nprcommunity.npronecommunity.Store.SettingsAndTokenManager;

public class Login extends AppCompatActivity {

    private String TAG = "LOGIN",
                    URL = Config.OATH_URL;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionDialog(this));

        setContentView(R.layout.activity_login);

        if(hasValidToken()) {
            Intent i = new Intent(Login.this, Navigate.class);
            startActivity(i);
            return;
        }

        setUpWebLogin();
    }

    private void setUpWebLogin() {
        //Start Login WebView
        final WebView myWebView = findViewById(R.id.login_webview);
        //Enable javascript
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        progressBar = findViewById(R.id.login_progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                //change the progress bar for loading the webpages
                if (progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setVisibility(View.GONE);
                    view.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //if there already exists a successful request then forward this information on
                if(request != null && request.getRequestHeaders() != null && request.getRequestHeaders().containsKey("X-Forwarded-Access-Token")) {
                    Intent i = new Intent(Login.this, Navigate.class);
                    startActivity(i);
                    finish();
                    return true;
                }
                //change the progress bar for loading the webpages
                if (progressBar.getVisibility() == View.GONE) {
                    progressBar.setVisibility(View.VISIBLE);
                    view.setVisibility(View.GONE);
                }
                return false;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                //custom response message because there is NO onReceivedHttpSuccess(...) method, we engineer a custom
                //response that we can detect to forward on the user
                if(errorResponse.getStatusCode() == 418 && request.getUrl().toString().equals(Config.OATH_URL)) {
                    SettingsAndTokenManager tokenManager = new SettingsAndTokenManager(Login.this);
                    tokenManager.setToken(errorResponse.getResponseHeaders().get("x-forwarded-access-token"));
                    Intent i = new Intent(Login.this, Navigate.class);
                    startActivity(i);
                    finish();
                    return;
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }
        });
        String params = "rd=/oauth2/success-418";
        myWebView.setVisibility(View.GONE);
        myWebView.postUrl(Config.LOGIN_URL, params.getBytes());
    }

    private boolean hasValidToken() {
        SettingsAndTokenManager tokenManager = new SettingsAndTokenManager(this);
        if(tokenManager.getToken() != null && CookieManager.getInstance().getCookie(URL) != null) {
            return true;
        }
        return false;
    }
}

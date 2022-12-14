package com.example.emeraldtv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiModeManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String EMPTY_STRING = "";
    private static String LINE_SEPARATOR() {
        return System.lineSeparator();
    }
    private WebView webview;
    private String adServers;
    private String deviceStyle = "mobile-style.css";
    private String deviceScript = "mobile-scripts.js";
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        //String newUA= "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0";
        String newUA= "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:104.0) Gecko/20100101 Firefox/104.0";

        // Checks if device is TV and changes style sheet
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            deviceStyle = "tv-style.css";
            deviceScript = "tv-scripts.js";
        }

        webview=(WebView)findViewById(R.id.webView);
        webview.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }


            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (adServers == null)
                    updateAdServerList();
                try {
                    if (adServers != null)
                        if (adServers.contains(" ".concat(new URL(url).getHost()))){
                            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(EMPTY_STRING.getBytes()));}
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                injectJS("utils.js");
                injectJS("fastforward.js");
                injectJS("onPageFinishedScripts.js");
                injectJS(deviceScript);
                injectCSS();
                super.onPageFinished(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url){
                injectJS(deviceScript);
                super.onLoadResource(view, url);
            }
        });

        webview.setWebChromeClient(new CustomChromeClient(activity));

        WebSettings webSettings = webview.getSettings();
        webSettings.setUserAgentString(newUA);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getApplicationContext().getFilesDir().getAbsolutePath() + "/cache");
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationDatabasePath(getApplicationContext().getFilesDir().getAbsolutePath() + "/databases");
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        webview.loadUrl("https://www.hulu.com/profiles?next=/");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webview.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webview.restoreState(savedInstanceState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webview.canGoBack()) {
                    webview.goBack();
                } else {
                    finish();
                }
                return true;
            }

        }

        return super.onKeyDown(keyCode, event);
    }


    //TODO REFINE TV REMOTE CONTROLS

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:
                if(event.getAction() == KeyEvent.ACTION_UP){
                    injectJS("nav-scripts.js");
                }
                break;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if(event.getAction() == KeyEvent.ACTION_UP){
                    simulateKeyPress(KeyEvent.KEYCODE_SPACE);
                }

        }
        return super.dispatchKeyEvent(event);
    }


    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen and disables status and action bar accordingly
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
            this.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            this.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE
            );
        }
    }

    private void updateAdServerList() {
        adServers = EMPTY_STRING;

        StringBuilder builder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("host.txt")))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (line.startsWith("127.0.0.1 "))
                    builder.append(line).append(LINE_SEPARATOR());
            }
            adServers = builder.toString();

        } catch (IOException e) {
            e.printStackTrace();
            adServers = null;
        }
    }

    private void injectJS(String filename) {
        try {
            InputStream inputStream = getAssets().open(filename);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            webview.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void injectCSS() {
        try {
            InputStream inputStream = getAssets().open(deviceStyle);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            webview.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void simulateKeyPress(int key){
        activity.getWindow().getDecorView().getRootView();
        BaseInputConnection inputConnection = new BaseInputConnection(activity.getWindow().getDecorView().getRootView(),
                true);
        KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, key);
        KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, key);
        inputConnection.sendKeyEvent(downEvent);
        inputConnection.sendKeyEvent(upEvent);
    }
}
package com.celeblingo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.HttpAuthHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class WebViewActivity extends AppCompatActivity implements MenuItem.OnMenuItemClickListener {
    private String url;
    private WebView webView;
    private ProgressBar progressBar;
    private Button closeBtn;
    private TTSManager ttsManager;
    int paragraphCount;
    private String USER_AGENT = "(Android " + Build.VERSION.RELEASE + ") Chrome/110.0.5481.63 Mobile";
    private RelativeLayout relativeLayout;
    private Button opeSettingBtn, reloadBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        url = getIntent().getStringExtra("url");
        initViews();

        ttsManager = new TTSManager(this);

        paragraphCount = 1;

        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);


        setClickListeners();
        setWebViewSettings();
        setWebViewClient();

        if (isInternetAvailable()) {
            webView.loadUrl(url);
        }else {
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
        }

    }

    private void setWebViewClient() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (isInternetAvailable()) {
                    view.loadUrl(url);
                }else {
                    progressBar.setVisibility(View.GONE);
                    webView.setVisibility(View.GONE);
                    relativeLayout.setVisibility(View.VISIBLE);
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                Log.d("==url", url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                Log.d("==http", host + " : " + realm);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Toast.makeText(WebViewActivity.this, "Error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setWebViewSettings() {
        WebSettings webSettings = webView.getSettings();

        webSettings.setDomStorageEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.getSaveFormData();
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSavePassword(true);
        // webSettings.setSupportMultipleWindows(true); //?a href problem
        webSettings.getJavaScriptEnabled();
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webView.setInitialScale(125);
        webSettings.setUserAgentString(USER_AGENT);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        closeBtn = findViewById(R.id.close_button);
        progressBar = findViewById(R.id.progress_bar);
        relativeLayout = findViewById(R.id.relative_layout);
        opeSettingBtn = findViewById(R.id.open_setting_btn);
        reloadBtn = findViewById(R.id.reload_btn);
    }

    private void setClickListeners() {
        closeBtn.setOnClickListener(view -> {
            startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        reloadBtn.setOnClickListener(view -> {
            if (isInternetAvailable()) {
                finish();
                startActivity(getIntent().putExtra("url", url));
            } else {
                Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
            }
        });

        opeSettingBtn.setOnClickListener(view -> {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        });
    }


    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    ActionMode mActionMode = null;

    @Override
    public void onActionModeStarted(ActionMode mode) {
        if (mActionMode == null) {
            mActionMode = mode;
            Menu menu = mode.getMenu();
            menu.clear();
            getMenuInflater().inflate(R.menu.custom_menu, menu);
            List menuItems = new ArrayList<>();
            for (int i = 0; i < menu.size(); i++) {
                menuItems.add(menu.getItem(i));
            }
            menu.clear();
            int size = menuItems.size();
            for (int i = 0; i < size; i++) {
                addMenuItem(menu, (MenuItem) menuItems.get(i), i, true);
            }
            super.onActionModeStarted(mode);
        }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        mActionMode = null;
        super.onActionModeFinished(mode);
    }

    private void addMenuItem(Menu menu, MenuItem item, int order, boolean isClick) {
        MenuItem menuItem = menu.add(item.getGroupId(),
                item.getItemId(),
                order,
                item.getTitle());
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (isClick)
            menuItem.setOnMenuItemClickListener(this);
    }


    @Override
    public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.speak_aloud) {
            webView.evaluateJavascript("(function() { return window.getSelection().toString(); })();", value -> {
                Log.d("==selecetdetdte", value + "");
                String utteranceId = UUID.randomUUID().toString();
                ttsManager.speak(value, utteranceId);
            });
        }
        return false;
    }


    public class TTSManager {

        private TextToSpeech textToSpeech;

        public TTSManager(Context context) {
            textToSpeech = new TextToSpeech(context, status -> {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            });
            // Create an UtteranceProgressListener object to handle callbacks
            UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // Called when TTS starts speaking
                }

                @Override
                public void onDone(String utteranceId) {
                    // Called when TTS finishes speaking
                }

                @Override
                public void onError(String utteranceId) {
                    // Called when TTS encounters an error
                }
            };
            // Add the UtteranceProgressListener to the TextToSpeech object
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }

        public void speak(String text, String utteranceId) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }

        public void shutdown() {
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onResume() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);
        ttsManager = new TTSManager(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        ttsManager.shutdown();
        super.onPause();
    }

    public boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null){
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
            return false;
        }
        if (networkInfo.isConnectedOrConnecting()){
            webView.setVisibility(View.VISIBLE);
            relativeLayout.setVisibility(View.GONE);
            return true;
        }else {
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
            return false;
        }
    }

}
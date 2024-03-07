package com.celeblingo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.celeblingo.helper.BaseActivity;
import com.celeblingo.helper.DriveManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class WebViewActivity extends BaseActivity implements MenuItem.OnMenuItemClickListener {
    private String url, type, meetingId;
    private WebView webView;
    private ProgressBar progressBar;
    private ImageView closeBtn;
    private ImageView takeSSBtn;
    private TTSManager ttsManager;
    int paragraphCount;
    private String USER_AGENT = "(Android " + Build.VERSION.RELEASE + ") Chrome/110.0.5481.63 Mobile";
    private RelativeLayout relativeLayout;
    private AppCompatButton opeSettingBtn, reloadBtn;
    private RelativeLayout rootLayout;
    private ImageView customIv;
    private Handler handler;
    private Runnable runnable;
    private boolean isFullscreen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        url = getIntent().getStringExtra("url");
        type = getIntent().getStringExtra("type");
        meetingId = getIntent().getStringExtra("meetingId");
        initViews();

        ttsManager = new TTSManager(this);

        paragraphCount = 1;

        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);


        setClickListeners();
        setWebViewSettings();
        setWebViewClient();

        if (type != null) {
            if (type.equals("meeting")) {
                updateMeetingLinkInDB();
            }
        }

        webView.addJavascriptInterface(new JavaScriptInterface(), "Android");

        if (isInternetAvailable()) {
            webView.loadUrl(url);
        } else {
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
        }

        fullScreeObserver();

    }

    private void fullScreeObserver() {
        // Monitor the layout for changes (e.g., keyboard showing/hiding)
        final View contentView = findViewById(android.R.id.content);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                contentView.getWindowVisibleDisplayFrame(r);
                int screenHeight = contentView.getRootView().getHeight();

                // Determine if the keyboard is shown
                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight > 150) { // If more than 150 pixels, it's probably a keyboard.
                    if (isFullscreen) {
                        toggleFullscreen(false);
                        isFullscreen = false;
                    }
                } else {
                    if (!isFullscreen) {
                        contentView.postDelayed(() -> {
                            toggleFullscreen(true);
                            isFullscreen = true;
                        }, 200); // Delay to ensure smooth transition
                    }
                }
            }
        });
    }

    private void updateMeetingLinkInDB() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (meetingId != null) {
                    FirebaseDatabase.getInstance().getReference().child("Meetings")
                            .child(meetingId).child("description")
                            .setValue(webView.getUrl());
                }
            }
        };
        handler.postDelayed(runnable, 5 * 60 * 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    public static void displayToastUnderView(Activity activity, View view, String text) {
        Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, view.getBottom());
        toast.show();
    }

    private void captureScreenshot() {
        displayToastUnderView(this, takeSSBtn, "Save to drive");
        Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        webView.draw(canvas);
        showSaveImageDialog(bitmap);
    }

    private void showSaveImageDialog(Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_capture_screen);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        ImageView screenShotImg = dialog.findViewById(R.id.capture_img);
        AppCompatButton saveBtn = dialog.findViewById(R.id.save_img_btn);
        AppCompatButton closeBtn = dialog.findViewById(R.id.close_capture_btn);

        screenShotImg.setImageBitmap(bitmap);
        saveBtn.setOnClickListener(view -> {
            dialog.dismiss();
            createNewFolder(bitmap);
        });

        closeBtn.setOnClickListener(view -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void createNewFolder(Bitmap bitmap) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String folderName = dateFormatter.format(new Date());
            DriveManager driveManager = new DriveManager(credential, folderName, bitmap, null, new DriveManager.DriveTaskListener() {
                @Override
                public void onDriveTaskCompleted(String id) {
                    WebViewActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WebViewActivity.this, "Image saved successfully...", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onDriveTaskFailed() {
                    WebViewActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WebViewActivity.this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            driveManager.execute();
        }
    }

    public class JavaScriptInterface {
        @android.webkit.JavascriptInterface
        public void onWordSelected(String selectedWord, float left, float top, float width, float height) {
            runOnUiThread(new Runnable() {
                @SuppressLint("UseCompatLoadingForDrawables")
                @Override
                public void run() {
                    float density = getResources().getDisplayMetrics().density;
                    int leftPx = (int) (left * density);
                    int topPx = (int) (top * density);
                    int widthPx = (int) (150);
                    int heightPx = (int) (150);
                    if (customIv != null) {
                        rootLayout.removeView(customIv);
                    }
                    customIv = new ImageView(WebViewActivity.this);
                    customIv.setImageResource(R.drawable.text_to_speech_icon);
                    customIv.setBackground(getDrawable(R.drawable.tts_icon_bg));
                    customIv.setPadding(10, 10, 10, 10);

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(widthPx, heightPx);
                    params.rightMargin = leftPx;
                    params.topMargin = topPx - heightPx;
                    customIv.setLayoutParams(params);

                    rootLayout.addView(customIv);
                    customIv.setOnClickListener(view -> {
                        webView.evaluateJavascript("(function() { return window.getSelection().toString(); })();", value -> {
                            String utteranceId = UUID.randomUUID().toString();
                            ttsManager.speak(value, utteranceId);
                        });
                    });

                    webView.evaluateJavascript(
                            "javascript:var selection = window.getSelection();" +
                                    "var range = selection.getRangeAt(0);" +
                                    "range.setStart(range.startContainer, 0);" +
                                    "range.setEnd(range.endContainer, range.endContainer.length);" +
                                    "Android.onSentenceSelected(range.toString().trim());", null);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onSentenceSelected(String selectedSentence) {
            // Handle the selected sentence
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("Selected sentence: ", selectedSentence);
                }
            });
        }

        @JavascriptInterface
        public void onTextDeselected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (customIv != null) {
                        rootLayout.removeView(customIv);
                    }
                }
            });
        }

        @JavascriptInterface
        public void onInputFocus() {
            runOnUiThread(() ->
                    toggleFullscreen(false));
        }

    }

    private void toggleFullscreen(boolean fullscreen) {
        View decorView = getWindow().getDecorView();
        if (fullscreen) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void setWebViewClient() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (isInternetAvailable()) {
                    view.loadUrl(url);
                } else {
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
                Log.d("url fin", url + " : ");
                progressBar.setVisibility(View.GONE);
                injectRtlScript(view);
                injectTextSelectionScript(view);
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                Log.d("==http", host + " : " + realm);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

            }
        });
    }

    private void injectTextSelectionScript(WebView view) {
        webView.evaluateJavascript(
                "javascript:document.addEventListener('selectionchange', function() {" +
                        "    var selection = window.getSelection();" +
                        "    var selectedWord = selection.toString().trim();" +
                        "    if (selectedWord !== '') {" +
                        "        var range = selection.getRangeAt(0);" +
                        "        var rect = range.getBoundingClientRect();" +
                        "        Android.onWordSelected(selectedWord, rect.left, rect.top, rect.width, rect.height);" + // Call Java method
                        "    } else {" +
                        "        Android.onTextDeselected();" +
                        "    }" +
                        "});", null);
    }

    private void injectRtlScript(WebView view) {
        String jsRtl = "var elements = document.querySelectorAll('p, div, span, li');"
                + "Array.prototype.forEach.call(elements, function(el) {"
                + "    el.style.direction = 'rtl';"
                + "    el.style.textAlign = 'right';"
                + "});";
        view.evaluateJavascript(jsRtl, null);
    }

    private void injectImgAtLineEndScript(WebView view) {
        String imageUrl = "https://cdn.vectorstock.com/i/1000x1000/53/07/24-hours-icon-clock-open-time-service-or-delivery-vector-34525307.webp";
        String javascriptCode5 = "(function() {" +
                "var lines = document.querySelectorAll('p, div, span, li');" +
                "lines.forEach(function(line) {" +
                "  if (line.childNodes.length === 1 && line.childNodes[0].nodeType === Node.TEXT_NODE) {" + // Check if the line contains only text
                "    var imageView = document.createElement('img');" +
                "    imageView.src = '" + imageUrl + "';" + // Set the src attribute to the image URL
                "    imageView.style.cursor = 'pointer';" +
                "    imageView.style.width = '25px';" + // Adjust width as needed
                "    imageView.style.height = '25px';" + // Adjust height as needed
                "    imageView.addEventListener('click', function() {" +
                "      var range = document.createRange();" +
                "      range.selectNodeContents(line);" +
                "      var selection = window.getSelection();" +
                "      selection.removeAllRanges();" +
                "      selection.addRange(range);" +
                "    });" +
                "    line.appendChild(imageView);" +
                "  }" +
                "});" +
                "})();";

        String javascriptCode = "(function() {" +
                "var chatMessages = document.querySelectorAll('.chat-message');" + // Adjust the selector to match your chat message elements
                "chatMessages.forEach(function(message) {" +
                "  var lastChild = message.lastElementChild;" + // Get the last child element of the chat message
                "  if (lastChild && lastChild.tagName.toLowerCase() !== 'img') {" + // Check if the last child is not already an image
                "    var imageView = document.createElement('img');" +
                "    imageView.src = '" + imageUrl + "';" + // Set the src attribute to the image URL
                "    imageView.style.cursor = 'pointer';" +
                "    imageView.style.width = '25px';" + // Adjust width as needed
                "    imageView.style.height = '25px';" + // Adjust height as needed
                "    imageView.addEventListener('click', function() {" +
                "      var range = document.createRange();" +
                "      range.selectNodeContents(message);" + // Select the entire chat message
                "      var selection = window.getSelection();" +
                "      selection.removeAllRanges();" +
                "      selection.addRange(range);" +
                "    });" +
                "    message.appendChild(imageView);" +
                "  }" +
                "});" +
                "})();";

        String javascriptCode1 = "(function() { var messages = document.querySelectorAll('.chat-message'); messages.forEach(function(message) { var lastChild = message.lastChild; if (lastChild && lastChild.tagName.toLowerCase() !== 'img') { var imageView = document.createElement('img'); imageView.src = '" + imageUrl + "'; imageView.style.cursor = 'pointer'; imageView.style.width = '50px'; imageView.style.height = '50px'; imageView.addEventListener('click', function() { var range = document.createRange(); range.selectNodeContents(message); var selection = window.getSelection(); selection.removeAllRanges(); selection.addRange(range); }); message.appendChild(imageView); } }); })();";


        String jsCode = "javascript:(function() {" +
                "var btn = document.createElement('button');" +
                "btn.innerHTML = 'JS Button';" +
                "btn.onclick = function() { alert('Button clicked!'); };" +
                "var chatComponent = document.querySelector('.chat-conversation');" +
                "chatComponent.insertBefore(btn, chatComponent.firstChild);" +
                "})()";


        view.evaluateJavascript(jsCode, null);
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
        webView.setInitialScale(1);
        webSettings.setUserAgentString(USER_AGENT);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_lyt);
        webView = findViewById(R.id.webView);
        closeBtn = findViewById(R.id.close_button);
        takeSSBtn = findViewById(R.id.screenshot_button);
        progressBar = findViewById(R.id.progress_bar);
        relativeLayout = findViewById(R.id.relative_layout);
        opeSettingBtn = findViewById(R.id.open_setting_btn);
        reloadBtn = findViewById(R.id.reload_btn);
    }

    private void setClickListeners() {
        takeSSBtn.setOnClickListener(view -> {
            captureScreenshot();
        });
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
            mActionMode = null;
//            getMenuInflater().inflate(R.menu.custom_menu, menu);
//            List menuItems = new ArrayList<>();
//            for (int i = 0; i < menu.size(); i++) {
//                menuItems.add(menu.getItem(i));
//            }
//            menu.clear();
//            int size = menuItems.size();
//            for (int i = 0; i < size; i++) {
//                addMenuItem(menu, (MenuItem) menuItems.get(i), i, true);
//            }
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
            UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                }

                @Override
                public void onError(String utteranceId) {
                }
            };
            textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
        }

        public void speak(String text, String utteranceId) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            textToSpeech.setPitch(0.9f);
            textToSpeech.setSpeechRate(0.75f);
        }

        public void shutdown() {
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onResume() {
        toggleFullscreen(true);
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
        if (networkInfo == null) {
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
            return false;
        }
        if (networkInfo.isConnectedOrConnecting()) {
            webView.setVisibility(View.VISIBLE);
            relativeLayout.setVisibility(View.GONE);
            return true;
        } else {
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            relativeLayout.setVisibility(View.VISIBLE);
            return false;
        }
    }

}
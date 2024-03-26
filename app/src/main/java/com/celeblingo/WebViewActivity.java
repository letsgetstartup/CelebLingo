package com.celeblingo;

import static com.celeblingo.MainActivity.extractVideoId;
import static com.celeblingo.MainActivity.extractYTId;
import static com.celeblingo.helper.Utils.hideProgressDialog;
import static com.celeblingo.helper.Utils.showProgressDialog;

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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.celeblingo.helper.BaseActivity;
import com.celeblingo.helper.Constants;
import com.celeblingo.helper.DriveManager;
import com.celeblingo.helper.HtmlHelper;
import com.celeblingo.model.Meetings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewActivity extends BaseActivity implements MenuItem.OnMenuItemClickListener {
    private String url, type, meetingId, meetingName;
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
    private Handler autoSSHandler = new Handler();
    private Runnable autoSSRunable = this::captureAutoScreenshot;
    private Meetings currentMeeting;
    private String videoUrl, htmlUrl;
    private String youtubeVideoId = null;
    private Dialog dialog;
    private TextView titleTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        url = getIntent().getStringExtra("url");
        type = getIntent().getStringExtra("type");
        meetingId = getIntent().getStringExtra("meetingId");
        initViews();

        ttsManager = new TTSManager(this);
        dialog = new Dialog(this);

        paragraphCount = 1;

        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);


        setClickListeners();
        setWebViewSettings();
        setWebViewClient();

        if (type != null) {
            if (type.equals("meeting")) {
                getMeetingName();
                updateMeetingLinkInDB();
                autoSSHandler.postDelayed(autoSSRunable, 10 * 60 * 1000);
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

    private void getMeetingName() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Meetings meetings = dataSnapshot.getValue(Meetings.class);
                        assert meetings != null;
                        if (meetings.getId().equals(meetingId)) {
                            videoUrl = meetings.getVideoUrl();
                            htmlUrl = meetings.getHtmlUrl();
                            currentMeeting = meetings;
                            titleTxt.setText(meetings.getSummary());
                            meetingName = meetings.getSummary();
                            DateTime eventEndTime = new DateTime(meetings.getEndTime());
                            long fiveMinutesBeforeEnd = eventEndTime.getValue() - 5 * 60 * 1000;
                            Handler handler = new Handler(Looper.getMainLooper());
                            Runnable showDialogRunnable = () -> showEventEndDialog(meetings);
                            long delay = fiveMinutesBeforeEnd - System.currentTimeMillis();
                            handler.postDelayed(showDialogRunnable, delay);
                            //
                            long oneMinuteBeforeEnd = eventEndTime.getValue() - 60000;
                            long delayToShowDialog = oneMinuteBeforeEnd - System.currentTimeMillis();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                showAlertDialogWithTimer();
                            }, delayToShowDialog);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showAlertDialogWithTimer() {
        final AlertDialog[] dialog = new AlertDialog[1];
        CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                if (dialog[0] != null) {
                    dialog[0].setMessage("Class ended. Chat will be close in " + millisUntilFinished / 1000 + " seconds");
                }
            }

            public void onFinish() {
                startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
        }.start();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Class Ended");
        builder.setMessage("Class ended...");
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
            countDownTimer.cancel();
            dialog[0].dismiss();
        });
        dialog[0] = builder.create();
        dialog[0].show();
        Button negativeButton = dialog[0].getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }


    int i = 0;

    private void createAndSaveHtml(DatabaseReference reference, Meetings meetings, String type) {
        StringBuilder imagesBuilder = new StringBuilder();
        webView.evaluateJavascript(
                "(function() {" +
                        "    var images = document.getElementsByTagName('img');" +
                        "    var srcList = [];" +
                        "    for (var i = 0; i < images.length; i++) {" +
                        "        srcList.push(images[i].src);" +
                        "    }" +
                        "    return srcList.toString();" + // Convert array to string to pass back to Java
                        "})()",
                value -> {
                    String[] urls = value.replaceAll("^\"|\"$", "").split(",");
                    if (urls.length == 0) {
                        String allImageUrl = imagesBuilder.toString();
                        Log.d("==images", allImageUrl);
                        webView.evaluateJavascript(
                                "(function() { return document.body.innerText; })();",
                                new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        String modifiedValue = value.replace("\\n", "<br>");

                                        modifiedValue = modifiedValue.replaceAll("^\"|\"$", "");


                                        Log.d("==WebViewText", modifiedValue);

                                        String htmlContent = "<!DOCTYPE html><html lang=\"he\" dir=\"rtl\">" +
                                                "<head><meta charset=\"UTF-8\"><title>" + meetings.getSummary() + "</title><style>body { direction: rtl; text-align: right; }</style></head>" +
                                                "<body>" + modifiedValue + allImageUrl + "</body></html>";

                                        String html = HtmlHelper.createChatDataHtml(meetings.getSummary(), modifiedValue, allImageUrl);

                                        Log.d("==new html", html);

                                        File htmlFile = createHtmlFile(html, meetings.getSummary());

                                        if (htmlFile != null) {
                                            uploadFileToFirebaseStorage(htmlFile, task -> {
                                                if (task.isSuccessful()) {
                                                    Uri downloadUri = task.getResult();
                                                    reference.child(meetings.getId())
                                                            .child("htmlUrl").setValue(downloadUri.toString());
                                                    new Thread(() -> updateCalenderEvent(meetings, downloadUri.toString())).start();

                                                    Log.d("==file", "File uploaded with URI: " + downloadUri.toString());
                                                    deleteFile(htmlFile);
                                                } else {
                                                    // Handle failure
                                                    hideProgressDialog();
                                                    Log.e("==file", "Upload failed", task.getException());
                                                }
                                            });
                                        }

                                        if (type.equals("close")){
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    hideProgressDialog();
                                                    startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                                }
                                            },1000);

                                        }

                                    }
                                }
                        );
                    }
                    Pattern pattern = Pattern.compile("gravatar\\.com.*cdn\\.auth0\\.com");
                    i = 0;
                    for (String url : urls) {
                        Matcher matcher = pattern.matcher(url);
                        if (!matcher.find()) {
                            Log.d("==imagggggg", url + " " + urls.length);
                            //uploadImageToFirebase(url.trim());
                            if (!url.isEmpty()) {

                                ExecutorService executorService = Executors.newFixedThreadPool(4);
                                executorService.submit(() -> {
                                    try {
                                        // Download the image
                                        URL url1 = new URL(url.trim());
                                        HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
                                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                                        File file = File.createTempFile("image", ".jpg", getCacheDir());
                                        FileOutputStream fileOutputStream = new FileOutputStream(file);

                                        byte[] buffer = new byte[1024];
                                        int read;
                                        while ((read = inputStream.read(buffer)) != -1) {
                                            fileOutputStream.write(buffer, 0, read);
                                        }
                                        fileOutputStream.close();
                                        inputStream.close();

                                        // Upload the image to Firebase Storage
                                        Uri fileUri = Uri.fromFile(file);

                                        FirebaseStorage storage = FirebaseStorage.getInstance();
                                        StorageReference storageRef = storage.getReference().child("images/" + file.getName());

                                        storageRef.putFile(fileUri)
                                                .addOnSuccessListener(taskSnapshot -> {
                                                    storageRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Uri> task) {
                                                            if (task.isSuccessful()) {
                                                                Uri downloadUri = task.getResult();
                                                                System.out.println("==upload Download URL:" + i + " " + downloadUri.toString());
                                                                // Use downloadUri as needed
                                                                imagesBuilder.append("<div class=\"image-container\"><img src=\"")
                                                                        .append(downloadUri.toString())
                                                                        .append("\" alt=\"Image\"><div class=\"caption\"></div></div>");
                                                                // Cleanup temp file
                                                                i = i + 1;
                                                                if (i == (urls.length)) {
                                                                    String allImageUrl = imagesBuilder.toString();
                                                                    Log.d("==images", allImageUrl);
                                                                    webView.evaluateJavascript(
                                                                            "(function() { return document.body.innerText; })();",
                                                                            new ValueCallback<String>() {
                                                                                @Override
                                                                                public void onReceiveValue(String value) {
                                                                                    String modifiedValue = value.replace("\\n", "<br>");

                                                                                    modifiedValue = modifiedValue.replaceAll("^\"|\"$", "");


                                                                                    Log.d("==WebViewText", modifiedValue);

                                                                                    String htmlContent = "<!DOCTYPE html><html lang=\"he\" dir=\"rtl\">" +
                                                                                            "<head><meta charset=\"UTF-8\"><title>" + meetings.getSummary() + "</title><style>body { direction: rtl; text-align: right; }</style></head>" +
                                                                                            "<body>" + modifiedValue + allImageUrl + "</body></html>";

                                                                                    String html = HtmlHelper.createChatDataHtml(meetings.getSummary(), modifiedValue, allImageUrl);

                                                                                    Log.d("==new html", html);

                                                                                    File htmlFile = createHtmlFile(html, meetings.getSummary());

                                                                                    if (htmlFile != null) {
                                                                                        uploadFileToFirebaseStorage(htmlFile, task -> {
                                                                                            if (task.isSuccessful()) {
                                                                                                Uri downloadUri = task.getResult();
                                                                                                reference.child(meetings.getId())
                                                                                                        .child("htmlUrl").setValue(downloadUri.toString());
                                                                                                new Thread(() -> updateCalenderEvent(meetings, downloadUri.toString())).start();

                                                                                                Log.d("==file", "File uploaded with URI: " + downloadUri.toString());
                                                                                                deleteFile(htmlFile);
                                                                                            } else {
                                                                                                // Handle failure
                                                                                                hideProgressDialog();
                                                                                                Log.e("==file", "Upload failed", task.getException());
                                                                                            }
                                                                                        });
                                                                                    }
                                                                                    if (type.equals("close")){
                                                                                        new Handler().postDelayed(new Runnable() {
                                                                                            @Override
                                                                                            public void run() {
                                                                                                hideProgressDialog();
                                                                                                startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                                                                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                                                                            }
                                                                                        },3000);

                                                                                    }

                                                                                }
                                                                            }
                                                                    );
                                                                }
                                                                new File(fileUri.getPath()).delete();
                                                            }
                                                        }
                                                    });
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Failure handling
                                                    e.printStackTrace();
                                                });

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                i = i + 1;
                                if (i == (urls.length)) {
                                    String allImageUrl = imagesBuilder.toString();
                                    Log.d("==images", allImageUrl);
                                    webView.evaluateJavascript(
                                            "(function() { return document.body.innerText; })();",
                                            new ValueCallback<String>() {
                                                @Override
                                                public void onReceiveValue(String value) {
                                                    String modifiedValue = value.replace("\\n", "<br>");

                                                    modifiedValue = modifiedValue.replaceAll("^\"|\"$", "");


                                                    Log.d("==WebViewText", modifiedValue);

                                                    String htmlContent = "<!DOCTYPE html><html lang=\"he\" dir=\"rtl\">" +
                                                            "<head><meta charset=\"UTF-8\"><title>" + meetings.getSummary() + "</title><style>body { direction: rtl; text-align: right; }</style></head>" +
                                                            "<body>" + modifiedValue + allImageUrl + "</body></html>";

                                                    String html = HtmlHelper.createChatDataHtml(meetings.getSummary(), modifiedValue, allImageUrl);

                                                    Log.d("==new html", html);

                                                    File htmlFile = createHtmlFile(html, meetings.getSummary());

                                                    if (htmlFile != null) {
                                                        uploadFileToFirebaseStorage(htmlFile, task -> {
                                                            if (task.isSuccessful()) {
                                                                Uri downloadUri = task.getResult();
                                                                reference.child(meetings.getId())
                                                                        .child("htmlUrl").setValue(downloadUri.toString());
                                                                new Thread(() -> updateCalenderEvent(meetings, downloadUri.toString())).start();

                                                                Log.d("==file", "File uploaded with URI: " + downloadUri.toString());
                                                                deleteFile(htmlFile);
                                                            } else {
                                                                // Handle failure
                                                                hideProgressDialog();
                                                                Log.e("==file", "Upload failed", task.getException());
                                                            }
                                                        });
                                                    }

                                                    if (type.equals("close")){
                                                        new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                hideProgressDialog();
                                                                startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                                            }
                                                        },3000);

                                                    }

                                                }
                                            }
                                    );
                                }
                            }
                        } else {
                            i = i + 1;
                            if (i == (urls.length)) {
                                String allImageUrl = imagesBuilder.toString();
                                Log.d("==images", allImageUrl);
                                webView.evaluateJavascript(
                                        "(function() { return document.body.innerText; })();",
                                        new ValueCallback<String>() {
                                            @Override
                                            public void onReceiveValue(String value) {
                                                String modifiedValue = value.replace("\\n", "<br>");

                                                modifiedValue = modifiedValue.replaceAll("^\"|\"$", "");


                                                Log.d("==WebViewText", modifiedValue);

                                                String htmlContent = "<!DOCTYPE html><html lang=\"he\" dir=\"rtl\">" +
                                                        "<head><meta charset=\"UTF-8\"><title>" + meetings.getSummary() + "</title><style>body { direction: rtl; text-align: right; }</style></head>" +
                                                        "<body>" + modifiedValue + allImageUrl + "</body></html>";

                                                String html = HtmlHelper.createChatDataHtml(meetings.getSummary(), modifiedValue, allImageUrl);

                                                Log.d("==new html", html);

                                                File htmlFile = createHtmlFile(html, meetings.getSummary());

                                                if (htmlFile != null) {
                                                    uploadFileToFirebaseStorage(htmlFile, task -> {
                                                        if (task.isSuccessful()) {
                                                            Uri downloadUri = task.getResult();
                                                            reference.child(meetings.getId())
                                                                    .child("htmlUrl").setValue(downloadUri.toString());
                                                            new Thread(() -> updateCalenderEvent(meetings, downloadUri.toString())).start();

                                                            Log.d("==file", "File uploaded with URI: " + downloadUri.toString());
                                                            deleteFile(htmlFile);
                                                        } else {
                                                            // Handle failure
                                                            hideProgressDialog();
                                                            Log.e("==file", "Upload failed", task.getException());
                                                        }
                                                    });
                                                }

                                            }
                                        }
                                );
                            }
                        }
                    }
                }
        );
    }

    public void downloadImageAndUploadToFirebase(String imageUrl, String firebasePath) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        executorService.submit(() -> {
            try {
                // Download the image
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                File file = File.createTempFile("image", ".jpg", getCacheDir());
                FileOutputStream fileOutputStream = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, read);
                }
                fileOutputStream.close();
                inputStream.close();

                // Upload the image to Firebase Storage
                Uri fileUri = Uri.fromFile(file);
                uploadToFirebaseStorage(fileUri, "images/" + file.getName());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void uploadToFirebaseStorage(Uri fileUri, String firebasePath) {
        Log.d("==upload fb", firebasePath);

    }

    private void updateCalenderEvent(Meetings meetings, String htmlUrl) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            this, Arrays.asList(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY));
            credential.setSelectedAccount(account.getAccount());
            Calendar mCalendarService = new Calendar.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("CelebLingo").build();
            Event event = null;
            try {
                event = mCalendarService.events().get("primary", meetings.getId()).execute();

                String description = "gptUrl: " + meetings.getGptUrl() + "\n" +
                        "driveUrl: " + meetings.getDriveUrl() + "\n" +
                        "videoUrl: " + videoUrl + "\n" +
                        "htmlUrl: " + htmlUrl;

                event.setDescription(description);
                Event updatedEvent = mCalendarService.events().update("primary", event.getId(), event).execute();
                System.out.println("==upd" + updatedEvent.getUpdated() + " " + updatedEvent.getDescription());
                hideProgressDialog();
            } catch (IOException e) {
                hideProgressDialog();
                Log.d("==upd err", e.getMessage() + "");
            }
        } else {
            hideProgressDialog();
        }
    }

    private void fullScreeObserver() {
        final View contentView = findViewById(android.R.id.content);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            contentView.getWindowVisibleDisplayFrame(r);
            int screenHeight = contentView.getRootView().getHeight();

            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > 150) {
                if (isFullscreen) {
                    toggleFullscreen(false);
                    isFullscreen = false;
                }
            } else {
                if (!isFullscreen) {
                    contentView.postDelayed(() -> {
                        toggleFullscreen(true);
                        isFullscreen = true;
                    }, 200);
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
//                    FirebaseDatabase.getInstance().getReference().child("Meetings")
//                            .child(meetingId).child("description")
//                            .setValue(webView.getUrl());
                    showProgressDialog(WebViewActivity.this);
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    Meetings meetings = dataSnapshot.getValue(Meetings.class);
                                    String upToNCharacters = meetingId.substring(0, Math.min(meetingId.length(), 24));
                                    assert meetings != null;
                                    if (meetings.getId().contains(upToNCharacters)) {
                                        Log.d("==nch", upToNCharacters);
                                        reference.child(meetings.getId())
                                                .child("gptUrl")
                                                .setValue(webView.getUrl());
                                    }
                                }
                                hideProgressDialog();
                            } else {
                                hideProgressDialog();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            hideProgressDialog();
                        }
                    });
                }
            }
        };
        handler.postDelayed(runnable, 5 * 60 * 1000);
    }

    private void showEventEndDialog(Meetings meetings) {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_meeting_end);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        YouTubePlayerView youTubePlayerView = dialog.findViewById(R.id.youtube_view);
        AppCompatButton closeBtn = dialog.findViewById(R.id.close_meeting_btn);
        AppCompatButton closeChatBtn = dialog.findViewById(R.id.close_chat_btn);

        getLifecycle().addObserver(youTubePlayerView);

        youtubeVideoId = extractVideoId(Constants.DEFAULT_END_VIDEO_URL);
        if (youtubeVideoId.equals("NoId")) {
            youtubeVideoId = extractYTId(Constants.DEFAULT_END_VIDEO_URL);
        }
        Log.d("==videoid", youtubeVideoId + "");
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer) {
                youTubePlayer.cueVideo(youtubeVideoId, 0);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                super.onStateChange(youTubePlayer, state);
            }

            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                super.onError(youTubePlayer, error);
                Log.d("==youtube", error + "");
                youTubePlayer.loadVideo(youtubeVideoId, 0);
            }
        });

        closeBtn.setOnClickListener(view -> {
            youTubePlayerView.release();
            dialog.dismiss();
        });

        closeChatBtn.setOnClickListener(view -> {
            dialog.dismiss();
            youTubePlayerView.release();
            startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        if (autoSSHandler != null && autoSSRunable != null) {
            autoSSHandler.removeCallbacks(autoSSRunable);
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

    private void captureAutoScreenshot() {
        Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        webView.draw(canvas);
        showProgressDialog(WebViewActivity.this);
        createNewFolder(bitmap);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        if (currentMeeting != null) {
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Meetings meetings = dataSnapshot.getValue(Meetings.class);
                            assert meetings != null;
                            if (meetings.getId().equals(meetingId)) {
                                htmlUrl = meetings.getHtmlUrl();
                                currentMeeting = meetings;
                                createAndSaveHtml(reference, currentMeeting, "screenshot");
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        autoSSHandler.postDelayed(autoSSRunable, 10 * 60 * 1000);
    }

    private void showSaveImageDialog(Bitmap bitmap) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_capture_screen);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        ImageView screenShotImg = dialog.findViewById(R.id.capture_img);
        AppCompatButton saveBtn = dialog.findViewById(R.id.save_img_btn);
        AppCompatButton closeBtn = dialog.findViewById(R.id.close_capture_btn);

        screenShotImg.setImageBitmap(bitmap);
        saveBtn.setOnClickListener(view -> {
            dialog.dismiss();
            showProgressDialog(WebViewActivity.this);
            createNewFolder(bitmap);
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
            if (currentMeeting != null) {
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                Meetings meetings = dataSnapshot.getValue(Meetings.class);
                                assert meetings != null;
                                if (meetings.getId().equals(meetingId)) {
                                    htmlUrl = meetings.getHtmlUrl();
                                    currentMeeting = meetings;
                                    createAndSaveHtml(reference, currentMeeting, "screenshot");
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
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
                            this, Arrays.asList(DriveScopes.DRIVE_FILE, CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY));
            credential.setSelectedAccount(account.getAccount());
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String folderName;
            if (type != null) {
                if (type.equals("meeting")) {
                    if (meetingName != null) {
                        folderName = meetingName;
                    } else {
                        folderName = dateFormatter.format(new Date());
                    }
                } else {
                    folderName = dateFormatter.format(new Date());
                }
            } else {
                folderName = dateFormatter.format(new Date());
            }
            DriveManager driveManager = new DriveManager(credential, folderName, bitmap,
                    null, meetingId, webView.getUrl(), videoUrl, htmlUrl,
                    new DriveManager.DriveTaskListener() {
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
        } else {
            hideProgressDialog();
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
                if (!url.startsWith("https://auth0.openai.com/u/login/") &&
                        !url.startsWith("https://auth0.openai.com/u/signup/")) {
                    injectRtlScript(view);
                }
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
        titleTxt = findViewById(R.id.title_txt);
        rootLayout = findViewById(R.id.root_lyt);
        webView = findViewById(R.id.webView);
        closeBtn = findViewById(R.id.close_button);
        takeSSBtn = findViewById(R.id.screenshot_button);
        progressBar = findViewById(R.id.progress_bar);
        relativeLayout = findViewById(R.id.relative_layout);
        opeSettingBtn = findViewById(R.id.open_setting_btn);
        reloadBtn = findViewById(R.id.reload_btn);
    }

    private File createHtmlFile(String htmlContent, String name) {
        File htmlFile = null;
        try {
            htmlFile = File.createTempFile(name, ".html", getCacheDir());
            FileWriter writer = new FileWriter(htmlFile);
            writer.write(htmlContent);
            writer.close();
            Log.d("==file", "created" + "");
        } catch (IOException e) {
            Log.d("==file", e.getMessage() + "");

        }
        return htmlFile;
    }

    private void uploadFileToFirebaseStorage(File file, OnCompleteListener<Uri> onCompleteListener) {
        Uri fileUri = Uri.fromFile(file);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("htmlFiles/" + file.getName());
        storageRef.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.d("==file", task.getException() + " ");
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(onCompleteListener);
    }

    private void deleteFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d("==file", "File deleted successfully");
            } else {
                Log.d("==file", "Failed to delete file");
            }
        }
    }

    private void setClickListeners() {
        takeSSBtn.setOnClickListener(view -> {
            captureScreenshot();
        });
        closeBtn.setOnClickListener(view -> {
            showProgressDialog(WebViewActivity.this);
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
            if (currentMeeting != null) {
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                Meetings meetings = dataSnapshot.getValue(Meetings.class);
                                assert meetings != null;
                                if (meetings.getId().equals(meetingId)) {
                                    htmlUrl = meetings.getHtmlUrl();
                                    currentMeeting = meetings;
                                    createAndSaveHtml(reference, currentMeeting, "close");
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideProgressDialog();
                        startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }
                });
            }else {
                hideProgressDialog();
                startActivity(new Intent(WebViewActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
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
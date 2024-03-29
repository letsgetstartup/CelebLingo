package com.celeblingo;

import static com.celeblingo.helper.Constants.DEFAULT_DRIVE_URL;
import static com.celeblingo.helper.Constants.DEFAULT_EVENT_DESCRIPTION;
import static com.celeblingo.helper.Constants.DEFAULT_GPT_URL;
import static com.celeblingo.helper.Constants.DEFAULT_VIDEO_URL;
import static com.celeblingo.helper.Utils.hideProgressDialog;
import static com.celeblingo.helper.Utils.showProgressDialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.celeblingo.fragment.ConfigFragment;
import com.celeblingo.fragment.HomeFragment;
import com.celeblingo.fragment.MeetingFragment;
import com.celeblingo.fragment.StartFragment;
import com.celeblingo.helper.BaseActivity;
import com.celeblingo.helper.Utils;
import com.celeblingo.model.Meetings;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends BaseActivity {
    private static final int RC_SIGN_IN = 121, REQUEST_AUTHORIZATION = 1001;
    private GoogleAccountCredential mCredential = null;
    private com.google.api.services.calendar.Calendar mService;
    private String meetingId, joinMeetingUrl = null;
    private Dialog dialog;
    private TextView organizerNameTxt, summaryTtx, descriptionTxt,
            startTimeTxt, endTimeTxt, attendeesTxt;
    private YouTubePlayerView youTubePlayerView;
    private String youtubeVideoId;
    private BottomNavigationView navigationView;
    private String web_client = "333558564968-81tk2qejtq6gr1bppa9nm7qkmjl3117b.apps.googleusercontent.com";
    private boolean isIdExists = false, isSameOrganizer = false, isMeetingIdExist = false;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isActivityVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        navigationView = findViewById(R.id.bottom_nav_view);


        dialog = new Dialog(this);

        setUpBottomNavView();
        startSystemAlertWindowPermission();
        showJoinMeetingDialog();
        Utils.getDefaultUrlsFromFB();
        Utils.checkUserData(this);

    }

    private void setUpBottomNavView() {
        navigationView.setSelectedItemId(R.id.navigation_meetings);
        replaceFragment(getSupportFragmentManager(), new MeetingFragment(), R.id.fragment_container);
        navigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                replaceFragment(getSupportFragmentManager(), new HomeFragment(), R.id.fragment_container);
                return true;
            } else if (id == R.id.navigation_meetings) {
//                initCredentials();
//                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(MainActivity.this);
//                if (account != null) {
//                    mCredential.setSelectedAccountName(account.getEmail());
//                    getResultsFromApi();
//                } else {
//                    googleSignIn();
//                }
                replaceFragment(getSupportFragmentManager(), new MeetingFragment(), R.id.fragment_container);
                return true;
            } else if (id == R.id.navigation_config) {
//                initCredentials();
//                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(MainActivity.this);
//                if (account != null) {
//                    mCredential.setSelectedAccountName(account.getEmail());
//                    getResultsFromApi();
//                } else {
//                    googleSignIn();
//                }
                replaceFragment(getSupportFragmentManager(), new ConfigFragment(), R.id.fragment_container);
                return true;
            } else if (id == R.id.navigation_start) {
                replaceFragment(getSupportFragmentManager(), new StartFragment(), R.id.fragment_container);
                return true;
            }
            return false;
        });

    }

    private void replaceFragment(FragmentManager fragmentManager, Fragment fragment, int fragmentContainer) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(fragmentContainer, fragment);
        transaction.commit();
    }

    private boolean compareDate(String startDate) {
        Date currentDate = new Date();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+03:30")); // Set the provided timezone
        Date providedDate = null;
        try {
            providedDate = dateFormat.parse(startDate);
            if (currentDate.compareTo(providedDate) < 0) {
                System.out.println("Current date and time is before the provided datetime.");
                return false;
            } else if (currentDate.compareTo(providedDate) > 0) {
                System.out.println("Current date and time is after the provided datetime.");
                long fiveMinutesBefore = providedDate.getTime() + (5 * 60 * 1000);
                if (currentDate.getTime() <= fiveMinutesBefore) {
                    Log.d("===", "Less than 5 minutes remaining.");
                    return true;
                }
                return false;
            } else {
                System.out.println("Current date and time is equal to the provided datetime.");
                return true;

            }
        } catch (ParseException e) {
            //e.printStackTrace();
            return false;
        }

    }

    private void getMeetingData() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Meetings meetings = dataSnapshot.getValue(Meetings.class);
                        assert meetings != null;
                        Log.d("==event ", meetings.getId());
                        if (compareDate(meetings.getStartTime())) {
                            setMeetingDataToDialog(reference, meetings);
                            return;
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        if (isActivityVisible) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getMeetingData();
                }
            }, 2 * 60 * 1000);
        }

    }

    @SuppressLint("SetTextI18n")
    private void setMeetingDataToDialog(DatabaseReference meetingRef, Meetings meetings) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(MainActivity.this);
        if (account == null) {
            return;
        }
        meetingRef.child(meetings.getId()).child("Organizer")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                Meetings.Organizer organizer = dataSnapshot.getValue(Meetings.Organizer.class);
                                assert organizer != null;
                                Log.d("==organ", organizer.getEmail());
                                if (organizer.getEmail().equals(account.getEmail())) {
                                    if (!isFinishing()) {
                                        if (dialog != null && !dialog.isShowing()) {
                                            dialog.show();
                                        }
                                    }
                                }
                                organizerNameTxt.setText(organizer.getEmail());
                            }
                        } else {
                            organizerNameTxt.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        meetingRef.child(meetings.getId()).child("Attendees")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String attendeeEmail = "";
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                Meetings.Attendee attendee = dataSnapshot.getValue(Meetings.Attendee.class);
                                assert attendee != null;
                                Log.d("==attt", attendee.getEmail());
                                attendeeEmail = attendeeEmail + attendee.getDisplayName() + "\n";
                                attendeesTxt.setText(attendeeEmail);
                            }
                        } else {
                            attendeesTxt.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        summaryTtx.setText(meetings.getSummary());
        descriptionTxt.setText(meetings.getDescription());
        startTimeTxt.setText(extractDateTime(meetings.getStartTime()) +
                " - " +
                extractTime(meetings.getEndTime()));
        endTimeTxt.setText(extractDateTime(meetings.getEndTime()));
        joinMeetingUrl = meetings.getGptUrl();
        meetingId = meetings.getId();
        youtubeVideoId = extractVideoId(meetings.getVideoUrl());
        if (youtubeVideoId.equals("NoId")) {
            youtubeVideoId = extractYTId(meetings.getVideoUrl());
        }
        Log.d("==videoid", youtubeVideoId + "");
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(youtubeVideoId, 0);
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
    }

    private String extractDateTime(String date) {
        OffsetDateTime odt = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            odt = OffsetDateTime.parse(date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E,dd MMM yyyy HH:mm");
            return odt.format(formatter);
        }
        return date;
    }

    private String extractTime(String date) {
        OffsetDateTime odt = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            odt = OffsetDateTime.parse(date);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return odt.format(formatter);
        }
        return date;
    }

    public static String extractYTId(String ytUrl) {
        String vId = null;
        Pattern pattern = Pattern.compile(
                "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()) {
            vId = matcher.group(1);
        }
        return vId;
    }

    public static String extractVideoId(String url) {
        String pattern = "(?:https?://)?(?:www\\.)?(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "NoId";
    }

    private void showJoinMeetingDialog() {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_meeting_popup);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);


        AppCompatButton closeBtn = dialog.findViewById(R.id.close_meeting_btn);
        organizerNameTxt = dialog.findViewById(R.id.organizer_display_name_txt);
        summaryTtx = dialog.findViewById(R.id.summary_txt);
        descriptionTxt = dialog.findViewById(R.id.description_txt);
        startTimeTxt = dialog.findViewById(R.id.start_time_txt);
        endTimeTxt = dialog.findViewById(R.id.end_time_txt);
        attendeesTxt = dialog.findViewById(R.id.attendee_email_txt);
        youTubePlayerView = dialog.findViewById(R.id.youtube_view);
        AppCompatButton joinBtn = dialog.findViewById(R.id.join_meeting_btn);

        getLifecycle().addObserver(youTubePlayerView);

        closeBtn.setOnClickListener(view -> {
            dialog.dismiss();
        });

        joinBtn.setOnClickListener(view -> {
            youTubePlayerView.release();
            String url = extractUrl(joinMeetingUrl);
            Log.d("==url", url + " ");
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, WebViewActivity.class)
                    .putExtra("url", url)
                    .putExtra("type", "meeting")
                    .putExtra("meetingId", meetingId));
        });

        getMeetingData();

    }

    public static String extractUrl(String input) {
        if (input == null) {
            return DEFAULT_GPT_URL;
        }
        String htmlPattern = "<a[^>]+href=\"(.*?)\"[^>]*>(.*?)</a>";
        String plainUrlPattern = "https?://[^\\s]+";

        Pattern pattern = Pattern.compile(htmlPattern);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        pattern = Pattern.compile(plainUrlPattern);
        matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(0);
        }

        return DEFAULT_GPT_URL;
    }

    @Override
    protected void onResume() {
        isActivityVisible = true;
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);
        super.onResume();
    }

    @Override
    protected void onPause() {
        isActivityVisible = false;
        super.onPause();
    }

    private void startSystemAlertWindowPermission() {
        try {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        } catch (Exception e) {
            Log.e("==TAG", "[startSystemAlertWindowPermission] error:", e);
        }
    }

    private void initCredentials() {
        mCredential = GoogleAccountCredential.usingOAuth2(
                        this, Arrays.asList(CalendarScopes.CALENDAR,
                                CalendarScopes.CALENDAR_READONLY))
                .setBackOff(new ExponentialBackOff());
        initCalendarBuild(mCredential);
    }

    private void initCalendarBuild(GoogleAccountCredential credential) {
        Calendar.Builder builder = null;
        builder = new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("CelebLingo");
        mService = builder.build();
    }

    private void googleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(CalendarScopes.CALENDAR_READONLY),
                        new Scope(CalendarScopes.CALENDAR),
                        new Scope(CalendarScopes.CALENDAR_EVENTS_READONLY)
                        , new Scope(DriveScopes.DRIVE_FILE))
                .requestIdToken(web_client)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            mCredential.setSelectedAccountName(account.getEmail());
            getResultsFromApi();
        } catch (ApiException e) {
            if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                // Consent required, attempt to get consent
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, REQUEST_AUTHORIZATION);
            } else {
                Log.w("SignInError", "signInResult:failed code=" + e.getStatusCode());
            }
            replaceFragment(getSupportFragmentManager(), new MeetingFragment(), R.id.fragment_container);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if (requestCode == REQUEST_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                // Consent granted, try to obtain the token again.
                getAuthToken();
            } else {
                // User denied consent.
                Log.e("AuthError", "User denied consent.");
            }
        }
    }

    private void getResultsFromApi() {
        replaceFragment(getSupportFragmentManager(), new MeetingFragment(), R.id.fragment_container);
        makeRequestTask();
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    Exception mLastError = null;

    private void makeRequestTask() {
        handler.post(() -> {
            showProgressDialog(MainActivity.this);
        });

        executor.submit(() -> {
            List<Meetings> result = null;
            try {
                result = getDataFromCalendar();
            } catch (Exception e) {
                mLastError = e;
                hideProgressDialog();
            }

            final List<Meetings> finalResult = result;
            handler.post(() -> {
                if (finalResult == null || finalResult.isEmpty()) {
                    Log.d("Google", "No data");
                    hideProgressDialog();
                } else {
                    saveMeetingDataToFirebase(finalResult);
                }

                if (mLastError != null) {
                    if (mLastError instanceof UserRecoverableAuthIOException) {
                        googleSignIn();
                    } else if (mLastError instanceof UserRecoverableAuthException) {
                        getAuthToken();
                    } else if (mLastError != null) {
                        Log.d("==er ", "The following error occurred:\n" + mLastError.getMessage());
                    } else {
                        Log.d("==err", "Request cancelled.");
                    }
                }
            });
        });
    }

    private void getAuthToken() {
        new Thread(() -> {
            try {
                final String scope = "oauth2:" + CalendarScopes.CALENDAR;
                final String accountName = GoogleSignIn.getLastSignedInAccount(this).getEmail();

                final String token = GoogleAuthUtil.getToken(getApplicationContext(), accountName, scope);

            } catch (UserRecoverableAuthException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (GoogleAuthException | IOException e) {
                Log.e("AuthError", "Failed to obtain OAuth token", e);
            }
        }).start();
    }

    private void saveMeetingDataToFirebase(List<Meetings> meetingsList) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        for (int i = 0; i < meetingsList.size(); i++) {
            String id = meetingsList.get(i).getId();
            String summary = meetingsList.get(i).getSummary();
            String startTime = meetingsList.get(i).getStartTime();
            String endTime = meetingsList.get(i).getEndTime();
            String description = meetingsList.get(i).getDescription();
            String organizer = meetingsList.get(i).getOrganizer().getEmail();
            String gptUrl = meetingsList.get(i).getGptUrl();
            String driveUrl = meetingsList.get(i).getDriveUrl();
            String videoUrl = meetingsList.get(i).getVideoUrl();
            String htmlUrl = meetingsList.get(i).getHtmlUrl();
            //String displayName = meetingsList.get(i).getOrganizer().getDisplayName();
            boolean self = meetingsList.get(i).getOrganizer().isSelf();
            HashMap<String, Object> eventHashMap = new HashMap<>();
            eventHashMap.put("id", id);
            eventHashMap.put("summary", summary);
            eventHashMap.put("startTime", startTime);
            eventHashMap.put("endTime", endTime);
            eventHashMap.put("description", description);
            eventHashMap.put("gptUrl", gptUrl);
            eventHashMap.put("driveUrl", driveUrl);
            eventHashMap.put("videoUrl", videoUrl);
            eventHashMap.put("htmlUrl", htmlUrl);
            HashMap<String, Object> organizerHashMap = new HashMap<>();
            organizerHashMap.put("email", organizer);
            //organizerHashMap.put("displayName", displayName);
            organizerHashMap.put("self", self);
            reference.child(id).updateChildren(eventHashMap);
            reference.child(id).child("Organizer")
                    .child(1 + "").updateChildren(organizerHashMap);
            int attendeeId = 1;
            for (Meetings.Attendee attendee : meetingsList.get(i).getAttendees()) {
                HashMap<String, Object> attendeeHashMap = new HashMap<>();
                attendeeHashMap.put("email", attendee.getEmail());
                attendeeHashMap.put("displayName", attendee.getDisplayName());
                attendeeHashMap.put("responseStatus", attendee.getResponseStatus());
                reference.child(id).child("Attendees")
                        .child(attendeeId + "").updateChildren(attendeeHashMap);
                attendeeId = attendeeId + 1;
            }
            Log.d("==index", i + " " + meetingsList.size());
            if (i == meetingsList.size() - 1){
                hideProgressDialog();
            }
        }
        for (int i = 0; i < meetingsList.size(); i++) {
            isIdExists = false;
            isSameOrganizer = false;
            String mId = meetingsList.get(i).getId();
            String organizerEmail = meetingsList.get(i).getOrganizer().getEmail();
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Meetings meetings = dataSnapshot.getValue(Meetings.class);
                            assert meetings != null;
                            reference.child(meetings.getId()).child("Organizer")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                                    isIdExists = false;
                                                    isSameOrganizer = false;
                                                    Meetings.Organizer organizer = dataSnapshot1.getValue(Meetings.Organizer.class);
                                                    assert organizer != null;
                                                    if (organizer.getEmail().equals(organizerEmail)) {

                                                        Log.d("==cal id", meetings.getId() + " ");
                                                        checkIfEventExistsInGoogleCalendar(meetings.getId());
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            return;
        }
    }

    private void checkIfEventExistsInGoogleCalendar(String googleEventId) {
        new Thread(() -> {
            try {
                Event event = mService.events().get("primary", googleEventId).execute();
                if (event.getStatus().equals("cancelled")) {
                    FirebaseDatabase.getInstance().getReference().child("Meetings")
                            .child(googleEventId).removeValue();
                }
                Log.d("==cal status", "exists " + event.getStatus() + " " + googleEventId);
            } catch (GoogleJsonResponseException e) {
                Log.d("==cal exception", "exception " + e + " " + e.getStatusCode());
                if (e.getStatusCode() == 404) {
                    FirebaseDatabase.getInstance().getReference().child("Meetings")
                            .child(googleEventId).removeValue();
                }
            } catch (Exception e) {
                Log.d("==cal", e.getMessage() + " ");
            }
        }).start();
    }

    public List<Meetings> getDataFromCalendar() {
        List<Meetings> eventModels = new ArrayList<>();
        DateTime now = new DateTime(System.currentTimeMillis());

        try {
            com.google.api.services.calendar.model.Events events = mService.events().list("primary")
                    .setMaxResults(20)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();

            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                DateTime end = event.getEnd().getDateTime();
                if (end == null) {
                    end = event.getEnd().getDate();
                }

                List<EventAttendee> attendeeList = event.getAttendees();

                List<Meetings.Attendee> attendees = new ArrayList<>();
                if (attendeeList != null) {
                    for (EventAttendee attendee : attendeeList) {
                        Log.d("==attendee", attendee.getEmail() + " : " + attendee.getDisplayName() + " : " + attendee.getResponseStatus());
                        attendees.add(new Meetings.Attendee(attendee.getEmail(), attendee.getDisplayName(), attendee.getResponseStatus()));
                    }
                }

                Event.Organizer organizerList = event.getOrganizer();
                Meetings.Organizer organizer = new Meetings.Organizer(organizerList.getEmail(), organizerList.getSelf());
                String gptUrl = DEFAULT_GPT_URL, driveUrl = DEFAULT_DRIVE_URL, videoUrl = DEFAULT_VIDEO_URL, htmlUrl = "html";
                String eventDescription = event.getDescription();
                if (eventDescription != null) {
                    gptUrl = Utils.extractUrl(event.getDescription(), "gptUrl:", DEFAULT_GPT_URL);
                    driveUrl = Utils.extractUrl(event.getDescription(), "driveUrl:", DEFAULT_DRIVE_URL);
                    videoUrl = Utils.extractUrl(event.getDescription(), "videoUrl:", DEFAULT_VIDEO_URL);
                    htmlUrl = Utils.extractUrl(event.getDescription(), "htmlUrl: ", "htmlUrl");
                } else {
                    eventDescription = DEFAULT_EVENT_DESCRIPTION;
                }


                Log.d("==desc", eventDescription + "\n" + gptUrl + "\n" + driveUrl + "\n" + videoUrl);

                Meetings model = new Meetings(event.getId(), event.getSummary(),
                        eventDescription, start.toString(), end.toString(),
                        gptUrl, driveUrl, videoUrl, htmlUrl,
                        attendees, organizer);
                eventModels.add(model);
            }
        } catch (IOException e) {
            Log.d("Google ex", e.getMessage());
            hideProgressDialog();
        }

        return eventModels;
    }


}
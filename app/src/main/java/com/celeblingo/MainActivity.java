package com.celeblingo;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celeblingo.adapter.ItemAdapter;
import com.celeblingo.adapter.MeetingAdapter;
import com.celeblingo.model.GPTURL;
import com.celeblingo.model.Meetings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 121;
    private DatabaseReference urlRef;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Button closeBtn, signInBtn;
    private ImageView viewUpcomingMeeting;
    private RecyclerView.LayoutManager layoutManager;
    private ItemAdapter adapter;
    private ArrayList<GPTURL> gpturlArrayList = new ArrayList<>();

    private GoogleAccountCredential mCredential = null;
    private com.google.api.services.calendar.Calendar mService;
    private String meetingId, joinMeetingUrl = null;
    private ArrayList<Meetings> meetingsArrayList = new ArrayList<>();
    private RecyclerView meetingRecycler;
    private MeetingAdapter meetingAdapter;
    private Dialog dialog;
    private ImageView closeImg;
    private TextView organizerNameTxt, summaryTtx, descriptionTxt,
            startTimeTxt, endTimeTxt, attendeesTxt;
    private YouTubePlayerView youTubePlayerView;
    private String youtubeVideoId;
    private Button joinBtn;
    private BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.recycler);
        closeBtn = findViewById(R.id.close_button);
        signInBtn = findViewById(R.id.sign_button);
        viewUpcomingMeeting = findViewById(R.id.view_upcoming_meeting);
        layoutManager = new GridLayoutManager(this, 2,
                LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        setUpBottomNavView();

        closeBtn.setOnClickListener(view -> {
            finish();
        });

        getUrlFromDatabase();
        startSystemAlertWindowPermission();
        showJoinMeetingDialog();


        signInBtn.setOnClickListener(view -> {
            initCredentials();
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                mCredential.setSelectedAccountName(account.getEmail());
                //if (Objects.equals(account.getEmail(), "celeblingo@gmail.com"))
                {
                    getResultsFromApi();
                }
            } else {
                googleSignIn();
            }
        });

        viewUpcomingMeeting.setOnClickListener(view -> {
            showViewAllMeetingDialog();
        });

    }

    private void setUpBottomNavView() {

    }

    private void showViewAllMeetingDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_view_all_meeting);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);

        ImageView closeImg = dialog.findViewById(R.id.close_btn);
        TextView noDataTxt = dialog.findViewById(R.id.no_data_txt);
        meetingRecycler = dialog.findViewById(R.id.meeting_recycler);
        meetingRecycler.setHasFixedSize(true);
        meetingRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        getAllMeetingData(dialog, meetingRecycler, noDataTxt);

        closeImg.setOnClickListener(view -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void getAllMeetingData(Dialog dialog, RecyclerView meetingRecycler, TextView noDataTxt) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    meetingsArrayList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Meetings meetings = dataSnapshot.getValue(Meetings.class);
                        assert meetings != null;
                        Date currentDate = new Date();

                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+03:30")); // Set the provided timezone
                        Date providedStartDate = null, providedEndDate = null;
                        try {
                            providedStartDate = dateFormat.parse(meetings.getStartTime());
                            providedEndDate = dateFormat.parse(meetings.getEndTime());
                        } catch (ParseException e) {
                            Log.d("exception", Objects.requireNonNull(e.getMessage()));
                        }
                        if (currentDate.compareTo(providedStartDate) < 0 || currentDate.compareTo(providedStartDate) == 0) {
                            meetingsArrayList.add(meetings);
                            meetingAdapter.notifyDataSetChanged();
                        } else if (currentDate.compareTo(providedStartDate) > 0) {
                            if (currentDate.compareTo(providedEndDate)< 0 || currentDate.compareTo(providedEndDate) == 0){
                                meetingsArrayList.add(meetings);
                                meetingAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    if (meetingsArrayList.isEmpty()){
                        noDataTxt.setVisibility(View.VISIBLE);
                    }else {
                        noDataTxt.setVisibility(View.GONE);
                    }
                }else {
                    noDataTxt.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noDataTxt.setVisibility(View.VISIBLE);
            }
        });
        meetingAdapter = new MeetingAdapter(this, meetingsArrayList, dialog);
        meetingRecycler.setAdapter(meetingAdapter);
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
                            dialog.show();
                            return;
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getMeetingData();
            }
        }, 60 * 1000);

    }

    private void setMeetingDataToDialog(DatabaseReference meetingRef, Meetings meetings) {
        meetingRef.child(meetings.getId()).child("Organizer")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                Meetings.Organizer organizer = dataSnapshot.getValue(Meetings.Organizer.class);
                                assert organizer != null;
                                Log.d("==organ", organizer.getEmail());
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
        startTimeTxt.setText(meetings.getStartTime());
        endTimeTxt.setText(meetings.getEndTime());
        joinMeetingUrl = meetings.getDescription();
        meetingId = meetings.getId();
        youtubeVideoId = extractVideoId(meetings.getVideoUrl());
        if (youtubeVideoId.equals("NoId")){
            youtubeVideoId = extractYTId(meetings.getVideoUrl());
        }
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(youtubeVideoId, 0);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                super.onStateChange(youTubePlayer, state);
            }
        });
    }

    public static String extractYTId(String ytUrl) {
        String vId = null;
        Pattern pattern = Pattern.compile(
                "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()){
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
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_meeting_popup);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);


        closeImg = dialog.findViewById(R.id.close_btn);
        organizerNameTxt = dialog.findViewById(R.id.organizer_display_name_txt);
        summaryTtx = dialog.findViewById(R.id.summary_txt);
        descriptionTxt = dialog.findViewById(R.id.description_txt);
        startTimeTxt = dialog.findViewById(R.id.start_time_txt);
        endTimeTxt = dialog.findViewById(R.id.end_time_txt);
        attendeesTxt = dialog.findViewById(R.id.attendee_email_txt);
        youTubePlayerView = dialog.findViewById(R.id.youtube_view);
        joinBtn = dialog.findViewById(R.id.join_meeting_btn);

        getLifecycle().addObserver(youTubePlayerView);

        closeImg.setOnClickListener(view -> {
            dialog.dismiss();
        });

        joinBtn.setOnClickListener(view -> {
            youTubePlayerView.release();
            if (joinMeetingUrl != null && !joinMeetingUrl.isEmpty()) {
                String url = extractUrl(joinMeetingUrl);
                Log.d("==url", url + " ");
                dialog.dismiss();
                startActivity(new Intent(MainActivity.this, WebViewActivity.class)
                        .putExtra("url", url)
                        .putExtra("type", "meeting")
                        .putExtra("meetingId", meetingId));
            }
        });

        getMeetingData();

    }

    public static String extractUrl(String input) {
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

        return null;
    }

    @Override
    protected void onResume() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);
        super.onResume();
    }

    private void getUrlFromDatabase() {
        urlRef = FirebaseDatabase.getInstance().getReference().child("GPTURL");
        urlRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gpturlArrayList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        GPTURL gpturl = dataSnapshot.getValue(GPTURL.class);
                        gpturlArrayList.add(gpturl);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No Data.", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        adapter = new ItemAdapter(this, gpturlArrayList);
        recyclerView.setAdapter(adapter);
    }

    private void startSystemAlertWindowPermission() {
        try {
            if (!Settings.canDrawOverlays(this)) {
                Log.i("==TAG", "[startSystemAlertWindowPermission] requesting system alert window permission.");
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        } catch (Exception e) {
            Log.e("==TAG", "[startSystemAlertWindowPermission] error:", e);
        }
    }

    private void initCredentials() {
        mCredential = GoogleAccountCredential.usingOAuth2(
                        this, Arrays.asList(CalendarScopes.CALENDAR_READONLY))
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
                        new Scope(CalendarScopes.CALENDAR_EVENTS_READONLY)
                        , new Scope(DriveScopes.DRIVE_FILE))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            mCredential.setSelectedAccountName(account.getEmail());
//            if (Objects.equals(account.getEmail(), "celeblingo@gmail.com")) {
                getResultsFromApi();
//            }
        } catch (ApiException e) {
            Log.w("==TAG", "signInResult:failed code=" + e.getStatusCode());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void getResultsFromApi() {
        makeRequestTask();
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    Exception mLastError = null;

    private void makeRequestTask() {
        // Show progress dialog on the main thread
        handler.post(() ->
                Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show());

        executor.submit(() -> {
            List<Meetings> result = null;
            try {
                // This is your doInBackground equivalent
                result = getDataFromCalendar();
            } catch (Exception e) {
                mLastError = e;
            }

            // This is your onPostExecute equivalent
            final List<Meetings> finalResult = result;
            handler.post(() -> {
                // Hide progress dialog
                if (finalResult == null || finalResult.isEmpty()) {
                    Log.d("Google", "No data");
                } else {
                    saveMeetingDataToFirebase(finalResult);
                }

                if (mLastError != null) {
                    if (mLastError instanceof UserRecoverableAuthIOException) {
                        googleSignIn();
                    } else if (mLastError != null) {

                        Log.d("==er ", "The following error occurred:\n" + mLastError.getMessage());
                    } else {
                        Log.d("==err", "Request cancelled.");
                    }
                }
            });
        });
    }

    private void saveMeetingDataToFirebase(List<Meetings> meetingsList) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        for (int i = 0; i < meetingsList.size(); i++) {
            String id = meetingsList.get(i).getId();
            String summary = meetingsList.get(i).getSummary();
            String startTime = meetingsList.get(i).getStartTime().toString();
            String endTime = meetingsList.get(i).getEndTime().toString();
            String description = meetingsList.get(i).getDescription();
            String organizer = meetingsList.get(i).getOrganizer().getEmail();
            //String displayName = meetingsList.get(i).getOrganizer().getDisplayName();
            boolean self = meetingsList.get(i).getOrganizer().isSelf();
            HashMap<String, Object> eventHashMap = new HashMap<>();
            eventHashMap.put("id", id);
            eventHashMap.put("summary", summary);
            eventHashMap.put("startTime", startTime);
            eventHashMap.put("endTime", endTime);
            eventHashMap.put("description", description);
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
        }
    }

    public List<Meetings> getDataFromCalendar() {
        List<Meetings> eventModels = new ArrayList<>();
        DateTime now = new DateTime(System.currentTimeMillis());

        try {
            com.google.api.services.calendar.model.Events events = mService.events().list("primary")
                    .setMaxResults(10)
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

                // Step 1: Parse the RFC3339 string
                OffsetDateTime odt = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    odt = OffsetDateTime.parse(start.toString());
                    Date date = Date.from(odt.toInstant());

                    // Step 3: Format using SimpleDateFormat
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = sdf.format(date);

                    System.out.println("Formatted Date: " + formattedDate);
                }

                List<EventAttendee> attendeeList = event.getAttendees();

                List<Meetings.Attendee> attendees = new ArrayList<>();
                for (EventAttendee attendee : attendeeList) {
                    Log.d("==attendee", attendee.getEmail()+" : "+ attendee.getDisplayName()+" : "+ attendee.getResponseStatus());
                    attendees.add(new Meetings.Attendee(attendee.getEmail(), attendee.getDisplayName(), attendee.getResponseStatus()));
                }

                Event.Organizer organizerList = event.getOrganizer();
                Meetings.Organizer organizer = new Meetings.Organizer(organizerList.getEmail(), organizerList.getSelf());


                Meetings model = new Meetings(event.getId(), event.getSummary(),
                        event.getDescription(), start.toString(), end.toString(), "https://youtu.be/qYKKUbJqiHI", attendees, organizer);
                eventModels.add(model);
            }
        } catch (IOException e) {
            Log.d("Google ex", e.getMessage());
        }

        return eventModels;
    }


}
package com.celeblingo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celeblingo.adapter.ItemAdapter;
import com.celeblingo.model.GPTURL;
import com.celeblingo.model.Meetings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 121;
    private DatabaseReference urlRef;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Button closeBtn, signInBtn;
    private RecyclerView.LayoutManager layoutManager;
    private ItemAdapter adapter;
    private ArrayList<GPTURL> gpturlArrayList = new ArrayList<>();

    private GoogleAccountCredential mCredential = null;
    private com.google.api.services.calendar.Calendar mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.recycler);
        closeBtn = findViewById(R.id.close_button);
        signInBtn = findViewById(R.id.sign_button);
        layoutManager = new GridLayoutManager(this, 2,
                LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        closeBtn.setOnClickListener(view -> {
            finish();
        });

        getUrlFromDatabase();
        startSystemAlertWindowPermission();
        getMeetingData();


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


    }

    private void getMeetingData() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        Meetings meetings = dataSnapshot.getValue(Meetings.class);
                        assert meetings != null;
                        Log.d("==event ", meetings.getId());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("CelebLingo");
        mService = builder.build();
    }

    private void googleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(CalendarScopes.CALENDAR_READONLY))
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
            if (Objects.equals(account.getEmail(), "celeblingo@gmail.com")) {
                getResultsFromApi();
            }
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
                    ; // Implement this method to handle errors
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
            String displayName = meetingsList.get(i).getOrganizer().getDisplayName();
            boolean self = meetingsList.get(i).getOrganizer().isSelf();
            HashMap<String, Object> eventHashMap = new HashMap<>();
            eventHashMap.put("id", id);
            eventHashMap.put("summary", summary);
            eventHashMap.put("startTime", startTime);
            eventHashMap.put("endTime", endTime);
            eventHashMap.put("description", description);
            HashMap<String, Object> organizerHashMap = new HashMap<>();
            organizerHashMap.put("email", organizer);
            organizerHashMap.put("displayName", displayName);
            organizerHashMap.put("self", self);
            reference.child(id).updateChildren(eventHashMap);
            reference.child(id).child("Organizer")
                    .child(1+"").updateChildren(organizerHashMap);
            int attendeeId = 1;
            for (Meetings.Attendee attendee : meetingsList.get(i).getAttendees()) {
                HashMap<String, Object> attendeeHashMap = new HashMap<>();
                attendeeHashMap.put("email", attendee.getEmail());
                attendeeHashMap.put("responseStatus", attendee.getResponseStatus());
                reference.child(id).child("Attendees")
                        .child(attendeeId+"").updateChildren(attendeeHashMap);
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
                    // If there's no start time, use the start date
                    start = event.getStart().getDate();
                }
                DateTime end = event.getEnd().getDateTime();
                if (end == null) {
                    // If there's no end time, use the end date
                    end = event.getEnd().getDate();
                }

                // Step 1: Parse the RFC3339 string
                OffsetDateTime odt = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    odt = OffsetDateTime.parse(start.toString());
                    // Step 2: Convert OffsetDateTime to Date
                    Date date = Date.from(odt.toInstant());

                    // Step 3: Format using SimpleDateFormat
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = sdf.format(date);

                    System.out.println("Formatted Date: " + formattedDate);
                }

                List<EventAttendee> attendeeList = event.getAttendees();

                List<Meetings.Attendee> attendees = new ArrayList<>();
                for (EventAttendee attendee : attendeeList) {
                    attendees.add(new Meetings.Attendee(attendee.getEmail(), attendee.getResponseStatus()));
                }

                Event.Organizer organizerList = event.getOrganizer();
                Meetings.Organizer organizer = new Meetings.Organizer(organizerList.getEmail(), organizerList.getDisplayName(), organizerList.getSelf());


                Meetings model = new Meetings(event.getId(), event.getSummary(),
                        event.getDescription(), start.toString(), end.toString(), attendees, organizer);
                eventModels.add(model);
            }
        } catch (IOException e) {
            Log.d("Google ex", e.getMessage());
        }

        return eventModels;
    }


}
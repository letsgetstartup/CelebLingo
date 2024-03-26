package com.celeblingo.fragment;

import static com.celeblingo.MainActivity.extractUrl;
import static com.celeblingo.helper.Constants.CURRENT_USER_EMAIL;
import static com.celeblingo.helper.Constants.DEFAULT_DRIVE_URL;
import static com.celeblingo.helper.Constants.DEFAULT_EMAIL_ADDRESS;
import static com.celeblingo.helper.Constants.DEFAULT_EVENT_DESCRIPTION;
import static com.celeblingo.helper.Constants.DEFAULT_GPT_URL;
import static com.celeblingo.helper.Constants.DEFAULT_VIDEO_URL;
import static com.celeblingo.helper.Constants.IS_USER_LOGIN;
import static com.celeblingo.helper.Utils.hideKeyboard;
import static com.celeblingo.helper.Utils.hideProgressDialog;
import static com.celeblingo.helper.Utils.showProgressDialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.celeblingo.R;
import com.celeblingo.WebViewActivity;
import com.celeblingo.adapter.MeetingAdapter;
import com.celeblingo.helper.Utils;
import com.celeblingo.model.Meetings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryFragment extends Fragment {
    private View rootView;
    private TextView noDataTxt;
    private ProgressBar progressBar;
    private RecyclerView meetingRecycler;
    private ArrayList<Meetings> meetingsArrayListHistory = new ArrayList<>();
    private MeetingAdapter meetingAdapter;
    private String email;

    private TextView organizerNameTxt, summaryTtx, descriptionTxt,
            startTimeTxt, endTimeTxt, attendeesTxt;
    private YouTubePlayerView youTubePlayerView;
    private String youtubeVideoId;
    private String meetingId, joinMeetingUrl = null;
    private TextView noDataText;
    private RelativeLayout meetingLyt, createMeetingLyt;
    private EditText inputName;
    private String name;
    private AppCompatButton createMeetingBtn;
    private ArrayList<Meetings> meetingsArrayList = new ArrayList<>();
    private boolean isDialogShowing = false;


    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_history, container, false);
        initViews();
        getAllMeetingData();
        setupUI(rootView);
        setUpUpcomingNewClassLyt();
        return rootView;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupUI(View view) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideKeyboard(getActivity());
                view.clearFocus();
                return false;
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    private void initViews() {
        noDataTxt = rootView.findViewById(R.id.no_data_txt);
        progressBar = rootView.findViewById(R.id.progress_bar);
        meetingRecycler = rootView.findViewById(R.id.meeting_recycler);
        meetingRecycler.setHasFixedSize(true);
        meetingRecycler.setLayoutManager(new LinearLayoutManager(
                requireActivity(),
                LinearLayoutManager.VERTICAL,
                false));
    }

    private void getAllMeetingData() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            showProgressDialog(activity);
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
            if (account == null) {
                noDataTxt.setVisibility(View.GONE);
                if (IS_USER_LOGIN) {
                    if (CURRENT_USER_EMAIL != null && !CURRENT_USER_EMAIL.isEmpty()){
                        email = CURRENT_USER_EMAIL;
                    }else {
                        email = DEFAULT_EMAIL_ADDRESS;
                    }
                }else {
                    email = DEFAULT_EMAIL_ADDRESS;
                }
                getHistoryMeetings(email);
                return;
            }
            email = account.getEmail();
            getHistoryMeetings(email);
        }
    }

    private void getHistoryMeetings(String email) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    meetingsArrayListHistory.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Meetings meetings = dataSnapshot.getValue(Meetings.class);
                        assert meetings != null;
                        reference.child(meetings.getId()).child("Organizer")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                                Meetings.Organizer organizer = dataSnapshot1.getValue(Meetings.Organizer.class);
                                                if (organizer.getEmail().equals(email)) {
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
                                                    if (currentDate.compareTo(providedEndDate) > 0) {
                                                        meetingsArrayListHistory.add(meetings);
                                                        meetingsArrayListHistory.sort((t1, t2) -> {
                                                            try {
                                                                Date start = new SimpleDateFormat("yyyyddMMHHmmss")
                                                                        .parse(t1.getStartTime());
                                                                Date end = new SimpleDateFormat("yyyyddMMHHmmss")
                                                                        .parse(t2.getStartTime());
                                                                Log.d("== sts", start + " " + end);
                                                                assert end != null;
                                                                return end.compareTo(start);
                                                            } catch (ParseException e) {
                                                                return t2.getStartTime().compareToIgnoreCase(t1.getStartTime());
                                                            }
                                                        });
                                                        meetingAdapter.notifyDataSetChanged();
                                                    }
                                                }
                                            }
                                            if (meetingsArrayListHistory.isEmpty()) {
                                                noDataTxt.setVisibility(View.VISIBLE);
                                                hideProgressDialog();
                                            } else {
                                                noDataTxt.setVisibility(View.GONE);
                                                hideProgressDialog();
                                            }
                                        } else {
                                            noDataTxt.setVisibility(View.VISIBLE);
                                            hideProgressDialog();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }
                    if (meetingsArrayListHistory.isEmpty()) {
                        noDataTxt.setVisibility(View.VISIBLE);
                        hideProgressDialog();
                    } else {
                        noDataTxt.setVisibility(View.GONE);
                        hideProgressDialog();
                    }
                } else {
                    noDataTxt.setVisibility(View.VISIBLE);
                    hideProgressDialog();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noDataTxt.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                hideProgressDialog();
            }
        });
        meetingAdapter = new MeetingAdapter(requireActivity(), meetingsArrayListHistory, "History");
        meetingRecycler.setAdapter(meetingAdapter);
    }

    private void setUpUpcomingNewClassLyt() {
        noDataText = rootView.findViewById(R.id.no_data_txt);
        meetingLyt = rootView.findViewById(R.id.meeting_lyt);
        createMeetingLyt = rootView.findViewById(R.id.create_meeting_lyt);
        inputName = rootView.findViewById(R.id.input_name);
        createMeetingBtn = rootView.findViewById(R.id.create_meeting_btn);
        organizerNameTxt = rootView.findViewById(R.id.organizer_display_name_txt);
        summaryTtx = rootView.findViewById(R.id.summary_txt);
        descriptionTxt = rootView.findViewById(R.id.description_txt);
        startTimeTxt = rootView.findViewById(R.id.start_time_txt);
        endTimeTxt = rootView.findViewById(R.id.end_time_txt);
        attendeesTxt = rootView.findViewById(R.id.attendee_email_txt);
        youTubePlayerView = rootView.findViewById(R.id.youtube_view);
        AppCompatButton joinBtn = rootView.findViewById(R.id.join_meeting_btn);

        getLifecycle().addObserver(youTubePlayerView);

        joinBtn.setOnClickListener(view -> {
            Log.d("==url", joinMeetingUrl + " ");
            String url = extractUrl(joinMeetingUrl);
            startActivity(new Intent(requireActivity(), WebViewActivity.class)
                    .putExtra("url", url)
                    .putExtra("type", "meeting")
                    .putExtra("meetingId", meetingId));
        });

        getMeetingData();

        setClickListener();
    }


    private void setClickListener() {
        createMeetingBtn.setOnClickListener(view -> {
            name = inputName.getText().toString();
            if (name.isEmpty()) {
                inputName.setError(getString(R.string.enter_your_name));
                inputName.requestFocus();
            } else {
                Utils.showProgressDialog(requireActivity());
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
                if (account == null) {
                    createNewMeetingInDB();
                } else {
                    new Thread(this::updateCalenderEvent).start();
                }
            }
        });
    }

    private void updateCalenderEvent() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
            if (account != null) {
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                activity, Arrays.asList(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY));
                credential.setSelectedAccount(account.getAccount());
                com.google.api.services.calendar.Calendar mCalendarService = new com.google.api.services.calendar.Calendar.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential)
                        .setApplicationName("CelebLingo").build();
                Event event = new Event()
                        .setSummary(name)
                        .setDescription(DEFAULT_EVENT_DESCRIPTION);

                Calendar now = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                String currentDateTime = dateFormat.format(now.getTime());

                DateTime startDateTime = new DateTime(currentDateTime);
                EventDateTime start = new EventDateTime()
                        .setDateTime(startDateTime);
                event.setStart(start);

                now.add(Calendar.MINUTE, 45);

                String endDateTim = dateFormat.format(now.getTime());
                DateTime endDateTime = new DateTime(endDateTim);
                EventDateTime end = new EventDateTime()
                        .setDateTime(endDateTime);
                event.setEnd(end);

                EventAttendee[] attendees = new EventAttendee[]{
                        new EventAttendee().setEmail(account.getEmail()).setOrganizer(true), // Intended organizer
                        new EventAttendee().setEmail(account.getEmail())
                };
                event.setAttendees(Arrays.asList(attendees));

                String calendarId = "primary";
                try {
                    event = mCalendarService.events().insert(calendarId, event).execute();
                    saveEventToFB(event);
                    Log.d("Event created: %s\n", event.getHtmlLink());
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(activity, "Error creating class: " + e, Toast.LENGTH_SHORT).show();
                    Log.e("Error creating event", e.toString());
                }
            }
        } else {
            hideProgressDialog();
        }
    }

    private void saveEventToFB(Event event) {
        List<Meetings> meetingsList = new ArrayList<>();
        {
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
            meetingsList.add(model);
        }

        int i = 0;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");

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
        reference.child(id).child("Organizer")
                .child(1 + "").updateChildren(organizerHashMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        hideProgressDialog();
                        startActivity(new Intent(requireActivity(), WebViewActivity.class)
                                .putExtra("url", gptUrl)
                                .putExtra("type", "meeting")
                                .putExtra("meetingId", id));
                        inputName.setText("");
                    } else {
                        hideProgressDialog();
                        Toast.makeText(requireActivity(), "Please try again...", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(requireActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void createNewMeetingInDB() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        String id = reference.push().getKey();
        String summary = name;

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());

        String currentDateTime = dateFormat.format(now.getTime());
        System.out.println("Current DateTime: " + currentDateTime);

        now.add(Calendar.MINUTE, 45);

        // DateTime 45 minutes later
        String endDateTime = dateFormat.format(now.getTime());
        System.out.println("DateTime After 45 Minutes: " + endDateTime);
        String organizer = "";
        if (IS_USER_LOGIN) {
            if (CURRENT_USER_EMAIL != null && !CURRENT_USER_EMAIL.isEmpty()){
                organizer = CURRENT_USER_EMAIL;
            }else {
                organizer = DEFAULT_EMAIL_ADDRESS;
            }
        }else {
            organizer = DEFAULT_EMAIL_ADDRESS;
        }
        String gptUrl = DEFAULT_GPT_URL;
        String driveUrl = DEFAULT_DRIVE_URL;
        String videoUrl = DEFAULT_VIDEO_URL;
        String htmlUrl = "html";
        HashMap<String, Object> eventHashMap = new HashMap<>();
        eventHashMap.put("id", id);
        eventHashMap.put("summary", summary);
        eventHashMap.put("startTime", currentDateTime);
        eventHashMap.put("endTime", endDateTime);
        eventHashMap.put("description", DEFAULT_EVENT_DESCRIPTION);
        eventHashMap.put("gptUrl", gptUrl);
        eventHashMap.put("driveUrl", driveUrl);
        eventHashMap.put("videoUrl", videoUrl);
        eventHashMap.put("htmlUrl", htmlUrl);
        HashMap<String, Object> organizerHashMap = new HashMap<>();
        organizerHashMap.put("email", organizer);
        //organizerHashMap.put("displayName", displayName);
        organizerHashMap.put("self", true);
        reference.child(id).updateChildren(eventHashMap);
        reference.child(id).child("Organizer")
                .child(1 + "").updateChildren(organizerHashMap);
        int attendeeId = 1;
        HashMap<String, Object> attendeeHashMap = new HashMap<>();
        attendeeHashMap.put("email", name);
        attendeeHashMap.put("displayName", name);
        attendeeHashMap.put("responseStatus", "needAction");
        reference.child(id).child("Attendees")
                .child(attendeeId + "").updateChildren(attendeeHashMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        hideProgressDialog();
                        startActivity(new Intent(requireActivity(), WebViewActivity.class)
                                .putExtra("url", gptUrl)
                                .putExtra("type", "meeting")
                                .putExtra("meetingId", id));
                        inputName.setText("");
                    } else {
                        hideProgressDialog();
                        Toast.makeText(requireActivity(), "Please try again...", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(requireActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void getMeetingData() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            String email = null;
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
            if (account == null) {
                if (IS_USER_LOGIN) {
                    if (CURRENT_USER_EMAIL != null && !CURRENT_USER_EMAIL.isEmpty()){
                        email = CURRENT_USER_EMAIL;
                    }else {
                        email = DEFAULT_EMAIL_ADDRESS;
                    }
                }else {
                    email = DEFAULT_EMAIL_ADDRESS;
                }
                noDataText.setVisibility(View.GONE);
                meetingLyt.setVisibility(View.GONE);
                getUpcomingMeting(email);
                return;
            }
            email = account.getEmail();
            getUpcomingMeting(email);

        }

    }

    private void getUpcomingMeting(String email) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isDialogShowing = false;
                noDataText.setVisibility(View.GONE);
                meetingLyt.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Meetings meetings = dataSnapshot.getValue(Meetings.class);
                        assert meetings != null;
                        Log.d("==event ", meetings.getId());
                        reference.child(meetings.getId()).child("Organizer")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                                Meetings.Organizer organizer = dataSnapshot1.getValue(Meetings.Organizer.class);
                                                if (organizer.getEmail().equals(email)) {
                                                    if (compareDate(meetings.getStartTime(), meetings.getEndTime())) {
                                                        isDialogShowing = true;
//                                                            noDataText.setVisibility(View.GONE);
//                                                            meetingLyt.setVisibility(View.VISIBLE);
                                                        meetingsArrayList.add(meetings);
                                                        //setMeetingDataToDialog(reference, meetings);
                                                        return;
                                                    } else {
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
                                                            meetingsArrayList.sort(new Comparator<Meetings>() {
                                                                @Override
                                                                public int compare(Meetings t1, Meetings t2) {
                                                                    try {
                                                                        Date start = new SimpleDateFormat("yyyyddMMHHmmss")
                                                                                .parse(t1.getStartTime());
                                                                        Date end = new SimpleDateFormat("yyyyddMMHHmmss")
                                                                                .parse(t2.getStartTime());
                                                                        Log.d("==sts", start + " " + end);
                                                                        assert end != null;
                                                                        return end.compareTo(start);
                                                                    } catch (ParseException e) {
                                                                        return t2.getStartTime().compareToIgnoreCase(t1.getStartTime());
                                                                    }
                                                                }
                                                            });
                                                        } else if (currentDate.compareTo(providedStartDate) > 0) {
                                                            if (currentDate.compareTo(providedEndDate) < 0 || currentDate.compareTo(providedEndDate) == 0) {
                                                                meetingsArrayList.add(meetings);
                                                                meetingsArrayList.sort(new Comparator<Meetings>() {
                                                                    @Override
                                                                    public int compare(Meetings t1, Meetings t2) {
                                                                        try {
                                                                            Date start = new SimpleDateFormat("yyyyddMMHHmmss")
                                                                                    .parse(t1.getStartTime());
                                                                            Date end = new SimpleDateFormat("yyyyddMMHHmmss")
                                                                                    .parse(t2.getStartTime());
                                                                            Log.d("==sts", start + " " + end);
                                                                            assert end != null;
                                                                            return end.compareTo(start);
                                                                        } catch (
                                                                                ParseException e) {
                                                                            return t2.getStartTime().compareToIgnoreCase(t1.getStartTime());
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                    }
                    if (!isDialogShowing) {
                        new Handler().postDelayed(() -> {
                            if (!meetingsArrayList.isEmpty()) {
                                Collections.reverse(meetingsArrayList);
                                Log.d("==listreve", meetingsArrayList.get(0).getStartTime());
                                if (compareDate(meetingsArrayList.get(0).getEndTime())) {
                                    isDialogShowing = true;
                                    setMeetingDataToDialog(reference, meetingsArrayList.get(0));
                                }
                            } else {
                                noDataText.setVisibility(View.GONE);
                                meetingLyt.setVisibility(View.GONE);
                                createMeetingLyt.setVisibility(View.VISIBLE);
                            }
                        }, 2000);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noDataText.setVisibility(View.GONE);
                meetingLyt.setVisibility(View.GONE);
            }
        });
    }

    private boolean compareDate(String startDate, String endTime) {
        Date currentDate = new Date();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+03:30")); // Set the provided timezone
        Date providedDate = null, providedEndDate = null;
        try {
            providedDate = dateFormat.parse(startDate);
            providedEndDate = dateFormat.parse(endTime);
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
                if (currentDate.compareTo(providedDate) > 0) {
                    return currentDate.compareTo(providedEndDate) < 0 || currentDate.compareTo(providedEndDate) == 0;
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

    private boolean compareDate(String startDate) {
        Date currentDate = new Date();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+03:30")); // Set the provided timezone
        Date providedDate = null;
        try {
            providedDate = dateFormat.parse(startDate);
            if (currentDate.compareTo(providedDate) < 0) {
                System.out.println("Current date and time is before the provided datetime.");
                return true;
            } else if (currentDate.compareTo(providedDate) > 0) {
                System.out.println("Current date and time is after the provided datetime.");
                return false;
            } else {
                System.out.println("Current date and time is equal to the provided datetime.");
                return false;

            }
        } catch (ParseException e) {
            //e.printStackTrace();
            return false;
        }

    }

    @SuppressLint("SetTextI18n")
    private void setMeetingDataToDialog(DatabaseReference meetingRef, Meetings meetings) {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
//            if (account == null) {
//                return;
//            }
//            meetingRef.child(meetings.getId()).child("Organizer")
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (snapshot.exists()) {
//                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
//                                    Meetings.Organizer organizer = dataSnapshot.getValue(Meetings.Organizer.class);
//                                    assert organizer != null;
//                                    Log.d("==organ", organizer.getEmail());
//                                    if (organizer.getEmail().equals(account.getEmail())) {
//                                        organizerNameTxt.setText(organizer.getEmail());
//                                    }
//                                }
//                            } else {
//                                organizerNameTxt.setVisibility(View.GONE);
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//
//                        }
//                    });
//            meetingRef.child(meetings.getId()).child("Attendees")
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (snapshot.exists()) {
//                                String attendeeEmail = "";
//                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
//                                    Meetings.Attendee attendee = dataSnapshot.getValue(Meetings.Attendee.class);
//                                    assert attendee != null;
//                                    Log.d("==attt", attendee.getEmail());
//                                    attendeeEmail = attendeeEmail + attendee.getDisplayName() + "\n";
//                                    attendeesTxt.setText(attendeeEmail);
//                                }
//                            } else {
//                                attendeesTxt.setVisibility(View.GONE);
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//
//                        }
//                    });
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
            noDataText.setVisibility(View.GONE);
            meetingLyt.setVisibility(View.VISIBLE);
        }
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



}
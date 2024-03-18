package com.celeblingo.fragment;

import static com.celeblingo.MainActivity.extractUrl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.celeblingo.R;
import com.celeblingo.WebViewActivity;
import com.celeblingo.model.Meetings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartFragment extends Fragment {
    private View rootView;
    private TextView organizerNameTxt, summaryTtx, descriptionTxt,
            startTimeTxt, endTimeTxt, attendeesTxt;
    private YouTubePlayerView youTubePlayerView;
    private String youtubeVideoId;
    private String meetingId, joinMeetingUrl = null;
    private TextView noDataText;
    private RelativeLayout meetingLyt;
    private ArrayList<Meetings> meetingsArrayList = new ArrayList<>();
    private boolean isDialogShowing = false;


    public StartFragment() {
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
        rootView = inflater.inflate(R.layout.fragment_start, container, false);

        noDataText = rootView.findViewById(R.id.no_data_txt);
        meetingLyt = rootView.findViewById(R.id.meeting_lyt);
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
            youTubePlayerView.release();
            if (joinMeetingUrl != null && !joinMeetingUrl.isEmpty()) {
                String url = extractUrl(joinMeetingUrl);
                Log.d("==url", url + " ");
                startActivity(new Intent(requireActivity(), WebViewActivity.class)
                        .putExtra("url", url)
                        .putExtra("type", "meeting")
                        .putExtra("meetingId", meetingId));
            }
        });

        getMeetingData();

        return rootView;
    }

    private void getMeetingData() {
        Activity activity = getActivity();
        if (isAdded() && activity != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
            if (account == null) {
                noDataText.setVisibility(View.VISIBLE);
                meetingLyt.setVisibility(View.GONE);
                return;
            }
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isDialogShowing = false;
                    noDataText.setVisibility(View.VISIBLE);
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
                                                    if (organizer.getEmail().equals(account.getEmail())) {
                                                        if (compareDate(meetings.getStartTime(), meetings.getEndTime())) {
                                                            isDialogShowing = true;
                                                            noDataText.setVisibility(View.GONE);
                                                            meetingLyt.setVisibility(View.VISIBLE);
                                                            setMeetingDataToDialog(reference, meetings);
                                                            return;
                                                        }else {
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


                            ///////////////// before
//                            if (compareDate(meetings.getStartTime(), meetings.getEndTime())) {
//                                isDialogShowing = true;
//                                noDataText.setVisibility(View.GONE);
//                                meetingLyt.setVisibility(View.VISIBLE);
//                                setMeetingDataToDialog(reference, meetings);
//                                return;
//                            }
//                            else {
//                                Log.d("==start", "here");
//                                reference.child(meetings.getId()).child("Organizer")
//                                        .addListenerForSingleValueEvent(new ValueEventListener() {
//                                            @Override
//                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                                                if (snapshot.exists()) {
//                                                    for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
//                                                        Log.d("==start", "ord data");
//                                                        Meetings.Organizer organizer = dataSnapshot1.getValue(Meetings.Organizer.class);
//                                                        if (organizer.getEmail().equals(account.getEmail())) {
//                                                            Log.d("==start", "account ema");
//                                                            Date currentDate = new Date();
//
//                                                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
//                                                            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+03:30")); // Set the provided timezone
//                                                            Date providedStartDate = null, providedEndDate = null;
//                                                            try {
//                                                                providedStartDate = dateFormat.parse(meetings.getStartTime());
//                                                                providedEndDate = dateFormat.parse(meetings.getEndTime());
//                                                            } catch (ParseException e) {
//                                                                Log.d("exception", Objects.requireNonNull(e.getMessage()));
//                                                            }
//                                                            if (currentDate.compareTo(providedStartDate) < 0 || currentDate.compareTo(providedStartDate) == 0) {
//                                                                meetingsArrayList.add(meetings);
//                                                                meetingsArrayList.sort(new Comparator<Meetings>() {
//                                                                    @Override
//                                                                    public int compare(Meetings t1, Meetings t2) {
//                                                                        try {
//                                                                            Date start = new SimpleDateFormat("yyyyddMMHHmmss")
//                                                                                    .parse(t1.getStartTime());
//                                                                            Date end = new SimpleDateFormat("yyyyddMMHHmmss")
//                                                                                    .parse(t2.getStartTime());
//                                                                            Log.d("==sts", start + " " + end);
//                                                                            assert end != null;
//                                                                            return end.compareTo(start);
//                                                                        } catch (ParseException e) {
//                                                                            return t2.getStartTime().compareToIgnoreCase(t1.getStartTime());
//                                                                        }
//                                                                    }
//                                                                });
//                                                            } else if (currentDate.compareTo(providedStartDate) > 0) {
//                                                                if (currentDate.compareTo(providedEndDate) < 0 || currentDate.compareTo(providedEndDate) == 0) {
//                                                                    meetingsArrayList.add(meetings);
//                                                                    meetingsArrayList.sort(new Comparator<Meetings>() {
//                                                                        @Override
//                                                                        public int compare(Meetings t1, Meetings t2) {
//                                                                            try {
//                                                                                Date start = new SimpleDateFormat("yyyyddMMHHmmss")
//                                                                                        .parse(t1.getStartTime());
//                                                                                Date end = new SimpleDateFormat("yyyyddMMHHmmss")
//                                                                                        .parse(t2.getStartTime());
//                                                                                Log.d("==sts", start + " " + end);
//                                                                                assert end != null;
//                                                                                return end.compareTo(start);
//                                                                            } catch (
//                                                                                    ParseException e) {
//                                                                                return t2.getStartTime().compareToIgnoreCase(t1.getStartTime());
//                                                                            }
//                                                                        }
//                                                                    });
//                                                                }
//                                                            }
//                                                        }
//                                                    }
//                                                    Log.d("==start", "lis" + meetingsArrayList.size());
//                                                }
//                                            }
//
//                                            @Override
//                                            public void onCancelled(@NonNull DatabaseError error) {
//
//                                            }
//                                        });
//                            }

                        }
                        if (!isDialogShowing) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!meetingsArrayList.isEmpty()) {
                                        Collections.reverse(meetingsArrayList);
                                        Log.d("==listreve", meetingsArrayList.get(0).getStartTime());
                                        if (compareDate(meetingsArrayList.get(0).getEndTime())) {
                                            isDialogShowing = true;
                                            noDataText.setVisibility(View.GONE);
                                            meetingLyt.setVisibility(View.VISIBLE);
                                            setMeetingDataToDialog(reference, meetingsArrayList.get(0));
                                        }
                                    }
                                }
                            }, 2000);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    noDataText.setVisibility(View.VISIBLE);
                    meetingLyt.setVisibility(View.GONE);
                }
            });
        }

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
                                        organizerNameTxt.setText(organizer.getEmail());
                                    }
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
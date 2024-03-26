package com.celeblingo.fragment;

import static com.celeblingo.helper.Constants.CURRENT_USER_EMAIL;
import static com.celeblingo.helper.Constants.DEFAULT_EMAIL_ADDRESS;
import static com.celeblingo.helper.Constants.IS_USER_LOGIN;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.celeblingo.R;
import com.celeblingo.adapter.MeetingAdapter;
import com.celeblingo.model.Meetings;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class UpcomingFragment extends Fragment {
    private View rootView;
    private TextView noDataTxt;
    private ProgressBar progressBar;
    private RecyclerView meetingRecycler;
    private ArrayList<Meetings> meetingsArrayList = new ArrayList<>();
    private MeetingAdapter meetingAdapter;
    private String email;

    public UpcomingFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_upcoming, container, false);
        initViews();
        getAllMeetingData();
        return rootView;
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
                getUpcomingMeetingsData(email);
                return;
            }
            email = account.getEmail();
            getUpcomingMeetingsData(email);
        }
    }

    private void getUpcomingMeetingsData(String email) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    meetingsArrayList.clear();
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
                                                    if (currentDate.compareTo(providedStartDate) < 0 || currentDate.compareTo(providedStartDate) == 0) {
                                                        meetingsArrayList.add(meetings);
                                                        meetingsArrayList.sort((t1, t2) -> {
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
                                                        Collections.reverse(meetingsArrayList);
                                                        meetingAdapter.notifyDataSetChanged();
                                                    } else if (currentDate.compareTo(providedStartDate) > 0) {
                                                        if (currentDate.compareTo(providedEndDate) < 0 || currentDate.compareTo(providedEndDate) == 0) {
                                                            meetingsArrayList.add(meetings);
                                                            meetingsArrayList.sort((t1, t2) -> {
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
                                                            Collections.reverse(meetingsArrayList);
                                                            meetingAdapter.notifyDataSetChanged();
                                                        }
                                                    }
                                                }
                                            }
                                            if (meetingsArrayList.isEmpty()) {
                                                noDataTxt.setVisibility(View.VISIBLE);
                                            } else {
                                                noDataTxt.setVisibility(View.GONE);
                                            }
                                        } else {
                                            noDataTxt.setVisibility(View.VISIBLE);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }
                    if (meetingsArrayList.isEmpty()) {
                        noDataTxt.setVisibility(View.VISIBLE);
                    } else {
                        noDataTxt.setVisibility(View.GONE);
                    }
                } else {
                    noDataTxt.setVisibility(View.VISIBLE);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noDataTxt.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        });
        meetingAdapter = new MeetingAdapter(requireActivity(), meetingsArrayList, "Upcoming");
        meetingRecycler.setAdapter(meetingAdapter);
    }


}
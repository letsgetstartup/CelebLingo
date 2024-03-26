package com.celeblingo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.celeblingo.R;
import com.google.android.material.button.MaterialButtonToggleGroup;


public class MeetingFragment extends Fragment {
    private View rootView;
    private MaterialButtonToggleGroup toggleButton;

    public MeetingFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_meeting, container, false);
        initViews();
        setUpMeetingTabs();
        return rootView;
    }

    private void initViews() {
        toggleButton = rootView.findViewById(R.id.toggleButton);
    }

    private void setUpMeetingTabs() {

        replaceFragment(new HistoryFragment());

        toggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.upcoming_btn && isChecked){
                replaceFragment(new UpcomingFragment());
            }
            if (checkedId == R.id.history_btn && isChecked){
                replaceFragment(new HistoryFragment());
            }
        });

    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fm = getParentFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.tabs_fragment_container, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

}
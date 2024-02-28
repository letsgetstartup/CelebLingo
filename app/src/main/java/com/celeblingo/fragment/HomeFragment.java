package com.celeblingo.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.celeblingo.MainActivity;
import com.celeblingo.R;
import com.celeblingo.adapter.ItemAdapter;
import com.celeblingo.model.GPTURL;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class HomeFragment extends Fragment {
    private View rootView;
    private DatabaseReference urlRef;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private RecyclerView.LayoutManager layoutManager;
    private ItemAdapter adapter;
    private ArrayList<GPTURL> gpturlArrayList = new ArrayList<>();

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        initViews();
        getUrlFromDatabase();
        return rootView;
    }

    private void initViews() {
        progressBar = rootView.findViewById(R.id.progress_bar);
        recyclerView = rootView.findViewById(R.id.recycler);
        layoutManager = new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
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
                    Toast.makeText(requireActivity(), "No Data.", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireActivity(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        adapter = new ItemAdapter(requireActivity(), gpturlArrayList);
        recyclerView.setAdapter(adapter);
    }

}
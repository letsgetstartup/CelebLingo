package com.celeblingo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.celeblingo.adapter.ItemAdapter;
import com.celeblingo.model.GPTURL;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference urlRef;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Button closeBtn;
    private RecyclerView.LayoutManager layoutManager;
    private ItemAdapter adapter;
    private ArrayList<GPTURL> gpturlArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.recycler);
        closeBtn = findViewById(R.id.close_button);
        layoutManager = new GridLayoutManager(this, 2,
                LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        closeBtn.setOnClickListener(view -> {
            finish();
        });

        getUrlFromDatabase();
        startSystemAlertWindowPermission();

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
                if (snapshot.exists()){
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        GPTURL gpturl = dataSnapshot.getValue(GPTURL.class);
                        gpturlArrayList.add(gpturl);
                        adapter.notifyDataSetChanged();
                    }
                }else {
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

    private void startSystemAlertWindowPermission(){
        try{
            if (!Settings.canDrawOverlays(this)) {
                Log.i("==TAG", "[startSystemAlertWindowPermission] requesting system alert window permission.");
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        }catch (Exception e){
            Log.e("==TAG", "[startSystemAlertWindowPermission] error:", e);
        }
    }

}
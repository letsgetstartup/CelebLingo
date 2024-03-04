package com.celeblingo.helper;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.celeblingo.R;
import com.celeblingo.adapter.ScreensaverAdapter;
import com.celeblingo.model.BGImages;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public abstract class BaseActivity extends AppCompatActivity {

    private Handler idleHandler = new Handler();
    private Runnable idleRunnable = this::showScreensaver;
    protected Dialog screensaverDialog;
    private boolean isAppVisible = true;
    private ViewPager2 viewPager;
    private ScreensaverAdapter adapter;
    private ArrayList<String> imageUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resetIdleTimer();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetIdleTimer();
    }

    private void resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable);
        if (isAppVisible) {
            idleHandler.postDelayed(idleRunnable, 10 * 60 * 1000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppVisible = true;
        resetIdleTimer();
        handler.postDelayed(runnable, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        idleHandler.removeCallbacks(idleRunnable);
        dismissScreensaver();
        isAppVisible = false;
        handler.removeCallbacks(runnable);
    }

    private void showScreensaver() {
        if (!isAppVisible) {
            return;
        }
        runOnUiThread(() -> {
            if (screensaverDialog == null || !screensaverDialog.isShowing()) {
                screensaverDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                screensaverDialog.setContentView(R.layout.layout_screensaver);
                viewPager = screensaverDialog.findViewById(R.id.viewPagerScreensaver);
                adapter = new ScreensaverAdapter(this, imageUrls, position -> {
                    dismissScreensaver();
                });
                getImagesFromDB();
                viewPager.setPageTransformer(new FadeInOutPageTransformer());
                viewPager.setAdapter(adapter);
                screensaverDialog.show();
            }
        });
    }

    private void getImagesFromDB() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("BGImages");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        BGImages bgImages = dataSnapshot.getValue(BGImages.class);
                        assert bgImages != null;
                        imageUrls.add(bgImages.getImage());
                        adapter.notifyDataSetChanged();
                    }
                }else {
                    imageUrls.add("https://firebasestorage.googleapis.com/v0/b/celeblingo-5cb9e.appspot.com/o/background_Images%2FbackgroundImge1.png?alt=media&token=e0a926e2-c143-4ac7-b509-b98e8079aebd");
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (viewPager != null) {
                int currentPosition = viewPager.getCurrentItem();
                int itemCount = viewPager.getAdapter().getItemCount();
                if (currentPosition == itemCount - 1) {
                    viewPager.setCurrentItem(0, true);
                } else {
                    viewPager.setCurrentItem(currentPosition + 1, true);
                }
            }
            handler.postDelayed(this, 5000);
        }
    };

    private void dismissScreensaver() {
        if (screensaverDialog != null && screensaverDialog.isShowing()) {
            screensaverDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        idleHandler.removeCallbacksAndMessages(null); // Prevent memory leaks
    }

    public static class FadeInOutPageTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            page.setTranslationX(-position * page.getWidth());
            page.setAlpha(1 - Math.abs(position));
        }
    }

}

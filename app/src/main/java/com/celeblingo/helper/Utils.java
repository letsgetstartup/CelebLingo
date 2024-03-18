package com.celeblingo.helper;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String extractUrl(String inputHtml, String preText, String defaultUrl) {
        String urlPattern = "https?://[^\\s\"']+";
        // Find the specific part of the string first
        int index = inputHtml.indexOf(preText);
        if (index == -1) {
            return defaultUrl; // Pretext not found
        }
        String subString = inputHtml.substring(index);

        // Adjusted pattern to stop at quotes
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(subString);
        if (matcher.find()) {
            // Extract URL, ensuring to trim at the end quote if present
            String url = matcher.group();
            int endIndex = url.indexOf("\"");
            if (endIndex != -1) {
                url = url.substring(0, endIndex);
            }
            return url;
        }
        return defaultUrl; // URL pattern not found
    }

    public static void getDefaultUrlsFromFB() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("DefaultUrls");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String gptUrl = snapshot.child("gptUrl").getValue(String.class);
                    String driveUrl = snapshot.child("driveUrl").getValue(String.class);
                    String videoUrl = snapshot.child("videoUrl").getValue(String.class);
                    String endVideoUrl = snapshot.child("endVideoUrl").getValue(String.class);
                    Constants.DEFAULT_GPT_URL = gptUrl;
                    Constants.DEFAULT_DRIVE_URL = driveUrl;
                    Constants.DEFAULT_VIDEO_URL = videoUrl;
                    Constants.DEFAULT_END_VIDEO_URL = endVideoUrl;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}

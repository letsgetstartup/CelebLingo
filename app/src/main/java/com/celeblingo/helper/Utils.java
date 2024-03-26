package com.celeblingo.helper;

import static com.celeblingo.helper.Constants.CURRENT_USER_EMAIL;
import static com.celeblingo.helper.Constants.IS_USER_LOGIN;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.celeblingo.R;
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
        int index = inputHtml.indexOf(preText);
        if (index == -1) {
            return defaultUrl;
        }
        String subString = inputHtml.substring(index);

        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(subString);
        if (matcher.find()) {
            String url = matcher.group();
            int endIndex = url.indexOf("\"");
            if (endIndex != -1) {
                url = url.substring(0, endIndex);
            }
            return url;
        }
        return defaultUrl;
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

    private static Dialog progressDialog = null;

    public static void showProgressDialog(Context context){
        if (progressDialog == null) {
            progressDialog = new Dialog(context);
            progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            progressDialog.setContentView(R.layout.dialog_progess_layout);
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);

            TextView progressMsg = progressDialog.findViewById(R.id.progress_msg);
            if (progressDialog != null && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        }
    }

    public static void hideProgressDialog(){
        if (progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public static void checkUserData(Context context){
        SharedPreferences preferences = context.getSharedPreferences("Users", context.MODE_PRIVATE);
        CURRENT_USER_EMAIL = preferences.getString("email", null);
        IS_USER_LOGIN = preferences.getBoolean("isLogin", false);
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            // Find the currently focused view, so we can grab the correct window token from it.
            View view = activity.getCurrentFocus();
            // If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = new View(activity);
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}

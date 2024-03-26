package com.celeblingo.fragment;

import static android.app.Activity.RESULT_OK;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.celeblingo.MainActivity;
import com.celeblingo.R;
import com.celeblingo.helper.HtmlHelper;
import com.celeblingo.helper.Utils;
import com.celeblingo.model.Meetings;
import com.celeblingo.model.Users;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigFragment extends Fragment {
    private View rootView;
    private CardView loginBtn, logoutBtn, emailBtn;
    private static final int RC_SIGN_IN = 121, REQUEST_AUTHORIZATION = 1001;
    private GoogleAccountCredential mCredential = null;
    private com.google.api.services.calendar.Calendar mService;
    private String web_client = "333558564968-81tk2qejtq6gr1bppa9nm7qkmjl3117b.apps.googleusercontent.com";
    private GoogleSignInClient mGoogleSignInClient;
    private ArrayList<Meetings> meetingsArrayList = new ArrayList<>();
    private boolean isUserExists = false;
    private EditText inputEmail;
    private TextView versionNameTxt, userNameTxt;
    private TextView historyHtml;
    private String email;

    public ConfigFragment() {
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
        rootView = inflater.inflate(R.layout.fragment_config, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loginBtn = view.findViewById(R.id.login_btn);
        logoutBtn = view.findViewById(R.id.logout_btn);
        inputEmail = view.findViewById(R.id.input_email);
        emailBtn = view.findViewById(R.id.email_login_btn);
        versionNameTxt = view.findViewById(R.id.version_name_txt);
        userNameTxt = view.findViewById(R.id.user_email_txt);
        historyHtml = rootView.findViewById(R.id.all_meeting_html);

        initCredentials();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(CalendarScopes.CALENDAR_READONLY),
                        new Scope(CalendarScopes.CALENDAR),
                        new Scope(CalendarScopes.CALENDAR_EVENTS_READONLY)
                        , new Scope(DriveScopes.DRIVE_FILE))
                .requestIdToken(web_client)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        GoogleSignInAccount account1 = GoogleSignIn.getLastSignedInAccount(requireActivity());
        if (account1 == null) {
            if (IS_USER_LOGIN) {
                getUserMeetingHistory(CURRENT_USER_EMAIL);
                userNameTxt.setText("Welcome, " + CURRENT_USER_EMAIL);
            }else {
                userNameTxt.setText("");
            }
        }else {
            userNameTxt.setText("Welcome, " + account1.getEmail());
        }

        loginBtn.setOnClickListener(view12 -> {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
            if (account != null) {
                userNameTxt.setText("Welcome, " + account.getEmail());
                Toast.makeText(requireActivity(), "Already Logged in...", Toast.LENGTH_SHORT).show();
                mCredential.setSelectedAccountName(account.getEmail());
                getResultsFromApi();
            } else {
                if (IS_USER_LOGIN){
                    userNameTxt.setText("Welcome, " + CURRENT_USER_EMAIL);
                    Toast.makeText(requireActivity(), "Please Sign-out Email Account.", Toast.LENGTH_SHORT).show();
                }else {
                    googleSignIn();
                }
            }
        });

        logoutBtn.setOnClickListener(view1 -> {
            userNameTxt.setText("");
            historyHtml.setText("");
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
            if (account == null) {
                if (IS_USER_LOGIN) {
                    SharedPreferences.Editor preferences = requireActivity().
                            getSharedPreferences("Users", Context.MODE_PRIVATE).edit();
                    preferences.putString("id", "");
                    preferences.putString("email", "");
                    preferences.putBoolean("isLogin", false);
                    CURRENT_USER_EMAIL = "";
                    IS_USER_LOGIN = false;
                    preferences.apply();
                    Toast.makeText(requireActivity(), "Logout Successfully.", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(requireActivity(), "Please Login first...", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            showProgressDialog(requireActivity());
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    hideProgressDialog();
                    Toast.makeText(requireActivity(), "Logout Successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    hideProgressDialog();
                    Toast.makeText(requireActivity(), "Try again later...", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                hideProgressDialog();
                Toast.makeText(requireActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        emailBtn.setOnClickListener(view1 -> {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
            if (account == null) {
                String email = inputEmail.getText().toString();
                if (!email.contains("@")) {
                    inputEmail.setError(getString(R.string.enter_your_email));
                    inputEmail.requestFocus();
                } else {
                    showProgressDialog(requireActivity());
                    isUserExists = false;
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    Users users = dataSnapshot.getValue(Users.class);
                                    assert users != null;
                                    if (email.equals(users.getEmail())) {
                                        isUserExists = true;
                                        SharedPreferences.Editor preferences = requireActivity().
                                                getSharedPreferences("Users", Context.MODE_PRIVATE).edit();
                                        preferences.putString("id", users.getId());
                                        preferences.putString("email", email);
                                        preferences.putBoolean("isLogin", true);
                                        IS_USER_LOGIN = true;
                                        CURRENT_USER_EMAIL = email;
                                        userNameTxt.setText("Welcome, " + CURRENT_USER_EMAIL);
                                        preferences.apply();
                                        inputEmail.setText("");
                                        createAndSaveOfflineMeetingHtml();
                                        Toast.makeText(requireActivity(), "Logged In Successfully.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            }
                            if (!isUserExists) {
                                String id = reference.push().getKey();
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("id", id);
                                hashMap.put("name", "");
                                hashMap.put("email", email);
                                assert id != null;
                                reference.child(id).updateChildren(hashMap);
                                SharedPreferences.Editor preferences = requireActivity().
                                        getSharedPreferences("Users", Context.MODE_PRIVATE).edit();
                                preferences.putString("id", id);
                                preferences.putString("email", email);
                                preferences.putBoolean("isLogin", true);
                                preferences.apply();
                                IS_USER_LOGIN = true;
                                CURRENT_USER_EMAIL = email;
                                userNameTxt.setText("Welcome, " + CURRENT_USER_EMAIL);
                                createAndSaveOfflineMeetingHtml();
                                Toast.makeText(requireActivity(), "Account Registered Successfully", Toast.LENGTH_SHORT).show();
                            }
                            inputEmail.setText("");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            hideProgressDialog();
                            Toast.makeText(requireActivity(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }else {
                Toast.makeText(requireActivity(), "Please sign-out google account.", Toast.LENGTH_SHORT).show();
            }
        });

        try {
            PackageInfo pInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            versionNameTxt.setText("v " + version);
            Log.d("==version", version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("==version", "Not Fount");
        }

        setupUI(view);

    }

    private void getUserMeetingHistory(String currentUserEmail) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        Users users = dataSnapshot.getValue(Users.class);
                        assert users != null;
                        if (users.getEmail().equals(currentUserEmail)){
                            Log.d("==uid", users.getId());
                            if (users.getHistoryHtml() != null){
                                historyHtml.setText(users.getHistoryHtml());
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

    private void createAndSaveOfflineMeetingHtml() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireActivity());
        if (account == null) {
            if (IS_USER_LOGIN) {
                if (CURRENT_USER_EMAIL != null && !CURRENT_USER_EMAIL.isEmpty()){
                    email = CURRENT_USER_EMAIL;
                }else {
                    email = DEFAULT_EMAIL_ADDRESS;
                }
            }
        }else {
            email = account.getEmail();
        }
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                meetingsArrayList.clear();
                if (snapshot.exists()) {
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
                                                assert organizer != null;
                                                if (organizer.getEmail().equals(email)) {
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
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                    }
                }
                else {
                    hideProgressDialog();
                }
                new Handler().postDelayed(() -> {
                    if (!meetingsArrayList.isEmpty()) {
                        createAndUploadHtml();
                    }else {
                        hideProgressDialog();
                    }
                }, 2000);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
            }
        });
    }

    private void createAndUploadHtml() {

        String htmlContent = HtmlHelper.createMeetingInfoHTML(meetingsArrayList);

        File htmlFile = createHtmlFile(htmlContent, "Classes List");

        if (htmlFile != null) {
            uploadFileToFirebaseStorage(htmlFile, task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    historyHtml.setText(downloadUri.toString());

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                            .child("Users");
                    Activity activity = getActivity();
                    if (isAdded() && activity != null) {
                        SharedPreferences preferences = requireActivity().getSharedPreferences("Users", Context.MODE_PRIVATE);
                        String id = preferences.getString("id", null);
                        if (id != null) {
                            if (IS_USER_LOGIN) {
                                reference.child(id).child("historyHtml")
                                        .setValue(downloadUri.toString());
                            }
                        }
                    }
//                    for (int i = 0; i < meetingsArrayList.size(); i++){
//                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
//                        reference.child(meetingsArrayList.get(i).getId())
//                                .child("historyHtml").setValue(downloadUri.toString());
//                    }

                    Log.d("==fileh", "File uploaded with URI: " + downloadUri.toString());
                    deleteFile(htmlFile);
                    hideProgressDialog();
                } else {
                    // Handle failure
                    hideProgressDialog();
                    Log.e("==file", "Upload failed", task.getException());
                }
            });
        }

    }

    private File createHtmlFile(String htmlContent, String name) {
        File htmlFile = null;
        try {
            htmlFile = File.createTempFile(name, ".html", requireActivity().getCacheDir());
            FileWriter writer = new FileWriter(htmlFile);
            writer.write(htmlContent);
            writer.close();
            Log.d("==file", "created" + "");
        } catch (IOException e) {
            Log.d("==file", e.getMessage() + "");

        }
        return htmlFile;
    }

    private void uploadFileToFirebaseStorage(File file, OnCompleteListener<Uri> onCompleteListener) {
        Uri fileUri = Uri.fromFile(file);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("htmlFiles/" + file.getName());
        storageRef.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.d("==file", task.getException() + " ");
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(onCompleteListener);
    }

    private void deleteFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d("==file", "File deleted successfully");
            } else {
                Log.d("==file", "Failed to delete file");
            }
        }
    }

    private void initCredentials() {
        mCredential = GoogleAccountCredential.usingOAuth2(
                        requireActivity(), Arrays.asList(CalendarScopes.CALENDAR,
                                CalendarScopes.CALENDAR_READONLY))
                .setBackOff(new ExponentialBackOff());
        initCalendarBuild(mCredential);
    }

    private void initCalendarBuild(GoogleAccountCredential credential) {
        Calendar.Builder builder = null;
        builder = new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("CelebLingo");
        mService = builder.build();
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            mCredential.setSelectedAccountName(account.getEmail());
            userNameTxt.setText("Welcome, " + account.getEmail());
            //registerNewUser(account);
            Toast.makeText(requireActivity(), "Logged In successfully.", Toast.LENGTH_SHORT).show();
            getResultsFromApi();
        } catch (ApiException e) {
            if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                // Consent required, attempt to get consent
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, REQUEST_AUTHORIZATION);
            } else {
                Log.w("SignInError", "signInResult:failed code=" + e.getStatusCode());
            }
        }
    }

    private void registerNewUser(GoogleSignInAccount account) {
        String personName = account.getDisplayName();
        String personGivenName = account.getGivenName();
        String personFamilyName = account.getFamilyName();
        String personEmail = account.getEmail();
        String personId = account.getId();
        Uri personPhoto = account.getPhotoUrl();
        isUserExists = false;
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        Users user = dataSnapshot.getValue(Users.class);
                        assert personEmail != null;
                        assert user != null;
                        if (personEmail.equals(user.getEmail())){
                            isUserExists = true;
                            SharedPreferences.Editor preferences = requireActivity().
                                    getSharedPreferences("Users", Context.MODE_PRIVATE).edit();
                            preferences.putString("id", user.getId());
                            preferences.putString("email", personEmail);
                            preferences.putBoolean("isLogin", false);
                            preferences.apply();
                        }
                    }
                }
                if (!isUserExists){
                    String id = reference.push().getKey();
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("id", id);
                    hashMap.put("name", personName);
                    hashMap.put("email", personEmail);
                    assert id != null;
                    reference.child(id).updateChildren(hashMap);
                    SharedPreferences.Editor preferences = requireActivity().
                            getSharedPreferences("Users", Context.MODE_PRIVATE).edit();
                    preferences.putString("id", id);
                    preferences.putString("email", personEmail);
                    preferences.putBoolean("isLogin", false);
                    preferences.apply();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if (requestCode == REQUEST_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                // Consent granted, try to obtain the token again.
                getAuthToken();
            } else {
                // User denied consent.
                Log.e("AuthError", "User denied consent.");
            }
        }
    }

    private void getResultsFromApi() {
        makeRequestTask();
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    Exception mLastError = null;

    private void makeRequestTask() {
        handler.post(() -> {
            showProgressDialog(requireActivity());
        });

        executor.submit(() -> {
            List<Meetings> result = null;
            try {
                result = getDataFromCalendar();
            } catch (Exception e) {
                mLastError = e;
                hideProgressDialog();
            }

            final List<Meetings> finalResult = result;
            handler.post(() -> {
                if (finalResult == null || finalResult.isEmpty()) {
                    Log.d("Google", "No data");
                    hideProgressDialog();
                    createAndSaveOfflineMeetingHtml();
                } else {
                    saveMeetingDataToFirebase(finalResult);
                }

                if (mLastError != null) {
                    if (mLastError instanceof UserRecoverableAuthIOException) {
                        googleSignIn();
                    } else if (mLastError instanceof UserRecoverableAuthException) {
                        getAuthToken();
                    } else if (mLastError != null) {
                        Log.d("==er ", "The following error occurred:\n" + mLastError.getMessage());
                    } else {
                        Log.d("==err", "Request cancelled.");
                    }
                }
            });
        });
    }

    private void getAuthToken() {
        new Thread(() -> {
            try {
                final String scope = "oauth2:" + CalendarScopes.CALENDAR;
                final String accountName = GoogleSignIn.getLastSignedInAccount(requireActivity()).getEmail();

                final String token = GoogleAuthUtil.getToken(requireActivity().getApplicationContext(), accountName, scope);

            } catch (UserRecoverableAuthException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (GoogleAuthException | IOException e) {
                Log.e("AuthError", "Failed to obtain OAuth token", e);
            }
        }).start();
    }

    private void saveMeetingDataToFirebase(List<Meetings> meetingsList) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Meetings");
        for (int i = 0; i < meetingsList.size(); i++) {
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
            reference.child(id).child("Organizer")
                    .child(1 + "").updateChildren(organizerHashMap);
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
            Log.d("==index", i + " " + meetingsList.size());
            if (i == meetingsList.size() - 1) {
                createAndSaveOfflineMeetingHtml();
            }
        }
        for (int i = 0; i < meetingsList.size(); i++) {
            String mId = meetingsList.get(i).getId();
            String organizerEmail = meetingsList.get(i).getOrganizer().getEmail();
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
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
                                                    assert organizer != null;
                                                    if (organizer.getEmail().equals(organizerEmail)) {

                                                        Log.d("==cal id", meetings.getId() + " ");
                                                        checkIfEventExistsInGoogleCalendar(meetings.getId());
                                                    }
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            return;
        }
    }

    private void checkIfEventExistsInGoogleCalendar(String googleEventId) {
        new Thread(() -> {
            try {
                Event event = mService.events().get("primary", googleEventId).execute();
                if (event.getStatus().equals("cancelled")) {
                    FirebaseDatabase.getInstance().getReference().child("Meetings")
                            .child(googleEventId).removeValue();
                }
                Log.d("==cal status", "exists " + event.getStatus() + " " + googleEventId);
            } catch (GoogleJsonResponseException e) {
                Log.d("==cal exception", "exception " + e + " " + e.getStatusCode());
                if (e.getStatusCode() == 404) {
                    FirebaseDatabase.getInstance().getReference().child("Meetings")
                            .child(googleEventId).removeValue();
                }
            } catch (Exception e) {
                Log.d("==cal", e.getMessage() + " ");
            }
        }).start();
    }

    public List<Meetings> getDataFromCalendar() {
        List<Meetings> eventModels = new ArrayList<>();
        DateTime now = new DateTime(System.currentTimeMillis());

        try {
            com.google.api.services.calendar.model.Events events = mService.events().list("primary")
                    .setMaxResults(20)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<Event> items = events.getItems();

            for (Event event : items) {
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
                eventModels.add(model);
            }
        } catch (IOException e) {
            Log.d("Google ex", e.getMessage());
            hideProgressDialog();
        }

        return eventModels;
    }


}
package com.celeblingo.helper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class DriveManager extends AsyncTask<Void, Void, Void> {

    private final Drive mDriveService;
    private final String mFolderName;
    private final Bitmap bitmap;
    private final String mParentFolderId;
    private final DriveTaskListener mListener;

    public DriveManager(GoogleAccountCredential credential, String folderName,
                        Bitmap bitmap, String parentFolderId, DriveTaskListener listener) {
        mDriveService = new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("Celeblingo")
                .build();
        mFolderName = folderName;
        mParentFolderId = parentFolderId;
        mListener = listener;
        this.bitmap = bitmap;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            if (!folderExists(mFolderName)) {
                try {
                    File fileMetadata = new File();
                    fileMetadata.setName(mFolderName);
                    fileMetadata.setMimeType("application/vnd.google-apps.folder");
                    if (mParentFolderId != null) {
                        fileMetadata.setParents(Collections.singletonList(mParentFolderId));
                    }

                    File folder = mDriveService.files().create(fileMetadata)
                            .setFields("id")
                            .execute();
                        uploadDrawableToDrive(folder.getId(), mDriveService, bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mListener != null) {
                        mListener.onDriveTaskFailed();
                    }
                }
                return null;
            }else {
                Log.d("==drive", "folder exists " + getExistingFolderId(mFolderName));
                uploadDrawableToDrive(getExistingFolderId(mFolderName), mDriveService, bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mListener != null) {
                mListener.onDriveTaskFailed();
            }
        }
        return null;
    }

    public void uploadDrawableToDrive(String FOLDER_ID, Drive driveService, Bitmap bitmap1) throws IOException {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap1.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            File fileMetadata = new File();
            fileMetadata.setName(System.currentTimeMillis() + ".png");
            if (FOLDER_ID != null) {
                fileMetadata.setParents(Collections.singletonList(FOLDER_ID));
            }

            ByteArrayContent mediaContent = new ByteArrayContent("image/png", byteArray);
            File uploadedFile = driveService.files().create(fileMetadata, mediaContent).execute();

            if (mListener != null) {
                mListener.onDriveTaskCompleted(uploadedFile.getId());
            }

            System.out.println("Uploaded image with ID: " + uploadedFile.getId());
        } catch (IOException e) {
            e.printStackTrace();
            if (mListener != null) {
                mListener.onDriveTaskFailed();
            }
        }
    }

    private boolean folderExists(String folderName) throws IOException {
        List<File> files = mDriveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                .setSpaces("drive")
                .execute()
                .getFiles();
        return !files.isEmpty();
    }

    private String getExistingFolderId(String folderName) throws IOException {
        List<File> files = mDriveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                .setSpaces("drive")
                .execute()
                .getFiles();
        if (!files.isEmpty()) {
            return files.get(0).getId();
        }
        return null;
    }

    public interface DriveTaskListener {
        void onDriveTaskCompleted(String id);
        void onDriveTaskFailed();
    }

}

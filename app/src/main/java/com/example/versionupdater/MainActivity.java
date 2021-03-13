package com.example.versionupdater;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    TextView versionTV;
    private Integer versionNumber, receivedNumber;
    private String versionName;

    FirebaseDatabase database;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        versionTV = findViewById(R.id.text_version);

        // Allow installation of files. Needed for newer versions of Android
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        
        // Receive the version number from the server and compare with current version number
        getVersionNumberFromServer();

        // This block of code is used to receive the current version of the application from the Package Manager.
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionNumber = packageInfo.versionCode;
            versionName = packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        versionTV.setText(versionName);
    }

    private void getVersionNumberFromServer() {
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("app_versions");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                receivedNumber = snapshot.child("version_number").getValue(Integer.class);
                compareVersions(receivedNumber, versionNumber); //Function to compare the versions and download the new version if needed.
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("Failed to read value.", error.toException());
            }
        });
    }

    private void compareVersions(Integer receivedNumber, Integer versionNumber) {
        if (receivedNumber > versionNumber) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setTitle("Do you wish to update to a newer version?")
                    .setNegativeButton("No", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    })
                    .setPositiveButton("Yes", ((dialogInterface, i) -> {
                        downloadFile(); // Function to download the apk from the server.
                    }));

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            Toast.makeText(this, "Latest version installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadFile() {
        // Reference for the file in Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://version-updater-4c62f.appspot.com/apk bundle");
        StorageReference fileRef = storageRef.child("app-debug.apk");

        // Create a directory to store the downloaded file
        File rootPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "file_name");
        if (!rootPath.exists())
            rootPath.mkdirs();

        final File localFile = new File(rootPath, "app-debug.apk");

        // Download the file from Firebase Storage
        fileRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> Log.e("firebase", "; local temp file created." + localFile.toString()))
                .addOnFailureListener(exception -> Log.e("firebase ",";local tem file not created  created " +exception.toString()));

        // Install the newly downloaded file.
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(localFile.getAbsolutePath())), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}


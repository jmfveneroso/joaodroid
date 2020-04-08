package com.example.joaodroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.joaodroid.MESSAGE";
    static final String STATE_INITIALIZED = "BOOL_initialized";
    public boolean initialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            initialized = true;
            AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails userStateDetails) {
                    try {
                        Amplify.addPlugin(new AWSS3StoragePlugin());
                        Amplify.configure(getApplicationContext());
                        Log.i("AWS S3", "All set and ready to go!");
                    } catch (Exception e) {
                        Log.e("AWS S3", e.getMessage());
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e("AWS S3", "Initialization error.", e);
                }
            });

        }
        LogReader.load(getApplicationContext());
        LogReader.update(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogReader.update(getApplicationContext());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(STATE_INITIALIZED, initialized);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void openSyncActivity(View view) {
        Intent intent = new Intent(this, SyncActivity.class);
        startActivity(intent);
    }

    public void openFileList(View view) {
        Intent intent = new Intent(this, FileListActivity.class);
        startActivity(intent);
    }

    public void openLogList(View view) {
        String message = "test";
        Intent intent = new Intent(this, LogActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void openTagList(View view) {
        Intent intent = new Intent(this, TagActivity.class);
        startActivity(intent);
    }

    public void openChronoList(View view) {
        Intent intent = new Intent(this, ChronoActivity.class);
        startActivity(intent);
    }
}

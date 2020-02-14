package com.example.joaodroid;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import static com.example.joaodroid.FileListActivity.EXTRA_FILENAME;
import static com.example.joaodroid.LogActivity.EXTRA_ID;

public class FileViewerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_viewer_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tv = findViewById(R.id.file_content);

        Bundle extras = getIntent().getExtras();
        String filename = extras.getString(EXTRA_FILENAME);

        File f = new File(getApplicationContext().getFilesDir(), "files/" + filename);

        try {
            InputStream is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String text = "";
            String line;
            while ((line = reader.readLine()) != null) {
                text += line + "\n";
            }
            tv.setText(text);
        } catch (IOException e ) {

        }
    }

    public void saveFile(View view) {
        TextView tv = findViewById(R.id.file_content);
        String data = tv.getText().toString();

        Bundle extras = getIntent().getExtras();
        String filename = extras.getString(EXTRA_FILENAME);
        try {
            File f = new File(getApplicationContext().getFilesDir(), "files/" + filename);

            // Update id.
            FileWriter fw = new FileWriter(f);
            fw.write(data);
            fw.close();
        } catch (IOException e) {

        }
    }
}
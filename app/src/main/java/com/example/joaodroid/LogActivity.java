package com.example.joaodroid;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.joaodroid.dummy.DummyContent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LogActivity extends AppCompatActivity
        implements LogEntryFragment.OnListFragmentInteractionListener {
    public static final String EXTRA_ID = "com.example.joaodroid.ID";
    public static final String EXTRA_TIMESTAMP = "com.example.joaodroid.TIMESTAMP";
    public static final String EXTRA_TITLE = "com.example.joaodroid.TITLE";
    public static final String EXTRA_CONTENT = "com.example.joaodroid.CONTENT";
    public static final String EXTRA_TAGS = "com.example.joaodroid.TAGS";
    private FragmentRefreshListener fragmentRefreshListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        EditText editText = findViewById(R.id.textView);
        editText.addTextChangedListener(new TextWatcher() {
            private Timer timer=new Timer();
            private final long DELAY = 500; // milliseconds

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    fragmentRefreshListener.onRefresh(s.toString());
                                }
                            });
                        }
                    },
                    DELAY
                );
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });
    }

    private void openLogEntry(String id, Date timestamp, String title, String content,
                              ArrayList<String> tags) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(timestamp);

        StringBuilder sb = new StringBuilder();
        for (String t : tags) {
            sb.append(t + " ");
        }

        Intent intent = new Intent(this, SingleLogEntryActivity.class);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_TIMESTAMP, strDate);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_CONTENT, content);
        intent.putExtra(EXTRA_TAGS, sb.toString());
        startActivityForResult(intent, 0);
    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {
        openLogEntry(item.id, item.timestamp, item.title, item.content, item.tags);
    }

    public void createLogEntry(View view) {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String strDate = dateFormat.format(now);
            String filename = "log." + strDate + ".txt";

            File f = new File(getFilesDir(), "files/id.txt");
            byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
            int id = Integer.parseInt(new String(encoded, StandardCharsets.UTF_8));

            File file = new File(getFilesDir(), "files/" + filename);
            FileWriter fr = new FileWriter(file, file.exists());
            if (!file.exists()) {
                fr.write(strDate + "\n");
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            String str_id = String.format("%08d", id);
            String content = "\n" + str_id;
            content += " [" + dtf.format(now) + "] New entry\n\nNew content\n";

            fr.write(content);
            fr.close();

            file = new File(getFilesDir(), "files/id.txt");
            fr = new FileWriter(file);
            fr.write(Integer.toString(id+1));
            fr.close();

            Date d = Date.from( now.atZone(ZoneId.systemDefault()).toInstant());
            openLogEntry(str_id, d, "New entry", "New content", new ArrayList<>());

            // EditText editText = findViewById(R.id.textView);
            // this.fragmentRefreshListener.onRefresh(editText.getText().toString());
        } catch (IOException e) {
            Log.e("createLogEntry", e.getMessage());
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        EditText editText = findViewById(R.id.textView);
        this.fragmentRefreshListener.onRefresh(editText.getText().toString());
    }

    public void setFragmentRefreshListener(FragmentRefreshListener fragmentRefreshListener) {
        this.fragmentRefreshListener = fragmentRefreshListener;
    }

    public interface FragmentRefreshListener{
        void onRefresh(String q);
    }
}

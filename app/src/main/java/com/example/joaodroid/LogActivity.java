package com.example.joaodroid;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.joaodroid.FileListActivity.EXTRA_FILENAME;

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
    public static final String EXTRA_QUERY = "com.example.joaodroid.QUERY";
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

        Bundle extras = getIntent().getExtras();
        if (extras.containsKey(EXTRA_QUERY)) {
            String query = extras.getString(EXTRA_QUERY);
            editText.setText(query);
        }

        registerForContextMenu(findViewById(R.id.create_button));
    }

    private void openLogEntry(int id) {
        Intent intent = new Intent(this, SingleLogEntryActivity.class);
        intent.putExtra(EXTRA_ID, id);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onListFragmentInteraction(LogReader.LogEntry item) {
        openLogEntry(Integer.parseInt(item.id));
    }

    public void createLogEntry(View view) {
        int id = LogReader.createLogEntry(getApplicationContext());
        openLogEntry(id);
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

    private void createChronoDialog(boolean go) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LogActivity.this);
        alertDialog.setTitle("Chrono " + ((go) ? "Go" : "Back"));

        final EditText input = new EditText(LogActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        alertDialog.setView(input);

        alertDialog.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String chrono = input.getText().toString();
                        LogReader.addChronoEntry(getApplicationContext(), chrono, go);
                    }
                });

        alertDialog.setNegativeButton("NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    public void chronoGo(View view) {
        createChronoDialog(true);
    }

    public void chronoBack(View view) {
        createChronoDialog(false);
    }
}

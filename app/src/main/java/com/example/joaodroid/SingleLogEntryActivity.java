package com.example.joaodroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.joaodroid.LogActivity.EXTRA_ID;
import static com.example.joaodroid.LogActivity.EXTRA_CONTENT;
import static com.example.joaodroid.LogActivity.EXTRA_TIMESTAMP;
import static com.example.joaodroid.LogActivity.EXTRA_TITLE;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SingleLogEntryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_single_log_entry);

        TextView id = findViewById(R.id.id);
        TextView timestamp = findViewById(R.id.timestamp);
        TextView title = findViewById(R.id.title);
        TextView content = findViewById(R.id.log_output);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            id.setText(extras.getString(EXTRA_ID));
            timestamp.setText(extras.getString(EXTRA_TIMESTAMP));
            title.setText(extras.getString(EXTRA_TITLE));
            content.setText(extras.getString(EXTRA_CONTENT));
        }
    }

    private void deleteLogEntryAux(String entry_id) {
        String date_pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(date_pattern);

        try {
            Bundle extras = getIntent().getExtras();
            Date d = dateFormat.parse(extras.getString(EXTRA_TIMESTAMP));

            date_pattern = "yyyy-MM-dd";
            dateFormat = new SimpleDateFormat(date_pattern);

            String strDate = dateFormat.format(d);
            String filename = "log." + strDate + ".txt";

            File file = new File(getFilesDir(), "files/" + filename);
            BufferedReader br = new BufferedReader(new FileReader(file));

            Pattern p = Pattern.compile("^(\\d{8}) (\\[\\d{2}:\\d{2}:\\d{2}\\])");

            ArrayList<String> lines = new ArrayList<>();

            boolean erasing = false;
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.find()) {
                    String id = m.group(1);
                    if (id.equals(entry_id)) {
                        erasing = true;
                    } else {
                        erasing = false;
                    }
                }

                if (!erasing) {
                    lines.add(line);
                }
            }
            br.close();

            file = new File(getFilesDir(), "files/" + filename);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
            bw.close();
            Log.i("deleteLogEntry", "Deleted entry with id " + entry_id);
        } catch (ParseException e) {
            Log.e("deleteLogEntry", e.getMessage());
        } catch (IOException e) {
            Log.e("deleteLogEntry", e.getMessage());
        }
    }

    public void createLogEntry() {
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
            String content = "\n" + String.format("%08d", id);
            content += " [" + dtf.format(now) + "] ";

            TextView title = findViewById(R.id.title);
            content += title.getText() + "\n\n";

            TextView log_output = findViewById(R.id.log_output);
            content += log_output.getText() + "\n";

            fr.write(content);
            fr.close();

            file = new File(getFilesDir(), "files/id.txt");
            fr = new FileWriter(file);
            fr.write(Integer.toString(id + 1));
            fr.close();
        } catch (IOException e) {
            Log.e("createLogEntry", e.getMessage());
        }
    }

    public void deleteLogEntry(View view) {
        Bundle extras = getIntent().getExtras();
        deleteLogEntryAux(extras.getString(EXTRA_ID));
        finish();
    }

    public void editLogEntry(View view) {
        Bundle extras = getIntent().getExtras();
        deleteLogEntryAux(extras.getString(EXTRA_ID));
        createLogEntry();
        finish();
    }
}

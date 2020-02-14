package com.example.joaodroid;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileListActivity extends AppCompatActivity {
    public static final String EXTRA_FILENAME = "com.example.joaodroid.FILENAME";

    private TextView createTextView() {
        float d = getResources().getDisplayMetrics().density;

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins((int) d * 24, (int) d * 8, (int) d * 24, (int) d * 8);
        tv.setLayoutParams(params);

        tv.setPadding((int) d * 16, (int) d * 12, (int) d * 16, (int) d * 12);
        tv.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.textview_border));
        tv.setTypeface(Typeface.MONOSPACE);
        return tv;
    }

    private void openFile(String filename) {
        Intent intent = new Intent(this, FileViewerActivity.class);
        intent.putExtra(EXTRA_FILENAME, filename);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_list_activity);

        LinearLayout ll = findViewById(R.id.linear_layout);
        File directory = new File(getApplicationContext().getFilesDir(), "files");
        File[] files = directory.listFiles();
        ArrayList<String> filenames = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            filenames.add(files[i].getName());
        }

        Collections.sort(filenames);
        for (String f : filenames) {
            TextView tv = createTextView();
            tv.setText(f);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFile(f);
                }
            });
            ll.addView(tv);
        }
    }

}

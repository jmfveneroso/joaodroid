package com.example.joaodroid;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import static com.example.joaodroid.LogActivity.EXTRA_QUERY;

public class TagActivity extends AppCompatActivity {

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

    private void listTags(String query) {
        Intent intent = new Intent(this, LogActivity.class);
        intent.putExtra(EXTRA_QUERY, query);
        startActivity(intent);
    }

    protected void update() {
        LinearLayout ll = findViewById(R.id.linear_layout2);
        ll.removeAllViews();
        for (LogReader.Tag tag : LogReader.tags) {
            TextView tv = createTextView();

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String text = tag.name + "\n" + dateFormat.format(tag.modifiedAt);
            tv.setText(text);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listTags(tag.name);
                }
            });
            ll.addView(tv);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag);
        update();
    }
}

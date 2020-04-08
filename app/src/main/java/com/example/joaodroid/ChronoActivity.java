package com.example.joaodroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.example.joaodroid.LogActivity.EXTRA_QUERY;

public class ChronoActivity extends AppCompatActivity {
    private TextView createTextView() {
        float d = getResources().getDisplayMetrics().density;

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // params.setMargins((int) d * 24, (int) d * 8, (int) d * 8, (int) d * 0);
        tv.setLayoutParams(params);
        tv.setBackgroundColor(Color.rgb(0, 120, 120));
        tv.setTextColor(Color.rgb(230, 230, 230));

        tv.setPadding((int) d * 24, (int) d * 8, (int) d * 24, (int) d * 8);
        // tv.setPadding((int) d * 16, (int) d * 12, (int) d * 16, (int) d * 12);
        // tv.setBackground(ContextCompat.getDrawable(getApplicationContext(),
        //         R.drawable.textview_border));
        tv.setTypeface(Typeface.MONOSPACE);
        return tv;
    }

    private TextView createStats(String chronoKey) {
        float d = getResources().getDisplayMetrics().density;

        LogReader.Chrono chrono = LogReader.chronos.get(chronoKey);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins((int) d * 24, (int) d * 8, (int) d * 24, (int) d * 16);
        tv.setLayoutParams(params);
        // tv.setBackgroundColor(Color.rgb(0, 120, 120));
        // tv.setTextColor(Color.rgb(230, 230, 230));

        tv.setPadding((int) d * 16, (int) d * 12, (int) d * 16, (int) d * 12);
        tv.setBackground(ContextCompat.getDrawable(getApplicationContext(),
          R.drawable.textview_border));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(12);

        String s = chrono.avgStartTime + " -> " + chrono.avgEndTime + " (" + chrono.avgDuration + ")";
        tv.setText(s);
        return tv;
    }

    private Button createButton(String chronoKey, String text, boolean go) {
        float d = getResources().getDisplayMetrics().density;
        Button btn = new Button(this);
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params2.setMargins((int) d * 24, (int) d * 8, (int) d * 24, (int) d * 16);
        btn.setLayoutParams(params2);
        btn.setText(text);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogReader.addChronoEntry(getApplicationContext(), chronoKey, go);
                Intent intent = getIntent();
                finish();
                LogReader.update(getApplicationContext());
                startActivity(intent);
            }
        });
        return btn;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chrono);

        LinearLayout ll = findViewById(R.id.linear_layout2);
        for (String key : LogReader.chronos.keySet()) {
            TextView tv = createTextView();
            tv.setText(key);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            ll.addView(tv);

            ChronoChart cc = new ChronoChart(this, null, key);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                930,
                600
            );
            float d = getResources().getDisplayMetrics().density;
            params.setMargins((int) d * 24, (int) d * 16, (int) d * 24, (int) d * 16);
            cc.setLayoutParams(params);
            cc.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                    R.drawable.textview_border));

            ll.addView(cc);

            TextView tv2 = createStats(key);
            ll.addView(tv2);

            boolean go = true;
            String text = "";
            LogReader.Chrono chrono = LogReader.chronos.get(key);
            if (chrono.endDates.size() > 0 && chrono.startDates.size() > 0) {
                Date startDate = chrono.rawStartDates.get(chrono.rawStartDates.size() - 1);
                Date endDate = chrono.rawEndDates.get(chrono.rawEndDates.size() - 1);

                if (endDate.before(startDate)) {
                    go = false;
                }
            }

            if (go) {
                text = "Start";
            }

            Button btn = createButton(key, text, go);
            ll.addView(btn);

            Handler handler = new Handler();
            final Runnable r = new Runnable() {
                public void run() {
                    if (chrono.endDates.size() == 0 || chrono.startDates.size() == 0) {
                        return;
                    }

                    Date startDate = chrono.rawStartDates.get(chrono.rawStartDates.size() - 1);
                    Date endDate = chrono.rawEndDates.get(chrono.rawEndDates.size() - 1);

                    if (endDate.after(startDate)) {
                        return;
                    }

                    handler.postDelayed(this, 1000);

                    long diffInMillies = Math.abs(new Date().getTime() - startDate.getTime());
                    long s = TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS);

                    String elapsed = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);

                    String text = "Finish (" + elapsed + ")";
                    btn.setText(text);
                }
            };
            handler.postDelayed(r, 0000);
        }
    }

}

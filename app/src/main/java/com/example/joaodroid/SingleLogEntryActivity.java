package com.example.joaodroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.joaodroid.LogActivity.EXTRA_ID;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SingleLogEntryActivity extends AppCompatActivity {
    private LogReader.LogEntry entry;

    @Override
    protected void onPause() {
        super.onPause();
        this.entry.parent.rewrite(getApplicationContext());
    }

    public class CustomPagerAdapter extends PagerAdapter {
        LogReader.LogEntry entry;
        int entry_id;
        private Context mContext;

        public CustomPagerAdapter(Context context, int entry_id) {
            mContext = context;
            this.entry_id = entry_id;
        }

        private TextView createTextView() {
            float d = getResources().getDisplayMetrics().density;

            TextView tv = new TextView(mContext);
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

        private TextView createAttributeTextView() {
            float d = getResources().getDisplayMetrics().density;

            TextView tv = new TextView(mContext);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins((int) d * 24, (int) d * 8, (int) d * 24, (int) d * 8);
            tv.setLayoutParams(params);

            tv.setPadding((int) d * 16, (int) d * 12, (int) d * 16, (int) d * 12);
            tv.setTypeface(Typeface.MONOSPACE);
            return tv;
        }

        private CheckBox createCheckbox() {
            float d = getResources().getDisplayMetrics().density;

            CheckBox cb = new CheckBox(mContext);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins((int) d * 24, (int) d * 8, (int) d * 24, (int) d * 8);
            cb.setLayoutParams(params);
            cb.setTypeface(Typeface.MONOSPACE);
            return cb;
        }

        private TextView getChronoView(String key) {
            TextView tv = createTextView();

            LogReader.Chrono chrono = LogReader.getChrono(key);
            String text = "";
            text += "Avg Duration: " + chrono.avgDuration + "\n";
            text += "Avg Start: " + chrono.avgStartTime + "\n";
            text += "Avg End: " + chrono.avgEndTime + "\n";
            tv.setText(text);
            return tv;
        }

        private CheckBox createTask(String title, boolean complete, int taskIndex) {
            CheckBox cb = createCheckbox();
            cb.setText(title);
            cb.setChecked(complete);

            LogReader.LogEntry entry = this.entry;
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    entry.setTaskChecked(taskIndex, isChecked);
                }
            });

            return cb;
        }

        private boolean isSpecialBlock(String line) {
            return line.startsWith("+") || line.startsWith("[ ]") || line.startsWith("[x]");
        }

        private void createDynamicFeatures() {
            if (this.entry == null) return;

            LinearLayout ll = findViewById(R.id.dynamic_features);
            int taskIndex = 0;

            int i = 0;
            while (i < this.entry.content.size()) {
                String line = this.entry.content.get(i);
                if (line.length() == 0) {
                    ++i;
                    continue;
                }

                if (isSpecialBlock(line)) {
                    if (line.startsWith("[ ]")) {
                        String taskTitle = line.substring(4);
                        ll.addView(createTask(taskTitle, false, taskIndex++));
                    } else if (line.startsWith("[x]")) {
                        String taskTitle = line.substring(4);
                        ll.addView(createTask(taskTitle, true, taskIndex++));
                    } else if (line.startsWith("+")) {
                        TextView tv = createTextView();
                        if (line.startsWith("+chrono ")) {
                            tv = getChronoView(line.substring(8));
                        } else if (line.startsWith("progress")) {
                            tv.setText(line);
                        }
                        ll.addView(tv);
                    } else {
                        // Do nothing.
                    }
                    ++i;
                    continue;
                }

                ArrayList<String> block_content = new ArrayList<>();
                block_content.add(line);
                ++i;

                boolean foundSpace = false;
                while (i < this.entry.content.size()) {
                    line = this.entry.content.get(i);
                    if (line.length() == 0 && foundSpace) {
                        break;
                    } else if (isSpecialBlock(line)) {
                        break;
                    } else if (line.length() == 0) {
                        foundSpace = true;
                        block_content.add(line);
                    } else {
                        foundSpace = false;
                        block_content.add(line);
                    }
                    ++i;
                }

                // Remove extra blank lines.
                while (block_content.size() > 0) {
                    int last = block_content.size() - 1;
                    if (block_content.get(last).length() > 0) break;
                    block_content.remove(last);
                }

                TextView tv = createTextView();
                tv.setText(String.join("\n", block_content));
                ll.addView(tv);
            }
        }

        public void instantiateViewer() {
            TextView idTextView = findViewById(R.id.id);
            TextView timestamp = findViewById(R.id.timestamp);
            TextView title = findViewById(R.id.title);
            TextView tags = findViewById(R.id.tags);

            idTextView.setText(String.format("%08d", this.entry_id));
            this.entry = LogReader.getLogEntryById(this.entry_id);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String strDate = dateFormat.format(this.entry.timestamp);
            timestamp.setText(strDate);

            title.setText(this.entry.title);
            String tagsText = String.join(" ", this.entry.tags);
            tags.setText(tagsText);

            createDynamicFeatures();
        }

        public void instantiateEditor() {
            TextView idTextView = findViewById(R.id.id2);
            TextView timestamp = findViewById(R.id.timestamp2);
            TextView title = findViewById(R.id.title2);
            TextView tags = findViewById(R.id.tags);
            TextView content = findViewById(R.id.log_output);

            idTextView.setText(String.format("%08d", this.entry_id));
            this.entry = LogReader.getLogEntryById(this.entry_id);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String strDate = dateFormat.format(this.entry.timestamp);
            timestamp.setText(strDate);

            title.setText(this.entry.title);
            String tagsText = String.join(" ", this.entry.tags);
            tags.setText(tagsText);

            content.setText(this.entry.getContent());

            LogReader.LogEntry entry = this.entry;
            content.addTextChangedListener(new TextWatcher() {
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
                                    entry.setContent(content.getText().toString());
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

            title.addTextChangedListener(new TextWatcher() {
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
                                    entry.title = title.getText().toString();
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

            tags.addTextChangedListener(new TextWatcher() {
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
                                            String tagsStr = tags.getText().toString();
                                            entry.tags.clear();
                                            for (String tag : tagsStr.split(" ")) {
                                                if (tag.length() > 0) {
                                                    entry.tags.add(tag);
                                                }
                                            }
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

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            if (position == 0) {
                ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.activity_single_log_entry_view, collection, false);
                collection.addView(layout);

                instantiateViewer();
                return layout;
            } else {
                ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.activity_single_log_entry_edit, collection, false);
                collection.addView(layout);
                instantiateEditor();
                return layout;
            }
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "None";
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_single_log_entry);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            int id = extras.getInt(EXTRA_ID);
            ViewPager viewPager = findViewById(R.id.view_pager);
            CustomPagerAdapter adapter = new CustomPagerAdapter(this, id);
            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                private Timer timer=new Timer();
                private final long DELAY = 500; // milliseconds

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                            }
                        },
                        DELAY
                    );
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });

            viewPager.setAdapter(adapter);
            this.entry = LogReader.getLogEntryById(id);
        }
    }
}

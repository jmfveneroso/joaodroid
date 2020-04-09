package com.example.joaodroid;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LogReader {
    public static Context context;
    public static ArrayList<LogEntry> logEntries = new ArrayList<>();
    public static ArrayList<Tag> tags = new ArrayList<>();
    public static HashMap<Integer, LogEntry> logEntriesById = new HashMap<>();
    public static HashMap<Integer, Tag> tagsById = new HashMap<>();
    public static HashMap<String, Tag> tagsByName = new HashMap<>();

    public static class Tag {
        public int id;
        public String name;
        public ArrayList<Tag> children = new ArrayList<>();
        public ArrayList<LogEntry> entries = new ArrayList<>();
        public LocalDateTime modifiedAt;
        public Tag parent;

        public Tag(int id, String name, LocalDateTime modifiedAt) {
            this.id = id;
            this.name = name;
            this.modifiedAt = modifiedAt;
        }

        public void addChild(Tag child) {
            children.add(child);
            child.parent = this;
        }

        private void getEntriesInternal(Tag t, ArrayList<LogEntry> aux) {
            aux.addAll(t.entries);
            for (Tag c : t.children) {
                getEntriesInternal(c, aux);
            }
        }
        public ArrayList<LogEntry> getEntries() {
            ArrayList<LogEntry> res = new ArrayList<>();
            getEntriesInternal(this, res);

            Collections.sort(res, new Comparator<LogEntry>() {
                @Override
                public int compare(LogEntry e1, LogEntry e2) {
                    return e2.modifiedAt.compareTo(e1.modifiedAt);
                }
            });
            return res;
        }

        public void addEntry(LogEntry entry) {
            entries.add(entry);
            entry.category = this;
        }

        public void removeEntry(LogEntry entry) {
            entries.remove(entry);
        }
    }

    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////

    public static class LogEntry {
        public int id;
        public String title;
        public ArrayList<String> content = new ArrayList<>();
        public ArrayList<String> tags;
        public LocalDateTime datetime;
        public LocalDateTime modifiedAt;
        public Tag category;

        public double score = 0;

        public LogEntry(int id, String title, LocalDateTime created_at, LocalDateTime modified_at, String content) {
            this.id = id;
            this.title = title.trim();
            this.datetime = created_at;
            this.modifiedAt = modified_at;
            parseContent(content);
        }

        private void parseContent(String s) {
            String[] lines = s.split("\n");
            for(String l : lines){
                content.add(l);
            }

            // Remove blank lines on top.
            while (content.size() > 0) {
                if (content.get(0).length() > 0) break;
                content.remove(0);
            }

            // Remove blank lines on bottom.
            while (content.size() > 0) {
                int last = content.size() - 1;
                if (content.get(last).length() > 0) break;
                content.remove(last);
            }
        }

        public void setTaskChecked(int taskIndex, boolean checked) {
            for (int i = 0; i < this.content.size(); ++i) {
                String line = this.content.get(i);
                if (line.startsWith("[ ]") || line.startsWith("[x]")) {
                    if (taskIndex > 0) {
                        taskIndex--;
                        continue;
                    }

                    String replacement = (checked) ? "[x] " : "[ ] ";
                    replacement += line.substring(4);
                    this.content.set(i, replacement);
                    return;
                }
            }
        }

        public String getContent() {
            return String.join("\n", this.content);
        }

        public void setContent(String data) {
            this.content = new ArrayList<>(Arrays.asList(data.split("\n")));
            this.modifiedAt = LocalDateTime.now();
        }

        public void setTitle(String title) {
            this.title = title;
            this.modifiedAt = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return "";
        }
    }

    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////

    public static LocalDateTime  strToDate(String s) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(s, formatter);
    }

    public static void update(Context context) {
        /* buildChronoIndex(context); */
    }

    public static ArrayList<LogEntry> getLogs() {
        return LogReader.logEntries;
    }

    public static ArrayList<Tag> getTags() {
        return LogReader.tags;
    }

    public static LogEntry getLogEntryById(int id) {
        if (!logEntriesById.containsKey(id)) {
            return null;
        }
        return logEntriesById.get(id);
    }

    public static Tag getTagById(int id) {
        if (!tagsById.containsKey(id)) {
            return null;
        }
        return tagsById.get(id);
    }

    public static Tag getTagByName(String name) {
        if (!tagsByName.containsKey(name)) {
            return null;
        }
        return tagsByName.get(name);
    }

    //// API
    //// API
    //// API

    public static void sortEntries() {
        Collections.sort(logEntries, new Comparator<LogEntry>() {
            @Override
            public int compare(LogEntry e1, LogEntry e2) {
                return e2.modifiedAt.compareTo(e1.modifiedAt);
            }
        });
    }

    public static void load(Context context) {
        LogReader.context = context;
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://18.224.133.3/all";

        Response.Listener<String> res = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject res = new JSONObject(response);
                    JSONArray tags = res.getJSONArray("tags");
                    for (int i = 0; i < tags.length(); i++) {
                        JSONObject tag = tags.getJSONObject(i);
                        Log.i("log_tag", tag.getString("name"));

                        int id = tag.getInt("id");
                        String name = tag.getString("name");
                        LocalDateTime modified_at = strToDate(tag.getString("modified_at"));

                        Tag t = new Tag(id, name, modified_at);
                        LogReader.tags.add(t);
                        tagsById.put(t.id, t);
                        tagsByName.put(t.name, t);
                    }

                    for (int i = 0; i < tags.length(); i++) {
                        JSONObject obj = tags.getJSONObject(i);
                        JSONArray children = obj.getJSONArray("children");
                        for (int j = 0; j < children.length(); j++) {
                            Tag tag = getTagById(obj.getInt("id"));
                            Tag child = getTagById(children.getInt(j));
                            tag.addChild(child);
                        }
                    }

                    JSONArray entries = res.getJSONArray("entries");
                    for (int i = 0; i < entries.length(); i++){
                        JSONObject entry = entries.getJSONObject(i);

                        int id = entry.getInt("id");
                        String title = entry.getString("title");
                        LocalDateTime created_at = strToDate(entry.getString("created_at"));
                        LocalDateTime modified_at = strToDate(entry.getString("modified_at"));
                        String content = entry.getString("content");

                        LogEntry e = new LogEntry(id, title, created_at, modified_at, content);
                        Tag t = getTagById(entry.getInt("category"));
                        t.addEntry(e);
                        LogReader.logEntries.add(e);
                        logEntriesById.put(e.id, e);
                    }

                    sortEntries();

                } catch(JSONException e) {

                }
            }
        };

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                res,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(">>>>>>>>>>>>>>", error.getMessage());
                    }
                }
        );
        queue.add(stringRequest);

        buildChronoIndex(context);
    }

    public static int getNextEntryId() {
        Set<Integer> keys = logEntriesById.keySet();
        return Collections.max(keys) + 1;
    }

    public static int createLogEntry(LogEntry entry) {
        LogEntry e;
        if (entry == null) {
            int id = getNextEntryId();
            LocalDateTime now = LocalDateTime.now();
            e = new LogEntry(id, "New entry", now, now, "");
            Tag t = getTagById(0);
            t.addEntry(e);
            logEntriesById.put(e.id, e);
            LogReader.logEntries.add(e);
        } else {
            e = new LogEntry(entry.id, entry.title, entry.datetime, entry.modifiedAt, entry.getContent());
        }
        int id = e.id;

        RequestQueue queue = Volley.newRequestQueue(context);
        Response.Listener<JSONObject> res = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    LocalDateTime modified_at = strToDate(response.getString("modified_at"));
                    e.modifiedAt = modified_at;
                } catch (JSONException e) {

                }
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("title", "New entry");
        params.put("parent_id", "0");
        JSONObject json = new JSONObject(params);

        String url = "http://18.224.133.3/entries/";
        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.POST, url,
                json, res,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        );
        queue.add(putRequest);
        return id;
    }

    public static void updateEntry(LogEntry e) {
        RequestQueue queue = Volley.newRequestQueue(context);
        Response.Listener<JSONObject> res = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    LocalDateTime modified_at = strToDate(response.getString("modified_at"));
                    e.modifiedAt = modified_at;
                } catch (JSONException e) {

                }
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("id", Integer.toString(e.id));
        params.put("title", e.title);
        params.put("content", e.getContent());
        params.put("tag", e.category.name);
        JSONObject json = new JSONObject(params);

        String url = "http://18.224.133.3/entries/";
        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PATCH, url,
            json, res,
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }
        );
        queue.add(putRequest);
    }

    public static void deleteLogEntry(LogEntry e) {
        e.category.removeEntry(e);
        LogReader.logEntriesById.remove(e.id);
        LogReader.logEntries.remove(e);

        RequestQueue queue = Volley.newRequestQueue(context);
        Response.Listener<JSONObject> res = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {}
        };

        Map<String, String> params = new HashMap<>();
        params.put("id", Integer.toString(e.id));
        JSONObject json = new JSONObject(params);

        String url = "http://18.224.133.3/entries/" + Integer.toString(e.id) + "/";
        JsonObjectRequest deleteRequest = new JsonObjectRequest(Request.Method.DELETE, url,
                json, res,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        );
        queue.add(deleteRequest);
    }


    public static void reinsertLogEntry(LogEntry e, Tag tag) {
        Tag t = getTagById(0);
        t.addEntry(e);
        logEntriesById.put(e.id, e);
        LogReader.logEntries.add(e);
        tag.addEntry(e);
    }


    //// CHRONO
    //// CHRONO
    //// CHRONO

    public static HashMap<String, Chrono> chronos = new HashMap<>();

    public static class Chrono {
        Date start_date = null;
        ArrayList<Date> rawStartDates = new ArrayList<>();
        ArrayList<Date> rawEndDates = new ArrayList<>();
        String avgStartTime;
        String avgEndTime;
        String avgDuration;
        int avgStartTimeSecs;
        int avgEndTimeSecs;
        int avgDurationSecs;

        ArrayList<Integer> startDates = new ArrayList<>();
        ArrayList<Integer> endDates = new ArrayList<>();

        long avg_span = 0;
        long num_spans = 0;

        public Chrono() {}
    }

    public static void addChronoEntry(Context context, String chrono, boolean go) {
        try {
            File f = new File(context.getFilesDir(), "files/chrono.txt");
            FileWriter fw = new FileWriter(f, f.exists());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String line = LocalDateTime.now().format(formatter) + " ";
            line += ((go) ? ">" : "<") + " ";
            line += chrono;
            fw.append(line + "\n");
            fw.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
    }

    public static long secondsSinceMidnight(Date d) {
        Date midnight = (Date) d.clone();
        midnight.setHours(0);
        midnight.setMinutes(0);
        midnight.setSeconds(0);
        long diff_in_ms = Math.abs(d.getTime() - midnight.getTime());
        return TimeUnit.SECONDS.convert(diff_in_ms, TimeUnit.MILLISECONDS);
    }

    private static String secondsToString(long s) {
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    static double timeToDegrees(long seconds) {
        /* 1 second of time = 360/(24 * 3600) = 1/240th degree */
        return seconds / 240.0;
    }

    static double meanAngle(ArrayList<Integer> seconds) {
        int len = seconds.size();
        double sinSum = 0.0;
        for (int i = 0; i < len; i++) {
            sinSum += Math.sin(timeToDegrees(seconds.get(i)) * Math.PI / 180.0);
        }

        double cosSum = 0.0;
        for (int i = 0; i < len; i++) {
            cosSum += Math.cos(timeToDegrees(seconds.get(i)) * Math.PI / 180.0);
        }

        return Math.atan2(sinSum / len, cosSum / len) * 180.0 / Math.PI;
    }

    static int degreesToSecs(double d) {
        if (d < 0.0) d += 360.0;
        return (int) (d * 240.0);
    }

    static String degreesToTime(double d) {
        int secs = degreesToSecs(d);
        int hours = secs / 3600;
        int mins  = secs % 3600;
        secs = mins % 60;
        mins /= 60;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    public static void buildChronoIndex(Context context) {
        chronos.clear();
        try {
            File f = new File(context.getFilesDir(), "files/chrono.txt");
            InputStream is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] arr = line.split(" ");

                String tag = arr[3];
                if (!chronos.containsKey(tag)) {
                    chronos.put(tag, new Chrono());
                }
                Chrono chrono = chronos.get(tag);

                DateFormat formatter = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                Date datetime = formatter.parse(arr[0] + arr[1]);
                String type = arr[2];

                if (type.equals(">")) {
                    chrono.rawStartDates.add(datetime);
                    chrono.startDates.add((int) secondsSinceMidnight(datetime));
                    chrono.start_date = datetime;
                } else if (type.equals("<")) {
                    chrono.rawEndDates.add(datetime);
                    chrono.endDates.add((int) secondsSinceMidnight(datetime));
                    if (chrono.start_date != null) {
                        long diff_in_ms = Math.abs(datetime.getTime() - chrono.start_date.getTime());
                        long s = TimeUnit.SECONDS.convert(diff_in_ms, TimeUnit.MILLISECONDS);
                        chrono.avg_span += s;
                        chrono.num_spans++;
                    }
                    chrono.start_date = null;
                } else {
                    Log.e("buildChronoIndex", "Unknown type");
                }
            }

            for (String key : LogReader.chronos.keySet()) {
                Chrono c = LogReader.chronos.get(key);
                c.avg_span /= (c.num_spans > 0) ? c.num_spans : 1;

                long s = c.avg_span;
                c.avgDuration = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
                c.avgStartTime = degreesToTime(meanAngle(c.startDates));
                c.avgEndTime = degreesToTime(meanAngle(c.endDates));

                c.avgDurationSecs = (int) s;
                c.avgStartTimeSecs = degreesToSecs(meanAngle(c.startDates));
                c.avgEndTimeSecs = degreesToSecs(meanAngle(c.endDates));
            }
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        } catch (ParseException e) {

        }
    }

    public static Chrono getChrono(String tag) {
        if (!chronos.containsKey(tag)) {
            return new Chrono();
        }
        return chronos.get(tag);
    }
}


/*
public static class LogFile {
    ArrayList<LogReader.LogEntry> logEntries;
    LocalDate date;

    public LogFile(LocalDate date, ArrayList<String> content) {
        this.logEntries = new ArrayList<>();
        this.date = date;

        init(content);
    }

    public void init(ArrayList<String> content) {
        Pattern pattern = Pattern.compile("^(\\d{8}) (\\[\\d{2}:\\d{2}:\\d{2}\\])");
        int i = 0;
        while (i < content.size()) {
            String line = content.get(i);
            Matcher m = pattern.matcher(line);
            if (!m.find()) {
                ++i;
                continue;
            }

            String id = m.group(1);
            String header = line.substring(m.group(0).length());

            String timestamp = m.group(2);
            timestamp = timestamp.substring(1, timestamp.length()-1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss");
            LocalDateTime datetime = LocalDateTime.parse(date + timestamp, formatter);

            ArrayList<String> entryContent = new ArrayList<>();
            ++i;
            while (i < content.size()) {
                line = content.get(i);
                m = pattern.matcher(line);
                if (m.find()) break;
                entryContent.add(line);
                ++i;
            }

            this.logEntries.add(new LogReader.LogEntry(this, id, datetime, header, entryContent));
        }
    }

    public void rewrite(Context context) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = this.date.format(formatter);

        try {
            File f = new File(context.getFilesDir(), "files/log." + date + ".txt");

            String weekday = DayOfWeek.from(this.date).name();
            weekday = Character.toTitleCase(weekday.charAt(0)) +
                    weekday.substring(1).toLowerCase();

            String data = date + " (" + weekday  + ")\n\n\n";
            for (LogReader.LogEntry e : this.logEntries) {
                data += e.toString();
            }

            FileWriter fw = new FileWriter(f);
            fw.write(data);
            fw.close();

        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
    }

    public LogReader.LogEntry addLogEntry(Context context) {
        LogReader.LogEntry logEntry = null;
        try {
            File f = new File(context.getFilesDir(), "files/id.txt");
            byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
            int id = Integer.parseInt(new String(encoded, StandardCharsets.UTF_8)) + 1;

            // Update id.
            FileWriter fw = new FileWriter(f);
            fw.write(Integer.toString(id));
            fw.close();

            logEntry = new LogReader.LogEntry(this, String.format("%08d", id),
                    LocalDateTime.now(), "New Entry", new ArrayList<>());
        } catch (IOException e) {

        }
        this.logEntries.add(logEntry);
        return logEntry;
    }
}

*/
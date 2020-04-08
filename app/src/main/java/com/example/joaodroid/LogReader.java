package com.example.joaodroid;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReader {
    public static ArrayList<LogFile> logFiles = new ArrayList<>();
    public static ArrayList<LogEntry> logEntries = new ArrayList<>();
    public static ArrayList<Tag> tags = new ArrayList<>();
    public static HashMap<Integer, LogEntry> logEntriesById = new HashMap<>();
    public static HashMap<String, LogFile> logFilesByDate = new HashMap<>();
    public static HashMap<String, ArrayList<LogEntry> > logEntriesByTag = new HashMap<>();
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

    public static class Tag {
        String name;
        LocalDateTime datetime;
        LogEntry entry;

        public Tag(String name, LogEntry e) {
            this.name = name;
            this.datetime = e.datetime;
            this.entry = e;
        }
    }

    public static class LogEntry {
        public LogFile parent;
        public String id;
        public String title;
        public ArrayList<String> content = new ArrayList<>();
        public ArrayList<String> tags;
        public LocalDateTime datetime;
        public LocalDateTime modifiedAt;

        public double score = 0;

        public LogEntry(LogFile parent, String id, LocalDateTime datetime, String header,
                        ArrayList<String> content) {
            this.parent = parent;
            this.id = id;
            this.datetime = datetime;
            this.modifiedAt = datetime;
            this.tags = new ArrayList<>();

            parseHeader(header);
            parseContent(content);
        }

        private void parseHeader(String header) {
            header = header.replaceAll("^[ \t]+|[ \t]+$", "");
            Pattern pattern = Pattern.compile("^\\([^)]+\\)");
            Matcher m = pattern.matcher(header);
            if (m.find()) {
                String s = header.substring(m.group(0).length());
                this.title = s.replaceAll("^[ \t]+|[ \t]+$", "");

                // Extract tags.
                String str = m.group(0).toUpperCase();
                String[] tags = str.substring(1, str.length() - 1).split(",");
                for (String tag : tags) {
                    this.tags.add(tag);
                }
            } else {
                this.title = header;
            }
        }

        private void parseContent(ArrayList<String> content) {
            // Check modified time.
            if (content.size() > 0) {
                String line = content.get(0);
                if (line.startsWith("+modified-at ")) {
                    String date = line.substring(13);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    this.modifiedAt = LocalDateTime.parse(date, formatter);
                    content.remove(0);
                } else {
                    this.modifiedAt = this.datetime;
                }
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

            this.content = content;
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
            String data = this.id;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            data += " [" + this.datetime.format(formatter) + "] ";

            if (tags.size() > 0) {
                data += "(" + String.join(",", tags) + ") ";
            }

            data += this.title + "\n";

            if (!this.datetime.equals(this.modifiedAt)) {
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                data += "+modified-at " + formatter2.format(this.modifiedAt) + "\n";
            }

            data += "\n";

            for (String line : this.content) {
              data += line + "\n";
            }

            return data;
        }
    }

    public static class LogFile {
        ArrayList<LogEntry> logEntries;
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

                this.logEntries.add(new LogEntry(this, id, datetime, header, entryContent));
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
                for (LogEntry e : this.logEntries) {
                    data += e.toString();
                }

                FileWriter fw = new FileWriter(f);
                fw.write(data);
                fw.close();

            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
        }

        public LogEntry addLogEntry(Context context) {
            LogEntry logEntry = null;
            try {
                File f = new File(context.getFilesDir(), "files/id.txt");
                byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
                int id = Integer.parseInt(new String(encoded, StandardCharsets.UTF_8)) + 1;

                // Update id.
                FileWriter fw = new FileWriter(f);
                fw.write(Integer.toString(id));
                fw.close();

                logEntry = new LogEntry(this, String.format("%08d", id),
                        LocalDateTime.now(), "New Entry", new ArrayList<>());
            } catch (IOException e) {

            }
            this.logEntries.add(logEntry);
            return logEntry;
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

    private static void buildIdIndex() {
        logEntriesById.clear();
        for (LogEntry e : LogReader.logEntries) {
            logEntriesById.put(Integer.parseInt(e.id), e);
        }
    }

    private static void buildTagIndex() {
        logEntriesByTag.clear();
        for (LogEntry e : LogReader.logEntries) {
            for (String tag : e.tags) {
                tag = tag.toLowerCase();
                if (!logEntriesByTag.containsKey(tag)) {
                    logEntriesByTag.put(tag, new ArrayList<>());
                }
                logEntriesByTag.get(tag).add(e);
            }
        }

        tags.clear();
        for (String tag : LogReader.logEntriesByTag.keySet()) {
            tag = tag.toLowerCase();
            LogEntry e = logEntriesByTag.get(tag).get(0);
            tags.add(new Tag(tag, e));
        }

        Collections.sort(LogReader.tags, new Comparator<Tag>() {
            public int compare(Tag t1, Tag t2) {
                return t2.datetime.compareTo(t1.datetime);
            }
        });
    }

    public static void load(Context context) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "http://18.224.133.3/all";

        Log.e(">>>>>>>>>>>>>>", "just what");
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.e(">>>>>>>>>>>>>>", response);



                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(">>>>>>>>>>>>>>", error.getMessage());
                }
            }
        );
        queue.add(stringRequest);
    }

    public static void update(Context context) {
        /*LogReader.logFiles = new ArrayList<>();

        try {
            File directory = new File(context.getFilesDir(), "files");
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                String filename = files[i].getName();
                if (!filename.startsWith("log.")) continue;

                String date = filename.substring(4, 14);
                InputStream is = new FileInputStream(files[i]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                // Read file into memory.
                ArrayList<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                LogFile logFile = new LogFile(LocalDate.parse(date, formatter), lines);
                LogReader.logFiles.add(logFile);
                logFilesByDate.put(date, logFile);
            }

        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }

        LogReader.logEntries = new ArrayList<>();
        for (LogFile f : LogReader.logFiles) {
            LogReader.logEntries.addAll(f.logEntries);
        }

        Collections.sort(LogReader.logEntries, new Comparator<LogEntry>() {
            public int compare(LogEntry o1, LogEntry o2) {
                return o2.modifiedAt.compareTo(o1.modifiedAt);
            }
        });

        buildIdIndex();
        buildTagIndex();
        buildChronoIndex(context);*/
    }

    public static ArrayList<LogEntry> getLogs() {
        return LogReader.logEntries;
    }

    public static ArrayList<String> getTags() {
        ArrayList<String> list = new ArrayList<>();
        list.addAll(LogReader.logEntriesByTag.keySet());
        return list;
    }

    public static LogFile getCurrentLogFile(Context context) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String strDate = dateFormat.format(now);
        String filename = "log." + strDate + ".txt";
        File file = new File(context.getFilesDir(), "files/" + filename);

        try {
            if (!file.exists()) {
                FileWriter fr = new FileWriter(file, file.exists());
                DayOfWeek dayOfWeek = DayOfWeek.from(now);
                fr.write(strDate + " (" + dayOfWeek.name() + ")\n\n\n");
                LogFile logFile = new LogFile(now.toLocalDate(), new ArrayList<>());
                logFiles.add(logFile);
                logFilesByDate.put(strDate, logFile);
            }
        } catch (IOException e) {

        }

        return logFilesByDate.get(strDate);
    }

    public static int createLogEntry(Context context) {
        LogFile logFile = getCurrentLogFile(context);
        LogEntry logEntry = logFile.addLogEntry(context);
        logEntriesById.put(Integer.parseInt(logEntry.id), logEntry);
        logFile.rewrite(context);
        return Integer.parseInt(logEntry.id);
    }

    public static LogEntry getLogEntryById(int id) {
        if (!logEntriesById.containsKey(id)) {
            return null;
        }

        return logEntriesById.get(id);
    }

    public static void deleteLogEntry(Context context, LogEntry logEntry) {
        LogFile logFile = logEntry.parent;
        for (int i = 0; i < logFile.logEntries.size(); i++) {
            if (logFile.logEntries.get(i).id == logEntry.id) {
                logFile.logEntries.remove(i);
                break;
            }
        }
        logFile.rewrite(context);
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

    public static void reinsertLogEntry(Context context, LogEntry entry) {
        boolean found = false;
        LogFile logFile = entry.parent;
        for (int i = 0; i < logFile.logEntries.size(); ++i) {
            if (logFile.logEntries.get(i).datetime.isAfter(entry.datetime)) {
                logFile.logEntries.add(i, entry);
                found = true;
                break;
            }
        }
        if (!found){
            logFile.logEntries.add(entry);
        }

        logFile.rewrite(context);
    }
}

package com.example.joaodroid;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.storage.result.StorageDownloadFileResult;
import com.amplifyframework.storage.result.StorageListResult;
import com.amplifyframework.storage.result.StorageRemoveResult;
import com.amplifyframework.storage.result.StorageUploadFileResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class SyncActivity extends AppCompatActivity {
    private TextView text_view;
    private boolean dry_run = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        text_view = findViewById(R.id.log_output);

        File dir = new File(getFilesDir(), "files");
        dir.mkdir();
        dir = new File(getFilesDir(), "tmp");
        dir.mkdir();
    }

    private void log(String message) {
        text_view.append(message + "\n");
    }

    private void eraseLocalFile(String filename) {
        if (dry_run) {
            log("Would delete local file " + filename);
            return;
        }

        File directory = new File(getFilesDir() + "/files");
        new File(directory, filename).delete();
        log("Deleted local file " + filename);
    }

    private void eraseLocalDirectory() {
        if (dry_run) return;

        File directory = new File(getFilesDir() + "/files");
        String[] children = directory.list();
        for (int i = 0; i < children.length; i++) {
            new File(directory, children[i]).delete();
        }
    }

    private void downloadFile(String key, String write_filename, long timestamp) {
        if (dry_run) {
            log("Would download file " + key);
            return;
        }

        Amplify.Storage.downloadFile(
            key,
            getFilesDir() + "/" + write_filename,
            new ResultListener<StorageDownloadFileResult>() {
                @Override
                public void onResult(StorageDownloadFileResult result) {
                    String name = result.getFile().getName();
                    log("Downloaded " + name);

                    File f = new File(getFilesDir() + "/files/" + name);
                    f.setLastModified(timestamp * 1000);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("downloadFile", error.getMessage());
                }
            }
        );
    }

    private void uploadFile(String key, String local_path) {
        if (dry_run) {
            log("Would upload file " + key);
            return;
        }

        Amplify.Storage.uploadFile(
            key,
            local_path,
            new ResultListener<StorageUploadFileResult>() {
                @Override
                public void onResult(StorageUploadFileResult result) {
                    String name = result.getKey();
                    log("Uploaded " + name);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("uploadFile", error.getMessage());
                }
            }
        );
    }

    private void removeRemoteFile(String key) {
        if (dry_run) {
            log("Would delete remote file " + key);
            return;
        }

        Amplify.Storage.remove(
            key,
            new ResultListener<StorageRemoveResult>() {
                @Override
                public void onResult(StorageRemoveResult result) {
                    // String name = result.getKey();
                    // log("Removed " + name);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("deleteFile", error.getMessage());
                }
            }
        );
    }

    private JSONObject getMetadataContent(String filename) {
        File f = new File(filename);
        if (!f.exists()) {
            return null;
        }

        String content = "";
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
            content = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e("getMetadataContent", e.getMessage());
        }

        try {
            return new JSONObject(content);
        } catch (JSONException e) {
            Log.e("getMetadataContent", e.getMessage());
        }
        return null;
    }

    private JSONObject getLocalMetadataContent() {
        return getMetadataContent(getFilesDir() + "/files/metadata.json");
    }

    private void updateFiles(JSONObject local_metadata) {
        try {
            JSONArray local_files = local_metadata.getJSONArray("files");
            for (int i = 0; i < local_files.length(); i++) {
                JSONObject file = local_files.getJSONObject(i);
                String key = file.getString("name");
                long timestamp  = file.getLong("modified_at");

                String filename = key;
                if (!key.equals("metadata.json")) {
                    filename = filename + "." + timestamp;
                }
                uploadFile(filename, getFilesDir() + "/files/" + key);
            }
        } catch (JSONException e) {
            Log.e("syncRemoteBasedOnLocal", e.getMessage());
        }

        uploadFile("metadata.json", getFilesDir() + "/files/metadata.json");
    }

    private void eraseFileChain(JSONObject local_metadata, ArrayList<String> files, int i) {
        if (dry_run) return;
        if (i >= files.size()) {
            updateFiles(local_metadata);
            return;
        }

        String filename = files.get(i);
        Amplify.Storage.remove(
            filename,
            new ResultListener<StorageRemoveResult>() {
                @Override
                public void onResult(StorageRemoveResult storageRemoveResult) {
                    // log("Removed " + filename);
                    eraseFileChain(local_metadata, files, i+1);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("deleteFile", error.getMessage());
                }
            }
        );
    }

    private void eraseRemoteDirectory(JSONObject local_metadata) {
        if (dry_run) return;

        Amplify.Storage.list(
            "",
            new ResultListener<StorageListResult>() {
                @Override
                public void onResult(StorageListResult storageListResult) {
                    ArrayList<String> files = new ArrayList<>();
                    for (StorageListResult.Item item : storageListResult.getItems()) {
                        String key = item.getKey();
                        key = key.split("/")[1];
                        files.add(key);
                    }
                    eraseFileChain(local_metadata, files, 0);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("StorageQuickStart", error.getMessage());
                }
            }
        );
    }

    private void syncLocalBasedOnRemote(JSONObject local_metadata, JSONObject remote_metadata) {
        log("Syncing local based on remote");
        // eraseLocalDirectory();

        Hashtable local_metadata_files = new Hashtable();
        Hashtable remote_metadata_files = new Hashtable();

        try {
            JSONArray remote_files = remote_metadata.getJSONArray("files");
            for (int i = 0; i < remote_files.length(); i++) {
                JSONObject file = remote_files.getJSONObject(i);
                String filename = file.getString("name");
                long timestamp  = file.getLong("modified_at");
                remote_metadata_files.put(filename, timestamp);
            }

            JSONArray local_files = new JSONArray();
            if (local_metadata != null) {
                local_files = local_metadata.getJSONArray("files");
                for (int i = 0; i < local_files.length(); i++) {
                    JSONObject file = local_files.getJSONObject(i);
                    String filename = file.getString("name");
                    long timestamp = file.getLong("modified_at");
                    local_metadata_files.put(filename, timestamp);
                }
            }

            // Download new or modified files.
            for (int i = 0; i < remote_files.length(); i++) {
                JSONObject file = remote_files.getJSONObject(i);
                String filename = file.getString("name");
                long timestamp  = file.getLong("modified_at");

                String key = filename + "." + timestamp;
                if (!local_metadata_files.containsKey(filename)) {
                    downloadFile(key, "files/" + filename, timestamp);
                } else if ((long) local_metadata_files.get(filename) != timestamp) {
                    downloadFile(key, "files/" + filename, timestamp);
                }
            }

            // Delete existent deleted files.
            for (int i = 0; i < local_files.length(); i++) {
                JSONObject file = local_files.getJSONObject(i);
                String filename = file.getString("name");

                if (!remote_metadata_files.containsKey(filename)) {
                    eraseLocalFile(filename);
                }
            }
        } catch (JSONException e) {
            Log.e("syncRemoteBasedOnLocal", e.getMessage());
        }

        downloadFile("metadata.json", "files/metadata.json", 0);
        log("Finished syncing");
    }

    private void syncRemoteBasedOnLocal(JSONObject local_metadata, JSONObject remote_metadata) {
        log("Syncing remote based on local");
        // eraseRemoteDirectory(local_metadata);

        Hashtable local_metadata_files = new Hashtable();
        Hashtable remote_metadata_files = new Hashtable();

        try {
            JSONArray remote_files = remote_metadata.getJSONArray("files");
            for (int i = 0; i < remote_files.length(); i++) {
                JSONObject file = remote_files.getJSONObject(i);
                String filename = file.getString("name");
                long timestamp  = file.getLong("modified_at");
                remote_metadata_files.put(filename, timestamp);
            }

            JSONArray local_files = local_metadata.getJSONArray("files");
            for (int i = 0; i < local_files.length(); i++) {
                JSONObject file = local_files.getJSONObject(i);
                String filename = file.getString("name");
                long timestamp  = file.getLong("modified_at");
                local_metadata_files.put(filename, timestamp);
            }

            // Upload new or modified files.
            for (int i = 0; i < local_files.length(); i++) {
                JSONObject file = local_files.getJSONObject(i);
                String filename = file.getString("name");
                long timestamp  = file.getLong("modified_at");

                String key = filename + "." + timestamp;
                if (!remote_metadata_files.containsKey(filename)) {
                    uploadFile(key, getFilesDir() + "/files/" + filename);
                } else {
                    long remote_timestamp = (long) remote_metadata_files.get(filename);
                    if (remote_timestamp != timestamp) {
                        String old_key = filename + "." + remote_timestamp;
                        removeRemoteFile(old_key);
                        uploadFile(key, getFilesDir() + "/files/" + filename);
                    }
                }
            }

            // Delete existent deleted files.
            for (int i = 0; i < remote_files.length(); i++) {
                JSONObject file = remote_files.getJSONObject(i);
                String filename = file.getString("name");
                long timestamp  = file.getLong("modified_at");

                if (!local_metadata_files.containsKey(filename)) {
                    String key = filename + "." + timestamp;
                    removeRemoteFile(key);
                }
            }
        } catch (JSONException e) {
            Log.e("syncRemoteBasedOnLocal", e.getMessage());
        }

        uploadFile("metadata.json", getFilesDir() + "/files/metadata.json");
        log("Finished syncing");
    }

    private void updateLocalMetadata(JSONObject local_metadata, JSONObject remote_metadata) {
        Hashtable local_metadata_files = new Hashtable();
        Hashtable remote_metadata_files = new Hashtable();

        try {
            HashSet<String> metadata_files = new HashSet<>();

            boolean update_metadata = false;
            int id = local_metadata.getInt("id");
            JSONArray local_files = local_metadata.getJSONArray("files");
            for (int i = 0; i < local_files.length(); i++) {
                JSONObject file = local_files.getJSONObject(i);
                String filename = file.getString("name");
                metadata_files.add(filename);
                long expected_timestamp = file.getLong("modified_at");

                File f = new File(getFilesDir() + "/files/" + filename);
                long local_timestamp = f.lastModified() / 1000;
                if (local_timestamp != expected_timestamp) {
                    file.put("timestamp", local_timestamp);
                    log("File " + filename + " has been modified from " + expected_timestamp +
                        " to " + local_timestamp);
                    update_metadata = true;
                }
            }

            JSONArray new_metadata_files = new JSONArray();
            File directory = new File(getFilesDir() + "/files");
            String[] children = directory.list();
            for (int i = 0; i < children.length; i++) {
                String filename = children[i];
                if (filename.equals("metadata.json"))
                    continue;

                if (!metadata_files.contains(filename)) {
                    log("File " + filename + " has been created");
                    update_metadata = true;
                }

                JSONObject obj = new JSONObject();
                obj.put("name", filename);
                obj.put("modified_at", new File(directory, filename).lastModified() / 1000);
                // local_files.put(obj);

                new_metadata_files.put(obj);
            }

            if (update_metadata) {
                log("There are untracked changes on local folder");
                local_metadata.put("id", id+1);
                local_metadata.put("files", new_metadata_files);

                if (!dry_run) {
                    File metadata = new File(getFilesDir() + "/files/metadata.json");
                    FileWriter fr = new FileWriter(metadata);
                    fr.write(local_metadata.toString());
                    fr.close();
                }

                syncRemoteBasedOnLocal(local_metadata, remote_metadata);
            } else {
                log("Local metadata.json is up to date");
            }
        } catch (JSONException e) {
            Log.e("updateLocalMetadata", e.getMessage());
        } catch (IOException e) {
            Log.e("updateLocalMetadata", e.getMessage());
        }
    }

    private void compareLocalAndRemoteMetadata(JSONObject local_metadata,
                                               JSONObject remote_metadata) {
        // Local and remote folders are empty.
        if (local_metadata == null && remote_metadata == null) {
            log("Empty remote and local directories");
            return;
        }

        if (local_metadata == null) {
            log("Missing local metadata.json. Removing untracked local files");
            syncLocalBasedOnRemote(local_metadata, remote_metadata);
            return;
        }

        if (remote_metadata == null) {
            log("Missing remote metadata.json");
            syncRemoteBasedOnLocal(local_metadata, remote_metadata);
            return;
        }

        try {
            int local_id = local_metadata.getInt("id");
            int remote_id = remote_metadata.getInt("id");

            log("Local metadata id is " + local_id + " and remote id is " + remote_id);
            if (local_id > remote_id) {
                log("Local metadata is more recent than remote");
                syncRemoteBasedOnLocal(local_metadata, remote_metadata);
                return;
            }

            if (local_id < remote_id) {
                log("Remote metadata is more recent than local");
                syncLocalBasedOnRemote(local_metadata, remote_metadata);
                return;
            }

            updateLocalMetadata(local_metadata, remote_metadata);

        } catch (JSONException e) {
            Log.e("syncFiles", e.getMessage());
        }
    }

    private void getLocalAndRemoteMetadata() {
        JSONObject local_metadata = getLocalMetadataContent();

        Amplify.Storage.downloadFile(
            "metadata.json",
            getFilesDir() + "/tmp/remote_metadata.json",
            new ResultListener<StorageDownloadFileResult>() {
                @Override
                public void onResult(StorageDownloadFileResult result) {
                    JSONObject remote_metadata =
                            getMetadataContent(getFilesDir() + "/tmp/remote_metadata.json");
                    File f = new File(getFilesDir() + "/tmp/remote_metadata.json");
                    f.delete();

                    compareLocalAndRemoteMetadata(local_metadata, remote_metadata);
                }

                @Override
                public void onError(Throwable error) {
                    Log.e("StorageQuickStart", error.getMessage());
                }
            }
        );
    }

    private void syncFiles() {
        log("Local folder: " + getFilesDir() + "/files");

        if (this.dry_run) {
            log("Diffing local and remote");
        } else {
            log("Syncing local and remote");
        }

        getLocalAndRemoteMetadata();
    }

    public void rebase(View view) {
        this.eraseLocalDirectory();
        syncFiles();
    }

    public void diff(View view) {
        this.dry_run = true;
        syncFiles();
    }

    public void sync(View view) {
        this.dry_run = false;
        syncFiles();
    }
}

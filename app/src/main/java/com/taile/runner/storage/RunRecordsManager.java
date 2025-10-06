package com.taile.runner.storage;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.taile.runner.models.RunRecord;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RunRecordsManager {
    private static final String PREFS_NAME = "run_records";
    private static final String RECORDS_KEY = "records";
    private static final String LAST_ID_KEY = "last_id";

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private List<RunRecord> records;
    private long lastId = 0;

    public RunRecordsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadRecords();
    }

    private void loadRecords() {
        String recordsJson = sharedPreferences.getString(RECORDS_KEY, null);
        lastId = sharedPreferences.getLong(LAST_ID_KEY, 0);

        if (recordsJson != null) {
            Type type = new TypeToken<ArrayList<RunRecord>>() {}.getType();
            records = gson.fromJson(recordsJson, type);
        } else {
            records = new ArrayList<>();
        }
    }

    private void saveRecords() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String recordsJson = gson.toJson(records);
        editor.putString(RECORDS_KEY, recordsJson);
        editor.putLong(LAST_ID_KEY, lastId);
        editor.apply();
    }

    public List<RunRecord> getAllRecords() {
        return new ArrayList<>(records);
    }

    public List<RunRecord> getRecordsSortedByDate() {
        List<RunRecord> sortedRecords = new ArrayList<>(records);
        Collections.sort(sortedRecords, new Comparator<RunRecord>() {
            @Override
            public int compare(RunRecord r1, RunRecord r2) {
                // Sort descending (most recent first)
                return Long.compare(r2.getStartTime(), r1.getStartTime());
            }
        });
        return sortedRecords;
    }

    public void addRecord(RunRecord record) {
        // Set a unique ID
        record.setId(++lastId);
        records.add(record);
        saveRecords();
    }

    public void deleteRecord(long id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == id) {
                records.remove(i);
                saveRecords();
                return;
            }
        }
    }

    public void deleteAllRecords() {
        records.clear();
        lastId = 0;
        saveRecords();
    }
}

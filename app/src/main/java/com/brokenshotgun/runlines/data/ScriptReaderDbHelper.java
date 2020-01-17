/*
 * Copyright 2016 Jason Petterson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brokenshotgun.runlines.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.brokenshotgun.runlines.model.Script;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.brokenshotgun.runlines.data.ScriptReaderContract.ScriptEntry;

public class ScriptReaderDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ScriptReader.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_SCRIPT_TABLE =
            "CREATE TABLE " + ScriptEntry.TABLE_NAME + " (" +
                    ScriptEntry._ID + " INTEGER PRIMARY KEY," +
                    ScriptEntry.COLUMN_NAME_SCRIPT_JSON + TEXT_TYPE + COMMA_SEP +
                    ScriptEntry.COLUMN_NAME_CREATE_DATE + TEXT_TYPE +
                    " )";

    private static final String SQL_DELETE_SCRIPT_TABLE =
            "DROP TABLE IF EXISTS " + ScriptEntry.TABLE_NAME;

    private Gson gson;

    public ScriptReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        gson = new GsonBuilder().create();
    }

    public long insertScript(Script script) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues sValues = new ContentValues();
        sValues.put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(script));
        sValues.put(ScriptEntry.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis());

        long newScriptId;
        newScriptId = db.insert(
                ScriptEntry.TABLE_NAME,
                null,
                sValues);

        script.id = newScriptId;

        return newScriptId;
    }

    public void updateScript(Script script) {
        SQLiteDatabase db = getReadableDatabase();

        ContentValues values = new ContentValues();
        values.put(ScriptEntry.COLUMN_NAME_SCRIPT_JSON, serialize(script));

        String selection = ScriptEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(script.id)};

        db.update(
                ScriptEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    public void deleteScript(Script script) {
        SQLiteDatabase db = getWritableDatabase();

        String selection = ScriptEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(script.id)};

        db.delete(
                ScriptEntry.TABLE_NAME,
                selection,
                selectionArgs);
    }

    public List<Script> getScripts() {
        List<Script> results = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                ScriptEntry._ID,
                ScriptEntry.COLUMN_NAME_SCRIPT_JSON,
        };

        String sortOrder =
                ScriptEntry.COLUMN_NAME_CREATE_DATE + " DESC";

        try (Cursor c = db.query(
                ScriptEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        )) {
            while (c.moveToNext()) {
                long scriptId = c.getLong(0);
                String scriptJson = c.getString(1);
                Script script = deserialize(scriptJson);
                script.id = scriptId;
                results.add(script);
            }
        }

        return results;
    }

    private String serialize(Script script) {
        return gson.toJson(script);
    }

    private Script deserialize(String json) {
        try {
            Log.e(ScriptReaderDbHelper.class.getName(), "Deserialize attempt #1 failed");
            return gson.fromJson(json, Script.class);
        } catch (Exception unknownEx) {
            Log.e(ScriptReaderDbHelper.class.getName(), "Deserialize attempt #2 failed");
            return new Script("Error");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SCRIPT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: handle this when the time comes to insure db version bump upgrades gracefully
        Log.d(ScriptReaderDbHelper.class.getName(), ">>> onUpgrade!");
        db.execSQL(SQL_DELETE_SCRIPT_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(ScriptReaderDbHelper.class.getName(), ">>> onDowngrade!");
        System.exit(-1);
    }
}

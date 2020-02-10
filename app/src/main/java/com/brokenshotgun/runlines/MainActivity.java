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

package com.brokenshotgun.runlines;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.brokenshotgun.runlines.adapters.ScriptArrayAdapter;
import com.brokenshotgun.runlines.data.FountainSerializer;
import com.brokenshotgun.runlines.data.PdfParser;
import com.brokenshotgun.runlines.data.ScriptReaderDbHelper;
import com.brokenshotgun.runlines.model.Script;
import com.brokenshotgun.runlines.utils.Intents;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.kobakei.ratethisapp.RateThisApp;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ScriptArrayAdapter scriptListAdapter;
    private ScriptReaderDbHelper dbHelper;
    private ListView scriptListView;
    private Animation fabOpen, fabClose, fabRotateClockwise, fabRotateCounterClockwise;

    protected interface ImportCallback {
        void onSuccess(Script script);

        void onFailure();
    }
    private ProgressDialog progressDialog;
    private ImportCallback importScriptHandler = new ImportCallback() {
        @Override
        public void onSuccess(final Script script) {
            dbHelper.insertScript(script);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                    progressDialog = null;
                    scriptListAdapter.add(script);
                    Snackbar.make(scriptListView, R.string.alert_script_import_success, Snackbar.LENGTH_LONG).show();
                }
            });

        }

        @Override
        public void onFailure() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                    progressDialog = null;
                    Snackbar.make(scriptListView, R.string.alert_script_import_error, Snackbar.LENGTH_LONG).show();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        scriptListView = findViewById(R.id.script_list);
        final FloatingActionButton addScriptMenuButton = findViewById(R.id.add_script_menu);
        final FloatingActionButton addScriptButton = findViewById(R.id.add_script);
        final FloatingActionButton importScriptButton = findViewById(R.id.import_script);

        fabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        fabRotateClockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_clockwise);
        fabRotateCounterClockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_rotate_counter_clockwise);

        addScriptMenuButton.setOnClickListener(new View.OnClickListener() {
            private boolean isOpen = false;

            @Override
            public void onClick(View view) {
                if (isOpen) {
                    addScriptButton.startAnimation(fabClose);
                    importScriptButton.startAnimation(fabClose);
                    addScriptMenuButton.startAnimation(fabRotateCounterClockwise);
                    addScriptButton.setClickable(false);
                    importScriptButton.setClickable(false);
                    isOpen = false;
                } else {
                    addScriptButton.startAnimation(fabOpen);
                    importScriptButton.startAnimation(fabOpen);
                    addScriptMenuButton.startAnimation(fabRotateClockwise);
                    addScriptButton.setClickable(true);
                    importScriptButton.setClickable(true);
                    isOpen = true;
                }
            }
        });

        importScriptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImportFileSelect();
            }
        });

        dbHelper = new ScriptReaderDbHelper(this);

        scriptListAdapter = new ScriptArrayAdapter(this, new ArrayList<Script>());
        scriptListView.setAdapter(scriptListAdapter);
        scriptListView.setEmptyView(findViewById(android.R.id.empty));
        scriptListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openScript(scriptListAdapter.getItem(position));
            }
        });

        scriptListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showEditScriptDialog(scriptListAdapter.getItem(position), position);
                return true;
            }
        });

        addScriptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddScriptButtonClicked(v);
            }
        });

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                showImportProgressDialog();
                importScriptFromText(intent.getData(), importScriptHandler);
                intent.setData(null);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        PDFBoxResourceLoader.init(getApplicationContext());
        RateThisApp.onStart(this);
        RateThisApp.showRateDialogIfNeeded(this);
    }

    @Override
    protected void onResume() {
        scriptListAdapter.clear();
        scriptListAdapter.addAll(dbHelper.getScripts());
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.import_script) {
            showImportFileSelect();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openScript(Script script) {
        Intent openIntent = new Intent(this, ScriptSceneListActivity.class);
        openIntent.putExtra("script", script);
        startActivity(openIntent);
    }

    private static final int OPTION_EDIT_NAME = 0;
    private static final int OPTION_REMOVE = 1;

    private void showEditScriptDialog(final Script script, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_script);
        builder.setItems(R.array.edit_script_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case OPTION_EDIT_NAME:
                        showEditScriptNameDialog(script, position);
                        break;
                    case OPTION_REMOVE:
                        showConfirmDeleteDialog(script);
                        break;
                }
            }
        });
        builder.create().show();
    }

    private void showEditScriptNameDialog(final Script script, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_script_name);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 50, 50, 50);

        final EditText inputText = new EditText(this);
        inputText.setHint(R.string.hint_edit_script_name);
        inputText.setText(script.getName());
        inputLayout.addView(inputText, params);

        builder.setView(inputLayout);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Script selectedScript = scriptListAdapter.getItem(position);
                if (selectedScript != null) {
                    selectedScript.setName(inputText.getText().toString().trim());
                    scriptListAdapter.notifyDataSetInvalidated();
                    dbHelper.updateScript(script);
                }
            }
        });
        builder.create().show();
    }

    private void showConfirmDeleteDialog(final Script script) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.title_dialog_delete_script) + " \'" + script.getName() + "\'?");
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scriptListAdapter.remove(script);
                scriptListAdapter.notifyDataSetInvalidated();
                dbHelper.deleteScript(script);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void showAddScriptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_add_script);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 50, 50, 50);

        final EditText inputText = new EditText(this);
        inputText.setHint(R.string.hint_add_script);
        inputLayout.addView(inputText, params);

        builder.setView(inputLayout);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Script newScript = new Script(inputText.getText().toString().trim());
                scriptListAdapter.add(newScript);
                dbHelper.insertScript(newScript);
            }
        });
        builder.create().show();
    }

    public void onAddScriptButtonClicked(View view) {
        showAddScriptDialog();
    }

    public void onImportScriptButtonClicked(View view) {
        showImportFileSelect();
    }

    private static final int IMPORT_FILE_SELECT_REQUEST = 0;

    public void showImportFileSelect() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath() + "\\Run Lines\\");
        intent.setDataAndType(uri, "*/*");
        Intents.maybeStartActivityForResult(this, intent, IMPORT_FILE_SELECT_REQUEST);
    }

    protected void importScriptFromText(final Uri filename, final ImportCallback importCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String content = fileToString(filename);
                if (content == null) {
                    if (importCallback != null) {
                        importCallback.onFailure();
                    }
                    return;
                }
                Script result = FountainSerializer.deserialize(content);
                if (importCallback != null) {
                    importCallback.onSuccess(result);
                }
            }
        }).start();
    }

    protected void importScriptFromPdf(final Uri filename, final ImportCallback importCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PDDocument document = null;
                try {
                    InputStream fileStream = getContentResolver().openInputStream(filename);
                    if (fileStream == null) {
                        Log.e(MainActivity.class.getName(), "Could not open file, InputStream is null");
                        if (importCallback != null)
                            importCallback.onFailure();
                        return;
                    }
                    document = PDDocument.load(fileStream, MemoryUsageSetting.setupTempFileOnly());
                    PDFTextStripper textStripper = new PDFTextStripper();
                    textStripper.setAddMoreFormatting(true);
                    textStripper.setLineSeparator("\n");
                    textStripper.setPageEnd("");
                    String content = textStripper.getText(document);
                    Script result = PdfParser.parse(content);
                    if (importCallback != null)
                        importCallback.onSuccess(result);
                } catch (IOException e) {
                    Log.e(MainActivity.class.getName(), e.getMessage(), e);
                    if (importCallback != null)
                        importCallback.onFailure();
                } finally {
                    if (document != null) {
                        try {
                            document.close();
                        } catch (IOException e) {
                            Log.e(MainActivity.class.getName(), e.getMessage(), e);
                        }
                    }
                }
            }
        }).start();
    }

    protected String fileToString(Uri filename) {
        BufferedReader fileReader = null;
        try {
            InputStream fileStream = getContentResolver().openInputStream(filename);
            assert fileStream != null;
            fileReader = new BufferedReader(new InputStreamReader(fileStream));
            StringBuilder fileBuilder = new StringBuilder();
            int c;
            int length = 80;
            char[] buf = new char[80];
            while ((c = fileReader.read(buf, 0, length)) != -1) {
                fileBuilder.append(buf, 0, c);
            }
            return fileBuilder.toString().replaceAll("\\r\\n?", "\n");
        } catch (IOException e) {
            Log.e(MainActivity.class.getName(), e.getMessage(), e);
        } finally {
            if (fileReader != null)
                try {
                    fileReader.close();
                } catch (IOException e) {
                    Log.e(MainActivity.class.getName(), e.getMessage(), e);
                }
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_FILE_SELECT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri fileUri = data.getData();

                if (fileUri == null) {
                    Log.e(MainActivity.class.getName(), "File URI is null");
                    Snackbar.make(scriptListView, R.string.alert_error_file_open, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                String filename;
                String extension = "";
                try (Cursor fileCursor = getContentResolver().query(fileUri, null, null, null, null)) {
                    if (fileCursor == null) {
                        filename = fileUri.getLastPathSegment();

                        if (filename == null) {
                            Snackbar.make(scriptListView, R.string.alert_invalid_filename, Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        int lastPeriodIndex = filename.lastIndexOf(".");

                        if (lastPeriodIndex != -1)
                            extension = filename.substring(lastPeriodIndex).toLowerCase();
                    } else {
                        fileCursor.moveToFirst();
                        int nameIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        filename = fileCursor.getString(nameIndex);

                        if (filename == null) {
                            Snackbar.make(scriptListView, R.string.alert_invalid_filename, Snackbar.LENGTH_SHORT).show();
                            return;
                        }

                        int lastPeriodIndex = filename.lastIndexOf(".");

                        if (lastPeriodIndex != -1)
                            extension = filename.substring(lastPeriodIndex).toLowerCase();
                    }
                }

                String mime = getContentResolver().getType(fileUri);
                Log.d(MainActivity.class.getName(),
                        String.format(Locale.US,
                                "Import [URI=%s | Ext=%s | MIME=%s]",
                                fileUri.toString(), extension, mime));

                switch (extension) {
                    case ".txt":
                    case ".fountain":
                        showImportProgressDialog();
                        importScriptFromText(fileUri, importScriptHandler);
                        break;
                    case ".pdf":
                        showImportProgressDialog();
                        importScriptFromPdf(fileUri, importScriptHandler);
                        break;
                    default:
                        final Snackbar alert = Snackbar.make(scriptListView,
                                String.format(Locale.US, getString(R.string.alert_script_import_error_unknown_ext), extension),
                                Snackbar.LENGTH_INDEFINITE);

                        alert.setAction(getString(R.string.dismiss), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                alert.dismiss();
                            }
                        });

                        alert.show();
                        break;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showImportProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getString(R.string.title_dialog_importing));
        progressDialog.setMessage(getString(R.string.message_dialog_importing));
        progressDialog.show();
    }
}

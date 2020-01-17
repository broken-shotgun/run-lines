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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.brokenshotgun.runlines.adapters.SceneArrayAdapter;
import com.brokenshotgun.runlines.data.ScriptReaderDbHelper;
import com.brokenshotgun.runlines.model.Scene;
import com.brokenshotgun.runlines.model.Script;
import com.brokenshotgun.runlines.utils.DialogUtil;
import com.brokenshotgun.runlines.utils.FileUtil;
import com.brokenshotgun.runlines.utils.ScriptUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.Objects;

public class ScriptSceneListActivity extends AppCompatActivity {
    private Script script;
    private ListView sceneListView;
    private SceneArrayAdapter sceneArrayAdapter;
    private ScriptReaderDbHelper dbHelper;
    private DialogUtil dialogUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_scene_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        script = extras.getParcelable("script");
        dbHelper = new ScriptReaderDbHelper(this);
        dialogUtil = new DialogUtil();

        setTitle(getString(R.string.script_scene_list_title_prefix) + " \"" + (script.getName().equals("") ? getString(R.string.label_no_script_name) : script.getName()) + "\"");

        sceneListView = findViewById(R.id.scenes_list);
        sceneArrayAdapter = new SceneArrayAdapter(this, script.getScenes());
        sceneListView.setAdapter(sceneArrayAdapter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sceneListView.setNestedScrollingEnabled(true);
        }
        sceneListView.setEmptyView(findViewById(android.R.id.empty));

        sceneListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openScene(position);
            }
        });

        sceneListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showEditSceneOptionsDialog(position);
                return true;
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddSceneDialog();
            }
        });

        FloatingActionButton exportScript = findViewById(R.id.export_script);
        assert exportScript != null;
        exportScript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportScript(script);
            }
        });
    }

    @Override
    protected void onPause() {
        dialogUtil.dismiss();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_script_scene_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.export_script) {
            exportScript(script);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private Script tmpExportScript;

    private void exportScript(final Script script) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            tmpExportScript = script;
            requestWriteExternalStoragePermission();
            return;
        }

        File exportFile = new File(FileUtil.getDocStorageDir(), script.getName() + ".fountain");
        ScriptUtil.exportScript(script, exportFile, new ScriptUtil.ExportScriptHandler() {
            @Override
            public void onSuccess(File exportFile) {
                Snackbar.make(sceneListView, getString(R.string.alert_script_export_success) + " " + exportFile.getPath(), Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onError() {
                Snackbar.make(sceneListView, R.string.alert_script_export_error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void requestWriteExternalStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private static final int OPEN_SCRIPT_REQUEST = 0;

    private void openScene(int sceneIndex) {
        Intent readIntent = new Intent(this, ReadSceneActivity.class);
        readIntent.putExtra("script", script);
        readIntent.putExtra("sceneIndex", sceneIndex);
        startActivityForResult(readIntent, OPEN_SCRIPT_REQUEST);
    }

    private static final int OPTION_EDIT_SCENE_NAME = 0;

    private void showEditSceneOptionsDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_options)
                .setItems(R.array.edit_scene_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == OPTION_EDIT_SCENE_NAME) {
                            showEditSceneNameDialog(position);
                        }
                    }
                });

        dialogUtil.showDialog(builder.create());
    }

    private void showEditSceneNameDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_scene_name);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 50, 50, 50);

        final EditText inputText = new EditText(this);
        inputText.setHint(R.string.hint_edit_line);
        Scene scene = sceneArrayAdapter.getItem(position);
        String sceneName = scene != null ? scene.getName() : "";
        inputText.setText(sceneName);
        inputLayout.addView(inputText, params);

        builder.setView(inputLayout);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Scene scene = sceneArrayAdapter.getItem(position);
                if (scene != null) {
                    scene.setName(inputText.getText().toString().trim());
                    sceneArrayAdapter.notifyDataSetInvalidated();
                    dbHelper.updateScript(script);
                }

            }
        });

        dialogUtil.showDialog(builder.create());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_SCRIPT_REQUEST) {
            if (resultCode == RESULT_OK) {
                script.copy((Script) Objects.requireNonNull(data.getParcelableExtra("script")));
                sceneArrayAdapter.notifyDataSetInvalidated();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(sceneListView, R.string.alert_write_permission_granted, Snackbar.LENGTH_LONG).show();
                if (tmpExportScript != null) {
                    exportScript(tmpExportScript);
                }
            } else {
                Snackbar.make(sceneListView, R.string.alert_write_permission_denied, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    public void onAddSceneButtonClicked(View view) {
        showAddSceneDialog();
    }

    private void showAddSceneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_add_scene);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 50, 50, 50);

        final EditText inputText = new EditText(this);
        inputText.setHint(R.string.hint_add_scene);
        inputLayout.addView(inputText, params);

        builder.setView(inputLayout);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Scene newScene = new Scene(inputText.getText().toString().trim());
                script.addScene(newScene);
                sceneArrayAdapter.notifyDataSetChanged();
                dbHelper.updateScript(script);
            }
        });

        dialogUtil.showDialog(builder.create());
    }
}

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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.brokenshotgun.runlines.adapters.LineArrayAdapter;
import com.brokenshotgun.runlines.data.ScriptReaderDbHelper;
import com.brokenshotgun.runlines.model.Actor;
import com.brokenshotgun.runlines.model.Line;
import com.brokenshotgun.runlines.model.Script;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class EditSceneActivity extends AppCompatActivity {

    private Script script;
    private LineArrayAdapter lineArrayAdapter;
    private ScriptReaderDbHelper dbHelper;
    private boolean hasUnsavedChanges = false;

    private CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_scene);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        extras.setClassLoader(Script.class.getClassLoader());
        Script originalScript = extras.getParcelable("script");
        if (originalScript == null) {
            finish();
            return;
        }
        script = new Script(originalScript);
        int sceneIndex = (int) extras.get("sceneIndex");

        setTitle(getString(R.string.title_activity_edit_script_prefix) + " \"" + (script.getScene(sceneIndex).getName().equals("") ? getString(R.string.label_no_scene_name) : script.getScene(sceneIndex).getName()) + "\"");

        dbHelper = new ScriptReaderDbHelper(this);

        coordinatorLayout = findViewById(R.id.coordinator_layout);
        assert coordinatorLayout != null;

        ListView linesListView = findViewById(R.id.lines_list);
        assert linesListView != null;
        lineArrayAdapter = new LineArrayAdapter(this, script.getScene(sceneIndex).getLines());
        linesListView.setAdapter(lineArrayAdapter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            linesListView.setNestedScrollingEnabled(true);
        }

        linesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showEditOptionDialog(position);
                return true;
            }
        });

        FloatingActionButton addActorButton = findViewById(R.id.add_actor);
        assert addActorButton != null;
        addActorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddActorDialog();
            }
        });

        FloatingActionButton saveButton = findViewById(R.id.save);
        assert saveButton != null;
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbHelper.updateScript(script);

                hasUnsavedChanges = false;

                Snackbar.make(view, getString(R.string.saved_changes), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        FloatingActionButton addLineButton = findViewById(R.id.add_line);
        assert addLineButton != null;
        addLineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPickActorDialog(view, new OnActorSelectListener() {
                    @Override
                    public void onSelected(Actor actor) {
                        showAddLineDialog(actor);
                    }
                });
            }
        });
    }

    private void showAddActorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_add_actor);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 50, 50, 50);

        final EditText inputText = new EditText(this);
        inputText.setHint(R.string.hint_actor_name);
        inputLayout.addView(inputText, params);

        builder.setView(inputLayout);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                script.addActor(new Actor(inputText.getText().toString()));

                hasUnsavedChanges = true;
            }
        });

        builder.create().show();
    }

    private void showPickActorDialog(final View view, final OnActorSelectListener onActorSelectListener) {
        if (script.getActors().isEmpty()) {
            Snackbar.make(view, R.string.alert_no_actors, Snackbar.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_pick_actor);

        final ArrayAdapter<Actor> actorArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, script.getActors());
        builder.setAdapter(actorArrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (onActorSelectListener != null) {
                    onActorSelectListener.onSelected(actorArrayAdapter.getItem(which));
                }
            }
        });

        builder.create().show();
    }

    private void showAddLineDialog(final Actor actor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.title_dialog_add_line) + " " + actor.getName());

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 50, 50, 50);

        final EditText inputText = new EditText(this);
        inputText.setHint(R.string.hint_add_line);
        inputLayout.addView(inputText, params);

        builder.setView(inputLayout);
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Line newLine = new Line(actor, inputText.getText().toString().trim());
                lineArrayAdapter.add(newLine);
                newLine.order = lineArrayAdapter.getCount() - 1;

                hasUnsavedChanges = true;
            }
        });

        builder.create().show();
    }

    private static final int OPTION_CHANGE_ACTOR = 0;
    private static final int OPTION_CHANGE_VOICE = 1;
    private static final int OPTION_EDIT_LINE = 2;
    private static final int OPTION_REMOVE_LINE = 3;
    private static final int OPTION_INSERT_ABOVE = 4;
    private static final int OPTION_INSERT_BELOW = 5;
    private static final int OPTION_REMOVE_ACTOR = 6;
    private void showEditOptionDialog(final int linePosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_options)
                .setItems(R.array.edit_line_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case OPTION_CHANGE_ACTOR:
                                showChangeActorDialog(linePosition);
                                break;
                            case OPTION_CHANGE_VOICE:
                                showChangeVoiceDialog(linePosition);
                                break;
                            case OPTION_EDIT_LINE:
                                showEditLineDialog(linePosition);
                                break;
                            case OPTION_REMOVE_LINE:
                                lineArrayAdapter.remove(lineArrayAdapter.getItem(linePosition));
                                updateOrder();
                                break;
                            case OPTION_INSERT_ABOVE:
                                int aboveIndex = linePosition - 1;
                                if (aboveIndex < 0) {
                                    aboveIndex = 0;
                                }
                                lineArrayAdapter.insert(new Line(script.getActors().get(0), ""), aboveIndex);
                                updateOrder();
                                break;
                            case OPTION_INSERT_BELOW:
                                int belowIndex = linePosition + 1;
                                if (belowIndex >= lineArrayAdapter.getCount()){
                                    belowIndex = lineArrayAdapter.getCount() - 1;
                                }
                                lineArrayAdapter.insert(new Line(script.getActors().get(0), ""), belowIndex);
                                updateOrder();
                                break;
                            case OPTION_REMOVE_ACTOR:
                                Line selectedLine = lineArrayAdapter.getItem(linePosition);
                                if (selectedLine != null) {
                                    removeActor(selectedLine.getActor());
                                }
                                break;
                        }
                    }
                });

        builder.create().show();
    }

    private void removeActor(Actor actor) {
        // TODO prompt to delete all lines attached to actor

        if (script.getActors().size() == 1) {
            return;
        }

        if (actor.equals(Actor.ACTION)) {
            Snackbar.make(coordinatorLayout, R.string.alert_attempt_remove_actor_warning, Snackbar.LENGTH_SHORT).show();
            return;
        }

        hasUnsavedChanges = true;

        script.removeActor(actor);
        Actor replacement = script.getActors().get(0);

        for (int i = 0; i < lineArrayAdapter.getCount(); ++i) {
            Line line = lineArrayAdapter.getItem(i);
            if (line != null && line.getActor().equals(actor)) {
                line.setActor(replacement);
            }
        }

        lineArrayAdapter.notifyDataSetInvalidated();
    }

    private void updateOrder() {
        for (int i = 0; i < lineArrayAdapter.getCount(); ++i) {
            Line line = lineArrayAdapter.getItem(i);
            if (line != null)
                line.order = i;
        }
        lineArrayAdapter.notifyDataSetInvalidated();
    }

    private void showChangeActorDialog(final int linePosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_change_actor);

        final ArrayAdapter<Actor> actorArrayAdapter = new ArrayAdapter<Actor>(this, android.R.layout.simple_list_item_1, script.getActors());
        builder.setAdapter(actorArrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Line line = lineArrayAdapter.getItem(linePosition);
                if (line != null) {
                    line.setActor(actorArrayAdapter.getItem(which));
                    lineArrayAdapter.notifyDataSetInvalidated();
                }
            }
        });

        builder.create().show();
    }

    private void showChangeVoiceDialog(final int linePosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_change_actor);

        final ArrayAdapter<String> voicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, script.getAllVoices());
        builder.setAdapter(voicesArrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                script.assignVoice(
                        lineArrayAdapter.getItem(linePosition).getActor().getName(),
                        voicesArrayAdapter.getItem(which));
                lineArrayAdapter.notifyDataSetInvalidated();
            }
        });

        builder.create().show();
    }

    private void showEditLineDialog(final int linePosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_line);

        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 50, 50, 50);

        final EditText inputText = new EditText(this);
        inputText.setHint(R.string.hint_edit_line);
        inputText.setText(lineArrayAdapter.getItem(linePosition).getLine());
        inputLayout.addView(inputText, params);

        builder.setView(inputLayout);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                lineArrayAdapter.getItem(linePosition).setLine(inputText.getText().toString().trim());
                lineArrayAdapter.notifyDataSetInvalidated();
            }
        });

        builder.create().show();
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.discard_unsaved_changes);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    setResult(RESULT_CANCELED);
                    EditSceneActivity.super.onBackPressed();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

            builder.create().show();
        } else {
            Intent result = new Intent();
            result.putExtra("script", script);
            setResult(RESULT_OK, result);
            super.onBackPressed();
        }
    }

    private interface OnActorSelectListener {
        void onSelected(Actor actor);
    }
}

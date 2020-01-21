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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.brokenshotgun.runlines.adapters.LineArrayAdapter;
import com.brokenshotgun.runlines.model.Actor;
import com.brokenshotgun.runlines.model.Line;
import com.brokenshotgun.runlines.model.Script;
import com.brokenshotgun.runlines.utils.DialogUtil;
import com.brokenshotgun.runlines.utils.Intents;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ReadSceneActivity extends AppCompatActivity {

    private static final String TAG = "ReadSceneActivity";

    private static final int TTS_MAX_SPEECH_RATE_PROGRESS = 10;
    private static final float TTS_MAX_SPEECH_RATE = 2f;
    private static final int EDIT_SCRIPT_REQUEST = 1;
    private static final int TTS_DATA_CHECK_CODE = 2;

    private TextToSpeech textToSpeech;
    private UtteranceProgressListener utteranceProgressListener;
    private Script script;
    private int sceneIndex = -1;
    private String lastUtteranceId = "";
    private int lastReadLineIndex = -1;
    private LineArrayAdapter lineArrayAdapter;
    private final List<Boolean> isEnabled = new ArrayList<>();
    private boolean actionsEnabled;
    private boolean autoPauseEnabled;
    private String autoPauseActorName;
    private static final long TTS_RATE = 800L; //666L;
    private DialogUtil dialogUtil;
    private SharedPreferences sharedPreferences;
    private SharedPreferences scriptPreferences;

    private final Map<String, Voice> supportedVoiceMap = new HashMap<>();

    private ListView linesListView;
    private FloatingActionButton playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_scene);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkTtsData();

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        script = (Script) extras.get("script");
        sceneIndex = (int) extras.get("sceneIndex");
        assert script != null;

        setTitle(getString(R.string.title_activity_read_script_prefix) + " \"" + (script.getScene(sceneIndex).getName().equals("") ? getString(R.string.label_no_scene_name) : script.getScene(sceneIndex).getName()) + "\"");

        dialogUtil = new DialogUtil();

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        actionsEnabled = sharedPreferences.getBoolean("actionsEnabled", true);

        scriptPreferences = getSharedPreferences(script.getName(), Context.MODE_PRIVATE);
        autoPauseEnabled = scriptPreferences.getBoolean("autoPauseEnabled", false);
        autoPauseActorName = scriptPreferences.getString("autoPauseActorName", null);
        Set<String> disabledActorSet = scriptPreferences.getStringSet("disabledActors", new HashSet<String>());
        refreshEnabledActors(disabledActorSet);

        linesListView = findViewById(R.id.lines_list);
        lineArrayAdapter = new LineArrayAdapter(this, new ArrayList<Line>());
        linesListView.setAdapter(lineArrayAdapter);
        linesListView.setNestedScrollingEnabled(true);
        linesListView.setEmptyView(findViewById(android.R.id.empty));
        linesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                playButton.setImageResource(R.drawable.ic_pause_white_48dp);
                skipToLine(i);
            }
        });

        toggleActions(actionsEnabled);
        refreshLines();

        FloatingActionButton editGroupButton = findViewById(R.id.edit_group);
        editGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showToggleLinesDialog();
            }
        });

        playButton = findViewById(R.id.play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sceneIndex < 0 || sceneIndex >= script.getScenes().size()) {
                    Snackbar.make(view, R.string.alert_invalid_scene_index, Snackbar.LENGTH_LONG)
                            .show();
                    return;
                }

                if (script.getScene(sceneIndex).getLines().isEmpty()) {
                    Snackbar.make(view, R.string.alert_no_lines, Snackbar.LENGTH_LONG)
                            .show();
                    return;
                }

                if (textToSpeech == null) {
                    Snackbar.make(view, R.string.alert_no_tts, Snackbar.LENGTH_LONG)
                            .show();
                    return;
                }

                if (textToSpeech.isSpeaking()) {
                    playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                    textToSpeech.stop();
                }
                else {
                    playButton.setImageResource(R.drawable.ic_pause_white_48dp);
                    playLine(lastReadLineIndex);
                }
            }
        });

        FloatingActionButton prevButton = findViewById(R.id.prev);
        FloatingActionButton nextButton = findViewById(R.id.next);
        FloatingActionButton stopButton = findViewById(R.id.stop);

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToLine(lastReadLineIndex - 1);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipToLine(lastReadLineIndex + 1);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                if (textToSpeech != null) textToSpeech.stop();
                resetReadProgress();
            }
        });

        utteranceProgressListener = new UtteranceProgressListener() {
            boolean autoPauseTriggered = false;

            @Override
            public void onStart(String utteranceId) {
                Log.d(ReadSceneActivity.class.getName(), "onStart() utterance id = " + utteranceId);

                String indexStr = utteranceId.split("-")[0];
                final int index = Integer.valueOf(indexStr);
                final boolean isPause = utteranceId.endsWith("-$");
                lastReadLineIndex = index;
                lastUtteranceId = utteranceId;

                if (isPause) return;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Line currentLine = lineArrayAdapter.getItem(index);
                        if (autoPauseEnabled &&
                                currentLine != null &&
                                currentLine.getActor().getName().equalsIgnoreCase(autoPauseActorName) &&
                                !autoPauseTriggered &&
                                textToSpeech != null &&
                                textToSpeech.isSpeaking()) {
                            playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                            textToSpeech.stop();
                            autoPauseTriggered = true;
                        }
                        else {
                            playButton.setImageResource(R.drawable.ic_pause_white_48dp);
                            autoPauseTriggered = false;
                        }
                        lineArrayAdapter.setSelectedItem(index);
                        lineArrayAdapter.notifyDataSetChanged();
                        linesListView.setSelection(index);
                    }
                });
            }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals(lastUtteranceId)) {
                    resetReadProgress();
                    autoPauseTriggered = false;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                        }
                    });
                }
            }

            @Override public void onError(String utteranceId) {}
        };
    }

    private void refreshEnabledActors(@NonNull Set<String> disabledActorSet) {
        if (!script.getActors().isEmpty()) {
            isEnabled.clear();
            for (int i = 0; i < script.getActors().size(); ++i) {
                isEnabled.add(!disabledActorSet.contains(script.getActors().get(i).getName()));
            }
        }
    }

    private void checkTtsData() {
        try {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            Intents.maybeStartActivityForResult(this, checkIntent, TTS_DATA_CHECK_CODE);
        } catch (ActivityNotFoundException e) {
            Log.e(ReadSceneActivity.class.getName(), "Oops! The function is not available in your device." + e.fillInStackTrace());
            showTtsNotAvailableDialog();
        }
    }

    private void showTtsNotAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_tts_not_available);
        builder.setMessage(R.string.message_dialog_tts_not_available);

        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_read_script, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem toggleActions = menu.findItem(R.id.toggle_actions);
        toggleActions.setChecked(actionsEnabled);

        MenuItem toggleAutoPause = menu.findItem(R.id.toggle_auto_pause);
        toggleAutoPause.setChecked(autoPauseEnabled);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tts_settings:
                showTtsSettingsDialog();
                return true;
            case R.id.toggle_settings:
                showToggleLinesDialog();
                return true;
            case R.id.edit_settings:
                onEditScriptButtonClicked();
                return true;
            case R.id.toggle_actions:
                actionsEnabled = !item.isChecked();
                item.setChecked(actionsEnabled);
                toggleActions(actionsEnabled);

                sharedPreferences.edit().putBoolean("actionsEnabled", actionsEnabled).apply();
                return true;
            case R.id.toggle_auto_pause:
                autoPauseEnabled = !item.isChecked();
                item.setChecked(autoPauseEnabled);

                scriptPreferences.edit().putBoolean("autoPauseEnabled", autoPauseEnabled).apply();

                if (autoPauseEnabled) showAutoPauseActorSelectDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        if(textToSpeech != null) {
            textToSpeech.stop();
        }

        dialogUtil.dismiss();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("script", script);
        setResult(RESULT_OK, result);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDIT_SCRIPT_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (script == null) {
                    finish();
                    return;
                }

                Script editScript = data.getParcelableExtra("script");
                if (editScript != null) {
                    script.copy(editScript);
                }

                Set<String> disabledActorSet = scriptPreferences.getStringSet("disabledActors", new HashSet<String>());
                refreshEnabledActors(disabledActorSet);

                lineArrayAdapter.clear();
                lineArrayAdapter.addAll(script.getScene(sceneIndex).getLines());

                lineArrayAdapter.notifyDataSetInvalidated();
            }
        }

        if (requestCode == TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if(status != TextToSpeech.ERROR && textToSpeech.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE) {
                            textToSpeech.setLanguage(Locale.getDefault());
                        }

                        if (status == TextToSpeech.SUCCESS) {
                            Log.d(TAG, "[Voice] Voices Available: ");
                            Set<Voice> voices = textToSpeech.getVoices();
                            if (voices != null) {
                                script.defaultVoice = null;
                                for (Voice v : voices) {
                                    if (v.getLocale().equals(Locale.getDefault())) {
                                        Log.d(TAG, "[Voice]\tVoice " + v.getName());
                                        if (script.defaultVoice == null) {
                                            script.defaultVoice = v.getName();
                                        }
                                        supportedVoiceMap.put(v.getName(), v);
                                        script.addVoice(v.getName());
                                    }
                                }
                            }
                        }
                    }
                });

                float rate = (sharedPreferences.getInt("speechRate", TTS_MAX_SPEECH_RATE_PROGRESS / 2) / (float) TTS_MAX_SPEECH_RATE_PROGRESS) * TTS_MAX_SPEECH_RATE;
                textToSpeech.setSpeechRate(rate);

                textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
            }
            else {
                try {
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    Intents.maybeStartActivity(this, installIntent);
                } catch (ActivityNotFoundException ex) {
                    Log.e(ReadSceneActivity.class.getName(), ex.getMessage(), ex);
                    Snackbar.make(linesListView, R.string.alert_tts_not_available, Snackbar.LENGTH_INDEFINITE).show();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onEditScriptButtonClicked() {
        Intent editIntent = new Intent(this, EditSceneActivity.class);
        editIntent.putExtra("script", script);
        editIntent.putExtra("sceneIndex", sceneIndex);
        startActivityForResult(editIntent, EDIT_SCRIPT_REQUEST);
    }

    private void skipToLine(int lineIndex) {
        lastReadLineIndex = lineIndex - 1;
        lastUtteranceId = "";
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        playLine(lineIndex);
    }

    private void playLine(int startLineIndex) {
        int i;

        if (startLineIndex < 0) i = 0;
        else if (startLineIndex >= lineArrayAdapter.getCount()) i = lineArrayAdapter.getCount() - 1;
        else i = startLineIndex;

        if (i < 0 || lineArrayAdapter.isEmpty()) {
            Log.d(TAG, "attempting to play invalid index = " + i + ", line array count = " + lineArrayAdapter.getCount());
            return;
        }

        for (; i < lineArrayAdapter.getCount(); ++i) {
            Line line = lineArrayAdapter.getItem(i);

            if (line != null) {
                if (line.enabled) {
                    String voiceKey = script.getVoice(line.getActor().getName());
                    if (supportedVoiceMap.containsKey(voiceKey)) {
                        textToSpeech.setVoice(supportedVoiceMap.get(voiceKey));
                    }
                    addToSpeechQueue(i, line.getLine());
                }
                else {
                    addToSpeechQueue(i, line.getLine(), true);
                }
            }
        }
    }

    private void refreshLines() {
        resetReadProgress();
        Set<String> disabledActorSet = new HashSet<>();
        for (int i = 0; i < lineArrayAdapter.getCount(); ++i) {
            Line line = lineArrayAdapter.getItem(i);
            if (line != null) {
                int actorIndex = script.getActors().indexOf(line.getActor());

                if (actorIndex >= 0 && actorIndex < isEnabled.size()) {
                    line.enabled = isEnabled.get(actorIndex);
                }

                if (!line.enabled) {
                    disabledActorSet.add(line.getActor().getName());
                }
            }
        }
        scriptPreferences.edit().putStringSet("disabledActors", disabledActorSet).apply();
        lineArrayAdapter.notifyDataSetChanged();
    }

    private void resetReadProgress() {
        lastReadLineIndex = -1;
        lastUtteranceId = "";
    }

    private void addToSpeechQueue(int index, String line) {
        addToSpeechQueue(index, line, false);
    }

    private void addToSpeechQueue(int index, String line, boolean isSilent) {
        String utteranceId = String.valueOf(index);

        if (isSilent) {
            long silenceLengthInMilliseconds = getSilenceLength(line);
            silence(silenceLengthInMilliseconds, utteranceId + "-s");
            return;
        }

        int maxLength = TextToSpeech.getMaxSpeechInputLength();;
        for (int i = 0; i < line.length(); i += maxLength) {
            int end = Math.min(i + maxLength, line.length());
            String utterance = line.substring(i, end);
            speak(utterance, (i > 0) ? (utteranceId + "-" + i) : utteranceId);
        }

        silence(500L, utteranceId + "-$");
    }

    private void speak(String utterance, String utteranceId) {
        if (textToSpeech == null) {
            Snackbar.make(linesListView, R.string.alert_tts_not_ready, Snackbar.LENGTH_LONG).show();
            return;
        }

        textToSpeech.speak(utterance, TextToSpeech.QUEUE_ADD, null, utteranceId);
    }

    private void silence(long duration, String utteranceId) {
        if (textToSpeech == null) {
            Snackbar.make(linesListView, R.string.alert_tts_not_ready, Snackbar.LENGTH_LONG).show();
            return;
        }

        textToSpeech.playSilentUtterance(duration, TextToSpeech.QUEUE_ADD, utteranceId);
    }

    private long getSilenceLength(String utterance) {
        if (utterance.trim().equals("")) return 0L;
        int words = utterance.split(" ").length;
        return words * TTS_RATE;
    }

    private void toggleActions(boolean actionsEnabled) {
        if (actionsEnabled) {
            lastReadLineIndex = -1;
            lineArrayAdapter.clear();
            lineArrayAdapter.addAll(script.getScene(sceneIndex).getLines());
        }
        else {
            lastReadLineIndex = -1;
            lineArrayAdapter.clear();
            List<Line> lines = new ArrayList<>(script.getScene(sceneIndex).getLines());
            List<Line> actions = new ArrayList<>();
            for (Line line : lines) {
                if (line.getActor().equals(Actor.ACTION)) {
                    actions.add(line);
                }
            }
            lines.removeAll(actions);
            lineArrayAdapter.addAll(lines);
        }
    }

    private void showToggleLinesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_group);

        String[] actorNames = new String[script.getActors().size()];
        actorNames[0] = getString(R.string.action_actor_name);
        for (int i = 1; i < script.getActors().size(); ++i) {
            actorNames[i] = script.getActors().get(i).getName();
        }

        boolean[] isEnabledArray = new boolean[isEnabled.size()];
        for(int i = 0; i < isEnabled.size(); ++i) {
            isEnabledArray[i] = isEnabled.get(i);
        }
        builder.setMultiChoiceItems(actorNames, isEnabledArray, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (which >= 0 && which < isEnabled.size()) {
                    isEnabled.set(which, isChecked);
                }
                refreshLines();
            }
        });

        builder.create().show();
    }

    private void showTtsSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_edit_script);
        LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(50, 50, 50, 25);
        final TextView speechRateValueLabel = new TextView(this);
        speechRateValueLabel.setText(
            String.format(Locale.US,
                getString(R.string.speech_rate),
                String.valueOf((sharedPreferences.getInt("speechRate", TTS_MAX_SPEECH_RATE_PROGRESS / 2) / (float) TTS_MAX_SPEECH_RATE_PROGRESS) * TTS_MAX_SPEECH_RATE)));
        speechRateValueLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        inputLayout.addView(speechRateValueLabel, labelParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 0, 50, 25);

        final SeekBar speechRateSeekBar = new SeekBar(this);
        speechRateSeekBar.setMax(TTS_MAX_SPEECH_RATE_PROGRESS);
        speechRateSeekBar.setProgress(sharedPreferences.getInt("speechRate", TTS_MAX_SPEECH_RATE_PROGRESS / 2));
        speechRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float rate = (progress / (float) TTS_MAX_SPEECH_RATE_PROGRESS) * TTS_MAX_SPEECH_RATE;
                if (textToSpeech != null) {
                    textToSpeech.setSpeechRate(rate);
                }
                speechRateValueLabel.setText(String.format(Locale.US, getString(R.string.speech_rate), String.valueOf(rate)));
                sharedPreferences.edit().putInt("speechRate", progress).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(ReadSceneActivity.class.getName(), "Speech rate change to: " + ((seekBar.getProgress() / (float) TTS_MAX_SPEECH_RATE_PROGRESS) * 2f));
            }
        });
        inputLayout.addView(speechRateSeekBar, params);

        builder.setView(inputLayout);

        dialogUtil.showDialog(builder.create());
    }

    private void showAutoPauseActorSelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_dialog_auto_pause_actor_select);
        builder.setCancelable(false);

        final String[] actorNames = new String[script.getActors().size()];
        actorNames[0] = getString(R.string.action_actor_name);
        for (int i = 1; i < script.getActors().size(); ++i) {
            actorNames[i] = script.getActors().get(i).getName();
        }

        autoPauseActorName = actorNames[0];
        builder.setSingleChoiceItems(actorNames, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                autoPauseActorName = actorNames[i];
                scriptPreferences.edit().putString("autoPauseActorName", autoPauseActorName).apply();
            }
        });

        builder.setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialogUtil.showDialog(builder.create());
    }
}

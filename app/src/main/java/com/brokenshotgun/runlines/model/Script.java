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

package com.brokenshotgun.runlines.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Script implements Parcelable {
    private String name;
    private final List<Actor> actors;
    /**
     * Scheduled to be removed.
     *
     * Only remains as a way to upgrade people whole are upgrading from old version.
     *
     * @deprecated lines are now stored in Scenes.
     */
    @Deprecated
    private final List<Line> lines;
    private final List<Scene> scenes;
    private final List<String> allVoices;
    private final HashMap<String, String> actorVoices;
    public transient String defaultVoice;

    public long id = -1L;

    public Script(String name) {
        this.name = name;
        actors = new ArrayList<>();
        lines = new ArrayList<>();
        scenes = new ArrayList<>();
        allVoices = new ArrayList<>();
        actorVoices = new HashMap<>();

        actors.add(Actor.ACTION);
    }

    public Script(Script copy) {
        this.name = copy.name;
        this.actors = new ArrayList<>(copy.actors);
        this.lines = new ArrayList<>(copy.lines);
        this.scenes = new ArrayList<>(copy.scenes);
        this.allVoices = new ArrayList<>(copy.allVoices);
        this.actorVoices = new HashMap<>(copy.actorVoices);
        this.id = copy.id;
    }

    public void copy(Script copy) {
        this.name = copy.name;
        this.actors.clear();
        this.actors.addAll(copy.actors);
        this.lines.clear();
        this.lines.addAll(copy.lines);
        this.scenes.clear();
        this.scenes.addAll(copy.scenes);
        this.allVoices.clear();
        this.allVoices.addAll(copy.allVoices);
        this.actorVoices.clear();
        this.actorVoices.putAll(copy.actorVoices);
        this.id = copy.id;
    }

    public void addActor(Actor actor) {
        actors.add(actor);
    }

    /**
     * Scheduled to be removed.
     *
     * Only remains as a way to upgrade people whole are upgrading from old version.
     *
     * @deprecated lines are now stored in Scenes.
     */
    @Deprecated
    public void addLine(Line line) {
        lines.add(line);
    }

    public String getName() {
        return name;
    }

    public List<Actor> getActors() {
        return actors;
    }

    public List<Line> getLines() {
        return lines;
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public Scene getScene(int sceneIndex) {
        return scenes.get(sceneIndex);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void removeActor(Actor actor) {
        actors.remove(actor);
    }

    public boolean hasActor(Actor currentActor) {
        return actors.contains(currentActor);
    }

    public void addScene(Scene newScene) {
        newScene.setNumber(scenes.size());
        scenes.add(newScene);
    }

    public void assignVoice(String actor, String voice) {
        actorVoices.put(actor, voice);
    }

    public String getVoice(String actor) {
        if (actorVoices.containsKey(actor)) {
            return actorVoices.get(actor);
        }

        return defaultVoice;
    }

    public void addVoice(String voice) {
        allVoices.add(voice);
    }

    public List<String> getAllVoices() {
        return allVoices;
    }

    @Override
    public String toString() {
        return "Script{" +
                "name='" + name + '\'' +
                ", actors=" + actors +
                ", scenes=" + scenes +
                ", allVoices=" + allVoices +
                ", actorVoices=" + actorVoices +
                ", defaultVoice='" + defaultVoice + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeTypedList(this.actors);
        dest.writeTypedList(this.lines);
        dest.writeTypedList(this.scenes);
        dest.writeStringList(this.allVoices);
        dest.writeSerializable(this.actorVoices);
        dest.writeLong(this.id);
    }

    protected Script(Parcel in) {
        name = in.readString();
        ArrayList<Actor> tmpActors = in.createTypedArrayList(Actor.CREATOR);
        actors = (tmpActors != null) ? new ArrayList<>(tmpActors) : new ArrayList<Actor>();
        ArrayList<Line> tmpLines = in.createTypedArrayList(Line.CREATOR);
        lines = (tmpLines != null) ? new ArrayList<>(tmpLines) : new ArrayList<Line>();
        ArrayList<Scene> tmpScenes = in.createTypedArrayList(Scene.CREATOR);
        scenes = (tmpScenes != null) ? new ArrayList<>(tmpScenes) : new ArrayList<Scene>();
        allVoices = new ArrayList<>();
        in.readStringList(allVoices);
        Serializable tmpVoices = in.readSerializable();
        this.actorVoices = (tmpVoices == null) ? new HashMap<String, String>() : (HashMap<String, String>) tmpVoices;
        id = in.readLong();

        if (actors.size() == 0 || !actors.get(0).equals(Actor.ACTION)) {
            actors.add(0, Actor.ACTION);
        }
    }

    public static final Creator<Script> CREATOR = new Creator<Script>() {
        @Override
        public Script createFromParcel(Parcel source) {
            return new Script(source);
        }

        @Override
        public Script[] newArray(int size) {
            return new Script[size];
        }
    };
}

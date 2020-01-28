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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brokenshotgun.runlines.data.FountainSerializer;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Script implements Parcelable {
    private String name;
    private String credit;
    private String author;
    private String source;
    private String draftDate;
    private String contact;
    private final List<Actor> actors;
    private final List<Scene> scenes;
    private final List<String> allVoices;
    private final HashMap<String, String> actorVoices;
    public transient String defaultVoice;

    public long id = -1L;

    public Script(String name) {
        this.name = name;
        actors = new ArrayList<>();
        scenes = new ArrayList<>();
        allVoices = new ArrayList<>();
        actorVoices = new HashMap<>();

        actors.add(Actor.ACTION);
    }

    public Script(Script copy) {
        this.name = copy.name;
        this.credit = copy.credit;
        this.author = copy.author;
        this.source = copy.source;
        this.draftDate = copy.draftDate;
        this.contact = copy.contact;
        this.actors = new ArrayList<>(copy.actors);
        this.scenes = new ArrayList<>(copy.scenes);
        this.allVoices = new ArrayList<>(copy.allVoices);
        this.actorVoices = new HashMap<>(copy.actorVoices);
        this.id = copy.id;
    }

    public Script(@NotNull Map<String, ? extends List<String>> titleTokens, @NotNull FountainSerializer.FNElement[] bodyTokens) {
        actors = new ArrayList<>();
        scenes = new ArrayList<>();
        allVoices = new ArrayList<>();
        actorVoices = new HashMap<>();

        // TODO parse tokens to Script model
        parseTitleTokens(titleTokens);
        parseBodyTokens(bodyTokens);
    }

    private void parseTitleTokens(@NotNull Map<String, ? extends List<String>> titleTokens) {
        if (titleTokens.containsKey("title")) {
            name = listToString(titleTokens.get("title"));
        }

        if (titleTokens.containsKey("credit")) {
            credit = listToString(titleTokens.get("credit"));
        }

        if (titleTokens.containsKey("authors")) {
            author = listToString(titleTokens.get("authors"));
        }

        if (titleTokens.containsKey("source")) {
            source = listToString(titleTokens.get("source"));
        }

        if (titleTokens.containsKey("draft date")) {
            draftDate = listToString(titleTokens.get("draft date"));
        }

        if (titleTokens.containsKey("contact")) {
            contact = listToString(titleTokens.get("contact"));
        }
    }

    private String listToString(@Nullable List<String> value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for(String str : value) {
            result.append(str).append(" ");
        }
        return result.toString().trim();
    }

    private void parseBodyTokens(@NotNull FountainSerializer.FNElement[] bodyTokens) {

    }

    public void copy(Script copy) {
        this.name = copy.name;
        this.actors.clear();
        this.actors.addAll(copy.actors);
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

    public String getName() {
        return name;
    }

    public String getCredit() {
        return credit;
    }

    public String getAuthor() {
        return author;
    }

    public String getSource() {
        return source;
    }

    public String getDraftDate() {
        return draftDate;
    }

    public String getContact() {
        return contact;
    }

    public List<Actor> getActors() {
        return actors;
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

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setDraftDate(String draftDate) {
        this.draftDate = draftDate;
    }

    public void setContact(String contact) {
        this.contact = contact;
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

    @NonNull
    @Override
    public String toString() {
        return "Script{" +
                "name='" + name + '\'' +
                ", credit='" + credit + '\'' +
                ", author='" + author + '\'' +
                ", source='" + source + '\'' +
                ", draftDate='" + draftDate + '\'' +
                ", contact='" + contact + '\'' +
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
        dest.writeString(this.credit);
        dest.writeString(this.author);
        dest.writeString(this.source);
        dest.writeString(this.draftDate);
        dest.writeString(this.contact);
        dest.writeTypedList(this.actors);
        dest.writeTypedList(this.scenes);
        dest.writeStringList(this.allVoices);
        dest.writeSerializable(this.actorVoices);
        dest.writeLong(this.id);
    }

    @SuppressWarnings("unchecked")
    protected Script(Parcel in) {
        name = in.readString();
        credit = in.readString();
        author = in.readString();
        source = in.readString();
        draftDate = in.readString();
        contact = in.readString();
        ArrayList<Actor> tmpActors = in.createTypedArrayList(Actor.CREATOR);
        actors = (tmpActors != null) ? new ArrayList<>(tmpActors) : new ArrayList<Actor>();
        ArrayList<Scene> tmpScenes = in.createTypedArrayList(Scene.CREATOR);
        scenes = (tmpScenes != null) ? new ArrayList<>(tmpScenes) : new ArrayList<Scene>();
        allVoices = new ArrayList<>();
        in.readStringList(allVoices);
        Serializable tmpActorVoices = in.readSerializable();
        this.actorVoices = (tmpActorVoices == null) ? new HashMap<String, String>() : (HashMap<String, String>) tmpActorVoices;
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

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

import java.util.ArrayList;
import java.util.List;

public class Scene implements Parcelable {
    private String name;
    private int number;
    private final List<Line> lines;

    public Scene(String name) {
        this(name, 0, new ArrayList<Line>());
    }

    public Scene(String name, int number, List<Line> lines) {
        this.name = name;
        this.number = number;
        this.lines = lines;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void addLine(Line line) {
        lines.add(line);
    }

    public void addAction(String action) {
        lines.add(new Line(Actor.ACTION, action));
    }

    @NonNull
    @Override
    public String toString() {
        return "Scene{" +
                "name='" + name + '\'' +
                ", number=" + number +
                ", lines=" + lines +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(number);
        dest.writeTypedList(lines);
    }

    private Scene(Parcel in) {
        name = in.readString();
        number = in.readInt();
        lines = in.createTypedArrayList(Line.CREATOR);
    }

    public static final Creator<Scene> CREATOR = new Creator<Scene>() {
        @Override
        public Scene createFromParcel(Parcel in) {
            return new Scene(in);
        }

        @Override
        public Scene[] newArray(int size) {
            return new Scene[size];
        }
    };
}

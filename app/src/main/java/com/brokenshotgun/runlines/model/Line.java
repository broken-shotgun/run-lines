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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Line implements Parcelable {
    private Actor actor;
    private String line;
    public int order;
    public transient boolean enabled;

    public Line(Actor actor, String line) {
        this.actor = actor;
        this.line = line;
        this.enabled = true;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public void setLine(String line) {
        this.line = line;
        this.lineHtml = null; // clear cached html version
    }

    @NonNull
    public Actor getActor() {
        return actor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Line line1 = (Line) o;

        if (order != line1.order) return false;
        if (!actor.equals(line1.actor)) return false;
        return line.equals(line1.line);

    }

    @Override
    public int hashCode() {
        int result = actor.hashCode();
        result = 31 * result + line.hashCode();
        result = 31 * result + order;
        return result;
    }

    public String getLine() {
        return line;
    }

    private transient String lineHtml;

    private transient static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_([^_]+)_");           //_underscores_
    private transient static final Pattern ITALICIZE_PATTERN = Pattern.compile("\\*([^\\*]+)\\*");      //*italicize*
    private transient static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^\\*\\*]+)\\*\\*");  //**bold**

    public String getLineHtml() {
        if (lineHtml == null) {
            lineHtml = line;

            StringBuffer boldBuffer = new StringBuffer();
            Matcher boldMatcher = BOLD_PATTERN.matcher(lineHtml);
            while (boldMatcher.find()) {
                boldMatcher.appendReplacement(boldBuffer, "<b>" + boldMatcher.group(1) + "</b>");
            }
            boldMatcher.appendTail(boldBuffer);
            lineHtml = boldBuffer.toString();

            StringBuffer italicsBuffer = new StringBuffer();
            Matcher italicsMatcher = ITALICIZE_PATTERN.matcher(lineHtml);
            while (italicsMatcher.find()) {
                italicsMatcher.appendReplacement(italicsBuffer, "<i>" + italicsMatcher.group(1) + "</i>");
            }
            italicsMatcher.appendTail(italicsBuffer);
            lineHtml = italicsBuffer.toString();

            StringBuffer underlineBuffer = new StringBuffer();
            Matcher underlineMatcher = UNDERSCORE_PATTERN.matcher(lineHtml);
            while (underlineMatcher.find()) {
                underlineMatcher.appendReplacement(underlineBuffer, "<u>" + underlineMatcher.group(1) + "</u>");
            }
            underlineMatcher.appendTail(underlineBuffer);
            lineHtml = underlineBuffer.toString();

            lineHtml = lineHtml.replaceAll("\n", "<br>");
        }
        return lineHtml;
    }

    @Override
    public String toString() {
        return "Line{" +
                "line='" + line + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(actor, flags);
        dest.writeString(line);
        dest.writeInt(order);
    }

    private Line(Parcel in) {
        actor = in.readParcelable(Actor.class.getClassLoader());
        line = in.readString();
        order = in.readInt();
        enabled = true;
    }

    public static final Creator<Line> CREATOR = new Creator<Line>() {
        @Override
        public Line createFromParcel(Parcel in) {
            return new Line(in);
        }

        @Override
        public Line[] newArray(int size) {
            return new Line[size];
        }
    };
}

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

package com.brokenshotgun.runlines.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.brokenshotgun.runlines.R;
import com.brokenshotgun.runlines.model.Actor;
import com.brokenshotgun.runlines.model.Line;

import java.util.List;
import java.util.Random;

public class LineArrayAdapter extends ArrayAdapter<Line> {
    public LineArrayAdapter(Context context, List<Line> lines) {
        super(context, R.layout.item_line, lines);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_line, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.nameText = convertView.findViewById(R.id.name);
            viewHolder.lineText = convertView.findViewById(R.id.line);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Line line = getItem(position);

        boolean enabled = false;
        if (line != null) {
            StringBuilder name = new StringBuilder(line.getActor().getName());
            if (line.characterExtensions.size() > 0) {
                name.append(" ");
                for (String ext : line.characterExtensions) {
                    name.append(ext);
                }
            }

            if (Actor.ACTION_NAME.equals(name.toString())) {
                viewHolder.nameText.setVisibility(View.GONE);
            } else {
                viewHolder.nameText.setVisibility(View.VISIBLE);
            }

            viewHolder.nameText.setText(name.toString());
            viewHolder.lineText.setText(Html.fromHtml(line.getLineHtml()));
            enabled = line.enabled;
        }

        highlightItem(position, enabled, convertView);

        return convertView;
    }

    private int selectedItem = -1;

    private void highlightItem(int position, boolean enabled, View result) {
        if (!enabled) {
            setItemBackground(result, getContext().getResources().getDrawable(R.drawable.hidden_line_background));
        } else if (position == selectedItem) {
            Line line = getItem(position);
            if (line != null)
                result.setBackgroundColor(colorFromUsername(line.getActor().getName()));
        } else {
            setItemBackground(result, null);
        }
    }

    private void setItemBackground(View result, Drawable drawable) {
        result.setBackground(drawable);
    }

    private Random random = new Random();

    private int colorFromUsername(String name) {
        random.setSeed(name.hashCode());
        int r = (random.nextInt(100) + 128);
        int g = (random.nextInt(100) + 128);
        int b = (random.nextInt(100) + 128);
        return Color.rgb(r, g, b);
    }

    public void setSelectedItem(int selectedItem) {
        this.selectedItem = selectedItem;
    }

    private static class ViewHolder {
        TextView nameText;
        TextView lineText;
    }
}

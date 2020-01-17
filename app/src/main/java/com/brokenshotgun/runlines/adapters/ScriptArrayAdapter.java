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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.brokenshotgun.runlines.R;
import com.brokenshotgun.runlines.model.Script;

import java.util.List;

public class ScriptArrayAdapter extends ArrayAdapter<Script> {
    private final Context context;

    public ScriptArrayAdapter(Context context, List<Script> objects) {
        super(context, R.layout.item_script, objects);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_script, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.nameText = convertView.findViewById(R.id.name);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Script script = getItem(position);

        if (script != null) {
            viewHolder.nameText.setText(script.getName().equals("") ? context.getString(R.string.label_no_script_name) : script.getName());
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView nameText;
    }
}

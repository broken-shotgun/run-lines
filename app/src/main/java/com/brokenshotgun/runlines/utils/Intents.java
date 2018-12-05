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

package com.brokenshotgun.runlines.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import com.brokenshotgun.runlines.R;

import java.util.List;

import static android.widget.Toast.LENGTH_LONG;

public final class Intents {
    /**
     * Attempt to launch the supplied {@link Intent}. Queries on-device packages before launching and
     * will display a simple message if none are available to handle it.
     */
    public static boolean maybeStartActivity(Context context, Intent intent) {
        return maybeStartActivity(context, intent, false);
    }

    public static boolean maybeStartActivityForResult(Activity activity, Intent intent, int resultCode) {
        return maybeStartActivityForResult(activity, intent, false, resultCode);
    }

    /**
     * Attempt to launch Android's chooser for the supplied {@link Intent}. Queries on-device
     * packages before launching and will display a simple message if none are available to handle
     * it.
     */
    public static boolean maybeStartChooser(Context context, Intent intent) {
        return maybeStartActivity(context, intent, true);
    }

    public static boolean maybeStartChooserForResult(Activity activity, Intent intent, int resultCode) {
        return maybeStartActivityForResult(activity, intent, true, resultCode);
    }

    private static boolean maybeStartActivity(Context context, Intent intent, boolean chooser) {
        if (hasHandler(context, intent)) {
            if (chooser) {
                intent = Intent.createChooser(intent, null);
            }
            context.startActivity(intent);
            return true;
        } else {
            Toast.makeText(context, R.string.no_intent_handler, LENGTH_LONG).show();
            return false;
        }
    }

    private static boolean maybeStartActivityForResult(Activity activity, Intent intent, boolean chooser, int resultCode) {
        if (hasHandler(activity, intent)) {
            if (chooser) {
                intent = Intent.createChooser(intent, null);
            }
            activity.startActivityForResult(intent, resultCode);
            return true;
        } else {
            Toast.makeText(activity, R.string.no_intent_handler, LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Queries on-device packages for a handler for the supplied {@link Intent}.
     */
    private static boolean hasHandler(Context context, Intent intent) {
        List<ResolveInfo> handlers = context.getPackageManager().queryIntentActivities(intent, 0);
        return !handlers.isEmpty();
    }

    private Intents() {
        throw new AssertionError("No instances.");
    }
}
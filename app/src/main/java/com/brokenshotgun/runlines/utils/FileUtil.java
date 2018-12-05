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

import android.os.Environment;
import android.util.Log;

import com.brokenshotgun.runlines.MainActivity;

import java.io.File;

public class FileUtil {
    public static File getDocStorageDir() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Run Lines");
        if (!file.mkdirs()) {
            Log.e(MainActivity.class.getName(), "Directory not created");
        }
        return file;
    }
}

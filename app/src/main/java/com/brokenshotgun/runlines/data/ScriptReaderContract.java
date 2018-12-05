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

package com.brokenshotgun.runlines.data;

import android.provider.BaseColumns;

public final class ScriptReaderContract {
    public ScriptReaderContract() {
    }

    static abstract class ScriptEntry implements BaseColumns {
        static final String TABLE_NAME = "script";
        static final String COLUMN_NAME_SCRIPT_JSON = "script_json";
        static final String COLUMN_NAME_CREATE_DATE = "create_date";
    }
}

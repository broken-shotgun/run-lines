package com.brokenshotgun.runlines.data.local

import android.provider.BaseColumns

object ScriptReaderContract {
    object ScriptEntry : BaseColumns {
        const val TABLE_NAME = "script"
        const val COLUMN_NAME_SCRIPT_JSON = "script_json"
        const val COLUMN_NAME_CREATE_DATE = "create_date"
    }
}

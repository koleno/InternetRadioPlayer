package xyz.koleno.internetradioplayer.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit

class Preferences(appContext: Context) {

    private val preferences = appContext.getSharedPreferences("preferences", MODE_PRIVATE)

    fun isIgnoreSecurityEnabled(): Boolean =
        preferences.getBoolean(IGNORE_SECURITY_ENABLED_KEY, false)

    fun setIgnoreSecurityEnabled(enabled: Boolean) = preferences.edit {
        putBoolean(IGNORE_SECURITY_ENABLED_KEY, enabled)
    }

    fun getGridRows(default: Int): Int = preferences.getInt(GRID_ROWS_KEY, default)

    fun setGridRows(rows: Int) = preferences.edit {
        putInt(GRID_ROWS_KEY, rows)
    }

    fun getGridColumns(default: Int): Int = preferences.getInt(GRID_COLUMNS_KEY, default)

    fun setGridColumns(columns: Int) = preferences.edit {
        putInt(GRID_COLUMNS_KEY, columns)
    }

    fun hasAskedForNotificationPermissions(): Boolean = preferences.getBoolean(NOTIF_PERMISSIONS_KEY, false)

    fun setAskedForNotificationPermissions(asked: Boolean) = preferences.edit {
        putBoolean(NOTIF_PERMISSIONS_KEY, asked)
    }

    companion object {
        private const val IGNORE_SECURITY_ENABLED_KEY = "ignore_security_enabled"
        private const val GRID_ROWS_KEY = "grid_rows_key"
        private const val GRID_COLUMNS_KEY = "grid_columns_key"
        private const val NOTIF_PERMISSIONS_KEY = "notification_permissions_key"
    }


}
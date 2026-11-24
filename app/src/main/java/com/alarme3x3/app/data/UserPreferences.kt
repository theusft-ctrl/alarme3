package com.alarme3x3.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "alarme_3x3_prefs")

class UserPreferences(private val context: Context) {
    private object Keys {
        val START_DATE_ISO = stringPreferencesKey("start_date_iso")
        val VIBRATE_ENABLED = booleanPreferencesKey("vibrate_enabled")
    }

    val startDateFlow: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[Keys.START_DATE_ISO]?.let { LocalDate.parse(it) }
    }

    val vibrateEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.VIBRATE_ENABLED] ?: true
    }

    suspend fun getStartDateOnce(): LocalDate? = startDateFlow.first()
    suspend fun isVibrateEnabledOnce(): Boolean = vibrateEnabledFlow.first()

    suspend fun setStartDate(date: LocalDate) {
        context.dataStore.edit { it[Keys.START_DATE_ISO] = date.toString() }
    }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATE_ENABLED] = enabled }
    }
}

package com.harangpanul.counter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "counter")

class CounterRepository(private val context: Context) {
    private val key = intPreferencesKey("count")

    val count: Flow<Int> = context.dataStore.data.map { it[key] ?: 0 }

    suspend fun setCount(value: Int) {
        context.dataStore.edit { it[key] = value }
    }
}

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

    /** 읽기-수정-쓰기가 원자적이라 앱과 플로팅 서비스가 동시에 눌러도 값이 유실되지 않는다. */
    suspend fun increment() {
        context.dataStore.edit { it[key] = (it[key] ?: 0) + 1 }
    }

    suspend fun decrement() {
        context.dataStore.edit { it[key] = ((it[key] ?: 0) - 1).coerceAtLeast(0) }
    }

    suspend fun reset() {
        context.dataStore.edit { it[key] = 0 }
    }
}

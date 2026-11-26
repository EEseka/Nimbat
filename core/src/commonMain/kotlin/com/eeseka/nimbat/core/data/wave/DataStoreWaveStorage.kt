package com.eeseka.nimbat.core.data.wave

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.eeseka.nimbat.core.domain.wave.WaveStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreWaveStorage(
    private val dataStore: DataStore<Preferences>
) : WaveStorage {

    private companion object {
        val BEST_BAT_DASH_SCORE_KEY = intPreferencesKey("KEY_BEST_BAT_DASH_SCORE")
    }

    override val bestBatDashScore: Flow<Int> = dataStore.data.map { preferences ->
        preferences[BEST_BAT_DASH_SCORE_KEY] ?: 0
    }

    override suspend fun setBestBatDashScore(score: Int) {
        dataStore.edit { preferences ->
            preferences[BEST_BAT_DASH_SCORE_KEY] = score
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences -= BEST_BAT_DASH_SCORE_KEY
        }
    }
}

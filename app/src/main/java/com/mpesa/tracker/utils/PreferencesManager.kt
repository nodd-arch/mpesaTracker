package com.mpesa.tracker.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mpesa_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SELECTED_SIM = intPreferencesKey("selected_sim") // -1=both, 0=SIM1, 1=SIM2
        val MONITOR_SIM1 = booleanPreferencesKey("monitor_sim1")
        val MONITOR_SIM2 = booleanPreferencesKey("monitor_sim2")
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map {
        it[ONBOARDING_DONE] ?: false
    }

    val selectedSim: Flow<Int> = context.dataStore.data.map {
        it[SELECTED_SIM] ?: -1
    }

    val monitorSim1: Flow<Boolean> = context.dataStore.data.map {
        it[MONITOR_SIM1] ?: true
    }

    val monitorSim2: Flow<Boolean> = context.dataStore.data.map {
        it[MONITOR_SIM2] ?: false
    }

    suspend fun completeOnboarding(sim: Int, monitorSim1: Boolean, monitorSim2: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_DONE] = true
            prefs[SELECTED_SIM] = sim
            prefs[MONITOR_SIM1] = monitorSim1
            prefs[MONITOR_SIM2] = monitorSim2
        }
    }

    suspend fun updateSimPreference(sim: Int, monitorSim1: Boolean, monitorSim2: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_SIM] = sim
            prefs[MONITOR_SIM1] = monitorSim1
            prefs[MONITOR_SIM2] = monitorSim2
        }
    }
}

package com.example.savvy.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object OnboardingPrefs {
    private const val DS_NAME = "onboarding_prefs"
    private val KEY_SHOWN = booleanPreferencesKey("onboarding_shown")

    val Context.onboardingDataStore by preferencesDataStore(name = DS_NAME)

    suspend fun setOnboardingShown(context: Context) {
        context.onboardingDataStore.edit { it[KEY_SHOWN] = true }
    }

    fun isOnboardingShown(context: Context): Flow<Boolean> {
        return context.onboardingDataStore.data.map { it[KEY_SHOWN] ?: false }
    }
}

package com.mulkallah.aircontrole.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mulkallah.aircontrole.core.AirControleConstants
import com.mulkallah.aircontrole.core.model.QuickAccessApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.airControleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "air_controle_prefs",
)

class AirControlePreferences(private val context: Context) {

    private object Keys {
        val languageTag = stringPreferencesKey("language_tag")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val airControlEnabled = booleanPreferencesKey("air_control_enabled")
        val cursorSize = floatPreferencesKey("cursor_size")
        val cursorPulse = booleanPreferencesKey("cursor_pulse")
        val extraApps = stringSetPreferencesKey("extra_quick_access_apps")
    }

    val languageTag: Flow<String> = context.airControleDataStore.data.map { prefs ->
        prefs[Keys.languageTag] ?: AirControleConstants.DEFAULT_LANGUAGE_TAG
    }

    val onboardingComplete: Flow<Boolean> = context.airControleDataStore.data.map { prefs ->
        prefs[Keys.onboardingComplete] ?: false
    }

    val airControlEnabled: Flow<Boolean> = context.airControleDataStore.data.map { prefs ->
        prefs[Keys.airControlEnabled] ?: false
    }

    val cursorSize: Flow<Float> = context.airControleDataStore.data.map { prefs ->
        prefs[Keys.cursorSize] ?: 1f
    }

    val cursorPulse: Flow<Boolean> = context.airControleDataStore.data.map { prefs ->
        prefs[Keys.cursorPulse] ?: true
    }

    val extraApps: Flow<Set<String>> = context.airControleDataStore.data.map { prefs ->
        prefs[Keys.extraApps] ?: emptySet()
    }

    suspend fun languageTagOnce(): String = languageTag.first()

    suspend fun setLanguageTag(tag: String) {
        context.airControleDataStore.edit { it[Keys.languageTag] = tag }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.airControleDataStore.edit { it[Keys.onboardingComplete] = complete }
    }

    suspend fun setAirControlEnabled(enabled: Boolean) {
        context.airControleDataStore.edit { it[Keys.airControlEnabled] = enabled }
    }

    suspend fun setCursorSize(size: Float) {
        context.airControleDataStore.edit { it[Keys.cursorSize] = size.coerceIn(0.6f, 2f) }
    }

    suspend fun setCursorPulse(enabled: Boolean) {
        context.airControleDataStore.edit { it[Keys.cursorPulse] = enabled }
    }

    suspend fun addExtraApp(packageName: String) {
        context.airControleDataStore.edit { prefs ->
            val current = prefs[Keys.extraApps] ?: emptySet()
            prefs[Keys.extraApps] = current + packageName
        }
    }

    suspend fun removeExtraApp(packageName: String) {
        context.airControleDataStore.edit { prefs ->
            val current = prefs[Keys.extraApps] ?: emptySet()
            prefs[Keys.extraApps] = current - packageName
        }
    }

    fun resolveQuickAccess(extraPackages: Set<String>): List<QuickAccessApp> {
        val extras = extraPackages.map { pkg ->
            QuickAccessApp(
                id = pkg,
                label = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() },
                packageName = pkg,
                isBuiltIn = false,
            )
        }
        return QuickAccessApp.DEFAULTS + extras
    }
}

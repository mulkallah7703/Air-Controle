package com.mulkallah.aircontrole

import android.app.Application
import com.mulkallah.aircontrole.core.locale.LocaleController
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AirControleApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var preferences: AirControlePreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AirControlePreferences(this)
        applicationScope.launch {
            LocaleController.apply(preferences.languageTag.first())
        }
    }

    companion object {
        lateinit var instance: AirControleApplication
            private set
    }
}

package com.mulkallah.aircontrole

import android.app.Application
import com.mulkallah.aircontrole.core.AirControleLog
import com.mulkallah.aircontrole.core.locale.LocaleController
import com.mulkallah.aircontrole.core.prefs.AirControlePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AirControleApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val preferences: AirControlePreferences by lazy { AirControlePreferences(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applicationScope.launch {
            LocaleController.apply(preferences.languageTag.first())
            val enabled = preferences.airControlEnabled.first()
            AirControleLog.i(
                "app start enabled=$enabled — camera FGS starts from MainActivity/Home, not Application",
            )
        }
    }

    companion object {
        lateinit var instance: AirControleApplication
            private set
    }
}

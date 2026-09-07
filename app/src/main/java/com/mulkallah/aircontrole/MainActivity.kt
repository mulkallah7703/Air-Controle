package com.mulkallah.aircontrole

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.mulkallah.aircontrole.core.locale.LocaleController
import com.mulkallah.aircontrole.ui.AirControleApp
import com.mulkallah.aircontrole.ui.theme.AirControleTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val tag = runBlocking {
            AirControlePreferencesHolder.languageTag(newBase)
        }
        super.attachBaseContext(LocaleController.wrap(newBase, tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = (application as AirControleApplication).preferences
        lifecycleScope.launch {
            LocaleController.apply(prefs.languageTag.first())
        }
        setContent {
            val language by prefs.languageTag.collectAsState(initial = "ar")
            AirControleTheme(rtl = LocaleController.isRtl(language)) {
                AirControleApp(preferences = prefs)
            }
        }
    }
}

private object AirControlePreferencesHolder {
    fun languageTag(context: Context): String {
        val app = context.applicationContext as? AirControleApplication
        return if (app != null && app::preferences.isInitialized) {
            runBlocking { app.preferences.languageTagOnce() }
        } else {
            "ar"
        }
    }
}

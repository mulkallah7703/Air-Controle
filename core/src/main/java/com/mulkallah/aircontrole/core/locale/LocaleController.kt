package com.mulkallah.aircontrole.core.locale

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleController {
    fun apply(tag: String) {
        val locales = LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun wrap(context: Context, tag: String): Context {
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun isRtl(tag: String): Boolean {
        return Locale.forLanguageTag(tag).let { locale ->
            val direction = android.text.TextUtils.getLayoutDirectionFromLocale(locale)
            direction == android.util.LayoutDirection.RTL
        }
    }
}

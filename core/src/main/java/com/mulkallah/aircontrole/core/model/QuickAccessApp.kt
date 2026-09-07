package com.mulkallah.aircontrole.core.model

data class QuickAccessApp(
    val id: String,
    val label: String,
    val packageName: String,
    val isBuiltIn: Boolean = true,
) {
    companion object {
        val DEFAULTS: List<QuickAccessApp> = listOf(
            QuickAccessApp("instagram", "Instagram", "com.instagram.android"),
            QuickAccessApp("tiktok", "TikTok", "com.zhiliaoapp.musically"),
            QuickAccessApp("youtube", "YouTube", "com.google.android.youtube"),
            QuickAccessApp("whatsapp", "WhatsApp", "com.whatsapp"),
            QuickAccessApp("chrome", "Chrome", "com.android.chrome"),
        )
    }
}

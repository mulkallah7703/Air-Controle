package com.mulkallah.aircontrole.core.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledAccessibilityServicesTest {

    @Test
    fun nullBlankOrLiteralNullIsNotEnabled() {
        assertFalse(EnabledAccessibilityServices.isAirControleEnabled(null))
        assertFalse(EnabledAccessibilityServices.isAirControleEnabled(""))
        assertFalse(EnabledAccessibilityServices.isAirControleEnabled("   "))
        assertFalse(EnabledAccessibilityServices.isAirControleEnabled("null"))
        assertFalse(EnabledAccessibilityServices.isAirControleEnabled("NULL"))
    }

    @Test
    fun exactFlattenedComponentIsEnabled() {
        assertTrue(
            EnabledAccessibilityServices.isAirControleEnabled(
                EnabledAccessibilityServices.flattened,
            ),
        )
        assertTrue(
            EnabledAccessibilityServices.isAirControleEnabled(
                EnabledAccessibilityServices.shortFlattened,
            ),
        )
    }

    @Test
    fun componentInColonSeparatedListIsEnabled() {
        val raw =
            "com.android.systemui/.accessibility.fake:" +
                EnabledAccessibilityServices.flattened +
                ":com.samsung.android.app.aodservice/.AodService"
        assertTrue(EnabledAccessibilityServices.isAirControleEnabled(raw))
    }

    @Test
    fun otherServicesOrLooseNameAreNotEnabled() {
        assertFalse(EnabledAccessibilityServices.isAirControleEnabled("com.other/.Foo"))
        assertFalse(
            EnabledAccessibilityServices.isAirControleEnabled(
                "com.samsung.android.accessibility/AirControleAccessibilityService",
            ),
        )
        assertFalse(
            EnabledAccessibilityServices.isAirControleEnabled(
                "AirControleAccessibilityService",
            ),
        )
        assertFalse(
            EnabledAccessibilityServices.isAirControleEnabled(
                "com.mulkallah.aircontrole/com.mulkallah.aircontrole.accessibility.OtherService",
            ),
        )
    }

    @Test
    fun matchesOnlyAirControleComponent() {
        assertTrue(EnabledAccessibilityServices.matchesAirControle(EnabledAccessibilityServices.flattened))
        assertTrue(EnabledAccessibilityServices.matchesAirControle(EnabledAccessibilityServices.shortFlattened))
        assertFalse(EnabledAccessibilityServices.matchesAirControle(""))
        assertFalse(EnabledAccessibilityServices.matchesAirControle("com.mulkallah.aircontrole/"))
        assertFalse(
            EnabledAccessibilityServices.matchesAirControle(
                "com.mulkallah.aircontrole/com.mulkallah.aircontrole.control.AirControlForegroundService",
            ),
        )
    }
}

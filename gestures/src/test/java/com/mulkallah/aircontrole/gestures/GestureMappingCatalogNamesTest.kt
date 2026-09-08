package com.mulkallah.aircontrole.gestures

import com.mulkallah.aircontrole.core.model.GestureMappingCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureMappingCatalogNamesTest {
    @Test
    fun catalogKeysMatchGestureTypesExceptNone() {
        val types = GestureType.entries.filter { it != GestureType.NONE }.map { it.name }.toSet()
        assertEquals(types, GestureMappingCatalog.gestureKeys.toSet())
    }
}

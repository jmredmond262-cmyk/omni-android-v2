package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Omni", appName)
    }

    @Test
    fun `launch MainActivity successfully without crash`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun `verify farmacia de turno and schedule configuration`() {
        val viewModel = com.example.ui.MainViewModel()
        val stores = viewModel.stores.value
        val pharmacy = stores.firstOrNull { it.type == com.example.model.StoreType.FARMACIA }
        
        assertNotNull(pharmacy)
        assertEquals(true, pharmacy?.isOnDuty)
        assertEquals(true, pharmacy?.is24Hours)

        // Test updating schedule through merchant hub
        viewModel.updateManagedStoreSchedule(
            openTime = "07:30",
            closeTime = "22:00",
            weekendHoursText = "Sáb y Dom 08:00 - 13:00",
            is24Hours = false,
            isOnDuty = true,
            openOnHolidays = true,
            scheduleNotes = "Timbre nocturno habilitado",
            isOpenNow = true
        )

        val updated = viewModel.managedStore.value
        assertEquals("07:30", updated.openTime)
        assertEquals("22:00", updated.closeTime)
        assertEquals(true, updated.isOnDuty)
        assertEquals(true, updated.openOnHolidays)
        assertEquals(true, updated.isEffectivelyOpen)
    }
}

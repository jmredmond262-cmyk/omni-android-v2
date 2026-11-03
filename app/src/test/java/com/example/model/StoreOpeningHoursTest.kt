package com.example.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreOpeningHoursTest {
    private fun store(
        is24Hours: Boolean = false,
        isOnDuty: Boolean = false,
        hoursText: String = "",
        openTime: String = "",
        closeTime: String = "",
        isOpen: Boolean = false
    ) = Store(
        id = "test",
        name = "Comercio de prueba",
        type = StoreType.FARMACIA,
        rating = 0f,
        reviewsCount = 0,
        distanceMeters = 100,
        address = "Dirección de prueba",
        isOpen = isOpen,
        hoursText = hoursText,
        accentColorHex = 0L,
        photosCount = 0,
        hasVideoTour = false,
        videoTourDuration = "",
        description = "",
        specialOfferText = "",
        minSpendDiscountThreshold = 0.0,
        minSpendDiscountPercent = 0,
        comboDeals = emptyList(),
        inventory = emptyList(),
        mapOffsetX = 0.5f,
        mapOffsetY = 0.5f,
        openTime = openTime,
        closeTime = closeTime,
        is24Hours = is24Hours,
        isOnDuty = isOnDuty
    )

    private fun time(hour: Int, minute: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.TUESDAY, 3, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun pharmacyOnDutyWithoutHoursIsNotAssumedOpenAllDay() {
        assertFalse(store(isOnDuty = true).isOpenAtCurrentTime(time(3, 0)))
    }

    @Test
    fun explicitTwentyFourHourStoreIsOpen() {
        assertTrue(store(is24Hours = true).isOpenAtCurrentTime(time(3, 0)))
    }

    @Test
    fun publishedHoursDetermineOpenStatus() {
        val shop = store(hoursText = "08:00 - 20:00")
        assertTrue(shop.isOpenAtCurrentTime(time(10, 0)))
        assertFalse(shop.isOpenAtCurrentTime(time(21, 0)))
    }
}

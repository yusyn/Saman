package com.car.rental.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JalaliDateTest {

    @Test
    void knownGregorianMapsToExpectedJalali() {
        // 2026-08-16 ≈ 1405/05/25 (project previously validated this range)
        JalaliDate j = JalaliDate.fromGregorian(LocalDate.of(2026, 8, 16));
        assertEquals(1405, j.getYear());
        assertEquals(5, j.getMonth());
        assertEquals(25, j.getDay());
        assertEquals("1405/05/25", j.formatDate());
    }

    @Test
    void roundTripGregorianJalali() {
        LocalDate original = LocalDate.of(2024, 3, 20);
        LocalDate back = JalaliDate.fromGregorian(original).toGregorian();
        assertEquals(original, back);
    }

    @Test
    void formatDateTimeIncludesTime() {
        LocalDateTime dt = LocalDateTime.of(2026, 8, 16, 14, 5, 9);
        String formatted = JalaliDate.formatDateTime(dt);
        assertEquals("1405/05/25 14:05:09", formatted);
    }

    @Test
    void nullDateTimeFormatsToEmpty() {
        assertEquals("", JalaliDate.formatDateTime(null));
    }

    @Test
    void invalidJalaliConstructorRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JalaliDate(1405, 13, 1));
    }
}

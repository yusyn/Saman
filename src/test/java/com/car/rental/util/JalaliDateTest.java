package com.car.rental.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JalaliDateTest {

    @Test
    void testNormalizeDate() {
        assertEquals("1405/02/05", JalaliDate.normalizeDate("1405/2/5"));
        assertEquals("1405/02/05", JalaliDate.normalizeDate("۱۴۰۵-۰۲-۰۵"));
        assertEquals("1405/02/05", JalaliDate.normalizeDate("1405/02/05"));
        assertEquals("1405/11/09", JalaliDate.normalizeDate(" 1405-11-9 "));
    }

    @Test
    void testIsExpired() {
        LocalDate ref = LocalDate.of(2026, 3, 21); // Farvardin 1, 1405
        assertTrue(JalaliDate.isExpired("1404/12/29", ref));
        assertFalse(JalaliDate.isExpired("1405/01/02", ref));
        assertFalse(JalaliDate.isExpired("1405/01/01", ref));
    }

    @Test
    void testToday() {
        assertNotNull(JalaliDate.today());
        assertTrue(JalaliDate.today().matches("^\\d{4}/\\d{2}/\\d{2}$"));
    }
}

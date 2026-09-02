package com.car.rental.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InputValidatorsTest {

    @Test
    void acceptsSimpleEnglishName() {
        assertNull(InputValidators.validateEnglishFullName("Ali Reza"));
        assertNull(InputValidators.validateEnglishFullName("O'Connor"));
        assertNull(InputValidators.validateEnglishFullName("Mary-Jane"));
    }

    @Test
    void rejectsBlank() {
        assertNotNull(InputValidators.validateEnglishFullName(""));
        assertNotNull(InputValidators.validateEnglishFullName("   "));
        assertNotNull(InputValidators.validateEnglishFullName(null));
    }

    @Test
    void rejectsPersianOrDigits() {
        assertNotNull(InputValidators.validateEnglishFullName("علی"));
        assertNotNull(InputValidators.validateEnglishFullName("Ali123"));
    }
}

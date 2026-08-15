package com.library.util;

import com.library.exception.LibraryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @Test
    void requireNonBlank_throwsOnNull() {
        assertThrows(LibraryException.class, () -> Validator.requireNonBlank(null, "Name"));
    }

    @Test
    void requireNonBlank_throwsOnBlank() {
        assertThrows(LibraryException.class, () -> Validator.requireNonBlank("   ", "Name"));
    }

    @Test
    void requireNonBlank_passesForValidValue() {
        assertDoesNotThrow(() -> Validator.requireNonBlank("Manjunath", "Name"));
    }

    @Test
    void requireValidEmail_rejectsMalformedAddress() {
        assertThrows(LibraryException.class, () -> Validator.requireValidEmail("not-an-email"));
    }

    @Test
    void requireValidEmail_acceptsWellFormedAddress() {
        assertDoesNotThrow(() -> Validator.requireValidEmail("manjunathkr14379@gmail.com"));
    }

    @Test
    void requirePositive_rejectsZeroAndNegative() {
        assertThrows(LibraryException.class, () -> Validator.requirePositive(0, "Copies"));
        assertThrows(LibraryException.class, () -> Validator.requirePositive(-5, "Copies"));
    }
}

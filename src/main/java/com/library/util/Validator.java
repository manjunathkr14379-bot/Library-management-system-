package com.library.util;

import com.library.exception.LibraryException;

import java.util.regex.Pattern;

/**
 * Centralized input validation so the same rules aren't duplicated across
 * service methods and the UI layer.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private Validator() { }

    public static void requireNonBlank(String value, String fieldName) throws LibraryException {
        if (value == null || value.isBlank()) {
            throw new LibraryException(fieldName + " cannot be empty");
        }
    }

    public static void requirePositive(int value, String fieldName) throws LibraryException {
        if (value <= 0) {
            throw new LibraryException(fieldName + " must be greater than zero");
        }
    }

    public static void requireValidEmail(String email) throws LibraryException {
        requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new LibraryException("Invalid email format: " + email);
        }
    }
}

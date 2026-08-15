package com.library.exception;

/**
 * Unchecked wrapper around low-level SQLException so the DAO interface
 * doesn't force every caller up the stack to declare java.sql.SQLException.
 * The original SQLException is preserved as the cause for debugging.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}

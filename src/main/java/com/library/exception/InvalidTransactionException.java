package com.library.exception;

/** Thrown for illegal state transitions, e.g. returning an already-returned book. */
public class InvalidTransactionException extends LibraryException {
    public InvalidTransactionException(String message) { super(message); }
}

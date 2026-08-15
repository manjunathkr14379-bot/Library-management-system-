package com.library.exception;

/** Thrown when a member tries to issue a book with zero available copies. */
public class BookNotAvailableException extends LibraryException {
    public BookNotAvailableException(String title) {
        super("No copies available for: " + title);
    }
}

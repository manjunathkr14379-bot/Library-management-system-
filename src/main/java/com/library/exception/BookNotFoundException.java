package com.library.exception;

public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(int bookId) { super("Book not found with id: " + bookId); }
    public BookNotFoundException(String detail) { super(detail); }
}

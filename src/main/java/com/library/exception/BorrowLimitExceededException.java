package com.library.exception;

/** Thrown when a member has hit their membership-tier borrowing limit. */
public class BorrowLimitExceededException extends LibraryException {
    public BorrowLimitExceededException(String memberName, int limit) {
        super(memberName + " has reached the maximum borrow limit of " + limit + " books");
    }
}

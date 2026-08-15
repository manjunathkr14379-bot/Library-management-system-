package com.library.exception;

public class MemberNotFoundException extends LibraryException {
    public MemberNotFoundException(int memberId) { super("Member not found with id: " + memberId); }
}

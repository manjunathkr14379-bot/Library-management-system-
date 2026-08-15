package com.library.service;

import com.library.exception.*;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;

import java.util.List;

/**
 * Business/service layer contract. Controllers or UI code should only ever
 * talk to this interface, never to DAOs directly - keeps validation and
 * business rules (borrow limits, fines, availability) in exactly one place.
 */
public interface LibraryService {

    Book addBook(String isbn, String title, int authorId, String category, int copies, java.math.BigDecimal price) throws LibraryException;

    List<Book> searchBooks(String keyword);

    Member registerMember(String name, String email, String phone, Member.MembershipType type) throws LibraryException;

    Transaction issueBook(int bookId, int memberId) throws LibraryException;

    Transaction returnBook(int transactionId) throws LibraryException;

    List<Transaction> getOverdueBooks();

    java.math.BigDecimal calculateFine(Transaction transaction);
}

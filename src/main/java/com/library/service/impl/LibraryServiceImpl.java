package com.library.service.impl;

import com.library.dao.BookDAO;
import com.library.dao.MemberDAO;
import com.library.dao.TransactionDAO;
import com.library.exception.*;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.LibraryService;
import com.library.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Core business logic. Every public method here represents one real-world
 * "use case" (issue a book, return a book, register a member ...).
 *
 * Dependencies are injected through the constructor (constructor injection)
 * rather than instantiated with `new` inside the class - this is what lets
 * unit tests swap in fake DAOs without touching a real database.
 */
public class LibraryServiceImpl implements LibraryService {

    private static final int LOAN_PERIOD_DAYS = 14;
    private static final BigDecimal FINE_PER_DAY = new BigDecimal("5.00"); // INR per day overdue

    private final BookDAO bookDAO;
    private final MemberDAO memberDAO;
    private final TransactionDAO transactionDAO;

    public LibraryServiceImpl(BookDAO bookDAO, MemberDAO memberDAO, TransactionDAO transactionDAO) {
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
        this.transactionDAO = transactionDAO;
    }

    @Override
    public Book addBook(String isbn, String title, int authorId, String category, int copies, BigDecimal price) throws LibraryException {
        Validator.requireNonBlank(isbn, "ISBN");
        Validator.requireNonBlank(title, "Title");
        Validator.requirePositive(copies, "Copies");

        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .authorId(authorId)
                .category(category)
                .totalCopies(copies)
                .availableCopies(copies)
                .price(price == null ? BigDecimal.ZERO : price)
                .build();
        return bookDAO.save(book);
    }

    @Override
    public List<Book> searchBooks(String keyword) {
        return bookDAO.searchByTitle(keyword);
    }

    @Override
    public Member registerMember(String name, String email, String phone, Member.MembershipType type) throws LibraryException {
        Validator.requireNonBlank(name, "Name");
        Validator.requireValidEmail(email);

        Member member = new Member();
        member.setName(name);
        member.setEmail(email);
        member.setPhone(phone);
        member.setMembershipType(type == null ? Member.MembershipType.GENERAL : type);
        member.setJoinedOn(LocalDate.now());
        member.setActive(true);
        return memberDAO.save(member);
    }

    /**
     * Issue-a-book use case. Order of checks matters:
     *   1. Member must exist and be active.
     *   2. Member must not have exceeded their tier's borrow limit.
     *   3. Book must exist and have at least one available copy.
     * Only after all three pass do we (a) decrement available_copies and
     * (b) write the transaction row - this keeps the two tables consistent
     * even if a check fails halfway through.
     */
    @Override
    public Transaction issueBook(int bookId, int memberId) throws LibraryException {
        Member member = memberDAO.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.isActive()) {
            throw new InvalidTransactionException("Member " + member.getName() + " is not an active member");
        }

        int activeBorrows = memberDAO.countActiveBorrowsByMember(memberId);
        if (activeBorrows >= member.getMaxBooksAllowed()) {
            throw new BorrowLimitExceededException(member.getName(), member.getMaxBooksAllowed());
        }

        Book book = bookDAO.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        if (!book.isAvailable()) {
            throw new BookNotAvailableException(book.getTitle());
        }

        boolean decremented = bookDAO.updateAvailableCopies(bookId, -1);
        if (!decremented) {
            // Guards against a race where another thread issued the last
            // copy between our availability check and this update.
            throw new BookNotAvailableException(book.getTitle());
        }

        Transaction txn = new Transaction();
        txn.setBookId(bookId);
        txn.setMemberId(memberId);
        txn.setIssueDate(LocalDate.now());
        txn.setDueDate(LocalDate.now().plusDays(LOAN_PERIOD_DAYS));
        txn.setFineAmount(BigDecimal.ZERO);
        txn.setStatus(Transaction.Status.ISSUED);
        return transactionDAO.save(txn);
    }

    /**
     * Return-a-book use case: computes any overdue fine, marks the
     * transaction RETURNED, and restores the book's available copy count.
     */
    @Override
    public Transaction returnBook(int transactionId) throws LibraryException {
        Transaction txn = transactionDAO.findById(transactionId)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found: " + transactionId));

        if (txn.getStatus() == Transaction.Status.RETURNED) {
            throw new InvalidTransactionException("Transaction #" + transactionId + " was already returned");
        }

        txn.setReturnDate(LocalDate.now());
        txn.setFineAmount(calculateFine(txn));
        txn.setStatus(Transaction.Status.RETURNED);

        transactionDAO.update(txn);
        bookDAO.updateAvailableCopies(txn.getBookId(), +1);
        return txn;
    }

    @Override
    public List<Transaction> getOverdueBooks() {
        return transactionDAO.findOverdue();
    }

    /**
     * INR 5/day flat fine for every day past the due date, evaluated against
     * the actual return date if the book has been returned, or "today" if
     * it is still outstanding (useful for showing members a running total).
     */
    @Override
    public BigDecimal calculateFine(Transaction txn) {
        LocalDate effectiveReturn = txn.getReturnDate() != null ? txn.getReturnDate() : LocalDate.now();
        long daysLate = ChronoUnit.DAYS.between(txn.getDueDate(), effectiveReturn);
        if (daysLate <= 0) return BigDecimal.ZERO;
        return FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate));
    }
}

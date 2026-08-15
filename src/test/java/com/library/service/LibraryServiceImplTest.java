package com.library.service;

import com.library.exception.*;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.impl.LibraryServiceImpl;
import com.library.testutil.FakeBookDAO;
import com.library.testutil.FakeMemberDAO;
import com.library.testutil.FakeTransactionDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the service layer, exercising the business rules directly
 * against in-memory fake DAOs (see com.library.testutil). No database,
 * no Mockito - just plain Java test doubles, which keeps the whole test
 * suite runnable with nothing beyond the JDK + JUnit.
 */
class LibraryServiceImplTest {

    private FakeBookDAO bookDAO;
    private FakeMemberDAO memberDAO;
    private FakeTransactionDAO transactionDAO;
    private LibraryServiceImpl service;

    @BeforeEach
    void setUp() {
        bookDAO = new FakeBookDAO();
        memberDAO = new FakeMemberDAO();
        transactionDAO = new FakeTransactionDAO();
        service = new LibraryServiceImpl(bookDAO, memberDAO, transactionDAO);
    }

    private Book seedBook(int copies) {
        Book book = Book.builder()
                .isbn("111-TEST")
                .title("Test Driven Development")
                .authorId(1)
                .category("Technology")
                .totalCopies(copies)
                .availableCopies(copies)
                .price(BigDecimal.TEN)
                .build();
        return bookDAO.save(book);
    }

    private Member seedMember(Member.MembershipType type, boolean active) {
        Member m = new Member();
        m.setName("Test Member");
        m.setEmail("test@example.com");
        m.setMembershipType(type);
        m.setActive(active);
        return memberDAO.save(m);
    }

    @Test
    void issueBook_decrementsAvailableCopies() throws LibraryException {
        Book book = seedBook(2);
        Member member = seedMember(Member.MembershipType.STUDENT, true);

        Transaction txn = service.issueBook(book.getBookId(), member.getMemberId());

        assertEquals(Transaction.Status.ISSUED, txn.getStatus());
        assertEquals(1, bookDAO.findById(book.getBookId()).get().getAvailableCopies());
        assertEquals(LocalDate.now().plusDays(14), txn.getDueDate());
    }

    @Test
    void issueBook_throwsWhenNoCopiesAvailable() {
        Book book = seedBook(0);
        Member member = seedMember(Member.MembershipType.STUDENT, true);

        assertThrows(BookNotAvailableException.class,
                () -> service.issueBook(book.getBookId(), member.getMemberId()));
    }

    @Test
    void issueBook_throwsWhenMemberDoesNotExist() {
        Book book = seedBook(1);
        assertThrows(MemberNotFoundException.class, () -> service.issueBook(book.getBookId(), 999));
    }

    @Test
    void issueBook_throwsWhenBorrowLimitExceeded() {
        Book book = seedBook(5);
        Member member = seedMember(Member.MembershipType.GENERAL, true); // limit = 3
        memberDAO.activeBorrowCounts.put(member.getMemberId(), 3);

        assertThrows(BorrowLimitExceededException.class,
                () -> service.issueBook(book.getBookId(), member.getMemberId()));
    }

    @Test
    void issueBook_throwsWhenMemberInactive() {
        Book book = seedBook(1);
        Member member = seedMember(Member.MembershipType.STUDENT, false);

        assertThrows(InvalidTransactionException.class,
                () -> service.issueBook(book.getBookId(), member.getMemberId()));
    }

    @Test
    void returnBook_onTime_hasZeroFine() throws LibraryException {
        Book book = seedBook(1);
        Member member = seedMember(Member.MembershipType.STUDENT, true);
        Transaction issued = service.issueBook(book.getBookId(), member.getMemberId());

        Transaction returned = service.returnBook(issued.getTransactionId());

        assertEquals(Transaction.Status.RETURNED, returned.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), returned.getFineAmount().setScale(2));
        assertEquals(1, bookDAO.findById(book.getBookId()).get().getAvailableCopies());
    }

    @Test
    void returnBook_late_calculatesFineAtFiveRupeesPerDay() throws LibraryException {
        Book book = seedBook(1);
        Member member = seedMember(Member.MembershipType.STUDENT, true);
        Transaction issued = service.issueBook(book.getBookId(), member.getMemberId());
        // simulate a book that was due 3 days ago
        issued.setDueDate(LocalDate.now().minusDays(3));
        transactionDAO.update(issued);

        Transaction returned = service.returnBook(issued.getTransactionId());

        assertEquals(new BigDecimal("15.00"), returned.getFineAmount().setScale(2));
    }

    @Test
    void returnBook_alreadyReturned_throwsInvalidTransaction() throws LibraryException {
        Book book = seedBook(1);
        Member member = seedMember(Member.MembershipType.STUDENT, true);
        Transaction issued = service.issueBook(book.getBookId(), member.getMemberId());
        service.returnBook(issued.getTransactionId());

        assertThrows(InvalidTransactionException.class, () -> service.returnBook(issued.getTransactionId()));
    }

    @Test
    void registerMember_rejectsInvalidEmail() {
        assertThrows(LibraryException.class,
                () -> service.registerMember("John", "not-an-email", "123", Member.MembershipType.GENERAL));
    }

    @Test
    void registerMember_succeedsWithValidData() throws LibraryException {
        Member m = service.registerMember("John Doe", "john@example.com", "9999999999", Member.MembershipType.FACULTY);
        assertTrue(m.getMemberId() > 0);
        assertEquals(10, m.getMaxBooksAllowed());
    }

    @Test
    void addBook_rejectsZeroCopies() {
        assertThrows(LibraryException.class,
                () -> service.addBook("999", "Some Title", 1, "Fiction", 0, BigDecimal.TEN));
    }
}

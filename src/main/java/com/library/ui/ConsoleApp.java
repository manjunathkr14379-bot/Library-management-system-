package com.library.ui;

import com.library.dao.DAOFactory;
import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.LibraryService;
import com.library.service.impl.LibraryServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * Thin presentation layer - a menu-driven console client for LibraryService.
 * Deliberately kept dumb: no business logic lives here, only input/output
 * and delegation to the service layer.
 */
public class ConsoleApp {

    private final LibraryService libraryService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleApp(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    public static void main(String[] args) {
        LibraryService service = new LibraryServiceImpl(
                DAOFactory.bookDAO(), DAOFactory.memberDAO(), DAOFactory.transactionDAO());
        new ConsoleApp(service).run();
    }

    public void run() {
        System.out.println("=== Library Management System ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> addBook();
                    case "2" -> searchBooks();
                    case "3" -> registerMember();
                    case "4" -> issueBook();
                    case "5" -> returnBook();
                    case "6" -> showOverdue();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (LibraryException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private void printMenu() {
        System.out.println("""

                1. Add Book
                2. Search Books
                3. Register Member
                4. Issue Book
                5. Return Book
                6. Show Overdue Books
                0. Exit
                Choose an option:""");
    }

    private void addBook() throws LibraryException {
        System.out.print("ISBN: ");
        String isbn = scanner.nextLine();
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Author ID: ");
        int authorId = Integer.parseInt(scanner.nextLine());
        System.out.print("Category: ");
        String category = scanner.nextLine();
        System.out.print("Copies: ");
        int copies = Integer.parseInt(scanner.nextLine());
        System.out.print("Price: ");
        BigDecimal price = new BigDecimal(scanner.nextLine());

        Book book = libraryService.addBook(isbn, title, authorId, category, copies, price);
        System.out.println("Added: " + book);
    }

    private void searchBooks() {
        System.out.print("Keyword: ");
        String keyword = scanner.nextLine();
        List<Book> results = libraryService.searchBooks(keyword);
        results.forEach(System.out::println);
        if (results.isEmpty()) System.out.println("No books matched.");
    }

    private void registerMember() throws LibraryException {
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Phone: ");
        String phone = scanner.nextLine();
        System.out.print("Type (STUDENT/FACULTY/GENERAL): ");
        Member.MembershipType type = Member.MembershipType.valueOf(scanner.nextLine().trim().toUpperCase());

        Member member = libraryService.registerMember(name, email, phone, type);
        System.out.println("Registered: " + member);
    }

    private void issueBook() throws LibraryException {
        System.out.print("Book ID: ");
        int bookId = Integer.parseInt(scanner.nextLine());
        System.out.print("Member ID: ");
        int memberId = Integer.parseInt(scanner.nextLine());

        Transaction txn = libraryService.issueBook(bookId, memberId);
        System.out.println("Issued: " + txn);
    }

    private void returnBook() throws LibraryException {
        System.out.print("Transaction ID: ");
        int txnId = Integer.parseInt(scanner.nextLine());

        Transaction txn = libraryService.returnBook(txnId);
        System.out.println("Returned: " + txn);
    }

    private void showOverdue() {
        List<Transaction> overdue = libraryService.getOverdueBooks();
        overdue.forEach(System.out::println);
        if (overdue.isEmpty()) System.out.println("No overdue books.");
    }
}

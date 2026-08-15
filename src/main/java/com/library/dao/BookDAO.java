package com.library.dao;

import com.library.model.Book;

import java.util.List;
import java.util.Optional;

/**
 * DAO layer contract for Book persistence.
 * Kept as an interface so the service layer depends on an abstraction,
 * not a concrete JDBC implementation - this is what makes the service
 * layer unit-testable with a fake/mock DAO (see BookDAOFake in tests).
 */
public interface BookDAO {
    Book save(Book book);
    Optional<Book> findById(int bookId);
    List<Book> findAll();
    List<Book> searchByTitle(String keyword);
    List<Book> findByCategory(String category);
    boolean updateAvailableCopies(int bookId, int delta);
    boolean delete(int bookId);
}

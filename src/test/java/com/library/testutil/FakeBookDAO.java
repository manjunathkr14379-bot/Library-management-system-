package com.library.testutil;

import com.library.dao.BookDAO;
import com.library.model.Book;

import java.util.*;

/**
 * In-memory fake implementing the BookDAO contract. Used only by unit tests
 * so LibraryServiceImpl's business logic can be tested in isolation, with
 * no MySQL instance required - this is the payoff of coding to interfaces
 * in the DAO layer.
 */
public class FakeBookDAO implements BookDAO {
    private final Map<Integer, Book> store = new HashMap<>();
    private int nextId = 1;

    @Override
    public Book save(Book book) {
        book.setBookId(nextId++);
        store.put(book.getBookId(), book);
        return book;
    }

    @Override
    public Optional<Book> findById(int bookId) {
        return Optional.ofNullable(store.get(bookId));
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Book> searchByTitle(String keyword) {
        return store.values().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    @Override
    public List<Book> findByCategory(String category) {
        return store.values().stream().filter(b -> b.getCategory().equals(category)).toList();
    }

    @Override
    public boolean updateAvailableCopies(int bookId, int delta) {
        Book b = store.get(bookId);
        if (b == null) return false;
        int newVal = b.getAvailableCopies() + delta;
        if (newVal < 0 || newVal > b.getTotalCopies()) return false;
        b.setAvailableCopies(newVal);
        return true;
    }

    @Override
    public boolean delete(int bookId) {
        return store.remove(bookId) != null;
    }
}

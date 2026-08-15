package com.library.dao.impl;

import com.library.config.ConnectionPool;
import com.library.dao.BookDAO;
import com.library.exception.DataAccessException;
import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookDAOImpl implements BookDAO {

    private static final String INSERT =
            "INSERT INTO books (isbn, title, author_id, category, total_copies, available_copies, price) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BASE =
            "SELECT b.book_id, b.isbn, b.title, b.author_id, a.name AS author_name, b.category, " +
            "b.total_copies, b.available_copies, b.price, b.added_on " +
            "FROM books b JOIN authors a ON b.author_id = a.author_id ";

    @Override
    public Book save(Book book) {
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setInt(3, book.getAuthorId());
            ps.setString(4, book.getCategory());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getAvailableCopies());
            ps.setBigDecimal(7, book.getPrice());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) book.setBookId(keys.getInt(1));
            }
            return book;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save book: " + book.getTitle(), e);
        }
    }

    @Override
    public Optional<Book> findById(int bookId) {
        String sql = SELECT_BASE + "WHERE b.book_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch book id=" + bookId, e);
        }
    }

    @Override
    public List<Book> findAll() {
        return runListQuery(SELECT_BASE + "ORDER BY b.title", ps -> {});
    }

    @Override
    public List<Book> searchByTitle(String keyword) {
        return runListQuery(SELECT_BASE + "WHERE b.title LIKE ?", ps -> ps.setString(1, "%" + keyword + "%"));
    }

    @Override
    public List<Book> findByCategory(String category) {
        return runListQuery(SELECT_BASE + "WHERE b.category = ?", ps -> ps.setString(1, category));
    }

    @Override
    public boolean updateAvailableCopies(int bookId, int delta) {
        // Atomic in-place update guarded by a WHERE clause that prevents the
        // count from ever going negative - avoids a separate check-then-act
        // race condition when two threads issue the last copy simultaneously.
        String sql = "UPDATE books SET available_copies = available_copies + ? " +
                     "WHERE book_id = ? AND available_copies + ? >= 0";
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, bookId);
            ps.setInt(3, delta);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update copies for book id=" + bookId, e);
        }
    }

    @Override
    public boolean delete(int bookId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete book id=" + bookId, e);
        }
    }

    private List<Book> runListQuery(String sql, SqlBinder binder) {
        List<Book> books = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) books.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
        return books;
    }

    private Book map(ResultSet rs) throws SQLException {
        return Book.builder()
                .bookId(rs.getInt("book_id"))
                .isbn(rs.getString("isbn"))
                .title(rs.getString("title"))
                .authorId(rs.getInt("author_id"))
                .authorName(rs.getString("author_name"))
                .category(rs.getString("category"))
                .totalCopies(rs.getInt("total_copies"))
                .availableCopies(rs.getInt("available_copies"))
                .price(rs.getBigDecimal("price"))
                .addedOn(rs.getDate("added_on").toLocalDate())
                .build();
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}

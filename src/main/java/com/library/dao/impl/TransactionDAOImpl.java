package com.library.dao.impl;

import com.library.config.ConnectionPool;
import com.library.dao.TransactionDAO;
import com.library.exception.DataAccessException;
import com.library.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionDAOImpl implements TransactionDAO {

    private static final String SELECT_BASE =
            "SELECT t.transaction_id, t.book_id, b.title AS book_title, t.member_id, m.name AS member_name, " +
            "t.issue_date, t.due_date, t.return_date, t.fine_amount, t.status " +
            "FROM transactions t " +
            "JOIN books b ON t.book_id = b.book_id " +
            "JOIN members m ON t.member_id = m.member_id ";

    @Override
    public Transaction save(Transaction txn) {
        String sql = "INSERT INTO transactions (book_id, member_id, issue_date, due_date, status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, txn.getBookId());
            ps.setInt(2, txn.getMemberId());
            ps.setDate(3, Date.valueOf(txn.getIssueDate()));
            ps.setDate(4, Date.valueOf(txn.getDueDate()));
            ps.setString(5, txn.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) txn.setTransactionId(keys.getInt(1));
            }
            return txn;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save transaction", e);
        }
    }

    @Override
    public Optional<Transaction> findById(int transactionId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "WHERE t.transaction_id = ?")) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch transaction id=" + transactionId, e);
        }
    }

    @Override
    public List<Transaction> findActiveByMember(int memberId) {
        return runListQuery(SELECT_BASE + "WHERE t.member_id = ? AND t.status = 'ISSUED'",
                ps -> ps.setInt(1, memberId));
    }

    @Override
    public List<Transaction> findAllActive() {
        return runListQuery(SELECT_BASE + "WHERE t.status = 'ISSUED' ORDER BY t.due_date", ps -> {});
    }

    @Override
    public List<Transaction> findOverdue() {
        return runListQuery(SELECT_BASE + "WHERE t.status = 'ISSUED' AND t.due_date < CURDATE()", ps -> {});
    }

    @Override
    public boolean update(Transaction txn) {
        String sql = "UPDATE transactions SET return_date = ?, fine_amount = ?, status = ? WHERE transaction_id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (txn.getReturnDate() != null) ps.setDate(1, Date.valueOf(txn.getReturnDate()));
            else ps.setNull(1, Types.DATE);
            ps.setBigDecimal(2, txn.getFineAmount());
            ps.setString(3, txn.getStatus().name());
            ps.setInt(4, txn.getTransactionId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update transaction id=" + txn.getTransactionId(), e);
        }
    }

    private List<Transaction> runListQuery(String sql, SqlBinder binder) {
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
        return list;
    }

    private Transaction map(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("transaction_id"));
        t.setBookId(rs.getInt("book_id"));
        t.setBookTitle(rs.getString("book_title"));
        t.setMemberId(rs.getInt("member_id"));
        t.setMemberName(rs.getString("member_name"));
        t.setIssueDate(rs.getDate("issue_date").toLocalDate());
        t.setDueDate(rs.getDate("due_date").toLocalDate());
        Date ret = rs.getDate("return_date");
        t.setReturnDate(ret == null ? null : ret.toLocalDate());
        t.setFineAmount(rs.getBigDecimal("fine_amount"));
        t.setStatus(Transaction.Status.valueOf(rs.getString("status")));
        return t;
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}

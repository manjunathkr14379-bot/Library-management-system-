package com.library.testutil;

import com.library.dao.TransactionDAO;
import com.library.model.Transaction;

import java.util.*;

public class FakeTransactionDAO implements TransactionDAO {
    private final Map<Integer, Transaction> store = new HashMap<>();
    private int nextId = 1;

    @Override
    public Transaction save(Transaction transaction) {
        transaction.setTransactionId(nextId++);
        store.put(transaction.getTransactionId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(int transactionId) {
        return Optional.ofNullable(store.get(transactionId));
    }

    @Override
    public List<Transaction> findActiveByMember(int memberId) {
        return store.values().stream()
                .filter(t -> t.getMemberId() == memberId && t.getStatus() == Transaction.Status.ISSUED)
                .toList();
    }

    @Override
    public List<Transaction> findAllActive() {
        return store.values().stream().filter(t -> t.getStatus() == Transaction.Status.ISSUED).toList();
    }

    @Override
    public List<Transaction> findOverdue() {
        return store.values().stream()
                .filter(t -> t.getStatus() == Transaction.Status.ISSUED && t.getDueDate().isBefore(java.time.LocalDate.now()))
                .toList();
    }

    @Override
    public boolean update(Transaction transaction) {
        if (!store.containsKey(transaction.getTransactionId())) return false;
        store.put(transaction.getTransactionId(), transaction);
        return true;
    }
}

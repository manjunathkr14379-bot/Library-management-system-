package com.library.dao;

import com.library.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionDAO {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(int transactionId);
    List<Transaction> findActiveByMember(int memberId);
    List<Transaction> findAllActive();
    List<Transaction> findOverdue();
    boolean update(Transaction transaction);
}

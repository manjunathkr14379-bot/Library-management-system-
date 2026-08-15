package com.library.dao;

import com.library.dao.impl.BookDAOImpl;
import com.library.dao.impl.MemberDAOImpl;
import com.library.dao.impl.TransactionDAOImpl;

/**
 * Simple Factory: centralizes which concrete DAO implementation the app
 * wires up. Swapping BookDAOImpl for, say, a PostgresBookDAOImpl later
 * only requires a change here.
 */
public final class DAOFactory {
    private DAOFactory() { }

    public static BookDAO bookDAO() { return new BookDAOImpl(); }
    public static MemberDAO memberDAO() { return new MemberDAOImpl(); }
    public static TransactionDAO transactionDAO() { return new TransactionDAOImpl(); }
}

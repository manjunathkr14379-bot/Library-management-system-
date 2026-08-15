package com.library.config;

import com.library.exception.DataAccessException;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Minimal thread-safe JDBC connection pool, implemented from scratch
 * (no HikariCP/C3P0 dependency) so the whole project builds with nothing
 * but the JDK + the MySQL driver jar.
 *
 * Design:
 *  - Eagerly creates `poolSize` real connections at startup.
 *  - Hands them out via a BlockingQueue (borrow = poll, release = offer)
 *    which gives free thread-safety without hand-written locks.
 *  - Each borrowed connection is wrapped in a dynamic Proxy so that
 *    application code calling connection.close() actually RETURNS the
 *    connection to the pool instead of closing the socket - callers can
 *    keep using try-with-resources idiomatically.
 *
 * Implemented as a Singleton because a connection pool is an expensive,
 * shared resource - the application should have exactly one.
 */
public final class ConnectionPool {

    private static volatile ConnectionPool instance;

    private final BlockingQueue<Connection> pool;
    private final int poolSize;

    private ConnectionPool(int poolSize) {
        this.poolSize = poolSize;
        this.pool = new ArrayBlockingQueue<>(poolSize);
        Properties props = loadProperties();
        try {
            Class.forName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");
            for (int i = 0; i < poolSize; i++) {
                pool.offer(DriverManager.getConnection(url, user, pass));
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new DataAccessException("Failed to initialize connection pool", e);
        }
    }

    public static ConnectionPool getInstance() {
        if (instance == null) {
            synchronized (ConnectionPool.class) {
                if (instance == null) {
                    instance = new ConnectionPool(loadPoolSize());
                }
            }
        }
        return instance;
    }

    private static int loadPoolSize() {
        Properties props = loadProperties();
        return Integer.parseInt(props.getProperty("db.pool.size", "5"));
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = ConnectionPool.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) throw new DataAccessException("db.properties not found on classpath", null);
            props.load(in);
        } catch (Exception e) {
            throw new DataAccessException("Could not load db.properties", e);
        }
        return props;
    }

    /**
     * Borrow a connection, waiting up to 5s if the pool is exhausted.
     * The returned Connection is a proxy: calling close() releases it
     * back to the pool rather than closing the underlying socket.
     */
    public Connection getConnection() {
        try {
            Connection real = pool.poll(5, TimeUnit.SECONDS);
            if (real == null) {
                throw new DataAccessException("Timed out waiting for a free DB connection (pool size=" + poolSize + ")", null);
            }
            return wrap(real);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataAccessException("Interrupted while waiting for a DB connection", e);
        }
    }

    private Connection wrap(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        pool.offer(real);
                        return null;
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (java.lang.reflect.InvocationTargetException ite) {
                        throw ite.getCause();
                    }
                });
    }

    /** Called once on application shutdown to release real sockets. */
    public void shutdown() {
        pool.forEach(c -> {
            try { c.close(); } catch (SQLException ignored) { }
        });
        pool.clear();
    }
}

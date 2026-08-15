-- ============================================================
-- Library Management System - Database Schema
-- Author: Manjunath K R
-- Engine: MySQL 8.x
-- ============================================================

DROP DATABASE IF EXISTS library_management;
CREATE DATABASE library_management CHARACTER SET utf8mb4;
USE library_management;

-- ------------------------------------------------------------
-- Table: authors  (normalized out of books -> avoids repeating
-- author name/nationality on every row, 2NF/3NF compliant)
-- ------------------------------------------------------------
CREATE TABLE authors (
    author_id     INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    nationality   VARCHAR(60)
);

-- ------------------------------------------------------------
-- Table: books
-- ------------------------------------------------------------
CREATE TABLE books (
    book_id         INT AUTO_INCREMENT PRIMARY KEY,
    isbn            VARCHAR(20) NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    author_id       INT NOT NULL,
    category        VARCHAR(60) NOT NULL,
    total_copies    INT NOT NULL DEFAULT 1 CHECK (total_copies >= 0),
    available_copies INT NOT NULL DEFAULT 1 CHECK (available_copies >= 0),
    price           DECIMAL(8,2) DEFAULT 0.00,
    added_on        DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_books_author FOREIGN KEY (author_id) REFERENCES authors(author_id),
    CONSTRAINT chk_copies CHECK (available_copies <= total_copies)
);

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_category ON books(category);

-- ------------------------------------------------------------
-- Table: members
-- ------------------------------------------------------------
CREATE TABLE members (
    member_id       INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    email           VARCHAR(120) NOT NULL UNIQUE,
    phone           VARCHAR(15),
    membership_type ENUM('STUDENT', 'FACULTY', 'GENERAL') NOT NULL DEFAULT 'GENERAL',
    joined_on       DATE NOT NULL DEFAULT (CURRENT_DATE),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

-- ------------------------------------------------------------
-- Table: transactions  (issue / return log — the audit trail)
-- ------------------------------------------------------------
CREATE TABLE transactions (
    transaction_id  INT AUTO_INCREMENT PRIMARY KEY,
    book_id         INT NOT NULL,
    member_id       INT NOT NULL,
    issue_date      DATE NOT NULL,
    due_date        DATE NOT NULL,
    return_date     DATE NULL,
    fine_amount     DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    status          ENUM('ISSUED', 'RETURNED', 'OVERDUE') NOT NULL DEFAULT 'ISSUED',
    CONSTRAINT fk_txn_book FOREIGN KEY (book_id) REFERENCES books(book_id),
    CONSTRAINT fk_txn_member FOREIGN KEY (member_id) REFERENCES members(member_id)
);

CREATE INDEX idx_txn_member ON transactions(member_id);
CREATE INDEX idx_txn_status ON transactions(status);

-- ------------------------------------------------------------
-- View: currently issued books with borrower detail
-- (demonstrates JOIN + view usage for reporting)
-- ------------------------------------------------------------
CREATE VIEW active_loans AS
SELECT t.transaction_id, b.title, a.name AS author, m.name AS member,
       t.issue_date, t.due_date,
       DATEDIFF(CURDATE(), t.due_date) AS days_overdue
FROM transactions t
JOIN books b ON t.book_id = b.book_id
JOIN authors a ON b.author_id = a.author_id
JOIN members m ON t.member_id = m.member_id
WHERE t.status = 'ISSUED';

-- ------------------------------------------------------------
-- Seed data
-- ------------------------------------------------------------
INSERT INTO authors (name, nationality) VALUES
('R.K. Narayan', 'Indian'),
('George Orwell', 'British'),
('Robert C. Martin', 'American'),
('Yuval Noah Harari', 'Israeli');

INSERT INTO books (isbn, title, author_id, category, total_copies, available_copies, price) VALUES
('978-0140185878', 'Malgudi Days', 1, 'Fiction', 4, 4, 250.00),
('978-0451524935', '1984', 2, 'Fiction', 5, 5, 399.00),
('978-0132350884', 'Clean Code', 3, 'Technology', 3, 3, 799.00),
('978-0062316097', 'Sapiens', 4, 'Non-Fiction', 6, 6, 550.00);

INSERT INTO members (name, email, phone, membership_type) VALUES
('Manjunath K R', 'manjunathkr14379@gmail.com', '9148948936', 'STUDENT'),
('Asha Rao', 'asha.rao@example.com', '9900011122', 'FACULTY');

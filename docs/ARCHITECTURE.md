# Architecture & Design Decisions

This document explains *why* the project is structured the way it is — useful both as
documentation and as interview prep (an interviewer will often ask "why did you do X
instead of Y").

## 1. Layered architecture

```
ConsoleApp  →  LibraryService  →  BookDAO / MemberDAO / TransactionDAO  →  ConnectionPool  →  MySQL
```

**Why layers at all?** Separation of concerns. The UI shouldn't know SQL exists; the DAO
shouldn't know what a "borrow limit" is. Each layer has exactly one reason to change:

- `ConsoleApp` changes if the *interface* changes (console → REST API, say)
- `LibraryServiceImpl` changes if a *business rule* changes (loan period, fine rate)
- `BookDAOImpl` changes if the *schema or database* changes

**Why interfaces for the DAO layer?** So the service layer depends on an abstraction
(`BookDAO`), not a concrete class (`BookDAOImpl`). This is the Dependency Inversion
Principle, and it's what makes `LibraryServiceImplTest` possible: the test constructs
`LibraryServiceImpl` with `FakeBookDAO` / `FakeMemberDAO` / `FakeTransactionDAO` instead
of the real JDBC classes. No test database, no mocking framework, no network — the whole
suite runs in under 300ms.

## 2. Database design

- **3NF normalization**: `authors` is a separate table from `books` rather than storing
  `author_name` directly on every book row. If two books share an author, we don't
  duplicate the author's data, and updating an author's name is a single-row update.
- **Foreign keys** (`books.author_id → authors`, `transactions.book_id → books`,
  `transactions.member_id → members`) enforce referential integrity at the database
  level — you cannot issue a transaction for a book that doesn't exist, even if
  application code has a bug.
- **CHECK constraints** (`available_copies <= total_copies`) are a second line of defense
  beyond application validation.
- **Indexes** on `books.title`, `books.category`, `transactions.member_id`, and
  `transactions.status` — the columns actually used in `WHERE` clauses — keep search and
  the overdue report fast as the table grows.
- **`active_loans` view** demonstrates using the database for reporting via a `JOIN`
  across three tables (transactions, books, members) rather than pulling all rows into
  Java and joining in memory.

## 3. Why a hand-rolled connection pool instead of HikariCP

The honest reason: no external dependency management (no Maven Central access on some
setups; wanted the project buildable with nothing but `javac`) — but the real value is
what it teaches. `ConnectionPool` (in `config/`) demonstrates understanding of what a
pool actually does, rather than just adding a library:

- Eagerly opens `poolSize` connections once at startup (`DriverManager.getConnection`
  is expensive — a TCP handshake + auth round-trip — so you don't want to pay that cost
  per request).
- Uses a `BlockingQueue<Connection>` as the pool storage. `poll()`/`offer()` are already
  thread-safe, so no hand-written locks are needed to hand connections to concurrent
  callers.
- Wraps each borrowed connection in a `java.lang.reflect.Proxy` that intercepts
  `close()`. Calling code still does `try (Connection c = pool.getConnection())`
  idiomatically — but `close()` **returns the connection to the pool** instead of
  actually closing the socket. This is exactly the trick real pooling libraries use.

In an interview, this is worth walking through on a whiteboard — it's a self-contained
system-design mini-topic (bounded resource pools, thread safety without locks, the
proxy/decorator pattern).

## 4. Business rules — where they live and why

All business rules live in `LibraryServiceImpl`, never in the DAO or the UI:

- **Borrow limits by membership tier** (`Member.getMaxBooksAllowed()` — Student: 5,
  Faculty: 10, General: 3): a small piece of logic that's arguably "the member's own
  business", so it lives as a method on the `Member` domain model itself rather than as
  a `switch` buried in the service.
- **Issue-book ordering of checks** (`issueBook`): member exists → member active →
  under borrow limit → book exists → book available → *then* mutate state. Checking
  everything before mutating anything avoids leaving the two tables (`books`,
  `transactions`) inconsistent if a later check fails.
- **Race-condition guard**: `BookDAOImpl.updateAvailableCopies` uses a single atomic
  `UPDATE ... WHERE available_copies + ? >= 0` rather than "SELECT available_copies,
  check in Java, then UPDATE" — the classic check-then-act race where two members could
  both "see" the last copy as available and both issue it. The database enforces the
  invariant, not application code.
- **Fine calculation** (`calculateFine`): ₹5/day flat rate, computed from `dueDate` to
  either `returnDate` (if returned) or `LocalDate.now()` (if still outstanding, so a
  member can be shown a running total before returning). Isolated in its own method so
  it's independently unit-tested and easy to change (e.g., to a tiered rate) later.

## 5. Exception design

- `LibraryException` — checked base class for all *expected* domain failures (book not
  found, borrow limit exceeded, invalid input). Checked, deliberately: callers (the UI)
  are forced to handle "book not available" as a normal flow, not an afterthought.
- `DataAccessException` — unchecked, wraps `SQLException`. Infrastructure failures
  (DB down, bad query) aren't something calling code can usually recover from
  meaningfully, so they propagate as a `RuntimeException` up to a top-level handler
  instead of forcing a `throws SQLException` on every DAO method signature.
- Specific subclasses (`BookNotAvailableException`, `BorrowLimitExceededException`, …)
  instead of one generic exception with a message — lets calling code branch on
  `catch (BookNotAvailableException e)` if it ever needs to (e.g., to offer a "reserve
  this book" flow specifically for that case).

## 6. Design patterns used (interview talking points)

| Pattern | Where | Why |
|---|---|---|
| **Builder** | `Book.Builder` | `Book` has several optional fields (price, addedOn); a builder avoids a 7-argument constructor and reads clearly at call sites |
| **Singleton** | `ConnectionPool.getInstance()` | A connection pool is an expensive shared resource — exactly one should exist per application, guarded with double-checked locking |
| **Simple Factory** | `DAOFactory` | Centralizes which concrete DAO implementation gets wired up; swapping databases later means editing one file |
| **Proxy** | `ConnectionPool.wrap()` | Intercepts `close()` on a `Connection` to redirect it back into the pool instead of actually closing it |
| **Dependency Injection (constructor)** | `LibraryServiceImpl(BookDAO, MemberDAO, TransactionDAO)` | Makes the service testable in isolation; no `new BookDAOImpl()` buried inside business logic |
| **DAO pattern** | `dao` package | Isolates persistence/SQL details behind an interface so the rest of the app is storage-agnostic |

## 7. Testing strategy

17 JUnit 5 tests in `LibraryServiceImplTest` and `ValidatorTest`, run against in-memory
fake DAOs — deliberately **not** Mockito, to show the underlying idea (a "fake" is just
another implementation of the same interface) rather than leaning on a mocking
framework to paper over a design that wasn't actually testable.

Representative cases covered:
- Issuing a book decrements available copies and sets a 14-day due date
- Issuing fails when copies are exhausted, the member doesn't exist, is inactive, or has
  hit their borrow limit
- Returning on time produces zero fine; returning late computes the correct ₹5/day fine
- Returning an already-returned transaction throws `InvalidTransactionException`
- Registering a member rejects malformed email addresses
- Adding a book rejects a non-positive copy count

## 8. What I'd add with more time

- Wrap `issueBook`/`returnBook` in an explicit JDBC transaction
  (`conn.setAutoCommit(false)` across the copies-update + transaction-insert) rather
  than relying on two separate auto-committed statements — currently a crash between
  the two leaves a small inconsistency window.
- A REST API (Spring Boot) in front of the same `LibraryService` interface, unchanged.
- Pagination on `findAll()`/`searchByTitle()` for large catalogs.
- A scheduled job to auto-mark `ISSUED` transactions `OVERDUE` and send reminders.

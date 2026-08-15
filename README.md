# Library Management System

A layered, production-style Library Management System built in core Java + JDBC + MySQL.
Built to demonstrate real backend engineering practices — not just CRUD screens — for
technical interviews: clean architecture, custom connection pooling, transaction-safe
book issuing, a fine-calculation engine, and a unit-tested service layer.

## Why this exists

Most student "library management system" projects are a single class with SQL scattered
through button handlers. This one is deliberately structured the way a real backend
service would be: separated layers, an interface-driven DAO layer, centralized business
rules, custom exceptions, and tests that run without touching a database.

## Features

- **Book catalog** — add, search by title/category, track total vs. available copies
- **Member management** — tiered membership (Student / Faculty / General) with different
  borrow limits per tier
- **Issue / return workflow** — atomic availability checks, borrow-limit enforcement,
  automatic due-date calculation (14-day loan period)
- **Fine engine** — ₹5/day flat fine computed off the due date, works for both returned
  and still-outstanding books
- **Overdue report** — SQL view (`active_loans`) + a `findOverdue()` query for at-a-glance
  reporting
- **Unit tests** — 17 JUnit 5 tests covering the business rules in `LibraryServiceImpl`,
  run against in-memory fake DAOs (no database required to run the test suite)

## Architecture

```
UI (ConsoleApp)
     │
     ▼
Service layer  (LibraryService / LibraryServiceImpl)   ← business rules live here only
     │
     ▼
DAO layer      (BookDAO, MemberDAO, TransactionDAO + JDBC impls)
     │
     ▼
ConnectionPool (hand-rolled, javax.sql-free pooling over java.sql.Connection)
     │
     ▼
MySQL
```

Each layer only knows about the layer directly below it, and only through an interface.
That's what makes `LibraryServiceImplTest` possible without a live database — the test
swaps in `FakeBookDAO` / `FakeMemberDAO` / `FakeTransactionDAO` (plain in-memory
`Map`-backed implementations of the same interfaces the real JDBC classes implement).

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full design write-up,
including the schema (ER-style), design patterns used, and the reasoning behind each
decision — this is also the doc to read before an interview about this project.

## Tech stack

| Layer          | Choice                                   |
|----------------|-------------------------------------------|
| Language       | Java 17+ (uses records-free modern syntax, switch expressions, `LocalDate`) |
| Persistence    | JDBC (`java.sql`) + MySQL 8               |
| Connection mgmt| Custom pool (`ConnectionPool`) — no HikariCP dependency |
| Testing        | JUnit 5, hand-written test doubles (no Mockito) |
| Build          | Plain `javac` via `build.sh` (no Maven/Gradle required) |

No external dependency jars are required to **compile and test** the project — only the
JDK. The one runtime dependency is the MySQL Connector/J jar, needed only when actually
connecting to a live MySQL instance (`build.sh run`).

## Project layout

```
LibraryManagementSystem/
├── sql/schema.sql                     # DDL + seed data
├── src/main/java/com/library/
│   ├── model/                         # Book, Member, Transaction (domain objects)
│   ├── dao/                           # DAO interfaces + DAOFactory
│   ├── dao/impl/                      # JDBC implementations
│   ├── service/                       # LibraryService interface
│   ├── service/impl/                  # LibraryServiceImpl (business rules)
│   ├── exception/                     # Custom checked/unchecked exception hierarchy
│   ├── config/                        # ConnectionPool (custom pooling)
│   ├── util/                          # Validator
│   └── ui/                            # ConsoleApp (menu-driven client)
├── src/test/java/com/library/
│   ├── testutil/                      # FakeBookDAO, FakeMemberDAO, FakeTransactionDAO
│   ├── service/LibraryServiceImplTest.java
│   └── util/ValidatorTest.java
├── build.sh                           # compile / test / run, no Maven needed
└── docs/ARCHITECTURE.md               # design write-up
```

## Getting started

### 1. Set up the database
```bash
mysql -u root -p < sql/schema.sql
```
Then update `src/main/resources/db.properties` with your MySQL username/password.

### 2. Run the unit tests (no database needed)
```bash
# Debian/Ubuntu: sudo apt-get install junit5
./build.sh test
```

### 3. Run the app (needs MySQL running + Connector/J jar)
```bash
# download mysql-connector-j-<version>.jar from dev.mysql.com, then:
MYSQL_JAR=/path/to/mysql-connector-j-8.x.x.jar ./build.sh run
```

## Sample interaction

```
=== Library Management System ===

1. Add Book
2. Search Books
3. Register Member
4. Issue Book
5. Return Book
6. Show Overdue Books
0. Exit
Choose an option: 4
Book ID: 3
Member ID: 1
Issued: Txn#1 | Clean Code -> Manjunath K R | issued:2026-08-15 due:2026-08-29 | fine:0.00 | ISSUED
```

## Possible extensions

- REST API layer (Spring Boot or plain `com.sun.net.httpserver`) in front of the same
  service layer
- Pagination for `findAll()` queries
- Role-based access (librarian vs. member logins)
- Email/SMS reminder job for books nearing their due date

## Author

**Manjunath K R** — B.E. Computer Science, Sai Vidya Institute of Technology

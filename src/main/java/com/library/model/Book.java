package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model for a Book.
 * Uses the Builder pattern instead of a telescoping constructor -
 * makes object creation readable when a class has many optional fields.
 */
public final class Book {
    private int bookId;
    private final String isbn;
    private final String title;
    private final int authorId;
    private final String authorName;   // populated via JOIN, not stored redundantly by DAO writes
    private final String category;
    private int totalCopies;
    private int availableCopies;
    private final BigDecimal price;
    private final LocalDate addedOn;

    private Book(Builder b) {
        this.bookId = b.bookId;
        this.isbn = b.isbn;
        this.title = b.title;
        this.authorId = b.authorId;
        this.authorName = b.authorName;
        this.category = b.category;
        this.totalCopies = b.totalCopies;
        this.availableCopies = b.availableCopies;
        this.price = b.price;
        this.addedOn = b.addedOn;
    }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public int getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
    public BigDecimal getPrice() { return price; }
    public LocalDate getAddedOn() { return addedOn; }

    public boolean isAvailable() { return availableCopies > 0; }

    @Override
    public String toString() {
        return String.format("[%d] %-30s by %-20s | %s | Available: %d/%d",
                bookId, title, authorName == null ? "" : authorName, category, availableCopies, totalCopies);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int bookId;
        private String isbn;
        private String title;
        private int authorId;
        private String authorName;
        private String category;
        private int totalCopies = 1;
        private int availableCopies = 1;
        private BigDecimal price = BigDecimal.ZERO;
        private LocalDate addedOn = LocalDate.now();

        public Builder bookId(int v) { this.bookId = v; return this; }
        public Builder isbn(String v) { this.isbn = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder authorId(int v) { this.authorId = v; return this; }
        public Builder authorName(String v) { this.authorName = v; return this; }
        public Builder category(String v) { this.category = v; return this; }
        public Builder totalCopies(int v) { this.totalCopies = v; return this; }
        public Builder availableCopies(int v) { this.availableCopies = v; return this; }
        public Builder price(BigDecimal v) { this.price = v; return this; }
        public Builder addedOn(LocalDate v) { this.addedOn = v; return this; }

        public Book build() {
            if (title == null || title.isBlank()) throw new IllegalStateException("Title is required");
            if (isbn == null || isbn.isBlank()) throw new IllegalStateException("ISBN is required");
            if (availableCopies > totalCopies) throw new IllegalStateException("availableCopies cannot exceed totalCopies");
            return new Book(this);
        }
    }
}

package com.library_user.model.helper;

public final class ErrorMessages {
    private ErrorMessages() {}
    public static final String BOOK_NOT_FOUND_ID = "Book not found with id: %s";
    public static final String BOOK_NOT_FOUND_ISBN = "Book not found with ISBN: %s";
    public static final String BOOK_ENTITY_NULL = "Book entity cannot be null";
    public static final String BOOK_DTO_NULL = "BookDto cannot be null";
    public static final String BOOK_ALREADY_EXISTS_ISBN = "Book already exists with ISBN: %s";
    public static final String USER_NOT_FOUND_ID = "User not found with id";
    public static final String LIBRARIAN_UPDATE = "Librarian can't update another librarian information's!";
    public static final String USER_NOT_FOUND_EMAIL = "User already exists with email";
    public static final String EMAIL_ALREADY_IN_USE = "Email already taken!";
    public static final String USER_CAN_NOT_BORROW_5 = "The user can borrow a maximum of 5 books.";
    public static final String BOOK_NOT_AVAILABLE = "Book is not available!";
    public static final String BOOK_ALREADY_RETURNED = "Book is already returned!";
    public static final String BORROWING_NOT_FOUND = "Borrowing book not found!";
    public static final String USER_AND_BORROW_ID_NOT_MATCH = "You can only return the book you purchased yourself.";

}

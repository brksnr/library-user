package com.library_user.model.helper;

public final class ErrorMessages {
    private ErrorMessages() {} // Helper class, instance oluşturulamaz

    public static final String BOOK_NOT_FOUND_ID = "Book not found with id: %s";
    public static final String BOOK_NOT_FOUND_ISBN = "Book not found with ISBN: %s";
    public static final String BOOK_ENTITY_NULL = "Book entity cannot be null";
    public static final String BOOK_DTO_NULL = "BookDto cannot be null";
    public static final String BOOK_AND_DTO_NULL = "Book and BookDto cannot be null";
    public static final String BOOK_ALREADY_EXISTS_ISBN = "Book already exists with ISBN: %s";
}

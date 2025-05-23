package com.library_user.service;

import com.library_user.model.dto.BookDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BookService {
    BookDto addBook(BookDto bookDto);
    BookDto updateBook(UUID id, BookDto bookDto);
    void deleteBook(UUID id);
    BookDto getBookById(UUID id);
    BookDto getBookByIsbn(String isbn);
    BookDto updateBookAvailability(UUID id, boolean availability);
}
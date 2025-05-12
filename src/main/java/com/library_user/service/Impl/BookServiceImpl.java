package com.library_user.service.Impl;

import com.library_user.exceptions.CustomException;
import com.library_user.model.dto.BookDto;
import com.library_user.model.entity.Book;
import com.library_user.helper.ErrorMessages;
import com.library_user.model.mapper.BookMapper;
import com.library_user.repository.BookRepository;
import com.library_user.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     Checks if a book with the given ISBN already exists.
     */
    private void checkBookExistsByIsbn(String isbn) {
        if (bookRepository.findByIsbn(isbn).isPresent()) {
            throw new CustomException(
                    String.format(ErrorMessages.BOOK_ALREADY_EXISTS_ISBN, isbn),
                    HttpStatus.CONFLICT
            );
        }
    }

    /**
     Adds a new book to the database. Throws exception if ISBN already exists.
      */
    @Override
    public BookDto addBook(BookDto bookDto) {
        checkBookExistsByIsbn(bookDto.getIsbn());
        Book book = BookMapper.toEntity(bookDto);
        Book saved = bookRepository.save(book);
        return BookMapper.toDto(saved);
    }

    /**
     Updates the details of an existing book. Throws exception if book not found.
      */
    @Override
    public BookDto updateBook(UUID id, BookDto bookDto) {
        Book book = findByIdOrThrow(id);
        BookMapper.updateEntity(book, bookDto);
        Book updated = bookRepository.save(book);
        return BookMapper.toDto(updated);
    }

    /**
     Deletes a book by ID. Throws exception if book does not exist.
      */
    @Override
    public void deleteBook(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw new CustomException(
                    String.format(ErrorMessages.BOOK_NOT_FOUND_ID, id),
                    HttpStatus.NOT_FOUND
            );
        }
        bookRepository.deleteById(id);
    }

    /**
     Retrieves a book by ID. Throws exception if book not found.
      */
    @Override
    public BookDto getBookById(UUID id) {
        Book book = findByIdOrThrow(id);
        return BookMapper.toDto(book);
    }

    /**
     Retrieves a book by ISBN. Throws exception if book not found.
     * */
    @Override
    public BookDto getBookByIsbn(String isbn) {
        Book book = bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new CustomException(ErrorMessages.BOOK_NOT_FOUND_ISBN + isbn, HttpStatus.NOT_FOUND));
        return BookMapper.toDto(book);
    }

    /**
     Updates the availability status of a book. Throws exception if book not found.
     * */
    @Override
    public BookDto updateBookAvailability(UUID id, boolean availability) {
        Book book = findByIdOrThrow(id);
        book.setAvailability(availability);
        Book updated = bookRepository.save(book);
        return BookMapper.toDto(updated);
    }

    /**
     Searches for books containing the title, returns paginated result.
     * */
    @Override
    public List<BookDto> searchBooksByTitle(String title, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCase(title, pageable)
                .map(BookMapper::toDto)
                .getContent();
    }

    /**
     Searches for books by author's name (case-insensitive), returns paginated result.
     * */
    @Override
    public List<BookDto> searchBooksByAuthor(String author, Pageable pageable) {
        return bookRepository.findByAuthorContainingIgnoreCase(author, pageable)
                .map(BookMapper::toDto)
                .getContent();
    }

    /**
     Searches for books by genre (case-insensitive), returns paginated result.
     * */
    @Override
    public List<BookDto> searchBooksByGenre(String genre, Pageable pageable) {
        return bookRepository.findByGenreContainingIgnoreCase(genre, pageable)
                .map(BookMapper::toDto)
                .getContent();
    }

    /**
     Searches for books by availability status, returns paginated result.
     * */
    @Override
    public List<BookDto> searchBooksByAvailability(boolean availability, Pageable pageable) {
        return bookRepository.findByAvailability(availability, pageable)
                .map(BookMapper::toDto)
                .getContent();
    }

    /**
     Retrieves a book by ID or throws a not found exception
     * */
    public Book findByIdOrThrow(UUID id){
        return bookRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        String.format(ErrorMessages.BOOK_NOT_FOUND_ID, id),
                        HttpStatus.NOT_FOUND
                ));
    }

}

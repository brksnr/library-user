package com.library_user.junit.controller;

import com.library_user.model.dto.BookDto;
import com.library_user.service.BookService;
import com.library_user.controller.BookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerJunitTest {

    @Mock
    private BookService bookService;

    @InjectMocks
    private BookController bookController;

    private BookDto testBookDto;
    private UUID bookId;
    private String testIsbn;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        testIsbn = "978-0743273565";

        testBookDto = BookDto.builder()
                .id(bookId)
                .title("The Great Gatsby")
                .author("F. Scott Fitzgerald")
                .isbn(testIsbn)
                .description("A story of the fabulously wealthy Jay Gatsby")
                .publicationDate(LocalDate.of(1925, 4, 10))
                .genre("Fiction")
                .availability(true)
                .build();
    }

    // Test adding a new book and ensuring it returns the created book
    @Test
    void whenAddBook_thenReturnCreatedBook() {
        when(bookService.addBook(any(BookDto.class))).thenReturn(testBookDto);

        ResponseEntity<BookDto> response = bookController.addBook(testBookDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("The Great Gatsby");
        verify(bookService).addBook(any(BookDto.class));
    }

    // Test updating an existing book and ensuring the updated book is returned
    @Test
    void whenUpdateBook_thenReturnUpdatedBook() {
        BookDto updatedDto = BookDto.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn(testIsbn)
                .description("Updated Description")
                .publicationDate(LocalDate.now())
                .genre("Updated Genre")
                .availability(false)
                .build();
        when(bookService.updateBook(eq(bookId), any(BookDto.class))).thenReturn(updatedDto);

        ResponseEntity<BookDto> response = bookController.updateBook(bookId, updatedDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Title");
        verify(bookService).updateBook(eq(bookId), any(BookDto.class));
    }

    // Test deleting a book and ensuring no content response is returned
    @Test
    void whenDeleteBook_thenReturnNoContent() {
        doNothing().when(bookService).deleteBook(bookId);

        ResponseEntity<Void> response = bookController.deleteBook(bookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(bookService).deleteBook(bookId);
    }

    // Test retrieving a book by ID and ensuring the correct book is returned
    @Test
    void whenGetBookById_thenReturnBook() {
        when(bookService.getBookById(bookId)).thenReturn(testBookDto);

        ResponseEntity<BookDto> response = bookController.getBookById(bookId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(bookId);
        verify(bookService).getBookById(bookId);
    }

    // Test retrieving a book by ISBN and ensuring the correct book is returned
    @Test
    void whenGetBookByIsbn_thenReturnBook() {
        when(bookService.getBookByIsbn(testIsbn)).thenReturn(testBookDto);

        ResponseEntity<BookDto> response = bookController.getBookByIsbn(testIsbn);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsbn()).isEqualTo(testIsbn);
        verify(bookService).getBookByIsbn(testIsbn);
    }

    // Test updating the availability of a book and ensuring the updated book is returned
    @Test
    void whenUpdateBookAvailability_thenReturnUpdatedBook() {
        when(bookService.updateBookAvailability(bookId, false)).thenReturn(testBookDto);

        ResponseEntity<BookDto> response = bookController.updateBookAvailability(bookId, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(bookService).updateBookAvailability(bookId, false);
    }

    // Test searching books by title and ensuring the correct books are returned
    @Test
    void whenSearchBooksByTitle_thenReturnBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        List<BookDto> books = Collections.singletonList(testBookDto);
        when(bookService.searchBooksByTitle("Gatsby", pageable)).thenReturn(books);

        ResponseEntity<List<BookDto>> response = bookController.searchBooksByTitle("Gatsby", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(bookService).searchBooksByTitle("Gatsby", pageable);
    }

    // Test searching books by author and ensuring the correct books are returned
    @Test
    void whenSearchBooksByAuthor_thenReturnBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        List<BookDto> books = Collections.singletonList(testBookDto);
        when(bookService.searchBooksByAuthor("Fitzgerald", pageable)).thenReturn(books);

        ResponseEntity<List<BookDto>> response = bookController.searchBooksByAuthor("Fitzgerald", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(bookService).searchBooksByAuthor("Fitzgerald", pageable);
    }

    // Test searching books by genre and ensuring the correct books are returned
    @Test
    void whenSearchBooksByGenre_thenReturnBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        List<BookDto> books = Collections.singletonList(testBookDto);
        when(bookService.searchBooksByGenre("Fiction", pageable)).thenReturn(books);

        ResponseEntity<List<BookDto>> response = bookController.searchBooksByGenre("Fiction", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(bookService).searchBooksByGenre("Fiction", pageable);
    }

    // Test searching books by availability and ensuring the correct books are returned
    @Test
    void whenSearchBooksByAvailability_thenReturnBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        List<BookDto> books = Collections.singletonList(testBookDto);
        when(bookService.searchBooksByAvailability(true, pageable)).thenReturn(books);

        ResponseEntity<List<BookDto>> response = bookController.searchBooksByAvailability(true, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(bookService).searchBooksByAvailability(true, pageable);
    }
}

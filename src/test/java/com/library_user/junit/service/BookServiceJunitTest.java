package com.library_user.junit.service;

import com.library_user.exceptions.CustomException;
import com.library_user.model.dto.BookDto;
import com.library_user.model.entity.Book;
import com.library_user.repository.BookRepository;
import com.library_user.service.Impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceJunitTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private BookDto testBookDto;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();

        testBook = Book.builder()
                .id(bookId)
                .title("The Great Gatsby")
                .author("F. Scott Fitzgerald")
                .isbn("978-0743273565")
                .description("A story of the fabulously wealthy Jay Gatsby")
                .publicationDate(LocalDate.of(1925, 4, 10))
                .genre("Fiction")
                .availability(true)
                .build();

        testBookDto = BookDto.builder()
                .id(bookId)
                .title("The Great Gatsby")
                .author("F. Scott Fitzgerald")
                .isbn("978-0743273565")
                .description("A story of the fabulously wealthy Jay Gatsby")
                .publicationDate(LocalDate.of(1925, 4, 10))
                .genre("Fiction")
                .availability(true)
                .build();
    }

    // Test case to add a new book to the library system
    @Test
    void whenAddBook_thenReturnSavedBook() {
        when(bookRepository.findByIsbn(testBookDto.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookDto savedBook = bookService.addBook(testBookDto);

        assertThat(savedBook).isNotNull();
        assertThat(savedBook.getTitle()).isEqualTo("The Great Gatsby");
        assertThat(savedBook.getAuthor()).isEqualTo("F. Scott Fitzgerald");
        assertThat(savedBook.getIsbn()).isEqualTo("978-0743273565");
        verify(bookRepository).save(any(Book.class));
    }

    // Test case to add a book with an existing ISBN and throw an exception
    @Test
    void whenAddBookWithExistingIsbn_thenThrowException() {
        when(bookRepository.findByIsbn(testBookDto.getIsbn())).thenReturn(Optional.of(testBook));

        assertThatThrownBy(() -> bookService.addBook(testBookDto))
                .isInstanceOf(CustomException.class);
    }

    // Test case to update an existing book's details
    @Test
    void whenUpdateBook_thenReturnUpdatedBook() {
        BookDto updatedDto = BookDto.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn("978-0743273565")
                .description("Updated Description")
                .publicationDate(LocalDate.now())
                .genre("Updated Genre")
                .availability(false)
                .build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookDto result = bookService.updateBook(bookId, updatedDto);

        assertThat(result).isNotNull();
        verify(bookRepository).save(any(Book.class));
    }

    // Test case to update a book that doesn't exist and throw an exception
    @Test
    void whenUpdateNonExistentBook_thenThrowException() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(bookId, testBookDto))
                .isInstanceOf(CustomException.class);
    }

    // Test case to delete an existing book from the library system
    @Test
    void whenDeleteBook_thenDeleteSuccessfully() {
        when(bookRepository.existsById(bookId)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(bookId);

        bookService.deleteBook(bookId);

        verify(bookRepository).deleteById(bookId);
    }

    // Test case to delete a book that does not exist and throw an exception
    @Test
    void whenDeleteNonExistentBook_thenThrowException() {
        when(bookRepository.existsById(bookId)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(bookId))
                .isInstanceOf(CustomException.class);
    }

    // Test case to get a book by its ID
    @Test
    void whenGetBookById_thenReturnBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));

        BookDto found = bookService.getBookById(bookId);

        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("The Great Gatsby");
        assertThat(found.getAuthor()).isEqualTo("F. Scott Fitzgerald");
    }

    // Test case to get a book by its ISBN
    @Test
    void whenGetBookByIsbn_thenReturnBook() {
        when(bookRepository.findByIsbn(testBook.getIsbn())).thenReturn(Optional.of(testBook));

        BookDto found = bookService.getBookByIsbn(testBook.getIsbn());

        assertThat(found).isNotNull();
        assertThat(found.getIsbn()).isEqualTo("978-0743273565");
    }

    // Test case to update the availability status of a book
    @Test
    void whenUpdateBookAvailability_thenReturnUpdatedBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        BookDto result = bookService.updateBookAvailability(bookId, false);

        assertThat(result).isNotNull();
        assertThat(result.isAvailability()).isFalse();
        verify(bookRepository).save(any(Book.class));
    }
}

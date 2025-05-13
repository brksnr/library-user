package com.library_user.junit.service;

import com.library_user.model.dto.BookDto;
import com.library_user.model.entity.Book;
import com.library_user.model.mapper.BookMapper;
import com.library_user.repository.BookRepository;
import com.library_user.service.Impl.ReactiveBookServiceImpl;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveBookServiceJunitTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ReactiveBookServiceImpl reactiveBookService;

    private Book book1;
    private Book book2;
    private BookDto bookDto1;
    private BookDto bookDto2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Setup method to initialize test data before each test
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        book1 = Book.builder().id(id1).title("Test Book 1").author("Author 1").isbn("111").genre("Genre 1").availability(true).build();
        book2 = Book.builder().id(id2).title("Another Test Book").author("Author 2").isbn("222").genre("Genre 2").availability(false).build();

        bookDto1 = BookMapper.toDto(book1);
        bookDto2 = BookMapper.toDto(book2);

        pageable = PageRequest.of(0, 10);
    }

    /**
     * Test case for searching books by title successfully.
     */
    @Test
    void searchBooksByTitle_ShouldReturnFluxOfBookDto_WhenBooksFound() {
        String title = "Test";
        List<Book> books = Arrays.asList(book1, book2);
        Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());
        when(bookRepository.findByTitleContainingIgnoreCase(eq(title), eq(pageable))).thenReturn(bookPage);

        Flux<BookDto> resultFlux = reactiveBookService.searchBooksByTitle(title, pageable);

        StepVerifier.create(resultFlux)
                .expectNext(bookDto1)
                .expectNext(bookDto2)
                .verifyComplete();

        verify(bookRepository).findByTitleContainingIgnoreCase(eq(title), eq(pageable));
    }

    /**
     * Test case for searching books by author successfully.
     */
    @Test
    void searchBooksByAuthor_ShouldReturnFluxOfBookDto_WhenBooksFound() {
        String author = "Author";
        List<Book> books = Arrays.asList(book1, book2);
        Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());
        when(bookRepository.findByAuthorContainingIgnoreCase(eq(author), eq(pageable))).thenReturn(bookPage);

        Flux<BookDto> resultFlux = reactiveBookService.searchBooksByAuthor(author, pageable);

        StepVerifier.create(resultFlux)
                .expectNext(bookDto1)
                .expectNext(bookDto2)
                .verifyComplete();

        verify(bookRepository).findByAuthorContainingIgnoreCase(eq(author), eq(pageable));
    }

    /**
     * Test case for searching books by genre successfully.
     */
    @Test
    void searchBooksByGenre_ShouldReturnFluxOfBookDto_WhenBooksFound() {
        // Arrange
        String genre = "Genre";
        List<Book> books = Collections.singletonList(book1);
        Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());
        when(bookRepository.findByGenreContainingIgnoreCase(eq(genre), eq(pageable))).thenReturn(bookPage);

        Flux<BookDto> resultFlux = reactiveBookService.searchBooksByGenre(genre, pageable);

        StepVerifier.create(resultFlux)
                .expectNext(bookDto1)
                .verifyComplete();

        verify(bookRepository).findByGenreContainingIgnoreCase(eq(genre), eq(pageable));
    }

    /**
     * Test case for searching available books by availability successfully.
     */
    @Test
    void searchBooksByAvailability_ShouldReturnFluxOfBookDto_WhenSearchingForAvailable() {
        // Arrange
        boolean availability = true;
        List<Book> books = Collections.singletonList(book1);
        Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());
        when(bookRepository.findByAvailability(eq(availability), eq(pageable))).thenReturn(bookPage);

        Flux<BookDto> resultFlux = reactiveBookService.searchBooksByAvailability(availability, pageable);

        StepVerifier.create(resultFlux)
                .expectNext(bookDto1)
                .verifyComplete();

        verify(bookRepository).findByAvailability(eq(availability), eq(pageable));
    }

    /**
     * Test case for searching unavailable books by availability successfully.
     */
    @Test
    void searchBooksByAvailability_ShouldReturnFluxOfBookDto_WhenSearchingForUnavailable() {
        boolean availability = false;
        List<Book> books = Collections.singletonList(book2);
        Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());
        when(bookRepository.findByAvailability(eq(availability), eq(pageable))).thenReturn(bookPage);

        Flux<BookDto> resultFlux = reactiveBookService.searchBooksByAvailability(availability, pageable);

        StepVerifier.create(resultFlux)
                .expectNext(bookDto2)
                .verifyComplete();

        verify(bookRepository).findByAvailability(eq(availability), eq(pageable));
    }

    /**
     * Test case for ensuring an empty Flux is returned when no books match the search criteria.
     */
    @Test
    void searchBooksByTitle_ShouldReturnEmptyFlux_WhenNoBooksFound() {
        String title = "NonExistent";
        Page<Book> emptyPage = Page.empty(pageable);
        when(bookRepository.findByTitleContainingIgnoreCase(eq(title), eq(pageable))).thenReturn(emptyPage);

        Flux<BookDto> resultFlux = reactiveBookService.searchBooksByTitle(title, pageable);

        StepVerifier.create(resultFlux)
                .expectNextCount(0)
                .verifyComplete();

        verify(bookRepository).findByTitleContainingIgnoreCase(eq(title), eq(pageable));
    }
}


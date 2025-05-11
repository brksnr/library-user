package com.library_user.h2.service;
import com.library_user.model.dto.BookDto;
import com.library_user.model.entity.Book;
import com.library_user.helper.ErrorMessages;
import com.library_user.model.mapper.BookMapper;
import com.library_user.repository.BookRepository;
import com.library_user.service.BookService;
import com.library_user.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookServiceH2Test {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    private Book bookEntity1, bookEntity2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();

        BookDto bookDto1 = new BookDto();
        bookDto1.setTitle("The Great Gatsby");
        bookDto1.setAuthor("F. Scott Fitzgerald");
        bookDto1.setIsbn("978-0743273565");
        bookDto1.setGenre("Classic");
        bookDto1.setDescription("A story of wealth, love, and the American Dream.");
        bookDto1.setPublicationDate(LocalDate.of(1925, 4, 10));
        bookDto1.setAvailability(true);

        bookEntity1 = bookRepository.save(BookMapper.toEntity(bookDto1));

        BookDto bookDto2 = new BookDto();
        bookDto2.setTitle("1984");
        bookDto2.setAuthor("George Orwell");
        bookDto2.setIsbn("978-0451524935");
        bookDto2.setGenre("Dystopian");
        bookDto2.setDescription("A novel about a totalitarian future society.");
        bookDto2.setPublicationDate(LocalDate.of(1949, 6, 8));
        bookDto2.setAvailability(false);

        bookEntity2 = bookRepository.save(BookMapper.toEntity(bookDto2));

        pageable = PageRequest.of(0, 10);
    }

    // successfully adding a new book.
    @Test
    void whenAddBook_thenReturnSavedBook() {
        BookDto newBookDto = new BookDto();
        newBookDto.setTitle("Brave New World");
        newBookDto.setAuthor("Aldous Huxley");
        newBookDto.setIsbn("754-0060853584");
        newBookDto.setGenre("Dystopian");
        newBookDto.setDescription("Another dystopian novel.");
        newBookDto.setPublicationDate(LocalDate.of(1932, 1, 1));
        newBookDto.setAvailability(true);

        BookDto savedBook = bookService.addBook(newBookDto);

        assertNotNull(savedBook);
        assertNotNull(savedBook.getId());
        assertEquals(newBookDto.getTitle(), savedBook.getTitle());
        assertTrue(bookRepository.existsById(savedBook.getId()));
    }

    // adding a book with an existing ISBN throws a CustomException.
    @Test
    void whenAddBookWithExistingIsbn_thenThrowException() {
        BookDto duplicateIsbnBookDto = new BookDto();
        duplicateIsbnBookDto.setTitle("Another Gatsby");
        duplicateIsbnBookDto.setAuthor("F. Scott Fitzgerald Clone");
        duplicateIsbnBookDto.setIsbn(bookEntity1.getIsbn()); // Mevcut ISBN
        duplicateIsbnBookDto.setGenre("Classic");
        duplicateIsbnBookDto.setDescription("A duplicate.");
        duplicateIsbnBookDto.setPublicationDate(LocalDate.now());
        duplicateIsbnBookDto.setAvailability(true);

        CustomException exception = assertThrows(CustomException.class, () -> {
            bookService.addBook(duplicateIsbnBookDto);
        });
        assertTrue(exception.getMessage().contains(ErrorMessages.BOOK_ALREADY_EXISTS_ISBN.formatted(bookEntity1.getIsbn())));
    }

    // successfully updating an existing book.
    @Test
    void whenUpdateBook_thenReturnUpdatedBook() {
        BookDto updateData = new BookDto();
        updateData.setTitle("The Great Gatsby - Updated Edition");
        updateData.setAuthor("F. S. Fitzgerald");
        updateData.setAvailability(false);

        BookDto updatedBook = bookService.updateBook(bookEntity1.getId(), updateData);

        assertNotNull(updatedBook);
        assertEquals(bookEntity1.getId(), updatedBook.getId());
        assertEquals("The Great Gatsby - Updated Edition", updatedBook.getTitle());
        assertEquals("F. S. Fitzgerald", updatedBook.getAuthor());
        assertFalse(updatedBook.isAvailability());

        Book bookFromDb = bookRepository.findById(bookEntity1.getId()).orElseThrow();
        assertEquals("The Great Gatsby - Updated Edition", bookFromDb.getTitle());
        assertFalse(bookFromDb.isAvailability());
    }

    // updating a non-existent book throws a CustomException.
    @Test
    void whenUpdateBookNotFound_thenThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        BookDto updateData = new BookDto();
        updateData.setTitle("Non Existent Update");

        CustomException exception = assertThrows(CustomException.class, () -> {
            bookService.updateBook(nonExistentId, updateData);
        });
        assertTrue(exception.getMessage().contains(ErrorMessages.BOOK_NOT_FOUND_ID.formatted(nonExistentId)));
    }


    // successfully deleting an existing book.
    @Test
    void whenDeleteBook_thenBookShouldBeDeleted() {
        UUID idToDelete = bookEntity1.getId();
        assertTrue(bookRepository.existsById(idToDelete));

        bookService.deleteBook(idToDelete);

        assertFalse(bookRepository.existsById(idToDelete));
    }

    // deleting a non-existent book throws a CustomException.
    @Test
    void whenDeleteBookNotFound_thenThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        CustomException exception = assertThrows(CustomException.class, () -> {
            bookService.deleteBook(nonExistentId);
        });
        assertTrue(exception.getMessage().contains(ErrorMessages.BOOK_NOT_FOUND_ID.formatted(nonExistentId)));
    }

    // retrieving a book by its ID.
    @Test
    void whenGetBookById_thenReturnBook() {
        BookDto foundBook = bookService.getBookById(bookEntity1.getId());

        assertNotNull(foundBook);
        assertEquals(bookEntity1.getId(), foundBook.getId());
        assertEquals(bookEntity1.getTitle(), foundBook.getTitle());
    }

    // retrieving a book by a non-existent ID throws a CustomException.
    @Test
    void whenGetBookByIdNotFound_thenThrowException() {
        UUID nonExistentId = UUID.randomUUID();
        CustomException exception = assertThrows(CustomException.class, () -> {
            bookService.getBookById(nonExistentId);
        });
        assertTrue(exception.getMessage().contains(ErrorMessages.BOOK_NOT_FOUND_ID.formatted(nonExistentId)));
    }

    // retrieving a book by its ISBN.
    @Test
    void whenGetBookByIsbn_thenReturnBook() {
        BookDto foundBook = bookService.getBookByIsbn(bookEntity1.getIsbn());

        assertNotNull(foundBook);
        assertEquals(bookEntity1.getId(), foundBook.getId());
        assertEquals(bookEntity1.getIsbn(), foundBook.getIsbn());
    }

    // retrieving a book by a non-existent ISBN throws a CustomException.
    @Test
    void whenGetBookByIsbnNotFound_thenThrowException() {
        String nonExistentIsbn = "000-0000000000";
        CustomException exception = assertThrows(CustomException.class, () -> {
            bookService.getBookByIsbn(nonExistentIsbn);
        });
        assertTrue(exception.getMessage().contains(ErrorMessages.BOOK_NOT_FOUND_ISBN + nonExistentIsbn));
    }

    // successfully updating a book's availability status.
    @Test
    void whenUpdateBookAvailability_thenReturnUpdatedBook() {
        assertTrue(bookRepository.findById(bookEntity1.getId()).get().isAvailability());

        BookDto updatedBook = bookService.updateBookAvailability(bookEntity1.getId(), false);

        assertNotNull(updatedBook);
        assertFalse(updatedBook.isAvailability());

        Book bookFromDb = bookRepository.findById(bookEntity1.getId()).orElseThrow();
        assertFalse(bookFromDb.isAvailability());

        BookDto updatedAgainBook = bookService.updateBookAvailability(bookEntity1.getId(), true);
        assertTrue(updatedAgainBook.isAvailability());
    }


    // searching for books by title returns the correct matching books.
    @Test
    void whenSearchBooksByTitle_thenReturnMatchingBooks() {
        BookDto bookDto3 = new BookDto();
        bookDto3.setTitle("The Old Man and the Sea");
        bookDto3.setAuthor("Ernest Hemingway");
        bookDto3.setIsbn("978-0684801223");
        bookDto3.setGenre("Classic");
        bookDto3.setDescription("A story of an old fisherman.");
        bookDto3.setPublicationDate(LocalDate.of(1952, 9, 1));
        bookDto3.setAvailability(true);
        bookService.addBook(bookDto3);

        List<BookDto> foundBooks = bookService.searchBooksByTitle("great", pageable);
        assertEquals(1, foundBooks.size());
        assertEquals("The Great Gatsby", foundBooks.getFirst().getTitle());

        List<BookDto> foundBooksThe = bookService.searchBooksByTitle("the", pageable);
        assertEquals(2, foundBooksThe.size());

    }

    // searching for books by author returns the correct matching books.
    @Test
    void whenSearchBooksByAuthor_thenReturnMatchingBooks() {
        List<BookDto> foundBooks = bookService.searchBooksByAuthor("Orwell", pageable);
        assertEquals(1, foundBooks.size());
        assertEquals("George Orwell", foundBooks.getFirst().getAuthor());
    }

    // searching for books by genre returns the correct matching books.
    @Test
    void whenSearchBooksByGenre_thenReturnMatchingBooks() {
        List<BookDto> foundBooks = bookService.searchBooksByGenre("Classic", pageable);
        assertEquals(1, foundBooks.size());
        assertEquals("Classic", foundBooks.getFirst().getGenre());
    }

    // searching for books by availability status returns the correct matching books.
    @Test
    void whenSearchBooksByAvailability_thenReturnMatchingBooks() {
        List<BookDto> availableBooks = bookService.searchBooksByAvailability(true, pageable);
        assertEquals(1, availableBooks.size());
        assertTrue(availableBooks.getFirst().isAvailability());
        assertEquals(bookEntity1.getTitle(), availableBooks.getFirst().getTitle());


        List<BookDto> unavailableBooks = bookService.searchBooksByAvailability(false, pageable);
        assertEquals(1, unavailableBooks.size());
        assertFalse(unavailableBooks.getFirst().isAvailability());
        assertEquals(bookEntity2.getTitle(), unavailableBooks.getFirst().getTitle());
    }
}
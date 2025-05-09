package com.library_user.controller;

import com.library_user.model.dto.BookDto;
import com.library_user.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Book Management", description = "Book CRUD and search operations for librarians and patrons")
public class BookController {

    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(
            summary = "Add a new book",
            description = "Librarians can add a new book to the library. Requires title, author, ISBN, publication date, and genre.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Book created successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class))),
                    @ApiResponse(responseCode = "409", description = "Book with the same ISBN already exists"),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<BookDto> addBook(@RequestBody BookDto bookDto) {
        BookDto created = bookService.addBook(bookDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Update book information",
            description = "Librarians can update book details by book ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class))),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<BookDto> updateBook(@PathVariable UUID id, @RequestBody BookDto bookDto) {
        BookDto updated = bookService.updateBook(id, bookDto);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete a book",
            description = "Librarians can delete a book from the system by book ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "View book details by ID",
            description = "Librarians and patrons can view detailed information about a book by its ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class))),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    public ResponseEntity<BookDto> getBookById(@PathVariable UUID id) {
        BookDto book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    @Operation(
            summary = "View book details by ISBN",
            description = "Librarians and patrons can view detailed information about a book by its ISBN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class))),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    @GetMapping("/isbn/{isbn}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    public ResponseEntity<BookDto> getBookByIsbn(@PathVariable String isbn) {
        BookDto book = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(book);
    }

    @Operation(
            summary = "Update book availability",
            description = "Librarians can update the availability status of a book.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book availability updated",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class))),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    @PatchMapping("/{id}/availability/{availability}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<BookDto> updateBookAvailability(
            @PathVariable UUID id,
            @PathVariable boolean availability) {
        BookDto updated = bookService.updateBookAvailability(id, availability);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Search books by title",
            description = "Librarians and patrons can search for books by title. Supports pagination.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Books found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class)))
            }
    )
    @GetMapping("/search/title/{title}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    public ResponseEntity<List<BookDto>> searchBooksByTitle(
            @PathVariable String title,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByTitle(title, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Search books by author",
            description = "Librarians and patrons can search for books by author. Supports pagination.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Books found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class)))
            }
    )
    @GetMapping("/search/author/{author}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    public ResponseEntity<List<BookDto>> searchBooksByAuthor(
            @PathVariable String author,
            @PageableDefault(size = 10) Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByAuthor(author, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Search books by genre",
            description = "Librarians and patrons can search for books by genre. Supports pagination.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Books found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class)))
            }
    )
    @GetMapping("/search/genre/{genre}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    public ResponseEntity<List<BookDto>> searchBooksByGenre(
            @PathVariable String genre,
            @PageableDefault(size = 10) Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByGenre(genre, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Search books by availability",
            description = "Librarians and patrons can search for books by availability. Supports pagination.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Books found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookDto.class)))
            }
    )
    @GetMapping("/search/availability/{availability}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON')")
    public ResponseEntity<List<BookDto>> searchBooksByAvailability(
            @PathVariable boolean availability,
            @PageableDefault(size = 10) Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByAvailability(availability, pageable);
        return ResponseEntity.ok(result);
    }
}
package com.library_user.controller;

import com.library_user.model.dto.BookDto;
import com.library_user.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // Kitap ekler
    @PostMapping
    public ResponseEntity<BookDto> addBook(@RequestBody BookDto bookDto) {
        BookDto created = bookService.addBook(bookDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Kitap günceller
    @PutMapping("/{id}")
    public ResponseEntity<BookDto> updateBook(@PathVariable UUID id, @RequestBody BookDto bookDto) {
        BookDto updated = bookService.updateBook(id, bookDto);
        return ResponseEntity.ok(updated);
    }

    // Kitap siler
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // ID ile kitap getirir
    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBookById(@PathVariable UUID id) {
        BookDto book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    // ISBN ile kitap getirir
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDto> getBookByIsbn(@PathVariable String isbn) {
        BookDto book = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(book);
    }

    // Uygunluğu günceller
    @PatchMapping("/{id}/availability/{availability}")
    public ResponseEntity<BookDto> updateBookAvailability(
            @PathVariable UUID id,
            @PathVariable boolean availability) {
        BookDto updated = bookService.updateBookAvailability(id, availability);
        return ResponseEntity.ok(updated);
    }

    // Başlığa göre arar
    @GetMapping("/search/title/{title}")
    public ResponseEntity<List<BookDto>> searchBooksByTitle(
            @PathVariable String title,
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByTitle(title, pageable);
        return ResponseEntity.ok(result);
    }

    // Yazara göre arar
    @GetMapping("/search/author/{author}")
    public ResponseEntity<List<BookDto>> searchBooksByAuthor(
            @PathVariable String author,
            @PageableDefault(size = 10)
            Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByAuthor(author, pageable);
        return ResponseEntity.ok(result);
    }

    // Türe göre arar
    @GetMapping("/search/genre/{genre}")
    public ResponseEntity<List<BookDto>> searchBooksByGenre(
            @PathVariable String genre,
            @PageableDefault(size = 10)
            Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByGenre(genre, pageable);
        return ResponseEntity.ok(result);
    }

    // Uygunluğa göre arar
    @GetMapping("/search/availability/{availability}")
    public ResponseEntity<List<BookDto>> searchBooksByAvailability(
            @PathVariable boolean availability,
            @PageableDefault(size = 10)
            Pageable pageable) {
        List<BookDto> result = bookService.searchBooksByAvailability(availability, pageable);
        return ResponseEntity.ok(result);
    }
}
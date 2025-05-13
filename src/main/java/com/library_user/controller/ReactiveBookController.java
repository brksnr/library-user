package com.library_user.controller;

import com.library_user.model.dto.BookDto;
import com.library_user.service.ReactiveBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@Tag(name = "Reactive Book Controller", description = "Provides reactive endpoints for searching books by title, author, genre, or availability.")
@RequestMapping("/api/reactive/books")
public class ReactiveBookController {

    private final ReactiveBookService reactiveBookService;


    @Autowired
    public ReactiveBookController(@Qualifier("reactiveBookService") ReactiveBookService reactiveBookService) {
        this.reactiveBookService = reactiveBookService;
    }


    @Operation(
            summary = "Search books by title",
            description = "Returns a list of books whose titles contain the given keyword."
    )
    @GetMapping("/search/title/{title}")
    public Flux<BookDto> searchBooksByTitle(
            @PathVariable String title,
            Pageable pageable
    ) {
        return reactiveBookService.searchBooksByTitle(title, pageable);
    }


    @Operation(
            summary = "Search books by author",
            description = "Returns a list of books written by authors whose names contain the given keyword."
    )
    @GetMapping("/search/author/{author}")
    public Flux<BookDto> searchBooksByAuthor(
            @PathVariable String author,
            Pageable pageable
    ) {
        return reactiveBookService.searchBooksByAuthor(author, pageable);
    }


    @Operation(
            summary = "Search books by genre",
            description = "Returns a list of books that belong to the specified genre."
    )
    @GetMapping("/search/genre/{genre}")
    public Flux<BookDto> searchBooksByGenre(
            @PathVariable String genre,
            Pageable pageable
    ) {
        return reactiveBookService.searchBooksByGenre(genre, pageable);
    }


    @Operation(
            summary = "Search books by availability",
            description = "Returns a list of books based on their availability status (true = available, false = unavailable)."
    )
    @GetMapping("/search/availability/{availability}")
    public Flux<BookDto> searchBooksByAvailability(
            @PathVariable boolean availability,
            Pageable pageable
    ) {
        return reactiveBookService.searchBooksByAvailability(availability, pageable);
    }
}

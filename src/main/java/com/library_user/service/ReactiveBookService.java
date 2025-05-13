package com.library_user.service;

import com.library_user.model.dto.BookDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
public interface ReactiveBookService {


    Flux<BookDto> searchBooksByTitle(String title, Pageable pageable);

    Flux<BookDto> searchBooksByAuthor(String author, Pageable pageable);

    Flux<BookDto> searchBooksByGenre(String genre, Pageable pageable);

    Flux<BookDto> searchBooksByAvailability(boolean availability, Pageable pageable);

}
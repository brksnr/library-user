package com.library_user.service.Impl;

import com.library_user.model.dto.BookDto;
import com.library_user.model.mapper.BookMapper;
import com.library_user.repository.BookRepository;
import com.library_user.service.ReactiveBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;


import java.util.concurrent.Callable;


@Service("reactiveBookService")
public class ReactiveBookServiceImpl implements ReactiveBookService {

    private final BookRepository bookRepository;
    private final Scheduler jdbcScheduler = Schedulers.boundedElastic();

    @Autowired
    public ReactiveBookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     Helper method to convert a blocking repository call into a reactive Flux
     */

    private <T> Flux<T> executeBlockingFlux(Callable<? extends Iterable<T>> supplier) {
        return Mono.fromCallable(supplier)
                .subscribeOn(jdbcScheduler)
                .flatMapMany(Flux::fromIterable);
    }


    /**
     *  Searches books by title (case-insensitive) and returns paginated results
     * */

    @Override
    public Flux<BookDto> searchBooksByTitle(String title, Pageable pageable) {
        return executeBlockingFlux(() ->
                bookRepository.findByTitleContainingIgnoreCase(title, pageable).getContent()
        )
                .map(BookMapper::toDto);
    }

    /**
     * Searches books by author (case-insensitive) and returns paginated results
     */
    @Override
    public Flux<BookDto> searchBooksByAuthor(String author, Pageable pageable) {
        return executeBlockingFlux(() ->
                bookRepository.findByAuthorContainingIgnoreCase(author, pageable).getContent()
        )
                .map(BookMapper::toDto);
    }


    /**
     * Searches books by genre (case-insensitive) and returns paginated results
     */
    @Override
    public Flux<BookDto> searchBooksByGenre(String genre, Pageable pageable) {
        return executeBlockingFlux(() ->
                bookRepository.findByGenreContainingIgnoreCase(genre, pageable).getContent()
        )
                .map(BookMapper::toDto);
    }

    /**
     * Searches books by availability (true = available, false = unavailable)
     **/
    @Override
    public Flux<BookDto> searchBooksByAvailability(boolean availability, Pageable pageable) {
        return executeBlockingFlux(() ->
                bookRepository.findByAvailability(availability, pageable).getContent()
        )
                .map(BookMapper::toDto);
    }
}
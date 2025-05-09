package com.library_user.repository;

import com.library_user.model.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);

    Optional<Book> findByIsbn(String isbn);

    Page<Book> findByGenreContainingIgnoreCase(String genre, Pageable pageable);

    Page<Book> findByAvailability(boolean availability, Pageable pageable);

}

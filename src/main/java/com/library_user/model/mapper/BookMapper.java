package com.library_user.model.mapper;

import com.library_user.exceptions.CustomException;
import com.library_user.model.dto.BookDto;
import com.library_user.model.entity.Book;
import com.library_user.model.helper.ErrorMessages;
import org.springframework.http.HttpStatus;

public class BookMapper {

    public static BookDto toDto(Book book) {
        if (book == null)
        {
            throw new CustomException(ErrorMessages.BOOK_ENTITY_NULL, HttpStatus.NOT_FOUND);
        }
        return BookDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .publicationDate(book.getPublicationDate())
                .genre(book.getGenre())
                .availability(book.isAvailability())
                .build();
    }

    public static Book toEntity(BookDto dto) {
        if (dto == null)
        {
            throw new CustomException(ErrorMessages.BOOK_DTO_NULL, HttpStatus.NOT_FOUND);
        }
        Book book = new Book();
        book.setId(null);
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setDescription(dto.getDescription());
        book.setPublicationDate(dto.getPublicationDate());
        book.setGenre(dto.getGenre());
        book.setAvailability(dto.isAvailability());
        return book;
    }

    public static void updateEntity(Book book, BookDto dto) {
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setIsbn(dto.getIsbn());
        book.setDescription(dto.getDescription());
        book.setPublicationDate(dto.getPublicationDate());
        book.setGenre(dto.getGenre());
        book.setAvailability(dto.isAvailability());
    }
}
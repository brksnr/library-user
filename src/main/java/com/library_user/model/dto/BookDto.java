package com.library_user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {

    private UUID id;

    @NotBlank(message = "Header can not null!")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Auther can not null!")
    @Size(min = 1, max = 255, message = "Author name must be between 1 and 255 characters")
    private String author;

    @NotBlank(message = "ISBN can not null!")
    @Size(min = 10, max = 20, message = "ISBN must be between 10 and 20 characters")
    private String isbn;

    @NotBlank(message = "Description can not null!")
    @Size(max = 1000, message = "Description can be up to 1000 characters")
    private String description;

    @NotNull(message = "Publication date tarihi can not null!")
    @PastOrPresent(message = "The month's date must be past or present")
    private LocalDate publicationDate;

    @NotBlank(message = "Genre can not null!")
    @Size(min = 1, max = 100, message = "Type must be between 1 and 100 characters")
    private String genre;

    private boolean availability;
}

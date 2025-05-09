package com.library_user.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionResponse {
    private String message;
    private int status;
    private LocalDateTime dateTime;

    public ExceptionResponse(String message, String customApplicationError) {
    }
}

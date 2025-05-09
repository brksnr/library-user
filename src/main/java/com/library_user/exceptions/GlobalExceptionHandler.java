package com.library_user.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleException(CustomException apiException){
        log.error("Exception occured: " , apiException);
        return new ResponseEntity<>(new ExceptionResponse(apiException.getMessage(),apiException.getHttpStatus().value(), LocalDateTime.now()),apiException.getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleException(Exception exception){
        log.error("Exception occured: " , exception);
        return new ResponseEntity<>(new ExceptionResponse(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now()),HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

package com.library_user.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleException(CustomException apiException){
        log.error("Exception occured: " , apiException);
        return new ResponseEntity<>(new ExceptionResponse(apiException.getMessage(),apiException.getHttpStatus().value(), LocalDateTime.now()),apiException.getHttpStatus());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        log.warn("AuthorizationDeniedException occurred: {}" , ex.getMessage());
        String errorMessage = "Access denied. You are not authorized for this operation.";
        return new ResponseEntity<>(new ExceptionResponse(errorMessage, HttpStatus.FORBIDDEN.value(), LocalDateTime.now()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("MethodArgumentNotValidException occurred: {}", ex.getMessage());

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ExceptionResponse exceptionResponse = new ExceptionResponse("invalid input data: " + errors, HttpStatus.BAD_REQUEST.value(), LocalDateTime.now());
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class) // Veya BadCredentialsException.class
    public ResponseEntity<ExceptionResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("AuthenticationException occurred: {}", ex.getMessage());
        String errorMessage = "invalid username or password.";
        return new ResponseEntity<>(new ExceptionResponse(errorMessage, HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleException(Exception exception){
        log.error("Exception occured: " , exception);
        return new ResponseEntity<>(new ExceptionResponse(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), LocalDateTime.now()),HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

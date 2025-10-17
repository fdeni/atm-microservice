package com.project.transaction_service.controller;

import com.project.transaction_service.dto.response.Base;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ExceptionHandleController {
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Base<String>> constraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Base.<String>builder().data(null).error(exception.getMessage()).status(HttpStatus.BAD_REQUEST.value()).build()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Base<String>> constraintViolation(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(
                Base.<String>builder().data(null).error(exception.getReason()).status(exception.getStatusCode().value()).build()
        );
    }
}

package com.morsel.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract sealed class ApplicationException extends RuntimeException
        permits DuplicateResourceException, ForbiddenException, ResourceNotFoundException {

    private final HttpStatus status;

    ApplicationException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }
}

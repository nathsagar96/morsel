package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException(String detail) {
        super(HttpStatus.CONFLICT, detail);
    }
}

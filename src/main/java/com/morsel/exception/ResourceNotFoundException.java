package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, detail);
    }
}

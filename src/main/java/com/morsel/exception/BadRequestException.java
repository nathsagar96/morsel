package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class BadRequestException extends ApplicationException {

    public BadRequestException(String detail) {
        super(HttpStatus.BAD_REQUEST, detail);
    }
}

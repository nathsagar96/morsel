package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class InvalidFileException extends ApplicationException {

    public InvalidFileException(String detail) {
        super(HttpStatus.BAD_REQUEST, detail);
    }
}

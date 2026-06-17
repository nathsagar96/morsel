package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class ForbiddenException extends ApplicationException {

    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN, detail);
    }
}

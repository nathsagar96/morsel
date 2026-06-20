package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, detail);
    }
}

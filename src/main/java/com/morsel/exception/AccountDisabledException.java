package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class AccountDisabledException extends ApplicationException {

    public AccountDisabledException(String detail) {
        super(HttpStatus.FORBIDDEN, detail);
    }
}

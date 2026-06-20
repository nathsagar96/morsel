package com.morsel.exception;

import org.springframework.http.HttpStatus;

public final class AccountLockedException extends ApplicationException {

    public AccountLockedException(String detail) {
        super(HttpStatus.TOO_MANY_REQUESTS, detail);
    }
}

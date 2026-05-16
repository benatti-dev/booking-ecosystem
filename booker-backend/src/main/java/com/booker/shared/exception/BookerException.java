package com.booker.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base runtime exception for all domain errors. */
@Getter
public class BookerException extends RuntimeException {

    private final HttpStatus status;

    public BookerException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static BookerException notFound(String message) {
        return new BookerException(message, HttpStatus.NOT_FOUND);
    }

    public static BookerException conflict(String message) {
        return new BookerException(message, HttpStatus.CONFLICT);
    }

    public static BookerException badRequest(String message) {
        return new BookerException(message, HttpStatus.BAD_REQUEST);
    }

    public static BookerException unauthorized(String message) {
        return new BookerException(message, HttpStatus.UNAUTHORIZED);
    }

    public static BookerException forbidden(String message) {
        return new BookerException(message, HttpStatus.FORBIDDEN);
    }
}

package com.asims.repository;

/** Thrown when the database fails. It wraps the checked SQLException so callers stay simple. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.stillfresh.app.authorizationservice.exception;

/**
 * Thrown when a login attempt is made with credentials that belong to a deleted account.
 * Controllers should return 410 Gone with code ACCOUNT_DELETED so the client can show an appropriate message.
 */
public class AccountDeletedException extends RuntimeException {

    public AccountDeletedException() {
        super("This account has been deleted.");
    }

    public AccountDeletedException(String message) {
        super(message);
    }
}

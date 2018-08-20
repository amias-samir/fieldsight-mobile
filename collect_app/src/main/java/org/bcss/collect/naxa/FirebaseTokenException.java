package org.bcss.collect.naxa;

public class FirebaseTokenException extends RuntimeException {
    public FirebaseTokenException(String message) {
        super(message);
    }

    public FirebaseTokenException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

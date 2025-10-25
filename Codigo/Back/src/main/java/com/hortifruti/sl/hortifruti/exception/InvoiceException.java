package com.hortifruti.sl.hortifruti.exception;

public class InvoiceException extends RuntimeException {
    public InvoiceException(String message) {
        super(message);
    }

    public InvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
}

package com.banking.transfer.exception;

public class DuplicateTransferException extends RuntimeException {
    public DuplicateTransferException(String message) {
        super(message);
    }
}

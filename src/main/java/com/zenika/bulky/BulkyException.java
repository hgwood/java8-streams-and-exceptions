package com.zenika.bulky;

import java.util.Collection;

public class BulkyException extends RuntimeException {

    private final Collection<?> partialResult;

    public BulkyException(Throwable cause, Collection<?> partialResult) {
        super(cause);
        this.partialResult = partialResult;
    }

    public <T> Collection<T> partialResult() {
        return (Collection<T>)partialResult;
    }
}

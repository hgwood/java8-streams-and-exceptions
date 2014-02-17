package com.zenika.bulky;

import java.util.Collection;

public class CollectException extends RuntimeException {

    private final Collection<?> partialResult;

    public CollectException(Throwable cause, Collection<?> partialResult) {
        super(cause);
        this.partialResult = partialResult;
    }

    public <T> Collection<T> partialResult() {
        return (Collection<T>)partialResult;
    }
}

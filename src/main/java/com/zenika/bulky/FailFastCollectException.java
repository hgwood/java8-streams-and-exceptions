package com.zenika.bulky;

import com.google.common.collect.ImmutableList;

public class FailFastCollectException extends RuntimeException {

    public final ImmutableList<?> results;

    public FailFastCollectException(Throwable cause, ImmutableList<?> results) {
        super(cause);
        this.results = results;
    }

    public <T> ImmutableList<T> getResults() {
        return (ImmutableList<T>)results;
    }
}

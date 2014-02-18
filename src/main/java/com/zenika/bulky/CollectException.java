package com.zenika.bulky;

import java.util.Collection;

import static java.util.Arrays.asList;

public class CollectException extends RuntimeException {

    private final Collection<Throwable> causes;
    private final Collection<?> results;

    public CollectException(Throwable cause, Collection<?> results) {
        super(cause);
        this.causes = asList(cause);
        this.results = results;
    }

    public CollectException(Collection<Throwable> causes, Collection<?> results) {
        super();
        this.causes = causes;
        this.results = results;
    }

    public Collection<Throwable> getCauses() {
        return causes;
    }

    public <T> Collection<T> getResults() {
        return (Collection<T>)results;
    }
}

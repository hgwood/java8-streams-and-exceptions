package me.hgwood.bulky;

import java.util.Collection;

public class FailAtEndCollectException extends RuntimeException {

    private final Collection<Throwable> causes;
    private final Collection<?> results;

    public FailAtEndCollectException(Collection<Throwable> causes, Collection<?> results) {
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

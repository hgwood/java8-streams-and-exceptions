package com.zenika.bulky;

import com.google.common.collect.ImmutableList;

import java.util.function.BiFunction;
import java.util.function.Function;

public class FailAtEndAccumulator<T> {

    private final ImmutableList.Builder<Throwable> failures = ImmutableList.builder();
    private final ImmutableList.Builder<T> results = ImmutableList.builder();
    private boolean hasFailures = false;

    public boolean hasFailures() {
        return hasFailures;
    }

    public FailAtEndAccumulator<T> addFailure(Throwable failure) {
        hasFailures = true;
        failures.add(failure);
        return this;
    }

    public FailAtEndAccumulator<T> addResult(T result) {
        results.add(result);
        return this;
    }

    public FailAtEndAccumulator<T> combine(FailAtEndAccumulator<T> other) {
        failures.addAll(other.failures.build());
        results.addAll(other.results.build());
        return this;
    }

    public <R> R unloadInto(BiFunction<ImmutableList<Throwable>, ImmutableList<T>, R> target) {
        return target.apply(failures.build(), results.build());
    }

    public <R> R unloadInto(Function<ImmutableList<T>, R> target) {
        return target.apply(results.build());
    }

}

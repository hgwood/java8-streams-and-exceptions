package com.zenika.bulky;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class Bulky {

    /**
     * Wraps a function that throws checked exceptions into a function that does not, by wrapping checked exceptions
     * into an unchecked {@link com.zenika.bulky.WrappedException}. Optionally, can also wrap some specified unchecked
     * exceptions.
     * <p>
     *     This method is useful to pass throwing lambdas and method references to the {@link java.util.stream.Stream}
     *     API.
     * </p>
     * @param f the function to wrap
     * @param runtimeExceptionClassesToWrap unchecked exception to wrap as if they were checked
     * @param <T> input type of the wrapped function
     * @param <R> output type of the wrapped function
     * @return a function that does the same thing as f, except it will throw the unchecked
     * {@link com.zenika.bulky.WrappedException} when the original function would have thrown checked exceptions
     */
    public static <T, R> Function<T, R> sneaky(
            ThrowingFunction<T, R> f, Class<? extends RuntimeException>... runtimeExceptionClassesToWrap) {
        return input -> {
            try {
                return f.apply(input);
            } catch (RuntimeException e) {
                if (!asList(runtimeExceptionClassesToWrap).contains(e.getClass())) throw e;
                else throw new WrappedException(e);
            } catch (Exception e) {
                throw new WrappedException(e);
            }
        };
    }

    /**
     * Returns a function that wraps a value in a {@link java.util.function.Supplier} of that value.
     * @param <T> the type of the value to wrap
     */
    public static <T> Function<T, Supplier<T>> toSupplier() {
        return input -> () -> input;
    }

    /**
     * Wraps a function into one that will accept and return {@link java.util.function.Supplier}s of the input and
     * output values, instead of the values themselves.
     * <p>
     *     Designed to be used to wrap functions passed to {@link Stream#map(java.util.function.Function)} calls
     *     following an initial map call that uses {@link #lazy(java.util.function.Function)}.
     * </p>
     * @param f the function to wrap
     * @param <T> input type of the wrapped function
     * @param <R> output type of the wrapped function
     * @return a function that does the same thing as f, except it accepts {@link java.util.function.Supplier}s of
     * values instead of values.
     */
    public static <T, R> Function<Supplier<T>, Supplier<R>> liftToSuppliers(Function<T, R> f) {
        return supplier -> () -> f.apply(supplier.get());
    }

    /**
     * Wraps a function into one that returns a {@link java.util.function.Supplier} of the result of the original
     * function.
     * <p>
     *     Wrapped functions can be passed to {@link Stream#map(java.util.function.Function)}, effectively delegating
     *     control of the computation to a downstream component such as a {@link java.util.stream.Collector}.
     * </p>
     * <p>
     *     {@code stream.map(lazy(f)} is equivalent to {@code stream.map(toSuppliers()).map(liftToSuppliers(f))}
     * </p>
     * @param f the function to wrap
     * @param <T> input type of the wrapped function
     * @param <R> output type of the wrapped function
     * @return a function that does the same thing as f, except it returns a {@link java.util.function.Supplier} of
     * the result.
     */
    public static <T, R> Function<T, Supplier<R>> lazy(Function<T, R> f) {
        return input -> () -> f.apply(input);
    }

    /**
     * Same as {@link #liftToSuppliers(java.util.function.Function)}.
     */
    public static <T, R> Function<Supplier<T>, Supplier<R>> lazylift(Function<T, R> f) {
        return liftToSuppliers(f);
    }

    /**
     * Returns a collector that filters out computations that failed with the specified exceptions classes.
     * @param failures exceptions to discard
     * @param <T> type of the results
     */
    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> discarding(
            Class<? extends RuntimeException>... failures) {
        if (failures.length == 0)
            throw new IllegalArgumentException("must at least provide one exception class to discard");
        return Collector.of(
            ArrayList::new,
            (accumulator, element) -> {
                try {
                    accumulator.add(element.get());
                } catch (RuntimeException e) {
                    if (!asList(failures).contains(e.getClass())) throw e;
                }
            },
            (left, right) -> { left.addAll(right); return left; },
            result -> result.stream()
        );
    }

    /**
     * Same as {@link #discarding(Class[])} but only {@link com.zenika.bulky.WrappedException} is treated as a failure.
     * Designed to be used in conjonction with {@link #sneaky(ThrowingFunction, Class[])} with no exceptions specified.
     * @param <T> type of the results
     */
    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> discardingFailures() {
        return discarding(WrappedException.class);
    }

    /**
     * Returns a collector that only computes and retains results until one of the specified exception is raised.
     * The exception is then swallowed and the computed results are returned as a new stream.
     * @param failures exceptions that should interrupt the overall operation
     * @param <T> type of the results
     */
    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> upTo(
            Class<? extends RuntimeException>... failures) {
        return new UpToFailureCollector<>(failures);
    }

    /**
     * Same as {@link #upTo(Class[])} but {@link com.zenika.bulky.WrappedException} is treated as a failure.
     * Designed to be used in conjonction with {@link #sneaky(ThrowingFunction, Class[])} with no exceptions specified.
     * @param <T> type of the results
     */
    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> upToFailure() {
        return upTo(WrappedException.class);
    }

    /**
     * Returns a collector that only computes and retains results until one of the specified exception is raised.
     * The exception is then wrapped and thrown as a {@link com.zenika.bulky.FailFastCollectException} also containing
     * the computed results.
     * @param failures exceptions that should interrupt the overall operation
     * @param <T> type of the results
     * @throws FailFastCollectException if a result fails to compute
     */
    public static <T> Collector<Supplier<T>, ImmutableList.Builder<T>, Stream<T>> upToAndThrow(
            Class<? extends RuntimeException>... failures) throws FailFastCollectException {
        return Collector.of(
            ImmutableList::builder,
            (accumulator, element) -> {
                try {
                    accumulator.add(element.get());
                } catch (RuntimeException e) {
                    if (asList(failures).contains(e.getClass()))
                        throw new FailFastCollectException(e, accumulator.build());
                }
            },
            (left, right) -> { left.addAll(right.build()); return left; },
            result -> result.build().stream()
        );
    }

    /**
     * Returns a collector that consumes the entire stream even if an exception is thrown, but throws
     * {@link com.zenika.bulky.FailAtEndCollectException} at the end if it was the case. All successfully computed
     * results and all thrown exceptions are accessible through {@link FailAtEndCollectException#getResults()} and
     * {@link FailAtEndCollectException#getCauses()}. If no exception is thrown before the end of the stream, a new
     * stream with the results is returned.
     * @param failures exceptions that should thrown at the end
     * @param <T> type of the results
     * @throws FailAtEndCollectException if a result fails to compute
     */
    public static <T> Collector<Supplier<T>, FailAtEndAccumulator<T>, Stream<T>> throwingAtEnd(
            Class<? extends RuntimeException>... failures) throws FailAtEndCollectException {
        return Collector.of(
            FailAtEndAccumulator::new,
            (accumulator, element) -> {
                try {
                    accumulator.addResult(element.get());
                } catch (RuntimeException e) {
                    if (asList(failures).contains(e.getClass()))
                        accumulator.addFailure(e);
                }
            },
            FailAtEndAccumulator::combine,
            accumulator -> {
                if (accumulator.hasFailures()) {
                    throw accumulator.unloadInto(FailAtEndCollectException::new);
                } else {
                    return accumulator.unloadInto(results -> results.stream());
                }
            }
        );
    }
}

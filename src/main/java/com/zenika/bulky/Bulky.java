package com.zenika.bulky;

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
    public static <T, R> Function<T, R> sneaky(ThrowingFunction<T, R> f, Class<? extends RuntimeException>... runtimeExceptionClassesToWrap) {
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
     * @param <T> the type of the value to wrap
     * @return a function that wraps a value in a {@link java.util.function.Supplier} of that value
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

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> discarding(Class<? extends RuntimeException>... discarded) {
        if (discarded.length == 0)
            throw new IllegalArgumentException("must at least provide one exception class to discard");
        return Collector.of(
            ArrayList::new,
            (accumulator, element) -> {
                try {
                    accumulator.add(element.get());
                } catch (RuntimeException e) {
                    if (!asList(discarded).contains(e.getClass())) throw e;
                }
            },
            (left, right) -> { left.addAll(right); return left; },
            result -> result.stream()
        );
    }

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> discardingFailures() {
        return discarding(WrappedException.class);
    }

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> upTo(Class<? extends RuntimeException> exceptionToCatch) {
        return new UpToFailureCollector<>(exceptionToCatch);
    }

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> upToFailure() {
        return upTo(WrappedException.class);
    }

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> upToAndThrow(Class<? extends RuntimeException> exceptionClassesToCatch) throws CollectException {
        return Collector.of(
            ArrayList::new,
            (accumulator, element) -> {
                try {
                    accumulator.add(element.get());
                } catch (RuntimeException e) {
                    if (e.getClass().equals(exceptionClassesToCatch)) throw new CollectException(e, accumulator);
                }
            },
            (left, right) -> { left.addAll(right); return left; },
            result -> result.stream()
        );
    }
}

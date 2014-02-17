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
     * @param ignored unchecked exception to process as if they were checked
     * @param <T> input type of the wrapped function
     * @param <R> output type of the wrapped function
     * @return a function that does the same thing as f, except it will throw the unchecked
     * {@link com.zenika.bulky.WrappedException} when the original function would have thrown checked exceptions
     */
    public static <T, R> Function<T, R> sneaky(ThrowingFunction<T, R> f, Class<? extends RuntimeException>... ignored) {
        return input -> {
            try {
                return f.apply(input);
            } catch (RuntimeException e) {
                if (!asList(ignored).contains(e.getClass())) throw e;
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

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> ignoringFailures() {
        return Collector.of(
            ArrayList::new,
            (accumulator, element) -> {
                try {
                    accumulator.add(element.get());
                } catch (WrappedException e) {
                    // swallow
                }
            },
            (left, right) -> { left.addAll(right); return left; },
            result -> result.stream()
        );
    }

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> uptoFailure() {
        return new UpToFailureCollector<>();
    }

    public static <T> Collector<Supplier<T>, Collection<T>, Stream<T>> throwingWithPartialResult() throws BulkyException {
        return Collector.of(
            ArrayList::new,
            (accumulator, element) -> {
                try {
                    accumulator.add(element.get());
                } catch (WrappedException e) {
                    throw new BulkyException(e.getCause(), accumulator);
                }
            },
            (left, right) -> { left.addAll(right); return left; },
            result -> result.stream()
        );
    }
}

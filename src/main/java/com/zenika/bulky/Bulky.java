package com.zenika.bulky;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class Bulky {

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

    public static <T, R> Function<T, Supplier<R>> lazy(Function<T, R> f) {
        return input -> () -> f.apply(input);
    }

    public static <T, R> Function<Supplier<T>, Supplier<R>> lift(Function<T, R> f) {
        return originalSupplier -> () -> f.apply(originalSupplier.get());
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

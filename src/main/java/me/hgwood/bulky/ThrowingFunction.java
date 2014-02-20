package me.hgwood.bulky;

public interface ThrowingFunction<T, R> {

    R apply(T input) throws Exception;

}

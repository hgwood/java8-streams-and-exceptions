Java 8's new Stream API, being FP-inspired, doesn't play with exceptions very well.

For example, let's say we want to map a set of URI strings to URI objects:

```java
uriStrings.stream().map(URI::create).collect(toList());
```

If one of the strings is not a valid URL, `map()` still runs OK because it does nothing but register the map operation,
however, `collect()` does call `URI::create`, which throws an `IllegalArgumentException` and the overall operation
fails without hope of retrieving the URIs that were valid. A custom collector cannot solve this issue because
`collect()` computes the value *before* it hands it to the collector.

This is my attempt to solve this problem. The idea is that the collector has to be able to
control when the exception-throwing function is called in order to catch exceptions and process them, so the call has
to be delayed. To achieve this, values are mapped to supplier of values using a method I called `lazy`:

```java
public static <T, R> Function<T, Supplier<R>> lazy(Function<T, R> f) {
    return input -> () -> f.apply(input);
}
```

Notice that `f.apply()` is never called inside lazy, nor is it called when the returned function is called. Mapping a
`lazy()`-wrapped `A` to `B` function onto a stream of `A`s results in a stream of suppliers of `B`s. A downstream
collector therefore gets to work with suppliers instead of values (URIs in the example). The collector can then choose
to call (or not call) `Supplier::get` and properly catch exceptions thrown by it.

# Examples

All the following examples take this data as input. The first and third strings are valid URIs: `URI::create` parses them OK. The second one make the same method choke and throw an `IllegalArgumentException`.

```java
Collection<String> data = asList("http://elevated", "invalid\nurl", "http://abstractions");
```

Let's review different ways to manage a map operation of `URI::create` on this data.

## Discarding failures

The `discarding` collector swallows exceptions of specified types. Exception types have to be explicitly passed in order to propagate other exceptions. The following code results in a stream containing `"http://elevated"` and `"http://abstractions"`.

```java
data.stream()
    .map(lazy(URI::create))
    .collect(discarding(IllegalArgumentException.class));
```

## Silently stopping the computation on failure, returning partial results

The `upTo` collector also swallows exception, but it does not continue to read the input once the first exception is thrown. The following code results in a stream containing `"http://elevated"`.

```java
data.stream()
    .map(lazy(URI::create))
    .collect(upTo(IllegalArgumentException.class));
```

## Throwing on failure, keeping partial results

The `upToAndThrow` collector wraps the first exception (of specified types) it encounters in a `FailFastCollectException`. The interesting bit is that the `FailFastCollectException` gives you access to the results that were successfully computed prior to the failure. In the following code, `e.getResults()` returns a stream containing `"http://elevated"`.

```java
try {
    data.stream()
        .map(lazy(URI::create))
        .collect(upToAndThrow(IllegalArgumentException.class));
} catch (FailFastCollectException e) {
    e.getCause();
    e.getResults();
}
```

## Throwing at the end, keeping partial results

The `throwingAtEnd` collector reads the whole input, collecting both results and exceptions as it goes. Once the stream is fully read, it throws a `CollectException` that gives access to what it's been collecting. In the following code, `e.getResults()` returns a stream containing `"http://elevated"` and `"http://abstractions"` while `e.getCauses()` return a collection containing an `IllegalArgumentException` instance.

```java
try {
    data.stream()
        .map(lazy(URI::create))
        .collect(throwingAtEnd(IllegalArgumentException.class));
} catch (CollectException e) {
    e.getCauses();
    e.getResults();
}
```

# Dealing With Checked Exceptions

This method can also be used with checked exceptions, if they are first wrapped into unchecked ones.

```java
data.stream()
    .map(lazy(sneaky(URI::new, e -> new CustomRuntimeException(e))))
    .collect(discarding(CustomRuntimeException.class));
```

The above code is simplified if the library provides a default wrapping.

```java
data.stream()
    .map(lazy(sneaky(URI::new)))
    .collect(discardingFailures());
```

# Acknowledgement

The motivation for this experiment was triggered by the work of Yohan Legat, a co-worker at 
[Zenika](http://zenika.com). His work is also on 
[GitHub](https://github.com/Zenika/Blogs/tree/master/20140214-Try) 
and he wrote a article (in French) on 
[Zenika's technical blog](http://blog.zenika.com/index.php?post/2014/02/19/Repenser-la-propagation-des-exceptions-avec-Java-8).

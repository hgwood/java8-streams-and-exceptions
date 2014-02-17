package com.zenika.bulky;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import static com.zenika.bulky.Bulky.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AcceptanceTest
{
    private final List<String> uris = asList("http://validUri", "ftp://anotherValidUri", "invalid\nuri", "http://goldenUri");

    @Test public void ignoringFailures_returns_all_result_successfully_computed() throws Exception {
        Collection<URI> result = uris.stream()
            .map(lazy(sneaky(URI::new)))
            .collect(ignoringFailures())
            .collect(toList());
        assertThat(result, contains(new URI(uris.get(0)), new URI(uris.get(1)), new URI(uris.get(3))));
    }

    @Test(expected = NullPointerException.class)
    public void ignoringFailures_lets_runtime_exceptions_propagate_normally() throws Exception {
        uris.stream()
            .map(lazy(uri -> { throw new NullPointerException(); }))
            .collect(ignoringFailures())
            .collect(toList());
    }

    @Test public void ignoringFailures_supports_chained_maps_using_lift() throws Exception {
        Collection<URI> result = uris.stream()
            .map(lazy(sneaky(URI::new)))
            .map(lift(sneaky(uri -> {
                if (uri.toString().startsWith("ftp")) throw new URISyntaxException("", "");
                else return uri;
            })))
            .collect(ignoringFailures())
            .collect(toList());
        assertThat(result, contains(new URI(uris.get(0)), new URI(uris.get(3))));
    }

    @Test public void upToFailure_returns_results_successfully_computed_before_the_failure() throws Exception {
        Collection<URI> result = uris.stream()
            .map(lazy(sneaky(URI::new)))
            .collect(uptoFailure())
            .collect(toList());
        assertThat(result, contains(new URI(uris.get(0)), new URI(uris.get(1))));
    }

    @Test public void throwingWithPartialResult_throws_an_exception_containing_both_the_cause_and_the_results_successfully_computed_before_the_failure() throws Exception {
        try {
            uris.stream()
                .map(lazy(sneaky(URI::new)))
                .collect(throwingWithPartialResult());
            fail();
        } catch (BulkyException e) {
            assertThat(e.getCause(), instanceOf(URISyntaxException.class));
            assertThat(e.partialResult(), contains(new URI(uris.get(0)), new URI(uris.get(1))));
        }
    }

    @Test public void throwingWithPartialResult_does_not_throw_an_exception_if_all_results_compute_successfully() throws Exception {
        Collection<URI> result = uris.stream().limit(2)
            .map(lazy(sneaky(URI::new)))
            .collect(throwingWithPartialResult())
            .collect(toList());
        assertThat(result, contains(new URI(uris.get(0)), new URI(uris.get(1))));
    }
}

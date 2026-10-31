package io.github.andercmd.autto.core.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class UniqueTest {

    @Test
    void valuesNeverRepeatAcrossThreads() {
        Set<String> values = java.util.concurrent.ConcurrentHashMap.newKeySet();
        IntStream.range(0, 2000).parallel().forEach(i -> values.add(Unique.email()));

        assertThat(values).hasSize(2000);
        assertThat(new HashSet<>(values)).allMatch(v -> v.endsWith("@example.test"));
    }

    @Test
    void prefixIsNormalised() {
        assertThat(Unique.of("My Customer!")).startsWith("my-customer-");
    }
}

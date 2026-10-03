package io.github.andercmd.autto.core.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Key/value store shared by all step definition classes of ONE scenario.
 *
 * <p>Cucumber's PicoContainer creates a fresh instance per scenario and injects it in any step class that declares
 * it in its constructor, so data never leaks between scenarios (or threads).
 *
 * <pre>{@code
 * public CheckoutSteps(ScenarioContext context) { this.context = context; }
 * context.put(ContextKey.ORDER_TOTAL, total);
 * }</pre>
 */
public class ScenarioContext {

    private final Map<String, Object> values = new HashMap<>();

    public <T> void put(String key, T value) {
        values.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        return find(key, type).orElseThrow(() -> new IllegalStateException(
                "Nothing stored in the scenario context under '" + key + "'"));
    }

    public <T> Optional<T> find(String key, Class<T> type) {
        return Optional.ofNullable(values.get(key)).map(type::cast);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }
}

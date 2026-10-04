package io.github.andercmd.autto.core.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class NetworkMockTest {

    private static final List<NetworkMock.Rule> RULES = List.of(
            new NetworkMock.Rule("/api/cart", NetworkMock.Action.respond(500, "application/json", "{}")),
            new NetworkMock.Rule("googletagmanager.com", NetworkMock.Action.fail()),
            new NetworkMock.Rule("/api/", NetworkMock.Action.delay(Duration.ofSeconds(1))));

    @Test
    void firstMatchingRuleWins() {
        assertThat(NetworkMock.decide(RULES, "https://shop.test/api/cart/42"))
                .containsInstanceOf(NetworkMock.Action.Respond.class);
        assertThat(NetworkMock.decide(RULES, "https://shop.test/api/products"))
                .containsInstanceOf(NetworkMock.Action.Delay.class);
        assertThat(NetworkMock.decide(RULES, "https://www.googletagmanager.com/gtm.js"))
                .containsInstanceOf(NetworkMock.Action.Fail.class);
    }

    @Test
    void unmatchedRequestsPassThrough() {
        assertThat(NetworkMock.decide(RULES, "https://shop.test/index.html")).isEmpty();
    }

    @Test
    void closingWithoutRulesIsHarmless() {
        NetworkMock mock = new NetworkMock();

        mock.close();

        assertThat(mock.rules()).isEmpty();
    }

    @Test
    void dockerTargetFailsFastWithAClearMessage() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> NetworkMock.requireSupportedTarget(io.github.andercmd.autto.core.config.ExecutionTarget.DOCKER))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("docker");
        NetworkMock.requireSupportedTarget(io.github.andercmd.autto.core.config.ExecutionTarget.LOCAL);
        NetworkMock.requireSupportedTarget(io.github.andercmd.autto.core.config.ExecutionTarget.REMOTE);
    }
}

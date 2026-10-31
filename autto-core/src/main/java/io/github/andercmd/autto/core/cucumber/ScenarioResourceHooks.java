package io.github.andercmd.autto.core.cucumber;

import io.cucumber.java.After;
import io.github.andercmd.autto.core.db.Database;
import io.github.andercmd.autto.core.network.NetworkMock;

/**
 * Releases what a scenario acquired through the scenario-scoped helpers: database clean-up statements and network
 * interception rules. Runs after the failure evidence has been collected and before the browser is closed.
 */
public class ScenarioResourceHooks {

    private final Database database;
    private final NetworkMock network;

    public ScenarioResourceHooks(Database database, NetworkMock network) {
        this.database = database;
        this.network = network;
    }

    @After(order = 50)
    public void release() {
        try {
            network.close();
        } finally {
            database.close();
        }
    }
}

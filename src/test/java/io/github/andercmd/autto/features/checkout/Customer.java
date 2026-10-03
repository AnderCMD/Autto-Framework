package io.github.andercmd.autto.features.checkout;

import io.github.andercmd.autto.core.data.TestData;
import net.datafaker.Faker;

/** Shipping information typed during checkout. */
public record Customer(String firstName, String lastName, String postalCode) {

    public static Customer random() {
        Faker faker = TestData.faker();
        return new Customer(faker.name().firstName(), faker.name().lastName(), faker.address().zipCode());
    }
}

package com.hillcommerce.backend.migration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class V5MigrationPresenceTest {

    @Test
    void shouldContainV5Migration() {
        assertTrue(Files.exists(Path.of("src/main/resources/db/migration/V5__seed_product_and_order_schema.sql")));
    }
}

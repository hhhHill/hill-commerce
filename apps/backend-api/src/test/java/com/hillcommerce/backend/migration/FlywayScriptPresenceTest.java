package com.hillcommerce.backend.migration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayScriptPresenceTest {

    @Test
    void shouldContainInitialMigrationScripts() {
        assertTrue(Files.exists(Path.of("src/main/resources/db/migration/V1__init_user_and_auth.sql")));
        assertTrue(Files.exists(Path.of("src/main/resources/db/migration/V2__init_product.sql")));
        assertTrue(Files.exists(Path.of("src/main/resources/db/migration/V3__init_cart_and_order.sql")));
        assertTrue(Files.exists(Path.of("src/main/resources/db/migration/V4__init_analytics.sql")));
    }
}

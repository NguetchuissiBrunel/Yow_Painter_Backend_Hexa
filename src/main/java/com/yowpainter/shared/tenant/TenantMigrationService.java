package com.yowpainter.shared.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationService {

    private final DataSource dataSource;
    private final org.springframework.core.env.Environment environment;

    public void migrateTenant(UUID organizationId) {
        if (java.util.Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            log.info("Skipping tenant migration in test profile");
            return;
        }
        String schemaName = "tenant_" + organizationId.toString().replace("-", "_");
        createSchemaIfNotExist(schemaName);

        log.info("Running Flyway migration for tenant schema: {}", schemaName);
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .load();

        flyway.migrate();
        log.info("Flyway migration completed for tenant schema: {}", schemaName);
    }

    private void createSchemaIfNotExist(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            log.info("Schema {} created or already exists", schemaName);
        } catch (SQLException e) {
            log.error("Failed to create schema {}", schemaName, e);
            throw new RuntimeException("Could not create schema " + schemaName, e);
        }
    }
}

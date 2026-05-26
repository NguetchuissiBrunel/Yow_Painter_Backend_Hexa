package com.yowpainter.shared.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final DataSource dataSource;

    /**
     * Crée un nouveau schéma PostgreSQL pour le tenant et y exécute les migrations Flyway.
     * @param tenantId L'identifiant (slug) du tenant.
     */
    public void provisionTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank() || "public".equals(tenantId)) {
            return;
        }

        log.info("Provisioning schema for tenant: {}", tenantId);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // 1. Création du schéma s'il n'existe pas
            // On entoure le nom du schéma de doubles quotes pour supporter les tirets et autres caractères spéciaux
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
            
            // 2. Exécution des migrations Flyway pour ce schéma spécifique
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(tenantId)
                    .locations("classpath:db/migration/tenants")
                    .baselineOnMigrate(true)
                    .load();
            
            flyway.migrate();
            
            log.info("Schema {} provisioned and migrated successfully.", tenantId);

        } catch (SQLException e) {
            log.error("Error creating schema for tenant: " + tenantId, e);
            throw new RuntimeException("Could not create tenant schema", e);
        }
    }
}

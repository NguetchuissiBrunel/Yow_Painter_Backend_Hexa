package com.yowpainter.shared.kernel.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ksm.kernel.legacy-migration.enabled", havingValue = "true")
public class KernelLegacyDataMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Démarrage migration legacy organizationId (schéma courant)...");

        int artworks = jdbcTemplate.update("""
                UPDATE artwork a
                SET organization_id = ar.organization_id
                FROM artist ar
                WHERE a.artist_id = ar.id
                  AND ar.organization_id IS NOT NULL
                  AND a.organization_id IS NULL
                """);

        int products = jdbcTemplate.update("""
                UPDATE product p
                SET organization_id = ar.organization_id
                FROM artist ar
                WHERE p.artist_id = ar.id
                  AND ar.organization_id IS NOT NULL
                  AND p.organization_id IS NULL
                """);

        int orders = jdbcTemplate.update("""
                UPDATE shop_order o
                SET organization_id = p.organization_id
                FROM order_item oi
                JOIN product p ON p.id = oi.product_id
                WHERE oi.order_id = o.id
                  AND p.organization_id IS NOT NULL
                  AND o.organization_id IS NULL
                """);

        log.info("Migration legacy terminée: artworks={}, products={}, orders={}", artworks, products, orders);
    }
}

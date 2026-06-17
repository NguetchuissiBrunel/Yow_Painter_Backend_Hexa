package com.yowpainter.shared.tenant;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class TenantBootstrapper implements ApplicationRunner {

    private final ArtistRepositoryPort artistRepository;
    private final TenantMigrationService tenantMigrationService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing multi-tenant schema migrations...");
        try {
            List<Artist> activeArtists = artistRepository.findByStatus("ACTIVE");
            log.info("Found {} active artists to migrate", activeArtists.size());
            for (Artist artist : activeArtists) {
                if (artist.getOrganizationId() != null) {
                    tenantMigrationService.migrateTenant(artist.getOrganizationId());
                } else {
                    log.warn("Artist {} is ACTIVE but has no organizationId", artist.getEmail());
                }
            }
            log.info("Multi-tenant schema migrations initialization completed.");
        } catch (Exception e) {
            log.error("Error during tenant schema bootstrapping", e);
        }
    }
}

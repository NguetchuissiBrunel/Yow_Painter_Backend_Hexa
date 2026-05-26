package com.yowpainter.modules.subscription.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.subscription.domain.model.Subscription;
import com.yowpainter.modules.subscription.domain.model.SubscriptionPlan;
import com.yowpainter.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepositoryPort subscriptionRepository;
    private final ArtistRepositoryPort artistRepository;
    private final com.yowpainter.modules.payment.application.service.PaymentService paymentService;
    private final com.yowpainter.shared.tenant.TenantIdentifierResolver tenantResolver;

    public SubscriptionService(
            SubscriptionRepositoryPort subscriptionRepository,
            ArtistRepositoryPort artistRepository,
            @org.springframework.context.annotation.Lazy com.yowpainter.modules.payment.application.service.PaymentService paymentService,
            com.yowpainter.shared.tenant.TenantIdentifierResolver tenantResolver) {
        this.subscriptionRepository = subscriptionRepository;
        this.artistRepository = artistRepository;
        this.paymentService = paymentService;
        this.tenantResolver = tenantResolver;
    }

    public Subscription getSubscriptionForArtist(String email) {
        Artist artist = artistRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouvé"));
        return subscriptionRepository.findByArtistId(artist.getId())
                .orElseGet(() -> createDefaultSubscription(artist.getId()));
    }

    @Transactional
    public Subscription createDefaultSubscription(UUID artistId) {
        return subscriptionRepository.save(Subscription.builder()
                .artistId(artistId)
                .plan(SubscriptionPlan.FREE)
                .startDate(LocalDateTime.now())
                .isActive(true)
                .build());
    }

    @Transactional
    public void upgradePlan(String email, SubscriptionPlan plan) {
        Artist artist = artistRepository.findByEmail(email).orElseThrow();
        Subscription sub = subscriptionRepository.findByArtistId(artist.getId())
                .orElseGet(() -> createDefaultSubscription(artist.getId()));
        
        sub.setPlan(plan);
        sub.setStartDate(LocalDateTime.now());
        // Simuler 30 jours
        sub.setEndDate(LocalDateTime.now().plusDays(30));
        subscriptionRepository.save(sub);
    }

    @Transactional
    public String initiateSubscriptionUpgrade(String email, SubscriptionPlan plan, String phoneNumber) {
        Artist artist = artistRepository.findByEmail(email).orElseThrow();
        String tenantId = tenantResolver.resolveCurrentTenantIdentifier();
        
        // On utilise l'ID de l'artiste comme référence. 
        // Note: Si l'utilisateur fait plusieurs tentatives, cela écrasera la précédente dans le polling
        return paymentService.initiateMobileMoneyPayment(
                artist.getId(), 
                "SUBSCRIPTION", 
                plan.getPrice(), 
                tenantId, 
                email, 
                phoneNumber
        );
    }

    @Transactional
    public void confirmUpgrade(UUID artistId, SubscriptionPlan plan) {
        Subscription sub = subscriptionRepository.findByArtistId(artistId)
                .orElseGet(() -> createDefaultSubscription(artistId));
        
        sub.setPlan(plan);
        sub.setStartDate(LocalDateTime.now());
        sub.setEndDate(LocalDateTime.now().plusDays(30));
        sub.setActive(true);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void cancelSubscription(String email) {
        Artist artist = artistRepository.findByEmail(email).orElseThrow();
        subscriptionRepository.findByArtistId(artist.getId()).ifPresent(sub -> {
            sub.setActive(false);
            subscriptionRepository.save(sub);
        });
    }
}

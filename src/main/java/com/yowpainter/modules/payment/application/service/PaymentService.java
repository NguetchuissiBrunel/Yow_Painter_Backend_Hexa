package com.yowpainter.modules.payment.application.service;

import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.event.application.service.EventService;
import com.yowpainter.modules.shop.domain.model.OrderStatus;
import com.yowpainter.modules.shop.domain.model.Payment;
import com.yowpainter.modules.shop.domain.model.PaymentStatus;
import com.yowpainter.modules.shop.domain.port.out.PaymentRepositoryPort;
import com.yowpainter.modules.shop.application.service.ShopService;
import com.yowpainter.modules.payment.infrastructure.adapter.in.web.dto.PaymentResponse;
import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.subscription.application.service.SubscriptionService;
import com.yowpainter.modules.payment.infrastructure.adapter.out.external.CampayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepositoryPort paymentRepository;
    private final AppUserRepositoryPort userRepository;
    private final ShopService shopService;
    private final EventService eventService;
    private final CampayClient campayClient;
    private final WalletService walletService;
    private final com.yowpainter.modules.notification.application.service.NotificationService notificationService;
    private final com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort artistRepository;
    private final com.yowpainter.modules.auth.application.service.EmailService emailService;
    private final com.yowpainter.modules.subscription.application.service.SubscriptionService subscriptionService;

    public PaymentService(
            PaymentRepositoryPort paymentRepository,
            AppUserRepositoryPort userRepository,
            ShopService shopService,
            EventService eventService,
            CampayClient campayClient,
            WalletService walletService,
            com.yowpainter.modules.notification.application.service.NotificationService notificationService,
            com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort artistRepository,
            com.yowpainter.modules.auth.application.service.EmailService emailService,
            @org.springframework.context.annotation.Lazy com.yowpainter.modules.subscription.application.service.SubscriptionService subscriptionService) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.shopService = shopService;
        this.eventService = eventService;
        this.campayClient = campayClient;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.artistRepository = artistRepository;
        this.emailService = emailService;
        this.subscriptionService = subscriptionService;
    }

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public List<PaymentResponse> getPaymentHistory(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String initiateMobileMoneyPayment(UUID referenceId, String type, BigDecimal amount, String tenantId, String userEmail, String phoneNumber) {
        try {
            // S'assurer que l'utilisateur existe
            AppUser user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

            // Obtenir le token Campay
            String token = campayClient.getToken();

            // Créer la requête de collecte
            CampayClient.CollectRequest collectRequest = CampayClient.CollectRequest.builder()
                    .amount(amount.toString())
                    .from(phoneNumber) // Format attendu : 237xxxxxxxxx
                    .description("Paiement YowPainter - " + (type.equals("ORDER") ? "Commande" : "Billet"))
                    .external_reference(referenceId.toString())
                    .currency("XAF")
                    .build();

            // Lancer la collecte
            CampayClient.CollectResponse collectResponse = campayClient.collect(token, collectRequest);

            // Créer l'enregistrement de paiement local
            Payment payment = Payment.builder()
                    .userId(user.getId())
                    .referenceId(referenceId)
                    .referenceType(type)
                    .amount(amount)
                    .currency("XAF")
                    .status(PaymentStatus.PENDING)
                    .providerReference(collectResponse.getReference())
                    .phoneNumber(phoneNumber)
                    .tenantId(tenantId)
                    .build();
            paymentRepository.save(payment);

            return collectResponse.getReference();
        } catch (Exception e) {
            log.error("Erreur lors de l'initiation du paiement CamPay", e);
            throw new RuntimeException("Erreur de paiement mobile money", e);
        }
    }

    @Transactional
    public void processSuccessfulPayment(String providerReference, String externalReference) {
        UUID referenceId = UUID.fromString(externalReference);
        
        log.info("Processing successful payment for reference: {}", referenceId);

        // Mettre à jour l'enregistrement de paiement local
        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Paiement non trouve pour la reference: " + referenceId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Payment already processed for reference: {}", referenceId);
            return;
        }

        // VERIFICATION DE SECURITE : On interroge CamPay directement pour confirmer le statut et le montant
        if (!verifyPaymentWithProvider(payment, providerReference)) {
            log.error("Payment verification failed for reference: {}. Possible fraud attempt.", referenceId);
            processFailedPayment(providerReference, externalReference, "VERIFICATION_FAILED");
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setProviderReference(providerReference);
        paymentRepository.save(payment);

        AppUser user = userRepository.findById(payment.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouve"));

        String referenceType = payment.getReferenceType();
        try {
            UUID userId = payment.getUserId();

            if ("ORDER".equals(referenceType) || "RESERVATION".equals(referenceType)) {
                // ... logique existante de commande/réservation ...
                if ("ORDER".equals(referenceType)) {
                    shopService.updateOrderStatus(referenceId, OrderStatus.PAID);
                    notificationService.createNotification(userId, "Votre commande #" + referenceId.toString().substring(0, 8) + " a été payée avec succès !");
                    emailService.sendPaymentConfirmation(user.getEmail(), referenceId.toString().substring(0, 8), payment.getAmount());
                } else {
                    eventService.confirmPaidReservation(referenceId);
                    notificationService.createNotification(userId, "Votre réservation a été confirmée !");
                    emailService.sendPaymentConfirmation(user.getEmail(), "Réservation #" + referenceId.toString().substring(0, 8), payment.getAmount());
                }
                
                // NOTIFICATION ET CREDIT DU WALLET DE L'ARTISTE
                var artistOpt = artistRepository.findBySlug(payment.getTenantId());
                if (artistOpt.isPresent()) {
                    Artist artist = artistOpt.get();
                    
                    // Calcul de la commission dynamique basée sur l'abonnement
                    var sub = subscriptionService.getSubscriptionForArtist(artist.getEmail());
                    java.math.BigDecimal rate = sub.getPlan().getCommissionRate();
                    java.math.BigDecimal commission = payment.getAmount().multiply(rate);
                    java.math.BigDecimal netAmount = payment.getAmount().subtract(commission);
                    
                    // Créditer le portefeuille
                    walletService.creditWallet(
                        artist, 
                        netAmount, 
                        com.yowpainter.modules.payment.domain.model.WalletTransactionType.SALE, 
                        payment.getId(), 
                        "Vente " + (referenceType.equals("ORDER") ? "oeuvre" : "billet") + " #" + referenceId.toString().substring(0, 8)
                    );
                    
                    emailService.sendNewSaleNotification(artist.getEmail(), referenceId.toString().substring(0, 8), netAmount);
                }
                
            } else if ("SUBSCRIPTION".equals(referenceType)) {
                com.yowpainter.modules.subscription.domain.model.SubscriptionPlan plan = deducePlanFromAmount(payment.getAmount());
                subscriptionService.confirmUpgrade(referenceId, plan);
                notificationService.createNotification(userId, "Votre abonnement " + plan.name() + " a été activé avec succès !");
                emailService.sendPaymentConfirmation(user.getEmail(), "Abonnement " + plan.name(), payment.getAmount());
            }
        } catch (Exception ex) {
            log.error("Erreur traitement paiement {}: {}", referenceId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Transactional
    public void processFailedPayment(String providerReference, String externalReference, String status) {
        UUID referenceId = UUID.fromString(externalReference);
        log.warn("Processing failed payment for reference: {} with status: {}", referenceId, status);

        Payment payment = paymentRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Paiement non trouve pour la reference: " + referenceId));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Attempt to fail an already SUCCEEDED payment: {}", referenceId);
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setProviderReference(providerReference);
        paymentRepository.save(payment);

        // Notifier l'utilisateur
        notificationService.createNotification(payment.getUserId(), "Le paiement pour votre " + 
                (payment.getReferenceType().equals("ORDER") ? "commande" : "réservation") + 
                " a échoué. (" + status + ")");
    }

    private com.yowpainter.modules.subscription.domain.model.SubscriptionPlan deducePlanFromAmount(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("25000")) >= 0) return com.yowpainter.modules.subscription.domain.model.SubscriptionPlan.ELITE;
        if (amount.compareTo(new BigDecimal("10000")) >= 0) return com.yowpainter.modules.subscription.domain.model.SubscriptionPlan.PRO;
        return com.yowpainter.modules.subscription.domain.model.SubscriptionPlan.FREE;
    }

    private boolean verifyPaymentWithProvider(Payment payment, String providerReference) {
        try {
            String token = campayClient.getToken();
            CampayClient.TransactionStatusResponse status = campayClient.checkTransactionStatus(token, providerReference);
            
            // 1. Vérifier le statut chez le fournisseur
            if (!"SUCCESSFUL".equals(status.getStatus())) {
                log.warn("Provider status is not SUCCESSFUL: {}", status.getStatus());
                return false;
            }
            
            // 2. Vérifier que le montant correspond (sécurité contre le changement de prix côté client)
            BigDecimal providerAmount = new BigDecimal(status.getAmount());
            if (providerAmount.compareTo(payment.getAmount()) != 0) {
                log.error("Amount mismatch! Expected: {}, Provider reported: {}", payment.getAmount(), providerAmount);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error during payment verification", e);
            return false;
        }
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .referenceId(payment.getReferenceId())
                .referenceType(payment.getReferenceType())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

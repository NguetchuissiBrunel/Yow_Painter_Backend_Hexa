package com.yowpainter.modules.payment.application.service;

import com.yowpainter.modules.payment.infrastructure.adapter.out.external.CampayClient;
import com.yowpainter.modules.shop.domain.model.Payment;
import com.yowpainter.modules.shop.domain.model.PaymentStatus;
import com.yowpainter.modules.shop.domain.port.out.PaymentRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPollingService {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentService paymentService;
    private final CampayClient campayClient;
    private final com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort artistRepository;

    /**
     * S'exécute toutes les 10 minutes pour vérifier les paiements en attente.
     */
    @Scheduled(fixedDelay = 600000) // 10 minutes
    public void pollPendingPayments() {
        log.info("Starting payment polling for PENDING transactions across tenants...");
        
        List<com.yowpainter.modules.artist.domain.model.Artist> activeArtists;
        try {
            activeArtists = artistRepository.findByStatus("ACTIVE");
        } catch (Exception e) {
            log.error("Failed to query active artists for payment polling", e);
            return;
        }

        for (com.yowpainter.modules.artist.domain.model.Artist artist : activeArtists) {
            if (artist.getOrganizationId() == null) {
                continue;
            }
            try {
                com.yowpainter.shared.context.OrganizationContext.setOrganizationId(artist.getOrganizationId());
                LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
                List<Payment> pendingPayments = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);
                if (!pendingPayments.isEmpty()) {
                    log.info("Found {} pending payments for tenant {}", pendingPayments.size(), artist.getOrganizationId());
                    for (Payment payment : pendingPayments) {
                        try {
                            checkAndUpdatePaymentStatus(payment);
                        } catch (Exception e) {
                            log.error("Failed to update status for payment reference: {}", payment.getReferenceId(), e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to poll payments for tenant {}", artist.getOrganizationId(), e);
            } finally {
                com.yowpainter.shared.context.OrganizationContext.clear();
            }
        }
    }

    private void checkAndUpdatePaymentStatus(Payment payment) throws Exception {
        String token = campayClient.getToken();
        CampayClient.TransactionStatusResponse response = campayClient.checkTransactionStatus(token, payment.getProviderReference());
        
        log.info("Checking status for reference {}: Provider status is {}", payment.getReferenceId(), response.getStatus());

        if ("SUCCESSFUL".equals(response.getStatus())) {
            paymentService.processSuccessfulPayment(payment.getProviderReference(), payment.getReferenceId().toString());
        } else if ("FAILED".equals(response.getStatus())) {
            paymentService.processFailedPayment(payment.getProviderReference(), payment.getReferenceId().toString(), "FAILED_BY_POLLING");
        } else if ("CANCELLED".equals(response.getStatus())) {
            paymentService.processFailedPayment(payment.getProviderReference(), payment.getReferenceId().toString(), "CANCELLED_BY_POLLING");
        } else {
            // Toujours PENDING chez le fournisseur, on laisse tel quel pour le prochain tour
            log.debug("Payment {} still pending at provider side", payment.getReferenceId());
        }
    }
}

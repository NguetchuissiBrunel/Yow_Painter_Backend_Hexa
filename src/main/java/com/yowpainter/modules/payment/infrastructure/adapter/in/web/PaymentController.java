package com.yowpainter.modules.payment.infrastructure.adapter.in.web;

import com.yowpainter.modules.payment.infrastructure.adapter.in.web.dto.PaymentResponse;
import com.yowpainter.modules.payment.application.service.PaymentService;
import com.yowpainter.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Intégration Mobile Money (MTN, Orange) via CamPay")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/callback")
    @Operation(summary = "Point d'entrée pour les callbacks CamPay")
    public ResponseEntity<String> handleCampayCallback(@RequestBody Map<String, String> payload) {
        log.info("Received CamPay Callback: {}", payload);

        String status = payload.get("status");
        String reference = payload.get("reference");
        String externalReference = payload.get("external_reference");

        if ("SUCCESSFUL".equals(status)) {
            paymentService.processSuccessfulPayment(reference, externalReference);
        } else {
            paymentService.processFailedPayment(reference, externalReference, status);
        }

        return ResponseEntity.ok("Received");
    }

    @GetMapping("/history")
    @Operation(summary = "Consulter son historique de paiements")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userDetails.getUsername()));
    }
}


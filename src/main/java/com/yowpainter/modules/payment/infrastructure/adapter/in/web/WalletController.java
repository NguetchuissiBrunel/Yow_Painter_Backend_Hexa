package com.yowpainter.modules.payment.infrastructure.adapter.in.web;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.payment.domain.model.Wallet;
import com.yowpainter.modules.payment.domain.model.WalletTransaction;
import com.yowpainter.modules.payment.application.service.PayoutService;
import com.yowpainter.modules.payment.application.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet & Payout", description = "Gestion des gains et retraits pour les artistes")
public class WalletController {

    private final WalletService walletService;
    private final PayoutService payoutService;
    private final ArtistRepositoryPort artistRepository;

    @GetMapping("/balance")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Consulter son solde actuel (Artiste)")
    public ResponseEntity<Wallet> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        Artist artist = artistRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(walletService.getWalletForArtist(artist));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Consulter l'historique des transactions financières")
    public ResponseEntity<List<WalletTransaction>> getTransactionHistory(@AuthenticationPrincipal UserDetails userDetails) {
        Artist artist = artistRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(walletService.getTransactionHistory(artist.getId()));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Demander un virement Mobile Money de ses gains")
    public ResponseEntity<Map<String, String>> requestPayout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam BigDecimal amount) {
        
        String reference = payoutService.requestPayout(userDetails.getUsername(), amount);
        return ResponseEntity.ok(Map.of(
            "message", "Retrait initié avec succès",
            "providerReference", reference
        ));
    }

    @PostMapping("/settings/payout")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Configurer ses informations de retrait (Mobile Money)")
    public ResponseEntity<Void> updatePayoutSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String phoneNumber,
            @RequestParam String network) {
        
        Artist artist = artistRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        artist.setPayoutPhone(phoneNumber);
        artist.setPayoutNetwork(network);
        artistRepository.save(artist);
        
        return ResponseEntity.ok().build();
    }
}

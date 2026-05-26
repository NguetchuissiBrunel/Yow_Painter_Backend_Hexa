package com.yowpainter.modules.payment.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.payment.domain.model.Wallet;
import com.yowpainter.modules.payment.domain.model.WalletTransaction;
import com.yowpainter.modules.payment.domain.model.WalletTransactionType;
import com.yowpainter.modules.payment.domain.port.out.WalletRepositoryPort;
import com.yowpainter.modules.payment.domain.port.out.WalletTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepositoryPort walletRepository;
    private final WalletTransactionRepositoryPort transactionRepository;

    public Wallet getWalletForArtist(Artist artist) {
        return walletRepository.findByArtistId(artist.getId())
                .orElseGet(() -> createWallet(artist));
    }

    @Transactional
    public Wallet createWallet(Artist artist) {
        log.info("Creating new wallet for artist: {}", artist.getArtistName());
        Wallet wallet = Wallet.builder()
                .artist(artist)
                .balance(BigDecimal.ZERO)
                .currency("XAF")
                .build();
        return walletRepository.save(wallet);
    }

    @Transactional
    public void creditWallet(Artist artist, BigDecimal amount, WalletTransactionType type, UUID referenceId, String description) {
        Wallet wallet = getWalletForArtist(artist);
        
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .referenceId(referenceId)
                .description(description)
                .build();
        transactionRepository.save(transaction);
        
        log.info("Credited {} to wallet of artist {} (Type: {})", amount, artist.getArtistName(), type);
    }

    @Transactional
    public void debitWallet(Artist artist, BigDecimal amount, WalletTransactionType type, UUID referenceId, String description) {
        Wallet wallet = getWalletForArtist(artist);
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Solde insuffisant dans le portefeuille");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .type(type)
                .referenceId(referenceId)
                .description(description)
                .build();
        transactionRepository.save(transaction);
        
        log.info("Debited {} from wallet of artist {} (Type: {})", amount, artist.getArtistName(), type);
    }

    public List<WalletTransaction> getTransactionHistory(UUID artistId) {
        Wallet wallet = walletRepository.findByArtistId(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Portefeuille non trouvé"));
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}

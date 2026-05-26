package com.yowpainter.modules.payment.domain.model;

public enum WalletTransactionType {
    SALE,        // Vente d'une oeuvre ou d'un billet
    WITHDRAWAL,  // Retrait de fonds (Payout)
    COMMISSION   // Prélèvement plateforme (optionnel si on suit juste le NET)
}

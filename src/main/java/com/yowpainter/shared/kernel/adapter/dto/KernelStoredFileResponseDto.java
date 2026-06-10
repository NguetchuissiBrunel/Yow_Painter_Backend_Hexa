package com.yowpainter.shared.kernel.adapter.dto;

import java.util.UUID;

public record KernelStoredFileResponseDto(
        UUID id,
        UUID organizationId,
        UUID uploadedByUserId,
        String fileName,
        String contentType,
        long size,
        String documentType,
        String analysisStatus,
        String analysisReason
) {
}

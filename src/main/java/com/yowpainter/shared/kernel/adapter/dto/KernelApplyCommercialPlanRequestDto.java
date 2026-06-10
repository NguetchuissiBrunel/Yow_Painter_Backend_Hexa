package com.yowpainter.shared.kernel.adapter.dto;

import java.util.List;

public record KernelApplyCommercialPlanRequestDto(String planCode, List<String> addOnCodes) {
    public KernelApplyCommercialPlanRequestDto(String planCode) {
        this(planCode, List.of());
    }
}

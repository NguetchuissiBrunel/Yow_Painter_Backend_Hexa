package com.yowpainter.modules.file.infrastructure.adapter.in.web.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileResponseDto {
    private UUID id;
    private String fileName;
    private String contentType;
    private String downloadUrl;
}

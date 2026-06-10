package com.yowpainter.shared.kernel.port;

import java.util.UUID;

public interface KernelFilePort {

    FileView upload(UploadFileCommand command, String accessToken);

    record UploadFileCommand(
            UUID organizationId,
            byte[] content,
            String fileName,
            String contentType,
            String documentType
    ) {
    }

    record FileView(
            UUID id,
            String fileName,
            String contentType,
            String downloadUrl
    ) {
    }
}

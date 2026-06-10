package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelStoredFileResponseDto;
import com.yowpainter.shared.kernel.port.KernelFilePort;
import org.springframework.stereotype.Component;

@Component
public class KernelFileHttpAdapter implements KernelFilePort {

    private final KernelHttpClient kernelHttpClient;
    private final KernelProperties properties;

    public KernelFileHttpAdapter(KernelHttpClient kernelHttpClient, KernelProperties properties) {
        this.kernelHttpClient = kernelHttpClient;
        this.properties = properties;
    }

    @Override
    public FileView upload(UploadFileCommand command, String accessToken) {
        KernelStoredFileResponseDto response = kernelHttpClient.uploadMultipart(
                "/api/files",
                command.content(),
                command.fileName(),
                command.contentType(),
                command.documentType(),
                KernelStoredFileResponseDto.class,
                command.organizationId(),
                accessToken
        );
        String baseUrl = properties.baseUrl() == null ? "" : properties.baseUrl().replaceAll("/$", "");
        return new FileView(
                response.id(),
                response.fileName(),
                response.contentType(),
                baseUrl + "/api/files/" + response.id()
        );
    }
}

package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelCreateProductRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelProductResponseDto;
import com.yowpainter.shared.kernel.port.KernelProductPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KernelProductHttpAdapter implements KernelProductPort {

    private final KernelHttpClient kernelHttpClient;
    private final KernelProperties properties;

    public KernelProductHttpAdapter(KernelHttpClient kernelHttpClient, KernelProperties properties) {
        this.kernelHttpClient = kernelHttpClient;
        this.properties = properties;
    }

    @Override
    public ProductView createProduct(CreateProductCommand command, String accessToken) {
        KernelProductResponseDto response = kernelHttpClient.post(
                "/api/products",
                new KernelCreateProductRequestDto(
                        command.organizationId(),
                        command.sku(),
                        command.name(),
                        "ARTWORK",
                        "DEFAULT",
                        command.description(),
                        command.unitPrice(),
                        command.currency() != null ? command.currency() : properties.defaultCurrency(),
                        "ACTIVE"
                ),
                KernelProductResponseDto.class,
                command.organizationId(),
                accessToken
        );
        return map(response);
    }

    @Override
    public ProductView getProduct(UUID productId, String accessToken) {
        return map(kernelHttpClient.get("/api/products/" + productId, KernelProductResponseDto.class, null, accessToken));
    }

    @Override
    public List<ProductView> listProducts(UUID organizationId, String accessToken) {
        return kernelHttpClient.getListWithQuery(
                "/api/products",
                Map.of("organizationId", organizationId.toString()),
                KernelProductResponseDto.class,
                organizationId,
                accessToken
        ).stream().map(this::map).toList();
    }

    private ProductView map(KernelProductResponseDto dto) {
        return new ProductView(
                dto.id(),
                dto.organizationId(),
                dto.sku(),
                dto.name(),
                dto.description(),
                dto.unitPrice(),
                dto.currency(),
                dto.status()
        );
    }
}

package com.yowpainter.modules.shop.application.service;

import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.artwork.domain.model.Artwork;
import com.yowpainter.modules.shop.infrastructure.adapter.in.web.dto.*;
import com.yowpainter.modules.shop.domain.model.*;
import com.yowpainter.modules.shop.domain.port.out.OrderRepositoryPort;
import com.yowpainter.modules.shop.domain.port.out.ProductRepositoryPort;
import com.yowpainter.shared.context.RequestContext;
import com.yowpainter.shared.kernel.port.KernelSalesPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final OrderRepositoryPort orderRepository;
    private final ProductRepositoryPort productRepository;
    private final AppUserRepositoryPort appUserRepository;
    private final KernelCommerceService kernelCommerceService;
    private final KernelSalesPort kernelSalesPort;

    public ProductResponse createProduct(String artistEmail, ProductCreateRequest request) {
        return kernelCommerceService.createProduct(artistEmail, request);
    }

    public List<ProductResponse> getProductsByArtist(UUID artistId) {
        if (artistId == null) {
            return productRepository.findAll().stream()
                    .filter(Product::isActive)
                    .map(this::mapToProductResponse)
                    .collect(Collectors.toList());
        }
        return productRepository.findByArtistIdAndIsActiveTrue(artistId).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByArtistSlug(String slug) {
        return kernelCommerceService.getProductsByArtistSlug(slug);
    }

    public List<ProductResponse> getAllPublicProducts() {
        return kernelCommerceService.getAllPublicProducts();
    }

    public OrderResponse placeOrder(String buyerEmail, OrderCreateRequest request) {
        return kernelCommerceService.placeOrder(buyerEmail, request);
    }

    public List<OrderResponse> getMySales(String artistEmail) {
        return kernelCommerceService.getMySales(artistEmail);
    }

    public List<OrderResponse> getMyPurchases(String buyerEmail) {
        AppUser buyer = appUserRepository.findByEmail(buyerEmail).orElseThrow();
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId()).stream()
                .map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
        if (status == OrderStatus.PAID && order.getKernelSalesOrderId() != null) {
            try {
                kernelSalesPort.confirmOrder(order.getKernelSalesOrderId(), RequestContext.accessToken());
            } catch (Exception ex) {
                log.warn("Echec confirmation commande kernel {}: {}", order.getKernelSalesOrderId(), ex.getMessage());
            }
        }
    }

    public OrderResponse getOrderById(UUID orderId) {
        return kernelCommerceService.getOrderById(orderId);
    }

    public List<ProductResponse> getInventory(String artistEmail) {
        return kernelCommerceService.getInventory(artistEmail);
    }

    @Transactional
    public void cancelAbandonedOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Order> abandonedOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, threshold);

        for (Order order : abandonedOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Cancelled abandoned order: {}", order.getId());

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());

                if (product.getArtwork() != null) {
                    Artwork artwork = product.getArtwork();
                    if (artwork.getStatus() == com.yowpainter.modules.artwork.domain.model.ArtworkStatus.SOLD) {
                        artwork.setStatus(com.yowpainter.modules.artwork.domain.model.ArtworkStatus.ON_SALE);
                    }
                }
                productRepository.save(product);
            }
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        AppUser buyer = appUserRepository.findById(order.getBuyerId()).orElse(null);
        String buyerName = (buyer != null) ? buyer.getFirstName() + " " + buyer.getLastName() : "Inconnu";

        return OrderResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyerId())
                .buyerName(buyerName)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(i -> OrderItemResponse.builder()
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .artistId(product.getArtistId())
                .artworkId(product.getArtwork() != null ? product.getArtwork().getId() : null)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .isActive(product.isActive())
                .build();
    }
}

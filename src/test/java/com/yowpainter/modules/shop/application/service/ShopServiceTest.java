package com.yowpainter.modules.shop.application.service;

import com.yowpainter.modules.artwork.domain.model.Artwork;
import com.yowpainter.modules.artwork.domain.model.ArtworkStatus;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.shop.domain.model.*;
import com.yowpainter.modules.shop.domain.port.out.OrderRepositoryPort;
import com.yowpainter.modules.shop.domain.port.out.ProductRepositoryPort;
import com.yowpainter.shared.kernel.port.KernelSalesPort;
import com.yowpainter.modules.shop.infrastructure.adapter.in.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShopServiceTest {

    @Mock private ProductRepositoryPort productRepository;
    @Mock private OrderRepositoryPort orderRepository;
    @Mock private AppUserRepositoryPort appUserRepository;
    @Mock private KernelCommerceService kernelCommerceService;
    @Mock private KernelSalesPort kernelSalesPort;

    @InjectMocks
    private ShopService shopService;

    private AppUser buyer;
    private Product product;
    private Artwork artwork;
    private Order order;

    @BeforeEach
    void setUp() {
        buyer = AppUser.builder()
                .firstName("Marie").lastName("Dupont")
                .email("marie@example.com")
                .build();
        buyer.setId(UUID.randomUUID());

        artwork = Artwork.builder()
                .artistId(UUID.randomUUID())
                .title("Toile Unique")
                .status(ArtworkStatus.ON_SALE)
                .build();
        artwork.setId(UUID.randomUUID());

        product = Product.builder()
                .artistId(UUID.randomUUID())
                .artwork(artwork)
                .name("Toile Unique")
                .description("Une toile originale")
                .price(new BigDecimal("50000"))
                .stockQuantity(1)
                .isActive(true)
                .build();
        product.setId(UUID.randomUUID());

        order = Order.builder()
                .buyerId(buyer.getId())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("50000"))
                .shippingAddress("Yaoundé, Cameroun")
                .build();
        order.setId(UUID.randomUUID());
        OrderItem item = OrderItem.builder()
                .product(product).quantity(1).unitPrice(product.getPrice()).build();
        order.addItem(item);
    }

    @Test
    void createProduct_shouldDelegateToKernelCommerceService() {
        ProductCreateRequest request = ProductCreateRequest.builder().name("Test").build();
        ProductResponse expected = ProductResponse.builder().name("Test").build();
        when(kernelCommerceService.createProduct("jean@example.com", request)).thenReturn(expected);

        assertThat(shopService.createProduct("jean@example.com", request)).isEqualTo(expected);
    }

    // ─── updateOrderStatus ───────────────────────────────────────────────────

    @Test
    void updateOrderStatus_shouldChangeStatusAndSave() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        shopService.updateOrderStatus(order.getId(), OrderStatus.PAID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    // ─── getMyPurchases ──────────────────────────────────────────────────────

    @Test
    void getMyPurchases_shouldReturnBuyerOrders() {
        when(appUserRepository.findByEmail("marie@example.com")).thenReturn(Optional.of(buyer));
        when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId())).thenReturn(List.of(order));
        when(appUserRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));

        List<OrderResponse> result = shopService.getMyPurchases("marie@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    // ─── cancelAbandonedOrders ───────────────────────────────────────────────

    @Test
    void cancelAbandonedOrders_shouldCancelExpiredOrders() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        product.setStockQuantity(0);
        artwork.setStatus(ArtworkStatus.SOLD);

        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(order));

        shopService.cancelAbandonedOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStockQuantity()).isEqualTo(1); // restocked
        assertThat(artwork.getStatus()).isEqualTo(ArtworkStatus.ON_SALE); // restored
        verify(orderRepository).save(order);
        verify(productRepository).save(product);
    }

    @Test
    void cancelAbandonedOrders_whenNoExpiredOrders_shouldDoNothing() {
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of());

        shopService.cancelAbandonedOrders();

        verify(orderRepository, never()).save(any());
    }
}

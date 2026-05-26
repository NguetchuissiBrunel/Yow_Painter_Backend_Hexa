package com.yowpainter.modules.shop.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.artwork.domain.model.Artwork;
import com.yowpainter.modules.artwork.domain.port.out.ArtworkRepositoryPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.shop.infrastructure.adapter.in.web.dto.*;
import com.yowpainter.modules.shop.domain.model.*;
import com.yowpainter.modules.shop.domain.port.out.OrderRepositoryPort;
import com.yowpainter.modules.shop.domain.port.out.PaymentRepositoryPort;
import com.yowpainter.modules.shop.domain.port.out.ProductRepositoryPort;
import com.yowpainter.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ProductRepositoryPort productRepository;
    private final OrderRepositoryPort orderRepository;
    private final PaymentRepositoryPort paymentRepository;
    private final ArtworkRepositoryPort artworkRepository;
    private final ArtistRepositoryPort artistRepository;
    private final AppUserRepositoryPort appUserRepository;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public ProductResponse createProduct(String artistEmail, ProductCreateRequest request) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();

        Artwork artwork = null;
        if (request.getArtworkId() != null) {
            artwork = artworkRepository.findById(request.getArtworkId()).orElseThrow();
            if (!artwork.getArtistId().equals(artist.getId())) throw new IllegalStateException("Not authorized");
            artwork.setStatus(com.yowpainter.modules.artwork.domain.model.ArtworkStatus.ON_SALE);
            artworkRepository.save(artwork);
        }

        Product product = Product.builder()
                .artistId(artist.getId())
                .artwork(artwork)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(true)
                .build();

        return mapToProductResponse(productRepository.save(product));
    }

    public List<ProductResponse> getProductsByArtist(UUID artistId) {
        if (artistId == null) {
            // En multi-tenant, si pas d'ID, on prend tout ce qui est actif dans le schéma courant
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
        Artist artist = artistRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Artiste non trouve"));
        return productRepository.findByArtistIdAndIsActiveTrue(artist.getId()).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getAllPublicProducts() {
        List<Artist> artists = artistRepository.findAll();
        log.info("Starting parallel product aggregation for {} artists", artists.size());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.setReadOnly(true);

        List<CompletableFuture<List<ProductResponse>>> futures = artists.stream()
                .map(artist -> CompletableFuture.supplyAsync(() -> {
                    return TenantContext.executeInTenant(artist.getSlug(), () -> 
                        transactionTemplate.execute(status -> {
                            return productRepository.findByIsActiveTrue().stream()
                                    .map(this::mapToProductResponse)
                                    .collect(Collectors.toList());
                        })
                    );
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse placeOrder(String buyerEmail, OrderCreateRequest request) {
        AppUser buyer = appUserRepository.findByEmail(buyerEmail).orElseThrow();
        Product product = productRepository.findByIdWithPessimisticWriteLock(request.getProductId()).orElseThrow();

        if (!product.isActive() || product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Produit epuise ou quantite insuffisante");
        }

        product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
        productRepository.save(product);

        if (product.getStockQuantity() == 0 && product.getArtwork() != null) {
            Artwork artwork = product.getArtwork();
            artwork.setStatus(com.yowpainter.modules.artwork.domain.model.ArtworkStatus.SOLD);
            artworkRepository.save(artwork);
        }

        BigDecimal totalPrice = product.getPrice().multiply(new BigDecimal(request.getQuantity()));

        Order order = Order.builder()
                .buyerId(buyer.getId())
                .shippingAddress(request.getShippingAddress())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(totalPrice)
                .build();

        order.addItem(OrderItem.builder().product(product).quantity(request.getQuantity()).unitPrice(product.getPrice()).build());
        order = orderRepository.save(order);

        return mapToOrderResponse(order);
    }

    public List<OrderResponse> getMySales(String artistEmail) {
        // En schema-per-tenant, tous les ordres du schema courant appartiennent a l'artiste courant
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToOrderResponse).collect(Collectors.toList());
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
    }

    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Commande non trouvée"));
        return mapToOrderResponse(order);
    }

    public List<ProductResponse> getInventory(String artistEmail) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        return productRepository.findByArtistId(artist.getId()).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

@Transactional
    public void cancelAbandonedOrdersForTenant(String tenantId) {
        log.debug("Cleaning up orders for tenant: {}", tenantId);
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30); 
        List<Order> abandonedOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, threshold);
        
        for (Order order : abandonedOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("[{}] Cancelled abandoned order: {}", tenantId, order.getId());

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                
                if (product.getArtwork() != null) {
                    Artwork artwork = product.getArtwork();
                    if (artwork.getStatus() == com.yowpainter.modules.artwork.domain.model.ArtworkStatus.SOLD) {
                        artwork.setStatus(com.yowpainter.modules.artwork.domain.model.ArtworkStatus.ON_SALE);
                        artworkRepository.save(artwork);
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

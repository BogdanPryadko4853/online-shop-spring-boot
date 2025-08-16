package com.audio.service.unit;

import com.audio.cache.CacheProductDto;
import com.audio.cache.ProductCacheService;
import com.audio.dto.request.ProductServiceUpdateRequest;
import com.audio.dto.response.ProductServiceResponse;
import com.audio.entity.ProductEntity;
import com.audio.exception.ProductNotFoundException;
import com.audio.mapper.ProductMapperImpl;
import com.audio.repository.ProductRepository;
import com.audio.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("Модульные тесты ProductCacheService")
public class TestProductCacheService {
    @InjectMocks
    private ProductServiceImpl productServiceImpl;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private ProductCacheService productCacheService;
    @Spy
    private ProductMapperImpl productMapperImpl;
    @Captor
    private ArgumentCaptor<ProductEntity> productEntityCaptor;
    private ProductEntity productEntity;
    private CacheProductDto productCacheDto;
    private ProductServiceUpdateRequest productServiceUpdateRequest;
    private ProductServiceResponse productServiceResponse;
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String NAME = UUID.randomUUID().toString();
    private static final String DESCRIPTION = UUID.randomUUID().toString();
    private static final BigDecimal PRICE = new BigDecimal(BigInteger.valueOf(10));
    private static final Integer QUANTITY = 25;
    private static final String SKU = UUID.randomUUID().toString();
    private static final String PHOTO = UUID.randomUUID().toString();
    private static final String DIGITAL = UUID.randomUUID().toString();
    private static final boolean ACTIVE = true;

    @BeforeEach
    void setUp() {
        productEntity = ProductEntity.builder()
                .id(PRODUCT_ID)
                .name(NAME)
                .description(DESCRIPTION)
                .price(PRICE)
                .stockQuantity(QUANTITY)
                .sku(SKU)
                .photoUrl(PHOTO)
                .digitalContent(DIGITAL)
                .isActive(ACTIVE)
                .build();

        productServiceResponse = new ProductServiceResponse(PRODUCT_ID, NAME, DESCRIPTION, PRICE, QUANTITY, SKU, PHOTO, ACTIVE, LocalDateTime.MAX, LocalDateTime.MIN);
        productCacheDto = CacheProductDto.builder()
                .id(PRODUCT_ID)
                .name(NAME)
                .description(DESCRIPTION)
                .price(PRICE)
                .stockQuantity(QUANTITY)
                .sku(SKU)
                .photoUrl(PHOTO)
                .digitalContent(DIGITAL)
                .isActive(ACTIVE)
                .build();

        productServiceUpdateRequest = ProductServiceUpdateRequest.builder()
                .name(NAME)
                .description(DESCRIPTION)
                .price(PRICE)
                .stockQuantity(QUANTITY)
                .sku(SKU)
                .isActive(ACTIVE)
                .build();

    }


    @Nested
    @DisplayName("Найти товар по id")
    class TestGetProductById {
        @Test
        @DisplayName("Получить продукт если существует")
        void whenProductExists_thenReturnsCacheDto() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));
            CacheProductDto result = productCacheService.getById(PRODUCT_ID);
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(productCacheDto);
            verify(productRepository, times(1)).findById(PRODUCT_ID);

        }

        @Test
        @DisplayName("Выкинуть ошибку если продукт не существует ")
        void whenProductNotExists_thenThrowsException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> productCacheService.getById(PRODUCT_ID))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found for user ID: " + PRODUCT_ID);
            verify(productRepository).findById(PRODUCT_ID);


        }
    }

    @Nested
    @DisplayName("Обновить продукт по id")
    class TestUpdateProductById {
        @Test
        @DisplayName("Обновить если продукт существует  ")
        void whenProductExists_thenReturnsUpdateCacheDto() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CacheProductDto expected = CacheProductDto.builder()
                    .id(PRODUCT_ID)
                    .name(NAME)
                    .description(DESCRIPTION)
                    .price(PRICE)
                    .stockQuantity(QUANTITY)
                    .sku(SKU)
                    .photoUrl(PHOTO)
                    .digitalContent(DIGITAL)
                    .isActive(ACTIVE)
                    .updatedAt(LocalDateTime.now())
                    .build();

            CacheProductDto result = productCacheService.update(PRODUCT_ID, productServiceUpdateRequest);

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .isEqualTo(expected);

            verify(productRepository).findById(PRODUCT_ID);
            verify(productRepository).save(any(ProductEntity.class));
        }


        @Test
        @DisplayName("Выкинуть ошибку при обновлении если продукт не существует  ")
        void whenProductNotExists_thenReturnsException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> productCacheService.update(PRODUCT_ID, productServiceUpdateRequest)).isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product not found for user ID: " + PRODUCT_ID);
            verify(productRepository).findById(PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("Обновить статус ")
    class updateActiveStatus {
        @Test
        @DisplayName("Обновить статус если продукт существует ")
        void whenProductExists_updateActiveStatus() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            productCacheService.updateActiveStatus(PRODUCT_ID, ACTIVE);

            verify(productRepository).save(productEntityCaptor.capture());
            verify(productRepository).findById(PRODUCT_ID);
            assertThat(productEntityCaptor.getValue().getIsActive()).isEqualTo(ACTIVE);
        }
    }

    @Nested
    @DisplayName("Обновить количество  ")
    class updateStockQuantity {
        @Test
        @DisplayName("Обновить количество если продукт существует ")
        void whenProductExists_updateStockQuantity() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            productEntity.setStockQuantity(0);
            productCacheService.updateStockQuantity(PRODUCT_ID, QUANTITY);

            verify(productRepository).save(productEntityCaptor.capture());
            verify(productRepository).findById(PRODUCT_ID);
            assertThat(productEntityCaptor.getValue().getStockQuantity()).isEqualTo(QUANTITY);


        }
    }

    @Nested
    @DisplayName("Удалить продукт ")
    class deleteActiveQuantity {
        @Test
        @DisplayName("Удалить  если продукт существует ")
        void whenProductExists_deleteProduct() {
            doNothing().when(productRepository).deleteById(PRODUCT_ID);
            productCacheService.delete(PRODUCT_ID);

            verify(productRepository, times(1)).deleteById(PRODUCT_ID);
        }
    }
}
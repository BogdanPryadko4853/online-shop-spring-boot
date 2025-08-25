package com.audio.service.impl;

import com.audio.cache.CacheProductDto;
import com.audio.cache.ProductCacheService;
import com.audio.dto.*;
import com.audio.dto.request.ProductServiceCreateRequest;
import com.audio.dto.request.ProductServiceUpdateRequest;
import com.audio.dto.response.ProductServiceDetailsResponse;
import com.audio.dto.response.ProductServiceResponse;
import com.audio.dto.response.ProductServiceResponsePaid;
import com.audio.entity.ProductEntity;
import com.audio.exception.ProductNotFoundException;
import com.audio.like.service.LikeService;
import com.audio.mapper.ProductMapper;
import com.audio.repository.ProductRepository;
import com.audio.service.CommentService;
import com.audio.service.FileStorageService;
import com.audio.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductCacheService productCacheService;
    private final ProductMapper productMapper;
    private final FileStorageService storageService;
    private final CommentService commentService;
    private final LikeService likeService;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductServiceResponse createProduct(ProductServiceCreateRequest createDto) {
        CacheProductDto cacheDto = productCacheService.create(createDto);
        return convertToResponseDto(cacheDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductServiceResponse getProductById(UUID id) {
        CacheProductDto cacheDto = productCacheService.getById(id);
        return convertToResponseDto(cacheDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductServiceResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(this::convertEntityToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductServiceResponse> searchProducts(
            String name,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isActive,
            Pageable pageable) {
        return productRepository.findByFilters(
                name,
                minPrice,
                maxPrice,
                isActive,
                pageable
        ).map(this::convertEntityToResponseDto);
    }

    @Override
    @Transactional
    public ProductServiceResponse updateProduct(UUID id, ProductServiceUpdateRequest updateDto) {
        CacheProductDto cacheDto = productCacheService.update(id, updateDto);
        return convertToResponseDto(cacheDto);
    }

    @Override
    @Transactional
    public ProductServiceResponse updateProductPhoto(UUID id, MultipartFile image) {
        CacheProductDto cacheDto = productCacheService.getById(id);
        try {
            if (cacheDto.getPhotoUrl() != null) {
                storageService.deleteFile(cacheDto.getPhotoUrl());
            }

            String extension = getFileExtension(image.getOriginalFilename());
            String newFileName = "product_" + id + "_" + System.currentTimeMillis() + "." + extension;
            String filePath = storageService.uploadFile(image, newFileName);

            ProductServiceUpdateRequest updateDto = ProductServiceUpdateRequest.builder()
                    .photoUrl(filePath)
                    .build();

            CacheProductDto updatedCacheDto = productCacheService.update(id, updateDto);
            return convertToResponseDto(updatedCacheDto);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update product photo", e);
        }
    }

    @Override
    @Transactional
    public ProductServiceResponse deleteProductPhoto(UUID id) {
        CacheProductDto cacheDto = productCacheService.getById(id);

        if (cacheDto.getPhotoUrl() != null) {
            try {
                storageService.deleteFile(cacheDto.getPhotoUrl());

                ProductServiceUpdateRequest updateDto = ProductServiceUpdateRequest.builder()
                        .photoUrl(null)
                        .build();

                CacheProductDto updatedCacheDto = productCacheService.update(id, updateDto);
                return convertToResponseDto(updatedCacheDto);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete product photo", e);
            }
        }
        return convertToResponseDto(cacheDto);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getProductPhoto(UUID id) {
        CacheProductDto cacheDto = productCacheService.getById(id);

        if (cacheDto.getPhotoUrl() == null) {
            throw new RuntimeException("Product photo not found");
        }

        try (InputStream inputStream = storageService.getFile(cacheDto.getPhotoUrl())) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get product photo", e);
        }
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        productCacheService.delete(id);
    }

    @Override
    @Transactional
    public ProductServiceResponse setProductActiveStatus(UUID id, boolean isActive) {
        CacheProductDto cacheDto = productCacheService.updateActiveStatus(id, isActive);
        return convertToResponseDto(cacheDto);
    }

    @Override
    @Transactional
    public ProductServiceResponse updateStockQuantity(UUID id, int quantityChange) {
        CacheProductDto cacheDto = productCacheService.updateStockQuantity(id, quantityChange);
        return convertToResponseDto(cacheDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductServiceResponsePaid getProductForPaid(UUID id) {
        CacheProductDto cacheDto = productCacheService.getById(id);
        return ProductServiceResponsePaid.builder()
                .id(cacheDto.getId())
                .name(cacheDto.getName())
                .price(cacheDto.getPrice())
                .photoUrl(cacheDto.getPhotoUrl())
                .stockQuantity(cacheDto.getStockQuantity())
                .isActive(cacheDto.getIsActive())
                .sku(cacheDto.getSku())
                .description(cacheDto.getDescription())
                .digitalContent(cacheDto.getDigitalContent())
                .build();
    }

    @Override
    public ProductServiceDetailsResponse getProductDetailsById(UUID productId, UUID currentUserId) {
        CacheProductDto cacheDto = productCacheService.getById(productId);

        long likesCount = likeService.getLikesCount(productId, "PRODUCT");
        boolean likedByCurrentUser = currentUserId != null &&
                likeService.checkIfLiked(productId, "PRODUCT", currentUserId);

        List<CommentServiceResponse> recentComments = commentService.getCommentsForEntity(productId, "PRODUCT")
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        return ProductServiceDetailsResponse.builder()
                .id(cacheDto.getId())
                .name(cacheDto.getName())
                .description(cacheDto.getDescription())
                .price(cacheDto.getPrice())
                .stockQuantity(cacheDto.getStockQuantity())
                .sku(cacheDto.getSku())
                .photoUrl(cacheDto.getPhotoUrl())
                .isActive(cacheDto.getIsActive())
                .createdAt(cacheDto.getCreatedAt())
                .updatedAt(cacheDto.getUpdatedAt())
                .likesCount(likesCount)
                .likedByCurrentUser(likedByCurrentUser)
                .recentComments(recentComments)
                .build();
    }

    @Override
    public CommentServiceResponse addCommentToProduct(UUID productId, UUID userId, String content) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return commentService.addComment(productId, "PRODUCT", userId, content);
    }

    private ProductServiceResponse convertToResponseDto(CacheProductDto cacheDto) {
        return ProductServiceResponse.builder()
                .id(cacheDto.getId())
                .name(cacheDto.getName())
                .description(cacheDto.getDescription())
                .price(cacheDto.getPrice())
                .stockQuantity(cacheDto.getStockQuantity())
                .sku(cacheDto.getSku())
                .photoUrl(cacheDto.getPhotoUrl())
                .isActive(cacheDto.getIsActive())
                .createdAt(cacheDto.getCreatedAt())
                .updatedAt(cacheDto.getUpdatedAt())
                .build();
    }

    private ProductServiceResponse convertEntityToResponseDto(ProductEntity entity) {
        return ProductServiceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .stockQuantity(entity.getStockQuantity())
                .sku(entity.getSku())
                .photoUrl(entity.getPhotoUrl())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "jpg" : filename.substring(lastDot + 1);
    }
}
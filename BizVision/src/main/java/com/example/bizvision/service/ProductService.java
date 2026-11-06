package com.example.bizvision.service;

import com.example.bizvision.dto.CreateProductRequest;
import com.example.bizvision.dto.ProductPageResponse;
import com.example.bizvision.exception.InsufficientStockException;
import org.springframework.stereotype.Service;
import com.example.bizvision.dto.ProductResponse;
import com.example.bizvision.mapper.ProductMapper;
import com.example.bizvision.entity.ProductEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public ProductPageResponse findAll(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }

        long offsetValue = (page - 1L) * size;
        if (offsetValue > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("页码超出支持范围");
        }

        int offset = (int) offsetValue;
        List<ProductEntity> entities = productMapper.findAll(size, offset);

        List<ProductResponse> result = new ArrayList<>();
        for (ProductEntity entity: entities) {
            result.add(new ProductResponse(entity.getId(), entity.getName(), entity.getPriceInCents(), entity.getStock()));
        }

        long total = productMapper.countAll();
        return new ProductPageResponse(result, total, page, size);
    }

    // 查找商品
    public Optional<ProductResponse> findProduct(long id) {
        ProductEntity entity = productMapper.findID(id);

        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(new ProductResponse( entity.getId(), entity.getName(), entity.getPriceInCents(), entity.getStock()));
    }

    // 添加商品
    public Long createProduct(CreateProductRequest request) {
        ProductEntity entity = toEntity(request);
        productMapper.insert(entity);
        return entity.getId();
    }

    // 修改商品
    @Transactional
    public Optional<Long> updateProduct(Long id, CreateProductRequest request) {
        if (id <= 0) {
            throw new IllegalArgumentException("商品编号必须大于 0");
        }

        ProductEntity entity = toEntity(request);
        ProductEntity existing = productMapper.findByIdForUpdate(id);
        if (existing == null){
            return Optional.empty();
        }

        entity.setId(id);
        productMapper.update(entity);
        return Optional.of(id);
    }

    @Transactional
    public  Optional<Long> decreaseStock(Long id, int quantity) {
        if (id <= 0) {
            throw new IllegalArgumentException("商品编号必须大于 0");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("扣减库存必须大于 0");
        }

        ProductEntity entity = productMapper.findByIdForUpdate(id);
        if (entity == null) {
            return Optional.empty();
        }

        int rows = productMapper.decreaseStock(id, quantity);
        if (rows == 0) {
            throw new InsufficientStockException("库存不足");
        }
        return Optional.of(id);
    }

    // 删除商品
    @Transactional
    public Optional<Long> deleteProduct(Long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("商品编号必须大于 0");
        }

        ProductEntity existing = productMapper.findByIdForUpdate(id);
        if (existing == null){
            return Optional.empty();
        }

        productMapper.deleteProduct(id);
        return Optional.of(id);
    }

    private ProductEntity toEntity(CreateProductRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("商品名称不能为空");
        }

        String name = request.getName().trim();
        if (name.length() > 100) {
            throw new IllegalArgumentException("商品名称不能超过100个字符");
        }

        if (request.getPriceInCents() == null || request.getPriceInCents() < 0) {
            throw new IllegalArgumentException("价格必须填写且不能为负数");
        }

        if (request.getStock() == null || request.getStock() < 0) {
            throw new IllegalArgumentException("库存必须填写且不能为负数");
        }

        ProductEntity entity = new ProductEntity();
        entity.setName(name);
        entity.setPriceInCents(request.getPriceInCents());
        entity.setStock(request.getStock());

        return entity;
    }
}

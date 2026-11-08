package com.example.bizvision.controller;

import com.example.bizvision.dto.products.CreateProductRequest;
import com.example.bizvision.dto.products.ProductPageResponse;
import com.example.bizvision.service.products.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.bizvision.dto.products.ProductResponse;

import java.util.Optional;

// @RestController：让方法返回值直接作为响应内容。
@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ProductPageResponse findAll(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "2") int size) {
        return productService.findAll(page, size);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> findProduct(@PathVariable("id") long id) {
        Optional<ProductResponse> result = productService.findProduct(id);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/products")
    public ResponseEntity<Long> createProduct(@RequestBody CreateProductRequest request) {
        Long id =  productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Long> updateProduct(@PathVariable Long id, @RequestBody CreateProductRequest request) {
        Optional<Long> result =  productService.updateProduct(id, request);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Long> deleteProduct(@PathVariable("id") long id) {
        Optional<Long> resilt = productService.deleteProduct(id);
        if (resilt.isPresent()){
            return ResponseEntity.ok(resilt.get());
        }
        return ResponseEntity.notFound().build();
    }

    // 扣减库存
    @PostMapping("/products/decrease-stock/{id}")
    public ResponseEntity<Long> decreaseStock(@PathVariable("id") Long id, @RequestBody int quantity) {
        Optional<Long> result = productService.decreaseStock(id, quantity);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }

        return ResponseEntity.notFound().build();
    }
}

package com.example.bizvision.controller;

import com.example.bizvision.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.example.bizvision.dto.ProductResponse;

import java.util.List;
import java.util.Optional;

// @RestController：让方法返回值直接作为响应内容。
@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> findAll() {
        List<ProductResponse> result = productService.findAll();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> findProduct(@PathVariable("id") long id) {
        Optional<ProductResponse> result = productService.findProduct(id);

        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }
}

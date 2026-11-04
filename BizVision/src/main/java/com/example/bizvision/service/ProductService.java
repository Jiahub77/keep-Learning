package com.example.bizvision.service;

import org.springframework.stereotype.Service;
import com.example.bizvision.dto.ProductResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    public List<ProductResponse> findAll() {
        return Arrays.asList(new ProductResponse("保温杯", 20),new ProductResponse("炒锅",5));
    }

    public Optional<ProductResponse> findProduct(long id) {
        if (id == 7) {
            return Optional.of(new ProductResponse("保温杯", 20));
        }

        return Optional.empty();
    }
}

package com.example.bizvision.dto;

public class ProductResponse {
    private final String name;
    private final int stock;

    public ProductResponse(String name, int stock) {
        this.name = name;
        this.stock = stock;
    }

    public String getName() {
        return this.name;
    }

    public int getStock() {
        return this.stock;
    }
}

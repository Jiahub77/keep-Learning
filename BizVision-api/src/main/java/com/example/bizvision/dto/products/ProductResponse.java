package com.example.bizvision.dto.products;

public class ProductResponse {
    private final long id;
    private final String name;
    private final int price_in_cents;
    private final int stock;

    public ProductResponse(long id, String name, int price_in_cents, int stock) {
        this.id = id;
        this.name = name;
        this.price_in_cents = price_in_cents;
        this.stock = stock;
    }

    public long getId() {
        return id;
    }

    public int getPrice_in_cents() {
        return price_in_cents;
    }

    public String getName() {
        return this.name;
    }

    public int getStock() {
        return this.stock;
    }
}

package com.example.bizvision.dto;

import java.util.List;

public class ProductPageResponse {
    private final List<ProductResponse> list;
    private final long total;
    private final int page;
    private final int size;

    public ProductPageResponse(List<ProductResponse> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<ProductResponse> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}

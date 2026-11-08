package com.example.bizvision.dto.article;

import com.example.bizvision.entity.artilce.ArticleEntity;

import java.util.List;

public class ArticlePageResponse {
    private final List<ArticleResponse> list;
    private final int page;
    private final int size;
    private final int offset;
    private final long total;

    public ArticlePageResponse(List<ArticleResponse> list, int page, int size, int offset, long total) {
        this.list = list;
        this.page = page;
        this.size = size;
        this.offset = offset;
        this.total = total;
    }

    public List<ArticleResponse> getList() {
        return list;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getOffset() {
        return offset;
    }

    public long getTotal() {
        return total;
    }
}

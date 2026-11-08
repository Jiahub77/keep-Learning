package com.example.bizvision.dto.article;

import com.example.bizvision.common.enums.ArticleStatus;

import java.time.LocalDateTime;

public class ArticleResponse {
    private final long id;
    private final String name;
    private final String content;
    private final String author;
    private final LocalDateTime createdAt;
    private final int isOn;

    public ArticleResponse(long id, String name, String content, String author,
                           LocalDateTime createdAt, ArticleStatus status) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.isOn = status == null? 0 : status.getCode();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getIsOn() {
        return isOn;
    }
}

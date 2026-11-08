package com.example.bizvision.entity.artilce;

import java.time.LocalDateTime;

public class ArticleEntity {
    private Long id;
    private String name;
    private String content;
    private String author;
    private int is_on;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getIs_on() {
        return is_on;
    }

    public void setIs_on(int is_on) {
        this.is_on = is_on;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

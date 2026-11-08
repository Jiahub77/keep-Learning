package com.example.bizvision.dto.article;

// 请求体 DTO 必须是「可变 + 无参构造 + setter」，否则 Jackson 无法把 JSON 反序列化成对象
public class ArticleRequest {
    private String name;
    private String content;
    private String author;

    public ArticleRequest() {
    }

    public ArticleRequest(String name, String content, String author) {
        this.name = name;
        this.content = content;
        this.author = author;
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
}

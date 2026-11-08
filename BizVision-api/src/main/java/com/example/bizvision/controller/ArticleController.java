package com.example.bizvision.controller;

import com.example.bizvision.dto.article.ArticlePageResponse;
import com.example.bizvision.dto.article.ArticleRequest;
import com.example.bizvision.dto.article.ArticleResponse;
import com.example.bizvision.service.article.ArticleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

// @RestController：必须加，否则类不会被注册成 Bean，所有 /article 接口都 404。
@RestController
public class ArticleController {
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    // 文章列表
    @GetMapping("/article/list")
    public ArticlePageResponse lists(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return articleService.list(page, size);
    }

    // 文章详情：不存在时返回 404，而不是 500
    @GetMapping("/article/{id}")
    public ResponseEntity<ArticleResponse> detail(@PathVariable("id") long id) {
        Optional<ArticleResponse> result = articleService.detail(id);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }

    // 添加文章：必须加 @RequestBody，否则 JSON 请求体绑不上，参数全是 null
    @PostMapping("/article/add")
    public ResponseEntity<Long> add(@RequestBody ArticleRequest request) {
        Long id = articleService.add(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    // 修改文章
    @PutMapping("/article/{id}")
    public ResponseEntity<Long> edit(@PathVariable("id") long id, @RequestBody ArticleRequest request) {
        Optional<Long> result = articleService.edit(id, request);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }

    // 删除文章（软删除：is_on 置 0）
    @DeleteMapping("/article/{id}")
    public ResponseEntity<Long> delete(@PathVariable("id") long id) {
        Optional<Long> result = articleService.delete(id);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.notFound().build();
    }
}

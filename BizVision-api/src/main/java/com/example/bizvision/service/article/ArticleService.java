package com.example.bizvision.service.article;

import com.example.bizvision.common.enums.ArticleStatus;
import com.example.bizvision.dto.article.ArticlePageResponse;
import com.example.bizvision.dto.article.ArticleRequest;
import com.example.bizvision.dto.article.ArticleResponse;
import com.example.bizvision.entity.artilce.ArticleEntity;
import com.example.bizvision.mapper.article.ArticleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// @Service：不加就不会被扫描成 Bean，Controller 注入失败
@Service
public class ArticleService {
    private final ArticleMapper articleMapper;

    public ArticleService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public ArticlePageResponse list(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于等于1");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页数量必须在1到100之间");
        }

        long offsetValue = (page - 1L) * size;
        if (offsetValue > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("页码超出支持范围");
        }

        int offset = (int) offsetValue;
        List<ArticleEntity> entities = articleMapper.list(size, offset);

        List<ArticleResponse> result = new ArrayList<>();
        for (ArticleEntity entity : entities) {
            result.add(toResponse(entity));
        }

        long total = articleMapper.countAll();
        // 注意参数顺序：构造器是 (list, page, size, offset, total)
        return new ArticlePageResponse(result, page, size, offset, total);
    }

    // 查看详情：查不到返回 Optional.empty()，交给 Controller 转 404
    public Optional<ArticleResponse> detail(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id必须大于0");
        }

        ArticleEntity entity = articleMapper.detail(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(entity));
    }

    // 添加
    public Long add(ArticleRequest request) {
        ArticleEntity entity = toEntity(request);
        articleMapper.add(entity);
        return entity.getId();
    }

    // 修改
    @Transactional
    public Optional<Long> edit(long id, ArticleRequest request) {
        if (id <= 0) {
            throw new IllegalArgumentException("文章编号必须大于 0");
        }

        ArticleEntity existing = articleMapper.detail(id);
        if (existing == null) {
            return Optional.empty();
        }

        ArticleEntity entity = toEntity(request);
        entity.setId(id);
        articleMapper.edit(entity);
        return Optional.of(id);
    }

    // 删除（软删除）
    @Transactional
    public Optional<Long> delete(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id必须大于0");
        }

        ArticleEntity existing = articleMapper.detail(id);
        if (existing == null) {
            return Optional.empty();
        }

        articleMapper.delete(id);
        return Optional.of(id);
    }

    private ArticleResponse toResponse(ArticleEntity entity) {
        return new ArticleResponse(entity.getId(), entity.getName(), entity.getContent(),
                entity.getAuthor(), entity.getCreatedAt(), entity.getStatus());
    }

    private ArticleEntity toEntity(ArticleRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("文章名称不能为空");
        }

        String name = request.getName().trim();
        if (name.length() > 100) {
            throw new IllegalArgumentException("文章名称不能超过100个字符");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("文章内容不能为空");
        }

        if (request.getAuthor() == null || request.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("作者不能为空");
        }

        String author = request.getAuthor().trim();
        if (author.length() > 50) {
            throw new IllegalArgumentException("作者不能超过50个字符");
        }

        ArticleEntity entity = new ArticleEntity();
        entity.setName(name);
        entity.setContent(request.getContent());
        entity.setAuthor(author);
        entity.setStatus(ArticleStatus.PUBLISHED);

        return entity;
    }
}

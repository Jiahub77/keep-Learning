package com.example.bizvision.mapper.article;

import com.example.bizvision.entity.artilce.ArticleEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

// @Mapper：不加的话 MyBatis 不会为它生成代理对象，ArticleService 注入时会启动失败
@Mapper
public interface ArticleMapper {
    // 列表查询：只查未删除的（is_on = 1）；created_at 起别名，避免依赖驼峰映射配置
    @Select("SELECT id, name, content, author, is_on, created_at AS createdAt " +
            "FROM articles WHERE is_on = 1 ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<ArticleEntity> list(@Param("size") int size, @Param("offset") int offset);

    // id 查找：同样过滤已删除数据
    @Select("SELECT id, name, content, author, is_on, created_at AS createdAt " +
            "FROM articles WHERE id = #{id} AND is_on = 1")
    ArticleEntity detail(@Param("id") long id);

    // 添加文章
    @Insert("INSERT INTO articles(name, content, author) VALUES (#{name}, #{content}, #{author})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int add(ArticleEntity article);

    // 修改文章
    @Update("UPDATE articles SET name = #{name}, content = #{content}, author = #{author} " +
            "WHERE id = #{id} AND is_on = 1")
    int edit(ArticleEntity article);

    // 删除文章（软删除）：原来缺 WHERE，会把全表文章一起下架
    @Update("UPDATE articles SET is_on = 0 WHERE id = #{id} AND is_on = 1")
    int delete(@Param("id") long id);

    // 总行数：只统计未删除的，否则分页总数和列表长度对不上
    @Select("SELECT COUNT(*) FROM articles WHERE is_on = 1")
    long countAll();
}

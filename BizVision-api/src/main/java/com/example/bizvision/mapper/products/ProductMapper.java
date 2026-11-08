package com.example.bizvision.mapper.products;

import com.example.bizvision.entity.products.ProductEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {
    // 按id查找
    @Select("SELECT id, name, price_in_cents AS priceInCents, stock " + "FROM products WHERE id = #{id}")
    ProductEntity findID(@Param("id") long id);

    // 插入
    @Insert("INSERT INTO products(name, price_in_cents, stock)" + "VALUES (#{name}, #{priceInCents}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductEntity product);

    // 修改
    @Update("UPDATE products SET name = #{name}, " + "price_in_cents = #{priceInCents}, stock = #{stock} " + "WHERE id = #{id}")
    int update(ProductEntity product);

    // 判断是否有更新
    @Select("SELECT id, name, price_in_cents AS priceInCents, stock " + "FROM products WHERE id = #{id} FOR UPDATE")
    ProductEntity findByIdForUpdate(@Param("id") long id);

    // 删除
    @Delete("DELETE  FROM products WHERE id = #{id}")
    int deleteProduct(@Param("id") long id);

    // 查找全部
    @Select("SELECT id, name, price_in_cents AS priceInCents, stock " + "FROM products ORDER BY id DESC " + "LIMIT #{size} OFFSET #{offset}")
    List<ProductEntity> findAll(@Param("size") int size, @Param("offset") int offset);

    // 总行数
    @Select("SELECT COUNT(*) FROM products")
    long countAll();

    // 扣库存
    @Update("UPDATE products " + "SET stock = stock - #{quantity} " + "WHERE id = #{id} " + "AND stock >= #{quantity}")
    int decreaseStock(@Param("id") long id, @Param("quantity") int quantity);
}

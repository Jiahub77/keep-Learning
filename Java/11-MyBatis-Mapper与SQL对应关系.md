# MyBatis：Mapper 与 SQL 的对应关系

## 本主题目标

理解 MyBatis 如何把 Java 方法和 SQL 连接起来。暂时只学习查询一个商品，不连接真实数据库。

## MyBatis 做什么

MyBatis 是持久层框架，负责把 Java 方法调用转换成 SQL 执行，并把查询结果转换成 Java 对象。它不会替代 MySQL：

```text
Service：决定要做什么业务
Mapper：描述如何访问数据库
MyBatis：执行 Mapper 对应的 SQL
MySQL：真正保存和查询数据
```

## Mapper 接口与注解 SQL

```java
@Mapper
public interface ProductMapper {

    @Select("SELECT id, name, stock FROM products WHERE id = #{id}")
    Optional<ProductRow> findById(long id);
}
```

- `@Mapper` 让 MyBatis/Spring 识别这是数据库访问接口。
- `findById(long id)` 是 Java 方法。
- `@Select(...)` 写查询 SQL。
- `#{id}` 会绑定方法参数 `id`，不要用字符串拼接 SQL。
- 查询结果可以映射为 `ProductRow`；查不到时可以约定返回空结果，再由 Service 转成业务响应。

这个示例假设数据库字段名是 `id`、`name`、`stock`，与 Java 对象属性名称对应。真实项目常见 `price_in_cents` 这类下划线字段，后续再学习字段映射。

## 与分层的关系

```java
@Service
public class ProductService {
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }
}
```

Service 通过构造器注入 Mapper，但业务代码不直接写 SQL。这样业务规则与数据库访问仍然分开。

## 当前状态

- 已掌握：MyBatis 是连接 Java 方法与 SQL 的持久层框架，Mapper 负责描述数据库访问。
- 待实践：配置数据源、创建 MySQL 表、执行真实查询。
- 下一步：学习 `#{}` 参数绑定、结果映射，以及 MyBatis 与 Spring Boot 的配置关系。

## 查询结果映射到 Java 对象

可以用 `record` 表示查询结果：

```java
public record ProductRow(
        long id,
        String name,
        int stock
) {
}
```

当 SQL 列名和 Java 组件名称一致时，MyBatis 通常可以自动映射：

```sql
SELECT id, name, stock FROM products WHERE id = #{id}
```

如果数据库使用下划线命名，例如 `price_in_cents`，Java 使用驼峰命名 `priceInCents`，可以在 SQL 中起别名：

```sql
SELECT id, name, price_in_cents AS priceInCents
FROM products
WHERE id = #{id}
```

这样查询列名就与 Java 属性对应。更复杂的映射可以使用 `@Results` 或 XML，后续再学习。

## 查询全部商品：Mapper → Service → Controller

Mapper 查询全部商品时，通常返回空列表而不是 `null`：

```java
@Mapper
public interface ProductMapper {
    @Select("""
        SELECT id, name, stock
        FROM products
        ORDER BY id
        """)
    List<ProductRow> findAll();
}
```

Service 可以把数据库结果转换为接口响应对象：

```java
@Service
public class ProductService {
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<ProductResponse> findAll() {
        return productMapper.findAll().stream()
                .map(row -> new ProductResponse(
                        row.id(), row.name(), row.stock()))
                .toList();
    }
}
```

Controller 只负责暴露 HTTP 接口：

```java
@GetMapping("/products")
public List<ProductResponse> getProducts() {
    return productService.findAll();
}
```

没有商品时，接口通常返回 HTTP 200 和空数组 `[]`。这表示请求成功，只是当前没有数据；它与“查询某个不存在的商品”返回 404 的语义不同。

## 本主题进度

- 已掌握：MyBatis starter 的作用、`@Mapper`、`#{}` 参数绑定、结果映射、空列表和 Mapper/Service/Controller 的基本链路。
- 待实践：配置数据源、创建表、执行真实查询。
- 下一步：学习 Spring Boot 数据源配置和 MySQL JDBC 驱动。

## Spring Boot 数据源与 MySQL JDBC 驱动

MyBatis 要执行 MySQL 查询，需要两部分：

1. MySQL JDBC 驱动：让 Java 能够通过 JDBC 协议连接 MySQL。
2. 数据源配置：告诉 Spring Boot 数据库地址、数据库名、用户名和密码。

Maven 依赖通常包括 MyBatis starter 和 MySQL 驱动：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>与 Spring Boot 兼容的版本</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

`runtime` 表示应用运行时需要驱动；具体版本应由项目使用的 Spring Boot 版本和依赖管理决定，不要随意复制不匹配的版本。

`application.yml` 示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shop
    username: shop_user
    password: ${SHOP_DB_PASSWORD}
```

URL 可以拆成：

```text
jdbc:mysql://localhost:3306/shop
  │     │       │    └─ 数据库名
  │     │       └────── MySQL 默认端口
  │     └────────────── 数据库服务器
  └──────────────────── JDBC + MySQL
```

`${SHOP_DB_PASSWORD}` 表示从环境变量读取密码，避免把真实密码直接提交到 Git。当前学习阶段可以使用虚构测试数据库和测试账号。

如果驱动、URL、账号或密码错误，通常会在应用启动创建数据源时失败；这属于启动配置问题，不是 Mapper Java 语法错误。

## 库存扣减与受影响行数

只在 Service 中先查询库存再扣减，多个请求同时执行时可能产生并发问题。可以把“库存足够”放进 UPDATE 条件：

```java
@Update("""
    UPDATE products
    SET stock = stock - #{quantity}
    WHERE id = #{productId}
      AND stock >= #{quantity}
    """)
int decreaseStock(
        @Param("productId") long productId,
        @Param("quantity") int quantity
);
```

Service 根据受影响行数判断结果：

```java
int affectedRows = productMapper.decreaseStock(productId, quantity);
if (affectedRows == 0) {
    throw new IllegalStateException("商品不存在或库存不足");
}
```

数据库只会在商品存在且当前库存足够时执行扣减。这样可以避免库存被更新为负数；后续还会结合事务和并发进一步实践。

## 新增后获取数据库生成的主键

数据库表使用 `AUTO_INCREMENT` 时，INSERT 不需要传入 `id`。MyBatis 可以把数据库生成的主键写回一个可修改的 Java 对象：

```java
public class ProductEntity {
    private Long id;
    private String name;
    private int priceInCents;
    private int stock;

    // 构造器、getter、setter 省略
}
```

```java
@Insert("""
    INSERT INTO products(name, price_in_cents, stock)
    VALUES (#{name}, #{priceInCents}, #{stock})
    """)
@Options(useGeneratedKeys = true, keyProperty = "id")
int insert(ProductEntity product);
```

执行成功后，数据库生成的 `id` 会写回 `product.id`。`keyProperty` 必须写 Java 对象的属性名，而不是数据库列名。

`record` 的组件不能重新赋值，因此不适合直接接收这种回写的自增主键。实际项目可以使用可变的持久化对象，或者执行插入后再通过其他方式查询新记录。

## Service 组织新增流程

新增商品时，常见的数据流是：

```text
CreateProductRequest → ProductEntity → Mapper.insert()
                    → 回写自增 id → ProductResponse
```

Service 负责在请求对象、数据库对象和响应对象之间转换：

```java
public ProductResponse createProduct(CreateProductRequest request) {
    ProductEntity entity = new ProductEntity();
    entity.setName(request.name());
    entity.setPriceInCents(request.priceInCents());
    entity.setStock(request.stock());

    productMapper.insert(entity);

    return new ProductResponse(
            entity.getId(), entity.getName(), entity.getStock());
}
```

通常不直接把 `ProductEntity` 返回给客户端，因为 Entity 可能包含数据库内部字段、敏感字段或未来会变化的结构。使用 `ProductResponse` 可以控制公开字段，减少接口与数据库表的耦合。

## 新增商品的参数校验职责

校验可以分为两类：

- 请求格式校验：字段是否缺失、类型是否正确，通常由 Controller/参数校验框架处理。
- 业务规则校验：价格不能为负数、库存不能为负数、商品名称不能重复，主要由 Service 处理。

简单的 Service 校验示例：

```java
public ProductResponse createProduct(CreateProductRequest request) {
    if (request.priceInCents() < 0) {
        throw new IllegalArgumentException("价格不能为负数");
    }
    if (request.stock() < 0) {
        throw new IllegalArgumentException("库存不能为负数");
    }

    // 创建 Entity、调用 Mapper、返回 Response
    return null;
}
```

这里的 `throw` 会中断当前业务方法。异常如何统一转换成 HTTP 400，会在后续异常处理主题学习；实际项目通常不会直接把异常堆栈返回给客户端。

## 请求校验与业务校验的区别

`@NotBlank`、`@PositiveOrZero` 和 `@Valid` 适合检查请求本身是否符合基本格式。它们不能替代 Service 业务判断。

例如，`@PositiveOrZero` 可以判断购买数量不是负数，但不能知道数据库中当前库存是多少。库存是否足够，仍然需要 Service 调用带条件的扣减 SQL 判断。

### 请求格式校验示例

```java
public record CreateProductRequest(
        @NotBlank String name,
        @PositiveOrZero int priceInCents,
        @PositiveOrZero int stock
) {
}
```

```java
@PostMapping("/products")
public ResponseEntity<ProductResponse> createProduct(
        @Valid @RequestBody CreateProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(productService.createProduct(request));
}
```

例如请求体中的 `name` 是空字符串，或 `stock` 是 `-1`，基础校验失败，通常直接返回 HTTP 400，不会进入 `productService.createProduct(...)`。

### 业务规则校验示例

```java
@Transactional
public void purchase(long productId, int quantity) {
    if (quantity <= 0) {
        throw new IllegalArgumentException("购买数量必须大于 0");
    }

    int affectedRows = productMapper.decreaseStock(productId, quantity);
    if (affectedRows == 0) {
        throw new IllegalStateException("商品不存在或库存不足");
    }
}
```

这里的“库存是否足够”依赖数据库中的当前库存，不能只靠 `@PositiveOrZero` 判断。Service 需要调用带 `stock >= #{quantity}` 条件的 SQL，并根据受影响行数判断业务结果。

## 本阶段进度补充

- 已掌握：请求校验注解与 Service 业务校验的边界。
- 待实践：用真实 HTTP 请求验证 400 响应，并验证库存业务规则。

## 更新与删除

更新商品库存：

```java
@Update("""
    UPDATE products
    SET stock = #{stock}
    WHERE id = #{id}
    """)
int updateStock(@Param("id") long id, @Param("stock") int stock);
```

删除商品：

```java
@Delete("DELETE FROM products WHERE id = #{id}")
int deleteById(@Param("id") long id);
```

`UPDATE` 和 `DELETE` 的返回值都是受影响行数：

```text
1 → 找到并修改/删除了一行
0 → 没有匹配到商品
```

Service 应先校验业务规则，再根据 `0` 抛出“商品不存在”等异常。商品已经被订单引用时，实际业务可能使用逻辑删除：

```sql
UPDATE products SET deleted = 1 WHERE id = #{id}
```

## 事务与库存扣减

一次业务操作包含多条 SQL 时，通常在 Service 方法上定义事务：

```java
@Transactional
public void purchase(long productId, int quantity) {
    if (quantity <= 0) {
        throw new IllegalArgumentException("购买数量必须大于 0");
    }

    int affectedRows = productMapper.decreaseStock(productId, quantity);
    if (affectedRows == 0) {
        throw new IllegalStateException("商品不存在或库存不足");
    }

    stockLogMapper.insert(productId, quantity);
}
```

正常结束时提交；运行时异常时通常回滚。库存扣减和库存记录写入要么一起成功，要么一起失败。数据库条件更新 `stock >= #{quantity}` 可以避免库存变成负数。

## 全局异常与 HTTP 响应

可以集中把 Service 异常转换成 HTTP 响应：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(
                new ApiError("INVALID_ARGUMENT", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(
            IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiError("BUSINESS_CONFLICT", exception.getMessage()));
    }
}
```

```java
public record ApiError(String code, String message) {
}
```

`code` 适合前端程序判断，`message` 适合展示或日志。前端不应依赖易变的中文文本判断错误类型。

## 请求校验与 HTTP 状态

```java
public record CreateProductRequest(
        @NotBlank String name,
        @PositiveOrZero int priceInCents,
        @PositiveOrZero int stock
) {
}
```

```java
public ProductResponse createProduct(
        @Valid @RequestBody CreateProductRequest request) {
    // 通过基础校验后，再进入 Service 业务规则
}
```

`@Valid`、`@NotBlank`、`@PositiveOrZero` 处理请求基本格式；库存是否足够、名称是否重复等依赖数据库状态的规则仍由 Service 处理。

## 分页查询

```java
@Select("""
    SELECT id, name, stock
    FROM products
    ORDER BY id
    LIMIT #{size} OFFSET #{offset}
    """)
List<ProductRow> findPage(
        @Param("size") int size,
        @Param("offset") int offset);

@Select("SELECT COUNT(*) FROM products")
long countAll();
```

计算方式：

```text
offset = (page - 1) * size
```

分页响应通常包含：

```java
public record ProductPageResponse(
        List<ProductResponse> items,
        long total,
        int page,
        int size
) {
}
```

`LIMIT` 控制返回条数，`OFFSET` 控制跳过条数；`COUNT(*)` 提供总记录数，方便前端计算总页数。没有商品时，列表接口通常返回 200 和空数组 `[]`。

## 完整学习状态

- 已掌握：Mapper、注解 SQL、`#{}` 参数绑定、结果映射、CRUD、数据源、JDBC 驱动、自增主键、Entity/Request/Response、请求与业务校验、事务、条件库存扣减、全局异常、分页。
- DTO 状态：已初步理解 `CreateProductRequest` 是请求 DTO、`ProductResponse` 是响应 DTO，`ProductEntity` 面向数据库；尚未完成独立 DTO 设计和实际转换实践。
- 待实践：安装 JDK、配置 Spring Boot/MySQL、创建测试表、执行真实 CRUD、验证事务和分页。
- 下一步：建立可运行的商品管理项目，先配置测试数据库，再把当前示例落地为完整接口。

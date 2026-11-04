# Spring Boot 启动类、Controller 与第一个 GET 接口

## 本主题目标

理解应用如何启动、Controller 如何接收 HTTP 请求，以及如何返回一个简单的商品信息。

## 版本说明

下面的 Spring 注解和分层思想适用于 Java 8、Java 17。使用 `record` 的代码片段仅适用于 Java 16+，本文按 Java 17/21 理解；Java 8 请改用普通 DTO 类。Java 8 项目通常使用支持 Java 8 的 Spring Boot 版本，Java 17 项目可以使用 Spring Boot 3；校验包分别常见 `javax.validation` 与 `jakarta.validation`。

## 最小示例

```java
// 版本：Java 8、Java 17 均适用。
package com.example.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}

@RestController
class ProductController {
    @GetMapping("/products/1")
    public String getProduct() {
        return "保温杯，库存 20";
    }
}
```

## 要点

- `@SpringBootApplication` 标记启动类。`SpringApplication.run(...)` 创建 Spring 容器并启动内置 Web 服务器。
- `@RestController` 表示这个类处理 Web 请求，方法返回值直接作为响应内容。
- `@GetMapping("/products/1")` 把 HTTP GET 请求路径映射到方法。访问 `/products/1` 时调用 `getProduct()`。
- 这是内存中的固定字符串，还没有连接数据库，也没有 Service 层；后续会逐步分层。

## 当前状态

- 已掌握：启动类、Controller、GET 路径映射、字符串响应的基本关系。
- 待实践：安装 JDK 后创建 Spring Boot 项目、启动服务、用浏览器或前端访问接口。
- 下一步：Controller 构造器注入 Service，并让 Service 返回商品数据。

## 复习结果

- `GET /products/1` 会返回 `"保温杯，库存 20"`。
- `GET /products/2` 没有匹配的 `@GetMapping`，通常返回 HTTP 404。

## Controller 注入 Service

Controller 负责接收请求，Service 负责业务逻辑。Controller 不应该把查库存、计算价格等业务全部写在请求方法里。

```java
// 版本：Java 8、Java 17 均适用。
@Service
public class ProductService {
    public String findProductText() {
        return "保温杯，库存 20";
    }
}
```

```java
// 版本：Java 8、Java 17 均适用。
@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products/1")
    public String getProduct() {
        return productService.findProductText();
    }
}
```

`@Service` 让 Spring 把 `ProductService` 注册为 Bean。Controller 的构造器声明需要 `ProductService`，Spring 创建 Controller 时会自动传入它。这就是构造器注入，也是之前接口与构造器注入知识在 Spring 中的应用。

## 返回商品对象与 JSON

实际接口通常返回结构化数据，而不是拼接字符串。可以使用已经学过的 `record` 表示只读的响应对象：

```java
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
public record ProductResponse(
        long id,
        String name,
        int stock
) {
}
```

```java
// 版本：Java 8、Java 17 均适用。
@Service
public class ProductService {
    public ProductResponse findProduct() {
        return new ProductResponse(1L, "保温杯", 20);
    }
}
```

```java
// 版本：Java 8、Java 17 均适用。
@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products/1")
    public ProductResponse getProduct() {
        return productService.findProduct();
    }
}
```

访问接口时，Spring Boot 通常通过 Jackson 把返回的 Java 对象序列化为 JSON：

```json
{
  "id": 1,
  "name": "保温杯",
  "stock": 20
}
```

这里没有手动拼接 JSON 字符串。`ProductResponse` 的返回类型让代码更清楚，也让编译器检查字段类型。

## 路径参数：`/products/{id}`

如果每个商品都写一个 `@GetMapping`，接口会重复。可以使用路径参数：

```java
// 版本：Java 8、Java 17 均适用。
@GetMapping("/products/{id}")
public ProductResponse getProduct(@PathVariable long id) {
    return productService.findProduct(id);
}
```

当请求为 `GET /products/7` 时，Spring 会把路径中的 `7` 转换为 `long`，传给参数 `id`。Service 可以根据 id 查询：

```java
// 版本：Java 8、Java 17 均适用。
@Service
public class ProductService {
    public ProductResponse findProduct(long id) {
        return new ProductResponse(id, "保温杯", 20);
    }
}
```

`@PathVariable` 的含义是“从 URL 路径中取值”。路径中的 `{id}` 和方法参数 `id` 对应。这里仍然是固定返回示例，尚未真正查询数据库。

如果请求路径中传入的值不能转换成 `long`，例如 `/products/abc`，通常会在请求处理阶段失败并返回 400 一类的客户端错误；这不是 Java 编译错误。

## 商品不存在时返回 404

Java 允许方法返回 `null`，但调用方如果直接访问返回值，可能发生空指针异常。`Optional` 更明确地表达“可能没有商品”：

```java
// 版本：Java 8、Java 17 均适用。
public Optional<ProductResponse> findProduct(long id) {
    if (id == 1) {
        return Optional.of(new ProductResponse(1L, "保温杯", 20));
    }
    return Optional.empty();
}
```

Controller 决定 HTTP 响应状态：

```java
// 版本：Java 8、Java 17 均适用。
@GetMapping("/products/{id}")
public ResponseEntity<ProductResponse> getProduct(
        @PathVariable long id) {
    Optional<ProductResponse> result = productService.findProduct(id);

    if (result.isPresent()) {
        return ResponseEntity.ok(result.get());
    }
    return ResponseEntity.notFound().build();
}
```

找到商品时返回 200 和 JSON；找不到商品时返回 404。`ResponseEntity` 让 Controller 同时表达响应状态和响应体。

## 查询参数：`@RequestParam`

路径参数适合标识某个资源，例如 `/products/7`；查询参数适合搜索和分页，例如：

```text
GET /products?keyword=杯&page=1
```

Controller 可以这样接收：

```java
// 版本：Java 8、Java 17 均适用。
@GetMapping("/products")
public String searchProducts(
        @RequestParam String keyword,
        @RequestParam int page) {
    return "搜索：" + keyword + "，第 " + page + " 页";
}
```

`@RequestParam String keyword` 会读取 URL 中的 `keyword`，`@RequestParam int page` 会读取 `page`，并把文本转换为 `int`。如果必填参数缺失，或 `page` 不能转换成整数，Spring 通常返回 400。

也可以提供默认值：

```java
// 版本：Java 8、Java 17 均适用。
@RequestParam(defaultValue = "1") int page
```

此时不传 `page` 时使用 1。

## POST 与 `@RequestBody`

GET 通常用于查询，新增商品可以使用 POST：

```text
POST /products
Content-Type: application/json
```

请求体：

```json
{
  "name": "炒锅",
  "priceInCents": 15900,
  "stock": 10
}
```

用 `record` 表示请求数据：

```java
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
public record CreateProductRequest(
        String name,
        int priceInCents,
        int stock
) {
}
```

Controller 使用 `@RequestBody` 接收 JSON：

```java
// 版本：Java 8、Java 17 均适用。
@PostMapping("/products")
public ProductResponse createProduct(
        @RequestBody CreateProductRequest request) {
    return productService.createProduct(request);
}
```

Spring Boot 会把 JSON 字段转换成 `CreateProductRequest` 对象，再传给方法。字段名需要能够对应；例如 JSON 中的 `priceInCents` 对应 record 的 `priceInCents`。

当前仍然没有数据库，Service 可以先返回模拟结果。参数校验（例如价格不能为负数）会在后续主题学习。

## 请求对象与响应对象

`CreateProductRequest` 和 `ProductResponse` 的字段由各自的 record 声明决定，不是任意参数容器。请求对象可以不包含数据库生成的 `id`，Service 创建结果时再把新 `id` 放进响应对象。

## POST 成功时返回 201

查询成功通常返回 200；创建资源成功通常使用 201 Created：

```java
// 版本：Java 8、Java 17 均适用。
@PostMapping("/products")
public ResponseEntity<ProductResponse> createProduct(
        @RequestBody CreateProductRequest request) {

    ProductResponse response = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

`ResponseEntity` 同时表示状态码和响应体。这里的 `response` 可以包含数据库生成的新 `id`。

## 阶段总结：请求到响应的完整链路

```text
HTTP 请求
  ↓
Controller：解析路径参数、查询参数或 JSON 请求体
  ↓
Service：执行商品、库存等业务规则
  ↓
Repository：访问数据库（后续由 MyBatis 实现）
  ↓
Service：组装结果
  ↓
Controller：返回状态码和 JSON 响应
```

### 常见注解与用途

| 注解 | 用途 |
|---|---|
| `@SpringBootApplication` | 标记启动类并启动 Spring Boot 应用 |
| `@RestController` | 声明 HTTP Controller，返回值通常写入响应体 |
| `@Service` | 把业务类注册为 Spring Bean |
| `@GetMapping` | 映射 GET 请求 |
| `@PostMapping` | 映射 POST 请求 |
| `@PathVariable` | 读取路径参数，如 `/products/{id}` |
| `@RequestParam` | 读取查询参数，如 `?page=1` |
| `@RequestBody` | 把 JSON 请求体转换成 Java 对象 |

### 错误类型区分

- **编译错误**：Java 类型或语法不匹配，例如方法声明返回 `String`，却返回 `ProductResponse`。
- **启动错误**：Spring 找不到需要注入的 Bean，例如 `ProductService` 没有 `@Service`。
- **请求处理错误**：请求数据格式不正确，例如 `page=abc` 无法转换为 `int`，通常返回 400。
- **业务结果**：商品不存在时由 Controller 返回 404；创建成功通常返回 201。

## 本阶段学习进度

- 已掌握：启动类、Bean、构造器注入、Controller、Service、JSON 响应、路径参数、查询参数、请求体、Optional、ResponseEntity、HTTP 200/201/400/404，以及三层职责。
- 待实践：安装 JDK 8 或 17，创建并启动 Spring Boot 项目，实际发送 GET 和 POST 请求。
- 下一步：进入 MyBatis，学习 Mapper 接口、SQL 与 Java 方法的对应关系，再连接 MySQL。

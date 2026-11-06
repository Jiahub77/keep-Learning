# Spring Boot 启动类、Controller 与第一个 GET 接口

## 最新实践记录（2026-11-04）

BizVision 模板工程已编译通过；用户反馈访问 GET /hello?name=Java 得到 Hello Java，确认模板启动及首次 GET 请求成功。此处实践的是模板 BasicController 的字符串响应与查询参数，并非本篇所有示例。概念学习有、讲解答题有；编译运行有（限模板）；项目实践有入门操作，商品业务未完成。下文各阶段旧的“待实践”描述为当时记录，其他接口与数据库仍待验证。下一步：预测并访问 name=BizVision 和省略 name 两种请求，验证默认值。

## 本主题目标

### 首个自己编写的商品接口（已运行验证）

用户已反馈查询参数与默认值两次访问结果：name=BizVision 对应 Hello BizVision（用户输入首字母小写，已提醒代码输出应为大写）；不传 name 对应 Hello unknown user。

下一步在 BizVision/src/main/java/com/example/bizvision/controller/ProductController.java 创建下面的类。适用 Java 8、Java 17，兼容本项目 Spring Boot 2.7.18；此路径位于启动类包的子包下，可被默认组件扫描发现。

```java
package com.example.bizvision.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
    @GetMapping("/products/1")
    public String getProduct() {
        return "保温杯，库存 20";
    }
}
```

学习者已创建文件并运行，反馈 GET /products/1 返回“保温杯，库存 20”，/products/2 报错。导师通过源码和 HTTP 请求核实：/products/1 返回 200 和该固定文本，/products/2 返回 404 Not Found。本次概念与讲解有、已答题；代码创建有、编译运行有、最小接口实践有，完整商品业务实践未完成。

本次 404 原因：只定义了 /products/1，没有匹配 /products/2 的接口或静态资源；不是 Java 语法、编译、启动错误，也不是执行了库存业务后失败。没有连接数据库，不能据此推断数据库里商品 2 是否存在。

答题记录：学习者认为本次 404 可以说明数据库没有商品 2，回答错误。已解释“路径没有匹配”和“调用接口查询后发现商品不存在”是不同原因，即使二者都可能返回 404，也不能仅凭状态码确定原因。当前 /products/1 返回的名称和库存同样是写死文本，不是数据库查询结果。复答时正确回答不能认定数据库库存为 20，因为当前没有数据库；固定响应与数据库数据的区别复答通过。下一步将固定文本改为商品 JSON。

### Java 8 商品 JSON 实践（已验证）

目标：让 Controller 返回普通 Java 对象，由 Spring Boot 默认配置的 Jackson 转成 JSON，方便前端按字段读取。字段仍为模拟值，不涉及数据库。以下两段均适用 Java 8、Java 17，兼容 Spring Boot 2.7.18；不使用 record。

创建 com.example.bizvision.dto 包，在 ProductResponse.java 中编写：

```java
package com.example.bizvision.dto;

public class ProductResponse {
    private final String name;
    private final int stock;

    public ProductResponse(String name, int stock) {
        this.name = name;
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }
}
```

在现有 ProductController 文件顶部导入 com.example.bizvision.dto.ProductResponse，然后替换原 getProduct 方法：

```java
@GetMapping("/products/1")
public ProductResponse getProduct() {
    return new ProductResponse("保温杯", 20);
}
```

本例默认 Jackson 通过公开 getter 读取属性。这里只向外输出对象，不讨论请求体反序列化，不需要为此添加 setter。学习者已反馈 /products/1 返回 {"name":"保温杯","stock":20}，正确回答将 20 改为 8 会改变 stock。导师核对源码及 HTTP 请求，验证状态为 200、Content-Type 为 application/json、响应一致。概念与讲解有、答题通过；本次代码创建、编译运行及最小商品 JSON 实践有。修改库存为 8 仅预测，尚未运行验证；无数据库实践。

### Java 8 Service 分层实践（已验证）

本次只把创建固定商品对象的逻辑从 Controller 移到 Service，保留响应结构。Controller 接收请求并调用 Service；Service 提供商品数据，当前仍使用模拟数据。以下两段适用 Java 8、Java 17，兼容 Spring Boot 2.7.18。

创建 com.example.bizvision.service.ProductService：

```java
package com.example.bizvision.service;

import com.example.bizvision.dto.ProductResponse;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    public ProductResponse findProduct() {
        return new ProductResponse("保温杯", 20);
    }
}
```

替换 ProductController：

```java
package com.example.bizvision.controller;

import com.example.bizvision.dto.ProductResponse;
import com.example.bizvision.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

@Service 将该类注册为 Spring Bean，Spring 创建 Controller 时经唯一构造器传入 ProductService。无需手动 new ProductService，也无需在这个唯一构造器上添加 @Autowired。ProductResponse 仍由普通 new 创建。保存并重启后访问 /products/1，预期 JSON 不变。

两题回答正确：用户反馈 JSON 仍为 {"name":"保温杯","stock":20}，由 Spring 传入 Service。导师核对源码与实际 HTTP 200 响应，确认 Service 文件已创建、构造器注入链路已运行。学习者将 Controller 方法命名为 findProduct 合法；路径由映射注解决定。概念、讲解答题、编译运行及最小分层实践有，数据库实践没有。

### 路径参数贯穿 Controller 与 Service（已验证）

以下两段适用 Java 8、Java 17，兼容 Spring Boot 2.7.18。保留 Controller 的字段和构造器，增加 import org.springframework.web.bind.annotation.PathVariable;，替换原固定路径方法：

```java
@GetMapping("/products/{id}")
public ProductResponse findProduct(@PathVariable("id") long id) {
    return productService.findProduct(id);
}
```

同时替换 Service 的原方法：

```java
public ProductResponse findProduct(long id) {
    return new ProductResponse("模拟商品 " + id, 20);
}
```

路径 /products/7 中的文本 7 会转换为 long 传入 Controller，再传给 Service。本例不查库，不判断商品是否存在，任何可转换为 long 的 id 都生成模拟结果。/products/abc 无法转为 long，默认返回 400，尚未进入该 Controller 方法体和 Service。这与之前固定路径不匹配导致的 404 不同。

两题回答正确：用户反馈 /products/7 返回 {"name":"模拟商品7","stock":20}，abc 不会进入 Service。导师核对代码及 HTTP 请求：前者为 200，后者为 400 Bad Request。用户代码名称字符串无空格，响应与代码一致。概念讲解、答题、代码修改、编译运行及本次路径参数实践有。

### 模拟商品不存在时返回 404（运行已验证，理解待确认）

当前所有合法 long 编号都返回模拟数据。下一步模拟只有商品 7 存在，使用之前学过的 Optional 表示有无结果，Controller 决定 HTTP 状态。仍未连接数据库。以下代码适用 Java 8、Java 17，兼容 Spring Boot 2.7.18。

Service 导入 java.util.Optional，替换 findProduct：

```java
public Optional<ProductResponse> findProduct(long id) {
    if (id == 7) {
        return Optional.of(new ProductResponse("保温杯", 20));
    }
    return Optional.empty();
}
```

Controller 导入 java.util.Optional 和 org.springframework.http.ResponseEntity，替换对应方法，保留字段和构造器：

```java
@GetMapping("/products/{id}")
public ResponseEntity<ProductResponse> findProduct(@PathVariable("id") long id) {
    Optional<ProductResponse> result = productService.findProduct(id);
    if (result.isPresent()) {
        return ResponseEntity.ok(result.get());
    }
    return ResponseEntity.notFound().build();
}
```

Optional.empty 表示无结果，不是 Java null；isPresent 为 true 后才取 get。匹配 /products/8 后会进入 Service 并返回空结果，由 Controller 主动返回 404 空响应体；不同于之前固定路径未匹配的 404。该示例由模拟规则判定不存在，不代表数据库查询。

学习者已正确修改两处代码，但怀疑服务没启动。导师检查 Maven compile 成功（编译 7 个主源码文件），Java 进程监听 8080，实际请求 /products/7 得到 200 和 {"name":"保温杯","stock":20}，/products/8 得到 404 且 Content-Length: 0。无需修改代码，行为符合预期。

本次 404 是运行中的 Controller 主动返回的 HTTP 响应；build() 未提供响应体，所以浏览器可能显示空白或自带错误提示。若本机端口没有服务监听，通常表现为连接被拒绝，而不是收到该服务的 HTTP 404。概念讲解有，本轮代码修改、编译运行和最小实践有；区分空响应与服务未启动的理解尚待确认。

后续用户已反馈 /products/7 的商品 JSON 与 /products/8 的 404，结果观察正确，导师再次 HTTP 请求验证一致。执行链路题回答“不会进入 Service、由 Service 决定 404”，两点错误。已逐步说明：8 可以转为 long，匹配 /products/{id} 后进入 Controller，调用 Service 的 findProduct(8)，因 id != 7 返回 Optional.empty()，Controller 检查 isPresent 为 false 后返回 ResponseEntity.notFound().build()。Service 当前只表达有无商品，不决定 HTTP 状态。与 /products/abc 在参数转换阶段失败不同。

纠错复答：正确回答 /products/9 会进入 Service，并指出 ResponseEntity.notFound().build() 这条语句。没有直接给出类名，导师补充：ProductController 决定返回 404，ResponseEntity 用于构造响应。已有概念讲解、答题及纠错记录；既有编译运行与最小实践状态不变。

### 首次断点观察（用户选择跳过，未完成、后补）

用户已明确要求跳过本阶段调试。以下指导仅留作后续参考，当前不再等待断点结果，也不标记调试完成。既有接口运行与最小实践不受影响，后续使用普通 Run 模式继续。

适用当前 Java 8 + Spring Boot 2.7.18 工程，同样适用于 Java 17。停止普通 Run 启动的服务，在 ProductService.findProduct 的 if (id == 7) 行添加断点，通过 IDEA 的 Debug 启动 BizVisionApplication。请求 /products/9 后应暂停在该断点，查看变量 id，再 Resume Program 让请求继续完成。若启动正常，暂停期间浏览器等待响应是预期行为，不是服务启动失败。

待反馈：断点是否命中、id 的实际值、恢复执行后的 HTTP 状态码。已提供调试指导，尚未收到实际操作结果；不能记录断点调试已完成。

用户要求继续，尚未反馈调试结果。继续指导：在 ProductController 的 if (result.isPresent()) 行添加断点；若请求停在之前的 Service 断点，使用 Resume Program 继续到 Controller。若没有暂停请求，则在 Debug 模式重新请求 /products/9。断点停在 if 行时，上一行 Service 调用已经完成，result 已赋值；当前行尚未执行。预期 result 为 Optional.empty，isPresent 为 false，可用 Step Over（单步跳过）观察程序走向返回 404 的语句，再 Resume 完成请求。避免重新启动多个实例占用同一端口。

本次只观察已有代码，不修改业务逻辑。待反馈：result 的实际值、下一条将执行的 return。概念讲解有；单步调试操作仍待确认，既有接口编译运行和最小实践状态不变。

### 商品列表 GET /products（2026-11-05 已验证）

本轮目标：单个商品返回对象，商品列表返回 JSON 数组。保留现有详情方法，在 Service 和 Controller 各新增 findAll 方法；依旧使用模拟数据。以下代码适用 Java 8、Java 17，兼容 Spring Boot 2.7.18。

Service 新增 java.util.List 和 java.util.Arrays 导入，并在类内新增：

```java
public List<ProductResponse> findAll() {
    return Arrays.asList(
            new ProductResponse("保温杯", 20),
            new ProductResponse("炒锅", 5)
    );
}
```

Controller 新增 java.util.List 导入，并在类内新增：

```java
@GetMapping("/products")
public List<ProductResponse> findAll() {
    return productService.findAll();
}
```

保存并以普通 Run 重启，访问 GET /products，预期 200 和含两个对象的 JSON 数组。GET /products/{id} 仍为详情路径，两者不冲突。当前没有空列表分支，空列表行为仅作预测：如果 Service 返回空 List，默认返回 200 和 []，不是单个商品不存在时的 404。

两题回答正确：用户反馈 [{"name":"保温杯","stock":20},{"name":"炒锅","stock":5}]，预测空 List 返回 []。导师核对代码并请求验证 HTTP 200、application/json 与该数组。本次复核 Controller 直接返回 List<ProductResponse>，默认返回 HTTP 200；已据当前源码对齐记录。文档、概念讲解、答题、代码修改、编译运行及最小列表实践有；当时空列表仅预测，后续已在 minStock=30 请求中实际验证；数据库实践没有。

### 查询参数筛选商品列表（已运行验证，首条反馈待核对）

本次将之前已学的查询参数与 Stream 筛选应用到列表，不重新讲集合或 Stream 基础。请求 GET /products?minStock=10，只保留库存大于等于 10 的商品。以下两段适用 Java 8、Java 17，兼容 Spring Boot 2.7.18。

Service 增加 java.util.stream.Collectors 导入，替换 findAll：

```java
public List<ProductResponse> findAll(int minStock) {
    return Arrays.asList(
            new ProductResponse("保温杯", 20),
            new ProductResponse("炒锅", 5)
    ).stream()
            .filter(product -> product.getStock() >= minStock)
            .collect(Collectors.toList());
}
```

Controller 增加 org.springframework.web.bind.annotation.RequestParam 导入，替换列表方法，保留详情方法与构造器：

```java
@GetMapping("/products")
public List<ProductResponse> findAll(
        @RequestParam(name = "minStock", defaultValue = "0") int minStock) {
    return productService.findAll(minStock);
}
```

不传 minStock 时用 0。这里筛选的是内存中的模拟列表，不是 MySQL WHERE 查询。Java 8 使用 Collectors.toList()，不使用 Stream.toList()。

2026-11-05 用户反馈第一条为两件商品、第二条为 []，未反馈状态码。导师核对源码，参数传递和过滤条件均正确；实际请求 minStock=10 得到 HTTP 200 和仅保温杯，minStock=30 得到 HTTP 200 和 []。本轮代码修改、编译运行及筛选实践已验证，空列表响应已实际验证。用户最新反馈已更正为仅保温杯和 []，与本次实测一致，结果核对完成。断点调试仍按用户要求跳过、后补。

理解应用如何启动、Controller 如何接收 HTTP 请求，以及如何返回一个简单的商品信息。

## 版本说明

### minStock 非负校验（已运行，边界修正通过）

本次约定最低库存不能为负。负数是合法 int，Spring 可以完成类型转换，必须另加规则判断；与 abc 不能转换成 int 的请求错误不同。本轮采用 Controller 内的显式校验，后续再实践 javax.validation 注解。示例适用 Java 8、Java 17 + Spring Boot 2.7.18。

Controller 增加 org.springframework.http.HttpStatus 和 org.springframework.web.server.ResponseStatusException 导入，替换列表方法：

```java
@GetMapping("/products")
public List<ProductResponse> findAll(
        @RequestParam(name = "minStock", defaultValue = "0") int minStock) {
    if (minStock < 0) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "最低库存不能小于 0");
    }
    return productService.findAll(minStock);
}
```

ResponseStatusException 是 Spring 提供的异常，默认异常处理会按指定状态返回 400；这是本次主动拒绝非法查询参数的策略。默认错误响应不一定显示中文原因，验证以状态码为准。minStock=30 没有商品符合条件是正常查询，仍为 200 和 []。

用户正确反馈 minStock=-1 返回 400、minStock=30 返回 200；导师实际请求验证一致。但源码使用了 minStock <= 0，误拦截允许的 0，导师进一步验证 minStock=0 和省略参数（默认 0）都返回 400。属于条件边界的逻辑错误，不是语法、编译或启动错误。不能仅凭最初两个用例通过就认定校验正确。

学习者已自行将条件改为 minStock < 0，反馈 minStock=0 和省略参数都返回两件商品。导师检查源码并复验：-1 返回 400，0 和省略参数返回 200 与两件商品，30 返回 200 与 []。本轮文档、概念讲解、答题、代码修正、编译运行及最小校验实践均有，边界修正通过；这不代表完整商品模块已完成。下一步确认练习 MySQL 环境，准备数据源连接，数据库实践仍未开始；断点调试按用户要求后补。

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

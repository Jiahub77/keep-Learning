# MyBatis：Mapper 与 SQL 的对应关系

## 更新验证进度（2026-11-05）

用户反馈 GET /products/2 返回 {"name":"更新练习杯","stock":8}；导师核对代码已为 PUT、Controller 有 200/404 分支、Service 有 Spring @Transactional，并只读请求确认 GET 200 与相同 JSON。现有 @PathVariable Long id 依赖参数名发现且当前请求已工作，建议明确 @PathVariable("id")，Long 可用于已提供的路径参数，不虚报为编译错误。

真实更新的名称和库存效果已有证据；PUT 本身的原始响应、数据库价格及事务回滚/并发未独立验证。下一步学习者在 Navicat SELECT id, name, price_in_cents, stock FROM bizvision_learning.products WHERE id = 2; 核对价格是否 10900。同时先 GET /products/999999，确认 404 后再对该不存在编号发合法 PUT，预期 404 且不会创建记录；若编号存在则另选编号，不修改现有非练习商品。导师不代发写请求。

最新反馈：用户确认 price_in_cents 为 10900，不存在商品 PUT 为 HTTP/1.1 404。以上两项按用户反馈验证通过；成功 PUT 原始响应及回滚/并发仍未验证。查、增、改已有实践，完整 CRUD 尚未完成。

## PUT 接口静态检查与纠错（待学习者修正）

用户正确回答：不存在不执行 UPDATE，内容相同不代表不存在。Mapper 的 findByIdForUpdate 已写对。检查发现 Service 更新方法遗漏 @Transactional，当前不能认定锁定查询与更新处于同一事务；指导导入 org.springframework.transaction.annotation.Transactional，在 updateProduct 上添加注解，参数改为 long id。

用户自行新增了 Controller 更新方法，但使用 @PostMapping、@RequestParam Long id、返回 201 与 Optional<Long> 响应体。指导改为 PUT /products/{id}、@PathVariable("id") long id，并返回 ResponseEntity<Long>；Service 结果有值时 200 和 id，空结果时 404。具体写法如下，适用 Java 8、Java 17 + Spring Boot 2.7.18：

```java
@PutMapping("/products/{id}")
public ResponseEntity<Long> updateProduct(
        @PathVariable("id") long id,
        @RequestBody CreateProductRequest request) {
    Optional<Long> result = productService.updateProduct(id, request);
    if (result.isPresent()) {
        return ResponseEntity.ok(result.get());
    }
    return ResponseEntity.notFound().build();
}
```

本例 PUT 要求提交 name、priceInCents、stock 全部字段，不是只改某一字段。修正后重启，由学习者对之前新增练习商品 id=2 发送 PUT（名称改为“更新练习杯”、价格10900分、库存8），记录状态码与响应体，再 GET /products/2 验证名称库存；价格需 SQL 单独查询核实。导师不代发写请求、不代改业务代码。当前修改、编译运行、实际 UPDATE 与事务行为仍待验证，不能因答题正确标记代码实践完成。

## 更新 Service：存在性检查与事务（待实践）

最新静态检查：学习者已在 updateProduct 接收 Long id 并调用 entity.setId(id)，toEntity 已改为 private。编号传递修正完成；真实更新尚未执行，上一道“更新是否生成新 id”的题未直接作答。以下继续采用 long 参数，URL 路径必须提供编号，不需要可空包装类型。

以下两段适用 Java 8、Java 17 + Spring Boot 2.7.18/MyBatis，数据库表为 InnoDB。在 Mapper 新增锁定读取，不替换原详情查询：

```java
@Select("SELECT id, name, price_in_cents AS priceInCents, stock "
        + "FROM products WHERE id = #{id} FOR UPDATE")
ProductEntity findByIdForUpdate(@Param("id") long id);
```

Service 导入 org.springframework.transaction.annotation.Transactional，替换更新方法：

```java
@Transactional
public Optional<Long> updateProduct(long id, CreateProductRequest request) {
    if (id <= 0) {
        throw new IllegalArgumentException("商品编号必须大于 0");
    }
    ProductEntity entity = toEntity(request);
    ProductEntity existing = productMapper.findByIdForUpdate(id);
    if (existing == null) {
        return Optional.empty();
    }
    entity.setId(id);
    productMapper.update(entity);
    return Optional.of(id);
}
```

toEntity 先验证并组装请求；查不到返回空结果，查到更新原编号。@Transactional 包住锁定查询和 UPDATE，FOR UPDATE 锁住查到的行，阻止其他事务在两步之间修改或删除该行。前提是从 Controller 调用 Spring 管理的 Service，不能同类内部绕过代理。此处只说明保护存在性检查所需的作用，不扩展完整并发课。

返回 Optional.empty 表达不存在，下一步 Controller 映射为 404；存在时即使新旧值相同也视为成功，不把 MySQL 的更新计数 0 简单等同不存在。事务运行时异常默认回滚，当前尚未运行验证回滚或并发行为。

待答题：商品不存在时是否执行 UPDATE？更新前后内容相同，能否因此判断商品不存在？本轮讲解有，源码修改、编译运行、真实更新及事务验证待完成，导师不代改业务代码。

## 修改第一步：Mapper UPDATE（待学习者编写）

在 ProductMapper 接口保留原查询和插入声明，新增以下方法。现有 org.apache.ibatis.annotations.* 已包含 Update；若使用逐项导入则添加 org.apache.ibatis.annotations.Update。适用 Java 8、Java 17 + Spring Boot 2.7.18/MyBatis。

```java
@Update("UPDATE products SET name = #{name}, "
        + "price_in_cents = #{priceInCents}, stock = #{stock} "
        + "WHERE id = #{id}")
int update(ProductEntity product);
```

SET 列出更新的字段；WHERE id 指定目标行，id 来自 product.getId()。更新不生成新 id，不使用 useGeneratedKeys。返回值是更新计数，不是商品 id；MySQL/JDBC 对相同值更新的计数语义取决于配置，后续不能未经处理就把 0 一律等同商品不存在。

两题回答正确：id 为 2 时针对该记录，去掉 WHERE 会更新所有记录。导师检查 update 声明已经写入，字段和条件对应正确。建议 stock = #{stock} 后补空格，清晰分隔 WHERE，不把此处参数占位符边界直接认定为已验证的 SQL 错误。真实 UPDATE 尚未执行，编译与运行待确认。

### 更新前准备：复用请求校验与 Entity 转换（待学习者编写）

本步不添加更新逻辑，先避免新增与更新重复写校验。适用 Java 8、Java 17。在 ProductService 将现有 public Long createProduct(CreateProductRequest request) 改名并改返回类型为 private ProductEntity toEntity(CreateProductRequest request)，保留全部校验和 entity.set...，删除末尾 productMapper.insert(entity)，将 return entity.getId() 改为 return entity。

然后新增简短的 public Long createProduct(CreateProductRequest request)，调用 toEntity、insert，再返回 entity.getId()。请求 DTO 暂时继续使用 CreateProductRequest，下一步更新也复用这组字段；后续可按需要区分请求类型。toEntity 只校验并组装内存对象，不能插入数据库；否则后续更新会误新增。

用户回答仅调用 toEntity 不插入数据库，正确。导师核对新增方法已复用 toEntity，校验与转换已提取；当前 toEntity 为 public，建议改为 private（类内辅助方法，不影响 SQL）。本轮未单独编译或执行重构后的新增。

学习者自行新增 updateProduct(CreateProductRequest request)，但 toEntity 不设置 id，因此传入 UPDATE 的 entity.id 为 null，WHERE id = NULL 不会匹配商品，返回 id 也为 null。不是 Java 编译错误，而是更新逻辑缺少目标编号。下一小步仅修正编号传递，不宣称更新流程完整。

适用 Java 8、Java 17，替换刚添加的 updateProduct 方法：

```java
public Long updateProduct(long id, CreateProductRequest request) {
    ProductEntity entity = toEntity(request);
    entity.setId(id);
    productMapper.update(entity);
    return entity.getId();
}
```

此方法仍是过渡版本：没有判断商品是否存在、未验证受影响行数，不能把返回 id 当作更新成功证明，暂不接 PUT 或发更新请求。下一步加存在性检查与事务再对外暴露。待答题：与 INSERT 不同，UPDATE 会自动生成新 id 吗？代码修改与真实 UPDATE 均待完成。

## 新增运行反馈（2026-11-05）

用户对 POST 实践反馈“成功”。导师检查 ProductController 的 POST、201 返回及 IllegalArgumentException → 400 处理均已写好；只读请求 GET /products/2 得到 HTTP 200 和 {"name":"新增练习杯","stock":0}。新增成功按用户反馈记录，记录可读已独立验证，不声称已独立观察 POST 的 201/主键响应。导师没有代发新增请求。

用户反馈缺失 stock 返回 400 和“库存必须填写且不能为负数”，错误响应实践按用户反馈记为通过。结合已核对的 Service 代码可知校验抛出异常在 insert 之前；未另行查询数据库行数。下一步进入修改商品教学。

## 最新实践：真实商品详情查询通过（2026-11-05）

用户修正为 productMapper.findID(id) 后，反馈 /products/1 返回 {"name":"iPhone 17 pro","stock":100}。导师核对 Service 调用 Mapper、Mapper 使用真实 SELECT，并请求接口验证 HTTP 200 与 JSON 一致。已打通 Controller → Service → Mapper → MySQL → Entity → Response；真实查询、名称及库存映射已有运行证据。价格虽已在 SQL 中选择，尚未通过响应验证。下方之前“查询待验证”为历史状态。增改删仍未实现，导师不代写业务代码。

### 新增第一步：请求 DTO（待学习者编写）

在 com.example.bizvision.dto 包创建普通 CreateProductRequest 类，适用 Java 8、Java 17：

```java
package com.example.bizvision.dto;

public class CreateProductRequest {
    private String name;
    private Integer priceInCents;
    private Integer stock;
}
```

通过 IDEA Generate → Getter and Setter 为三字段生成访问方法。用于后续接收 POST JSON；不含 id，因为新增主键交给数据库生成。Integer 可用 null 表达请求缺失的数字字段，int 默认 0 无法这样区分；后续会先校验再拆箱，避免空指针。本步只建 DTO，不修改 Controller 或 Mapper。

用户回答 null，正确；导师已检查 CreateProductRequest 的三个字段与 getter/setter 均正确。DTO 创建和本题答题完成，未单独编译或通过 POST 反序列化验证。

### 新增第二步：Mapper INSERT 与主键回写（待学习者编写）

在 ProductMapper 添加 org.apache.ibatis.annotations.Insert、org.apache.ibatis.annotations.Options 导入，保留 findID 方法，新增下列声明。适用 Java 8、Java 17 + Spring Boot 2.7.18/MyBatis。

```java
@Insert("INSERT INTO products(name, price_in_cents, stock) "
        + "VALUES(#{name}, #{priceInCents}, #{stock})")
@Options(useGeneratedKeys = true, keyProperty = "id")
int insert(ProductEntity product);
```

单个对象参数的占位符取自 Java 属性：#{priceInCents} 对应 getter，而非数据库列名 price_in_cents。SQL 不写 id，由数据库生成；useGeneratedKeys/keyProperty 让 MyBatis 把生成编号回写 product.id。insert 返回受影响行数，不是主键。这里只定义方法，不会自动执行插入；Service 与 POST 后续再接。

两题回答正确：insert 返回 1，product.getId() 为 12。导师已检查 Mapper 插入声明、参数属性和主键回写配置正确。当前 SQL 的 ) 与 VALUES 之间未留空格，但标点可分隔词法，不等同于此前 stockFROM 错误；建议排版补空格，不虚报语法错误。方法已编写并静态检查，真实插入未执行。

### 新增第三步：Service 校验并插入（待学习者编写）

ProductService 导入 com.example.bizvision.dto.CreateProductRequest，保留查询方法并新增以下方法。适用 Java 8、Java 17，使用普通类与已有 MyBatis Mapper。

```java
public Long createProduct(CreateProductRequest request) {
    if (request.getName() == null || request.getName().trim().isEmpty()) {
        throw new IllegalArgumentException("商品名称不能为空");
    }
    String name = request.getName().trim();
    if (name.length() > 100) {
        throw new IllegalArgumentException("商品名称不能超过100个字符");
    }
    if (request.getPriceInCents() == null || request.getPriceInCents() < 0) {
        throw new IllegalArgumentException("价格必须填写且不能为负数");
    }
    if (request.getStock() == null || request.getStock() < 0) {
        throw new IllegalArgumentException("库存必须填写且不能为负数");
    }

    ProductEntity entity = new ProductEntity();
    entity.setName(name);
    entity.setPriceInCents(request.getPriceInCents());
    entity.setStock(request.getStock());
    productMapper.insert(entity);
    return entity.getId();
}
```

方法按 HTTP 层保证 request 对象非 null 的前提教学。先校验再拆箱赋值给 Entity，避免 Integer 为 null 时自动拆箱发生空指针。0 价格和0库存均合法；返回回写的 id，不是受影响行数。此时还没有 POST 路由，IllegalArgumentException 也尚未映射为 HTTP 400，不能宣称已完成新增接口。

两题回答正确：stock 缺失不会调用 Mapper；stock 为 0 且其他字段合法允许插入。导师核对 Service.createProduct，校验顺序、Entity 赋值与返回生成 id 均正确。代码已编写并静态检查，尚未通过 POST 实际插入。

### 新增第四步：POST 接口与参数异常响应（待实践）

在现有 ProductController 中保留 GET 方法，增加 CreateProductRequest、PostMapping、RequestBody、ExceptionHandler 导入。已有 HttpStatus/ResponseEntity 可复用。以下代码适用 Java 8、Java 17 + Spring Boot 2.7.18：

```java
@PostMapping("/products")
public ResponseEntity<Long> createProduct(@RequestBody CreateProductRequest request) {
    Long id = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(id);
}

@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleInvalidArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
}
```

@RequestBody 将 JSON 转为请求 DTO；成功时返回 201，响应体为生成 id 的 JSON 数字。Controller 本地异常处理将 Service 主动抛出的 IllegalArgumentException 转为 400 文本提示，不把数据库故障统称参数错误；以后再重构统一错误 DTO 与全局处理器。

学习者重启后在系统终端用 curl -i -X POST http://localhost:8080/products -H 'Content-Type: application/json' -d '{"name":"新增练习杯","priceInCents":9900,"stock":0}' 验证一次。此请求实际插入一条记录，重复会重复新增；记录返回 id，再 GET /products/该id 验证。然后去掉 stock 字段测试缺失参数，应返回 400，不插入。暂不预设返回 id 为固定数。

待反馈成功 POST 的状态码/实际 id、缺失 stock 的状态码/响应体。本次 POST 与异常映射已讲解，代码创建、编译运行及数据库插入待用户完成；导师仅写笔记，不代写或代发新增请求。

## 最新步骤：Mapper 接入 Service（待学习者实践）

本轮编译错误：Service 已正确声明 productMapper 字段并完成构造器注入，但调用写成 ProductMapper.findID(id)，使用类型名调用实例方法，出现“无法从静态上下文中引用非静态方法”。指导改为 productMapper.findID(id)，使用 Spring 注入的实例，不把 Mapper 方法改为 static。大小写敏感，ProductMapper 是类型，productMapper 是字段。代码待学习者修改，导师未代改，真实 SQL 查询仍未验证。另外当前 Service 与 Controller 已同时移除列表方法，没有遗留 findAll 调用；本轮只调通详情，列表后续再恢复为数据库查询。

学习者要求继续，导师检查确认 ProductMapper 已改为 interface，stock 与 FROM 间已有空格，保留合法方法名 findID。两处源码错误已修正；尚未在本轮编译或执行查询。上一题 null 返回值复答未提供，不标记答题通过。

以下适用 Java 8、Java 17 + Spring Boot 2.7.18。ProductService 新增 ProductMapper 和 ProductEntity 导入，在类中加入字段和构造器，保留原列表方法，只替换详情方法：

```java
private final ProductMapper productMapper;

public ProductService(ProductMapper productMapper) {
    this.productMapper = productMapper;
}

public Optional<ProductResponse> findProduct(long id) {
    ProductEntity entity = productMapper.findID(id);
    if (entity == null) {
        return Optional.empty();
    }
    return Optional.of(new ProductResponse(entity.getName(), entity.getStock()));
}
```

Spring 注入 Mapper 代理，MyBatis 执行 SQL，将一行映射为 Entity。查不到返回 null，由 Service 转换为 Optional.empty，现有 Controller 再返回 404。返回 DTO 暂时仍只有名称和库存；列表方法仍用模拟数据，本轮不修改它。

待实践：学习者修改并重启，在 Navicat 查询 SELECT id, name, stock FROM bizvision_learning.products ORDER BY id;，选实际存在的 id 请求 /products/{id}；若表为空则先完成前文的两条练习数据插入，不假设 id 为 7。反馈实际 id、HTTP 状态和响应体；没有数据而返回 404 不足以验证成功结果映射。本轮只提供指导，不代改业务代码。

## 当前教学安排：由学习者亲自实现 CRUD

用户要求在 Java 项目实践 CRUD，并明确由导师教、自己写。导师误代写的 CRUD 代码已撤回，恢复原模拟 Controller/Service/Response、原有依赖和测试；数据库连接配置及用户已有文件保留。未执行 CRUD 测试或数据库写入，不记录 CRUD 完成。

ProductEntity 已由学习者创建。导师核对源码：包名、Long id、String name、int priceInCents、int stock 及全部 getter/setter 正确。代码编写与静态检查完成；本轮未单独编译，也未验证 MyBatis 结果映射。该对象承接数据库一行，ProductResponse 负责接口响应。

### 按 ID 查询的 Mapper（待学习者编写）

在 com.example.bizvision.mapper 包创建 ProductMapper 接口。以下适用 Java 8、Java 17 + Spring Boot 2.7.18/MyBatis starter 2.3.0：

```java
package com.example.bizvision.mapper;

import com.example.bizvision.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper {
    @Select("SELECT id, name, price_in_cents AS priceInCents, stock "
            + "FROM products WHERE id = #{id}")
    ProductEntity findById(@Param("id") long id);
}
```

@Mapper 用于识别 Mapper 接口，MyBatis 为它生成实现；@Select 绑定 SQL；@Param("id") 对应 #{id}；AS priceInCents 将查询结果列名映射到 Java 属性，不修改数据库列名。本例方法返回单个 ProductEntity，查询无结果时返回 null，下一步由 Service 转换为 Optional；现在尚未接入 Service，不能仅凭重启成功认定 SQL 已执行。

待答题：查询无匹配行时 findById 返回什么？AS priceInCents 是否会修改数据库表的列名？文档与讲解有，Mapper 文件创建、编译和真实查询均待完成。导师只修改笔记，不代写业务代码。

实际编写检查：学习者已创建 ProductMapper.java，但声明为 public class ProductMapper，内部 findID 方法没有方法体，IDE 报 Method body or 'abstract' modifier expected。这是 Java 编译阶段错误：本例应改为 interface，接口方法可以只声明、不写方法体，由 MyBatis 生成实现。不能仅加 abstract 来代替 Mapper 接口。

另发现 SQL 字符串拼接为 stockFROM，原因是 stock 后和 FROM 前都没有空格。Java 不会自动给拼接字符串补空格；修复接口声明后，这个问题仍会在 SQL 真正执行时引发数据库语法错误，而不是 Java 编译错误。指导在 stock 后补空格。

方法名 findID 本身合法，后续 Service 按实际名称调用即可，不要求为此更名。答题：查不到 ProductEntity 初答 0，错误，已纠正为 null；AS 不永久改列名回答正确。下一步只修正 class/interface 与 SQL 空格，复答“查不到返回 null 还是 0”，暂不接入 Service。代码尚未编译通过、真实查询未执行；导师未代改业务代码。

### 认证排查最新结果（2026-11-05）

**当前结论：** 指导定向改密和重试 TCP 后，用户反馈“可以连上了”。连接恢复按用户反馈记录；未独立读取改密或新连接日志，不认定完整根因已证实。下方为历史排查过程，过往“待执行/待验证”以本条最新状态为准。Java JDBC 与 MyBatis 查询仍未实践。

### 准备专用练习库（用户已确认完成）

在已成功连接的 MySQL 客户端执行以下 MySQL SQL，不依赖 Java 8/17：

```sql
CREATE DATABASE IF NOT EXISTS bizvision_learning
    CHARACTER SET utf8mb4;
USE bizvision_learning;
SELECT DATABASE() AS current_database;
```

CREATE DATABASE 创建库；IF NOT EXISTS 在同名库已存在时不重复创建，也不清空数据；USE 选择当前连接使用的库。预期最后结果为 bizvision_learning。当前只提供创建与选库指导，结果待反馈，不提前标记完成；尚未建商品表、配置 Java 数据源或执行 MyBatis 查询。

用户已确认 SELECT DATABASE() 返回 bizvision_learning，创建及选库按用户反馈记为完成。前文创建待反馈为当时状态；商品表、Java 数据源和 MyBatis 查询仍未完成。

### 配置 Java 数据源（连接检查已获用户确认）

目标：使 Spring Boot 能建立到练习库的数据源。适用 Java 8、Java 17 + Spring Boot 2.7.18。本地读取 BOM 确认其管理 mysql-connector-j 8.0.33，先沿用项目依赖管理；这不是服务端版本，本机服务端为 9.6.0，实际 JDBC 连接兼容性仍需运行验证。

在 pom.xml 的 dependencies 中加入：

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

重新加载 Maven。删除 application.properties 中 spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration，保留其他配置并增加：

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/bizvision_learning
spring.datasource.username=root
spring.datasource.password=${BIZVISION_DB_PASSWORD}
```

以上 XML 和 properties 用于 Java 8/17 + Spring Boot 2.7.18。IDEA 的 BizVisionApplication Run Configuration 中设置环境变量 BIZVISION_DB_PASSWORD 为刚才 TCP 登录成功的密码，密码不发到对话、不写进提交文件。运行时 Spring 读取该环境变量。

用户在环境变量配置指导后反馈“可以了”；导师检查到 JDBC 驱动依赖、数据源配置键已添加，排除项已删除，不读取密码值。环境变量操作按用户反馈记为完成。启动成功不单独证明数据库连接成功：连接池可能按需创建连接，现有模拟接口也不访问数据库；目前未获得 Java SQL 执行结果，下一步执行 SELECT DATABASE() 验证。

### 首次 Java SQL 连接验证（用户反馈成功）

创建 com.example.bizvision.config.DatabaseConnectionCheck，使用临时启动检查。以下适用 Java 8、Java 17 + Spring Boot 2.7.18。

```java
package com.example.bizvision.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionCheck implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class);
        System.out.println("数据库连接成功，当前库：" + database);
    }
}
```

CommandLineRunner 使 Spring Boot 在启动流程中执行 run；JdbcTemplate 借用已配置的数据源执行只读 SQL，String.class 表示按字符串读取结果。本次仅用于验证连接，不替代后续 MyBatis Mapper。创建并重启后应输出“数据库连接成功，当前库：bizvision_learning”。若 SQL 抛异常，打印不会执行，需根据实际异常排查；Started 日志单独出现不代表 runner 执行成功。

用户在本轮验证后反馈“成功了”。导师已检查 DatabaseConnectionCheck 源码与上述 SQL 一致；记录 JDBC 连接及只读 SQL 执行成功（用户反馈，未独立读取控制台日志），并非 MyBatis 商品查询完成。建议删去或注释临时类的 @Component，使其不再每次启动执行；停用操作待用户完成。

### 创建商品表（用户确认完成）

本次只建表，不插入或修改商品数据。以下为 MySQL SQL，适用于当前 MySQL 9.6；Java 8/17 项目均可使用，和 Java 语法版本无关。

```sql
USE bizvision_learning;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price_in_cents INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DESCRIBE products;
```

id 是自增主键，price_in_cents 以整数分存储金额，stock 默认为 0。NOT NULL 只限制不能为 NULL，并不会自动拒绝负数；库存、价格的范围校验后续实现。InnoDB 支持后续事务练习。IF NOT EXISTS 不重建同名表，因此需检查 DESCRIBE 实际结构符合预期。

用户确认 DESCRIBE 显示四列且 id 的 Extra 为 auto_increment，正确回答 id 自增，所以插入通常不必手填。文档、概念讲解、答题有；建表与结构检查按用户反馈记录完成，导师未独立查询数据库。MyBatis 查询尚未完成。

### 插入并查看练习商品（待实践）

以下是 MySQL SQL，适用当前 MySQL 9.6，Java 8/17 项目均可使用。先在 Navicat 执行；INSERT 只执行一次，反复执行会新增重复商品，SELECT 可重复执行。

```sql
USE bizvision_learning;

INSERT INTO products (name, price_in_cents, stock)
VALUES ('保温杯', 9900, 20),
       ('炒锅', 15900, 5);

SELECT id, name, price_in_cents, stock
FROM products
ORDER BY id;
```

id 由数据库生成，9900 分是 99 元。自增编号不保证连续或一定从 1 开始，以查询结果为准。现有 Java Service 仍返回写死的数据，不会因为手动插入就自动查询 MySQL。

待答题：反馈实际两行查询结果（尤其 id），说明 15900 分是多少元。文档与讲解有，插入、查询与答题尚未确认；不能记为 MyBatis CRUD 已完成。下一步再写 Mapper，连接真实查询链路。

### 历史认证排查记录

用户完成无默认配置对照：新 socket 会话（连接 38）成功，使用同一密码的显式 TCP 仍返回 1045。客户端默认文件和 login-path 干扰已排除。结合两个独立 root Host 账号，下一步指导用户在可用 socket 管理员会话中用 ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY 本机输入的密码，明确只更新该账号密码，再新建 TCP 连接验证。若该账号被其他本地工具使用，需要同步其保存密码；不修改 root@localhost，不需要 FLUSH PRIVILEGES。此为待用户执行的修复尝试，未确认根因、未执行改密、未确认 TCP 修复；不收集密码或含密码的 SQL。

用户查询确认 root@127.0.0.1 和 root@localhost 两个账号均存在，plugin 均为 caching_sha2_password，account_locked 和 password_expired 均为 N；本次查询未返回匿名账号。排除此前 auth_socket 假设和锁定、过期状态问题。两个 Host 对应独立账号，但不能据此认定密码不同或 TCP 实际匹配哪一行；1045 中的 localhost 不是 CURRENT_USER() 的成功认证结果。

下一步保留成功的管理员会话，在新的系统终端分别使用 mysql --no-defaults --no-login-paths --protocol=SOCKET --socket=/tmp/mysql.sock -u root -p 和 mysql --no-defaults --no-login-paths --protocol=TCP -h 127.0.0.1 -P 3306 -u root -p，交互输入同一已知密码，比较是否能新建连接。这样排除客户端默认文件与 login-path 设置、旧会话持续有效的干扰。只记录成功或错误信息，不收集密码，不修改账号。Java 数据库连接仍未完成。

## 最新实践状态：准备接入本机 MySQL（2026-11-05）

### 认证排查记录

用户使用 Navicat，以 root 登录持续收到 1045 Access denied（using password: YES）。这表示提交了密码但认证失败，不说明密码正确；尚未执行练习 SQL。

导师只读排查确认 Homebrew mysql 服务处于 started 状态，本机 mysql 客户端版本为 9.6.0（服务端版本仍未认证查询确认），/tmp/mysql.sock 存在。分别通过 TCP 127.0.0.1:3306 和该 Unix socket 以 root、不提供密码尝试只读查询，均返回 1045（using password: NO）。说明当前不能通过这两种方式空密码登录 root；不据此判断用户实际密码，也不能认定只有 Navicat 有问题。

未停止服务、未重置密码、未修改账号或数据库。下一步确认是否存在可用管理员连接，或用户是否知道此前设置的 root 密码；需要输入密码时仅在本机客户端输入。认证排查有实践，Java 数据源连接和业务 SQL 实践仍未完成。

后续证据：用户提供 mysql -u root -p 成功登录的终端输出，服务端版本确认为 9.6.0 Homebrew。密码对该连接有效，前面的空密码失败并不意味着用户密码错误。当前只剩 Navicat 与终端连接差异待排查，不需要重置密码。终端默认通常走 Unix socket，但不能仅凭命令认定实际传输方式。

下一步在系统终端显式执行 mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u root -p，交互输入刚才成功的密码。此为 MySQL 客户端命令，不依赖 Java 8/17。若 TCP 登录成功，对齐 Navicat 主机、端口、用户名和密码，进一步检查客户端错误；若失败，保留原成功会话并检查 status、CURRENT_USER() 等连接信息。显式 TCP 测试结果待用户反馈，Java JDBC 尚未连接。

显式 TCP 测试结果已反馈：127.0.0.1:3306 的 root 密码登录返回 1045（using password: YES），默认 mysql -u root -p 会话此前成功。不能因此认定为 Navicat 独有故障，也不能直接认定密码错误、认证插件或账号 Host 是原因。

下一步仅在成功的 mysql 会话运行 status; 以及 SELECT USER() AS login_identity, CURRENT_USER() AS authenticated_account, @@port AS server_port, @@socket AS server_socket;，确认传输方式、实际匹配账号和服务端地址。若默认连接实际指向另一实例或端口，该信息可帮助识别。不要查询或输出 authentication_string；无需分享密码。等结果再判断下一步，不修改账号或权限。

用户反馈成功会话：Connection 为 Localhost via UNIX socket，socket 为 /tmp/mysql.sock；USER() 和 CURRENT_USER() 均为 root@localhost，@@port 为 3306，服务端为 9.6.0 Homebrew。已确认 socket 连接方式与匹配账号，仍不能仅凭此判断 TCP 拒绝的根因。

下一项只读诊断：SELECT User, Host, plugin, account_locked, password_expired FROM mysql.user WHERE User IN ('root', '') ORDER BY User, Host;。仅检查 root 及匿名账号的 Host、认证插件和状态，不读取密码散列。若插件为 auth_socket，可解释仅 socket 认证；若为其他插件则继续依据实际结果排查，不提前认定结论。数据库连接排查进行中，Java 数据源连接仍未完成。

用户确认可连接本机 MySQL；导师只读检查发现 mysqld 监听 3306。BizVision 的 Java 8/Spring Boot 2.7.18 工程尚未添加 MySQL JDBC 驱动，数据源自动配置仍被临时排除。端口监听不等于 Java 连接成功。

当前先确认服务端版本和练习数据库。以下是 MySQL SQL，不依赖 Java 版本，可用于 Java 8/17 项目的数据库准备，在数据库客户端执行：

```sql
SELECT VERSION() AS mysql_version, DATABASE() AS current_database;
```

VERSION() 返回服务端版本；DATABASE() 返回当前选中的数据库名，未选库时为 NULL，并不代表连接失败。待用户反馈版本、拟使用的练习库名；不收集密码。文档与概念讲解有，本次查询答题待完成；Java 数据库连接、实际 MyBatis SQL 执行和数据库项目实践均没有。下一步才配置驱动和数据源并验证连接。

## 本主题目标

理解 MyBatis 如何把 Java 方法和 SQL 连接起来。暂时只学习查询一个商品，不连接真实数据库。

## 版本说明

Mapper 注解、SQL 参数绑定和分层思想适用于 Java 8、Java 17。本文中的文本块 `"""`、`record` 和 `Stream.toList()` 仅适用于 Java 15+/16+，按 Java 17/21 示例理解；Java 8 请使用普通字符串、普通 Java 类和 `Collectors.toList()`。Spring Boot 版本必须与 Java 版本匹配。

## MyBatis 做什么

MyBatis 是持久层框架，负责把 Java 方法调用转换成 SQL 执行，并把查询结果转换成 Java 对象。它不会替代 MySQL：

```text
Service：决定要做什么业务
Mapper：描述如何访问数据库
MyBatis：执行 Mapper 对应的 SQL
MySQL：真正保存和查询数据
```

## 小主题：多个参数如何绑定

当 Mapper 方法只有一个简单参数时，`#{id}` 通常可以直接绑定参数。方法有多个参数时，建议用 `@Param` 明确给每个参数命名：

```java
// 版本：Java 8、Java 17 均适用。
// 适用 Java 8、Java 17；需要 MyBatis 的 org.apache.ibatis.annotations.Param
@Select("SELECT id, name, stock FROM products "
        + "WHERE name LIKE CONCAT('%', #{keyword}, '%') "
        + "AND stock >= #{minStock}")
List<ProductRow> search(
        @Param("keyword") String keyword,
        @Param("minStock") int minStock);
```

调用 `search("杯", 1)` 时，`#{keyword}` 得到“杯”，`#{minStock}` 得到 1。`#{}` 是参数占位绑定，不是把字符串直接拼进 SQL，因此应优先使用它来避免 SQL 注入风险。

如果写成 `#{name}`，但方法参数没有 `@Param("name")`，也没有名为 `name` 的可用参数，通常会在 SQL 执行阶段出现参数找不到的运行错误。这个问题不是 Java 语法错误，也不是数据库没有商品。

Java 8 和 Java 17 的这段 Mapper 写法相同。Java 17/21 可以把 SQL 改成文本块以提高可读性；Java 8 必须使用普通字符串拼接或 XML。

注意：省略 `@Param` 不一定报错，是否能按源码参数名绑定还受编译参数和 MyBatis 配置影响。这里讨论的是占位符名称确实不存在于可用参数中的情况；多参数方法显式写 `@Param` 更清晰。

### 2026-11-04 复习记录

- 文档：有；概念学习与讲解：有；答题：有，筛选条件题复答通过；编译运行：没有；项目实践：没有。
- `search("杯", 1)`：保温杯库存 20、咖啡杯库存 0、炒锅库存 10 时，仅保温杯符合条件。初答包含咖啡杯，漏看库存条件；`AND` 要求名称和库存条件同时成立。
- 参数名称确实找不到时属于 MyBatis 运行时错误：回答正确。
- 复答：正确回答 `search("杯", 0)` 返回保温杯和咖啡杯，两者同时满足名称包含“杯”和库存大于等于 0。
- 参数命名题回答正确：注解改成 `@Param("stock")` 后，SQL 应对应改成 `#{stock}`；此题通过。
- 条件库存扣减阅读题两题正确（每题初始库存独立为 5）：扣减 3 返回 1、剩余 2；扣减 8 返回 0、库存仍为 5。已讲解并答题；编译运行没有，项目实践没有。
- 返回 0 的含义题回答正确：商品不存在也会返回 0，不能仅凭返回值区分商品不存在与库存不足。
- Service 异常流程题两题正确：rows 为 0 时不打印成功；rows 为 1 时不抛出该异常并打印成功。该主题已有文档、讲解和答题；编译运行没有，项目实践没有。
- 事务回滚复习：题目为库存 5 扣减 2 后日志写入抛出运行时异常；已正确回答回滚后库存为 5；无事务且扣减已自动提交的对照题回答 3，正确。已讲解 throw 与事务回滚的区别，尚未独立口述原因。前提：Spring 事务代理生效，两次写入使用同一事务和支持事务的 MySQL InnoDB 表，异常向外传播，不考虑其他请求修改库存。
- Java 8 工作项目基线已确认：JDK 8、Spring Boot 2.7.18、Maven 3.8.8；未据此标记环境安装或实践完成。

## Mapper 接口与注解 SQL

```java
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
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
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
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
// 版本：Java 8、Java 17 均适用。
int affectedRows = productMapper.decreaseStock(productId, quantity);
if (affectedRows == 0) {
    throw new IllegalStateException("商品不存在或库存不足");
}
```

数据库只会在商品存在且当前库存足够时执行扣减。这样可以避免库存被更新为负数；后续还会结合事务和并发进一步实践。

## 新增后获取数据库生成的主键

数据库表使用 `AUTO_INCREMENT` 时，INSERT 不需要传入 `id`。MyBatis 可以把数据库生成的主键写回一个可修改的 Java 对象：

```java
// 版本：Java 8、Java 17 均适用。
public class ProductEntity {
    private Long id;
    private String name;
    private int priceInCents;
    private int stock;

    // 构造器、getter、setter 省略
}
```

```java
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
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
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
public record CreateProductRequest(
        @NotBlank String name,
        @PositiveOrZero int priceInCents,
        @PositiveOrZero int stock
) {
}
```

```java
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
@Update("""
    UPDATE products
    SET stock = #{stock}
    WHERE id = #{id}
    """)
int updateStock(@Param("id") long id, @Param("stock") int stock);
```

删除商品：

```java
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 8、Java 17 均适用。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
public record ApiError(String code, String message) {
}
```

`code` 适合前端程序判断，`message` 适合展示或日志。前端不应依赖易变的中文文本判断错误类型。

## 请求校验与 HTTP 状态

```java
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
public record CreateProductRequest(
        @NotBlank String name,
        @PositiveOrZero int priceInCents,
        @PositiveOrZero int stock
) {
}
```

```java
// 版本：Java 8、Java 17 均适用。
public ProductResponse createProduct(
        @Valid @RequestBody CreateProductRequest request) {
    // 通过基础校验后，再进入 Service 业务规则
}
```

`@Valid`、`@NotBlank`、`@PositiveOrZero` 处理请求基本格式；库存是否足够、名称是否重复等依赖数据库状态的规则仍由 Service 处理。

## 分页查询

```java
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
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
// 版本：Java 17/21；Java 8 请改用普通类、普通字符串或 Collectors.toList()。
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

## 后续复习问题：事务边界

要让扣库存与写库存记录一起成功或一起回滚，事务应覆盖 Service 中的整个购买流程。只覆盖扣库存的 Mapper 调用、并在该调用结束时提交，无法撤销随后写记录失败前已提交的扣减。事务边界判断题已回答正确：扣库存事务已经提交后，不能再通过该事务恢复库存。概念学习有，已讲解并答题；编译运行没有，项目实践没有。


## 复习：业务异常转成 HTTP 响应（答题通过）

适用 Java 8、Java 17；兼容 Spring Boot 2.7.18。下面使用 Spring 自己的注解，不涉及 javax/jakarta 校验包。假设该类在组件扫描范围内，异常来自 Controller 调用 Service 的请求流程，且没有其他处理器优先处理。

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }
}
```

此教学例子把 IllegalStateException 映射为 409，响应体是文本。409 是处理器明确设置的，不是该 Java 异常天然对应的 HTTP 状态。实际项目宜使用专门的业务异常类，避免把其他程序错误也映射为业务冲突。本例只用于已经确定可公开的业务提示。

答题通过：Service 抛出 IllegalStateException("商品不存在或库存不足") 时，正确回答状态码 409、响应体为异常消息。本轮复习讲解有、答题有；编译运行没有，项目实践没有。


## 自定义业务异常（判断题通过）

使用专门的异常类型表达预期的业务失败，避免把其他程序错误也当成业务冲突。以下两段适用 Java 8、Java 17，兼容 Spring Boot 2.7.18。

保存为 BusinessException.java：

```java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

extends 表示继承，super(message) 调用父类构造器保存异常消息，之后可用 getMessage() 读取。它仍是运行时异常；本例不涉及 Java 17/21 新语法。

在已被扫描的 GlobalExceptionHandler 中，用以下方法替换旧的 IllegalStateException 处理方法：

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<String> handleBusiness(BusinessException e) {
    return ResponseEntity.status(409).body(e.getMessage());
}
```

Service 的预期业务失败改为抛出 BusinessException。此方法按异常类型匹配，不按中文消息内容匹配。只放入可公开的业务提示。

两题回答正确：抛出 BusinessException("库存不足") 时，由上述处理器返回 409 和异常消息“库存不足”；普通 IllegalStateException 不属于 BusinessException，不能由此处理方法直接匹配处理（假设没有包装其他异常）。消息由开发者抛出异常时传入，不是指客户端用户输入。

文档有，概念学习与讲解有，判断题已完成且正确；编译运行没有，项目实践没有。

代码书写练习记录：最初不会写，讲解后曾在类名外加 Markdown 反引号并遗漏分号；纠正后正确写出 `throw new BusinessException("购买数量必须大于 0");`。此次单行语句练习通过，尚未验证独立编写完整 Service。提交时使用 cpp 代码块标签，仅影响 Markdown 高亮，不是 Java 源码错误，建议改用 java 标签。

数量校验条件填空回答正确：`quantity <= 0`，能拦截 0 和负数。文档与概念学习有，已讲解并答题；编译运行没有，项目实践没有。

综合阅读题两题结果均正确：quantity 为 0 时不会调用 Mapper；quantity 为 3 且 Mapper 返回 0 时抛出“商品不存在或库存不足”。第一题未独立说明原因，导师已补充：前面的 throw 中断了正常执行流程。此为已有知识的组合练习，不表示已完成独立 Service 编写。

本轮小结：文档有、概念学习有、已讲解并答题；编译运行没有、项目实践没有。下一步计划：学习 try-catch 捕获异常对事务回滚的影响；该小主题尚未开始讲解或答题。


## 删除商品：先写 Mapper（2026-11-05）

适用 Java 8、Java 17，Spring Boot 2.7.18 + MyBatis。由学习者在 ProductMapper 接口内手写，导师不代改业务代码。

```java
@Delete("DELETE FROM products WHERE id = #{id}")
int deleteById(@Param("id") long id);
```

返回值为删除的行数。id 是主键：存在且删除成功返回 1，不存在返回 0；不存在通常不是 SQL 异常。WHERE 限定目标商品，不能省略。当前仅添加方法，不执行删除。

练习：同一 id 原先存在，连续删除两次（期间没有重新插入），分别返回多少？

状态：文档有；概念学习有（本轮讲解）；本轮答题待反馈；删除代码编译运行没有；删除项目实践没有。下一步为 Service 和 Controller，尚未开始实现。


## 删除代码检查（2026-11-05）

删除行数题回答正确：同一商品连续删除，返回 1、0。学习者已自行编写 Mapper、Service、Controller，导师仅静态检查，未代改业务代码或执行删除。

Mapper.deleteProduct 的 DELETE SQL 与 id 参数绑定正确。发现两处需修正：Controller 用了 MyBatis @Delete，应改为 Spring @DeleteMapping，否则不是 HTTP 路由；Service.deleteProduct 声明了 id、request 两个参数，Controller 只传 id，属于编译错误。request 未使用，建议删除该参数，采用 deleteProduct(long id)。现有事务内锁定查询、Optional<Long> 返回结果及 Controller 成功返回 200/id、不存在返回 404 的设计可以保留，并非必须改为 boolean 或 204。resilt 仅是命名拼写，不是编译错误。

适用 Java 8、Java 17 + Spring Boot 2.7.18。概念学习有，删除行数答题有；代码已编写但待修正；本轮未运行编译，删除运行及项目实践验证没有。下一步由学习者修正两处，再检查编译与接口。


删除修正复核（2026-11-05）：Controller 已改为 @DeleteMapping，Service 已删除多余 request 参数，当前使用 Long id，可接收 Controller 的 long 自动装箱。导师执行 Maven 3.8.8 的 -DskipTests compile，退出码 0，编译通过；没有启动新服务、执行删除或运行测试。保留的 MyBatis Delete 导入与 resilt 拼写是清理建议，不是编译错误。概念学习有、答题有；编译有，删除运行没有；删除代码已编写，项目接口验证未完成。下一步学习者重启后删除练习商品并验证重复删除。


删除接口验证（2026-11-05）：用户先反馈“删除了”，随后提供再次 DELETE /products/2 的 HTTP/1.1 404、Content-Length: 0。首次删除成功按用户反馈记录，首次原始状态码未提供；重复删除不存在分支已由用户请求结果验证。概念学习有、删除行数答题有、编译有（此前 Maven compile 通过）、运行有、项目实践有（基本删除链路）。商品按 ID 查询、新增、更新、删除已有基本实践，不代表完整模块、自动化测试、分页或事务回滚/并发验证完成。下一步复习 404 的 Service → Controller 返回链路，再恢复真实数据库商品列表。


## 真实商品列表：Mapper（2026-11-05）

重复删除的执行链路题回答正确：查不到商品提前返回，不调用删除 Mapper。

下一步由学习者在 ProductMapper 添加列表查询。适用 Java 8、Java 17，当前 Spring Boot 2.7.18 + MyBatis：

```java
// 放在文件顶部的导入区
import java.util.List;
```

```java
// 放在 ProductMapper 接口内
@Select("SELECT id, name, price_in_cents AS priceInCents, stock "
        + "FROM products ORDER BY id ASC")
List<ProductEntity> findAll();
```

MyBatis 将每行映射为一个 ProductEntity，装入 List。正常查询没有结果时返回空 List，不是 null。ORDER BY id ASC 明确按编号从小到大排序；本步骤读取全部商品，仅用于少量学习数据，后续再改分页。

练习：查到三条记录时列表 size 是多少？查不到任何记录时返回 null 还是空列表？

状态：文档有、概念讲解有；本轮答题待反馈；代码待学习者编写，编译运行没有、数据库列表项目实践没有。下一步接 Service 的 Entity 到 Response 转换。


列表 Mapper 复核（2026-11-06）：用户已添加 java.util.List 与 findAll()，并正确回答 3 条记录 size=3、无记录返回空列表。Mapper 代码尚未编译运行。下一步手写 Service 的 List<ProductResponse> 转换方法。


Service 列表转换复核（2026-11-06）：用户已完成 List<ProductEntity> 到 List<ProductResponse> 的 for 循环转换，方法命名为 findAll，逻辑正确。导师执行 Maven 3.8.8 -DskipTests compile，退出码 0，编译通过；尚未启动或请求列表接口。下一步在 Controller 添加 GET /products。


文件职责复习（2026-11-06）：已按当前 BizVision 源码说明 ProductController、ProductService、ProductMapper、ProductEntity、CreateProductRequest、ProductResponse、主启动类、application.properties、pom.xml 的职责和请求链路。当前列表 Controller 已存在，列表运行验证仍待完成；分页与库存扣减尚未开始。


## 分页与库存扣减路线（2026-11-06）

当前商品查、增、改、删已有基本实践。列表的 Mapper、Service、Controller 代码已写，列表 HTTP 运行仍待验证。

分页将按小步骤进行：先理解 `page`、`size` 和 `offset = (page - 1) * size`，再写 `LIMIT #{size} OFFSET #{offset}`；随后增加 `COUNT(*)` 和分页响应。库存扣减将先练习条件 UPDATE：`WHERE id = #{id} AND stock >= #{quantity}`，通过返回行数区分成功与商品不存在/库存不足，再进入 Service 事务和 HTTP 响应。

概念有部分示例；分页与库存扣减的 BizVision 代码、编译运行和项目实践均没有。


分页参数修正与验证（2026-11-06）：此前 [] 原因是 LIMIT/OFFSET 参数错位；用户已改为 Mapper 接收 size、offset，Service 计算 offset=(page-1)*size 并传入，Controller 默认 page=1、size=2。Maven 3.8.8 compile 通过；导师只读请求 GET /products?page=1&size=10 返回 3 条数据库商品。分页第一页已运行，第二页与参数校验待验证。


分页第二页验证（2026-11-06）：用户确认 GET /products?page=2&size=2 结果正确。分页第一页、第二页已运行；当前响应仍只有商品数组，没有 total/page/size 元数据。下一步添加 Mapper 的 COUNT(*) 查询。


分页响应 DTO 完成（2026-11-06）：用户已创建 ProductPageResponse，并修改 Service/Controller 返回分页对象；DTO 字段名为 list（含当前页商品），不是示例中的 items，但含义正确。Maven 3.8.8 compile 退出码 0。尚未重启并验证 JSON 响应。下一步请求 GET /products?page=1&size=2，检查 list、total、page、size。


分页响应运行验证（2026-11-06）：用户访问 GET /products?page=1&size=2，返回 list 2 条、total=3、page=1、size=2，分页响应项目实践验证通过。用户当前响应还包含 id 和 price_in_cents，说明其 Response DTO 已扩展；本次不重新推断字段实现。下一步开始条件库存扣减 Mapper，库存扣减尚未接入项目。


库存扣减代码检查（2026-11-06）：发现三处需修正后再运行：Mapper 条件 SQL 误写 ADD，应为 AND（否则属于 SQL 运行时错误）；Controller 路径没有 `{id}` 却使用 @PathVariable，应改为包含 id 的路径；Service 忽略 decreaseStock 返回行数，库存不足时也会错误返回成功。当前 Maven 编译可通过，但库存接口尚未启动验证。


库存扣减修正复核（2026-11-06）：SQL 已改 AND，Controller 已改 POST /products/decrease-stock/{id} 并绑定 id，Service 已检查影响行数。Maven compile 已通过；未发送库存写请求，扣减运行、回滚和并发均未验证。仍有业务语义问题：库存不足返回 Optional.empty 会被映射为 404；这是上一轮导师简化建议造成，应后续区分商品不存在（404）与库存不足（409），普通存在性查询还需考虑并发删除。浏览器地址栏为 GET，不能验证 POST 扣库存。


库存扣减首次反馈（2026-11-06）：用户提供商品 1 查询 JSON，stock=97，与此前库存 100 扣减 3 的预期一致。扣减效果按用户反馈记录为已验证；POST 原始状态码未提供。概念学习有，编译有，运行有（用户反馈），项目实践有（成功扣减基本链路）；库存不足 409、非法数量、事务回滚及并发仍待验证。终端 JSON 后的 % 是 zsh 对无末尾换行输出的提示，不属于 JSON。


库存不足 409 教学（2026-11-06）：当前源码还没有自定义业务异常。指导学习者创建 InsufficientStockException extends RuntimeException；扣减 Service 保留 @Transactional，改用已有 findByIdForUpdate 锁定存在记录，缺失仍返回 Optional.empty，条件 UPDATE 返回 0 时抛库存不足异常；Controller 添加专用 @ExceptionHandler 返回 409/异常消息。锁定读取用于防止存在性检查后商品被并发删除，单条条件 UPDATE 本身负责库存不足时不扣减。仅提供指导，未代改 Java；讲解有，答题待反馈，新增异常及 409 编译运行没有，项目实践未完成。成功扣减 POST 原始状态码仍未提供。


库存异常修改复查（2026-11-06）：InsufficientStockException 类、Service rows==0 抛异常、Controller 返回 409 的处理器均已写入。扣减方法仍使用 findID，未改为 findByIdForUpdate；指导仅修改这一行，保留 @Transactional，避免存在性检查后并发删除导致错误报库存不足。此次仅静态核对，未编译或发送库存请求；409 运行验证仍待完成。


库存不足分支运行通过（2026-11-06）：用户提供 POST /products/decrease-stock/1 请求体 98，响应 HTTP 409 和库存不足提示；随后 GET 商品 1 的 stock 仍为 97。源码复查确认扣减方法已使用 findByIdForUpdate，并保留 @Transactional。概念学习有、代码有、运行有、项目实践有（成功扣减与库存不足两分支）；本轮未独立执行 Maven 编译或写请求。库存不变由条件 UPDATE 未匹配解释，不能据此证明已成功更新后回滚。非法数量、商品不存在、扣减后异常回滚及并发仍待验证。下一步阅读判断条件未匹配与事务回滚的区别。


事务回滚预测题（2026-11-06）：用户正确回答有效 @Transactional 事务内库存 97 扣减 3 后抛出未捕获 RuntimeException，事务结束后仍为 97。下一步由用户在 decreaseStock 的 rows==0 检查之后、成功 return 之前临时添加 quantity==3 时抛 RuntimeException 的实验分支；用练习商品先查库存、请求扣减 3、再查库存验证。默认预期 HTTP 500，数据库应保持请求前库存；必须移除临时分支并重启。仅提供教学步骤，未代改代码或执行写请求。回滚概念与答题有，实验编译运行、回滚项目验证尚无。


分页校验静态复核（2026-11-06）：学习者已添加 page<1、size<1 时抛 IllegalArgumentException，写法正确；size 上限和 offset 整数溢出尚未处理。指导学习上限设为100（项目约定），使用 long 中间计算并检查 Integer.MAX_VALUE 后转为 int，保持 Mapper 参数不变。本轮未编译或请求验证。用户明确跳过真实事务回滚实验，记为概念/答题有、回滚运行验证没有；源码当前未见临时抛异常实验分支。


分页校验完成复核（2026-11-06）：page 下限、size 1至100、long offsetValue 计算、Integer.MAX_VALUE 范围检查与安全转 int 均已由学习者补齐，静态检查正确。Maven 3.8.8 -DskipTests compile 退出码0，编译通过；新校验 HTTP 400 分支未验证，自动化测试未执行。先 int 乘法再赋 long 是否避免溢出的题尚未回答，不记为答题通过。下一步整理 Controller 局部异常处理为全局异常处理，尚未迁移。


全局异常迁移复核（2026-11-06）：用户已将400/409两个处理方法移至 exception.GlobalExceptionHandler，@RestControllerAdvice 与导入正确，Controller 已无局部 @ExceptionHandler。Maven 3.8.8 -DskipTests compile 退出码0，编译通过；未确认重启后的全局处理HTTP响应，不能沿用迁移前409结果作为迁移验证。概念学习有、自动注册判断题答对；迁移代码与编译有，迁移后运行待验证。事务回滚实验仍按用户要求跳过。


全局异常400分支验证（2026-11-06）：用户提供 GET /products?page=0&size=2 返回 HTTP400、text/plain;charset=UTF-8 与“页码必须大于等于1”。结合此前局部处理器已删除的源码检查，全局参数异常处理按用户反馈运行通过。概念学习有，编译有，400运行和项目实践有；迁移后409仍待验证，其他分页边界未全部验证。


全局异常409分支验证（2026-11-06）：用户提供 POST /products/decrease-stock/1 请求体98，返回 HTTP409、text/plain;charset=UTF-8 和“库存不足”。结合前次400结果，全局异常处理两个分支均按用户反馈运行通过。概念学习有、代码与编译有、运行有、项目实践有（全局400/409基本链路）。本次没有再次提供库存查询，不新增认定库存检查已完成。回滚实验按用户要求跳过；并发、自动化测试和其他边界仍未完成。下一步建议先学习自动化测试中 Arrange/Act/Assert，用纯单元测试验证非法分页参数不会调用Mapper，不依赖数据库。尚未开始编写或执行测试。


测试阶段完成记录（2026-11-06）：用户授权导师补测，Maven测试31项通过（含原有启动测试），当前MySQL HTTP验证29项断言通过；10并发争抢5件库存得到5次200、5次409和库存0，独立商品id=4已清理。H2真实SQL执行后抛异常的事务回滚验证通过，MySQL回滚未验证。未发现需修改业务代码的阻塞问题；仅新增测试文件及测试所需POM配置，src/main未代改。测试由导师实现，不代表学习者独立编写测试已掌握。完整证据见[商品模块测试记录](12-商品模块测试记录.md)。本段优先于下方历史待测记录。

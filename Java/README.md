# Java 学习笔记

更新日期：2026-11-04。

## 学习背景与目标

已有前端、数据库经验和少量 Java 基础。目标是工作中能够独立开发、维护 Java 后端模块，每周投入 10～15 小时。

## 文档导航

1. [12 周学习计划](01-学习计划.md)
2. [Java 基础与商品对象](02-Java基础与商品对象.md)
3. [List 集合与方法返回值](03-List集合与方法返回值.md)
4. [Map 查询与条件判断](04-Map查询与条件判断.md)
5. [Stream 筛选、统计与排序](05-Stream筛选统计与排序.md)
6. [异常处理与接口入门](06-异常处理与接口入门.md)
7. [易错点与复习清单](07-易错点与复习清单.md)
8. [接口进阶、泛型、枚举、record 与 Optional](09-接口进阶泛型枚举与Optional.md)
9. [Java 基础知识](08-Java基础知识.md)：环境与运行、类型、运算符、流程控制、数组、字符串、方法、面向对象、集合、异常、金额与日期。
10. [Spring Boot 启动类、Controller 与 GET 接口](10-Spring-Boot启动类Controller与GET接口.md)
11. [MyBatis：Mapper 与 SQL 的对应关系](11-MyBatis-Mapper与SQL对应关系.md)

基础语法不熟悉时，可先阅读第 8 篇的第 1～6 节，再回到商品案例。新增资料用于补充学习，不代表已完成实践。

## 当前进度

| 知识点 | 概念学习 | 讲解与答题 | 编译运行 | 项目实践 |
|---|---|---|---|---|
| Java 类型、类与对象、封装、引用、方法 | 有 | 有 | 没有 | 没有 |
| List、Map、空值判断、布尔值与短路求值 | 有 | 有 | 没有 | 没有 |
| Stream 的 `filter`、`map`、统计、排序 | 有 | 有 | 没有 | 没有 |
| 异常基础、`throw`、`try-catch` | 有 | 部分；练习曾跳过 | 没有 | 没有 |
| 接口、多态、构造器注入、多个实现、`default` | 有 | 有 | 没有 | 没有 |
| 泛型、泛型方法、`? extends` | 有 | 有 | 没有 | 没有 |
| enum、record、Optional | 有 | 有 | 没有 | 没有 |
| Maven | 有；本阶段不重复讲 | 有 | 导师 compile 成功；用户也已确认编译通过 | 没有完整后端项目实践 |
| Spring Boot 启动类、Controller、GET/POST、Service、构造器注入 | 有 | 有 | 部分：启动、GET、商品 Controller → Service 构造器注入链路已运行；POST 未验证 | 有：模拟商品查询分层；数据库商品业务未完成 |
| JSON、路径参数、查询参数、`@RequestBody`、HTTP 状态码 | 有 | 有 | 部分：查询参数、JSON、路径参数传递及转换失败返回 400 已验证；请求体等未验证 | 有：模拟商品查询和参数绑定；请求体等等待实践 |
| DTO：Request DTO、Response DTO、Entity 的区别 | 有；已通过示例学习 | 有 | 没有 | 没有独立 DTO 实践 |
| MyBatis Mapper、SQL 参数绑定、结果映射、CRUD | 有 | 有 | 没有真实数据库执行 | 没有 |
| MySQL 数据源、JDBC 驱动、表结构、分页 | 有部分概念 | 部分 | 没有连接 MySQL 或创建表 | 没有 |
| 参数校验、全局异常、事务、库存扣减 | 有部分概念 | 部分 | 没有真实验证 | 没有 |

结论：主要概念已有阅读与答题。2026-11-04 导师使用 JDK 8 和 Maven 3.8.8 成功编译 BizVision 的 4 个主源码文件（BUILD SUCCESS），用户随后确认编译通过，并反馈访问 /hello?name=Java 得到 Hello Java。已完成模板启动与首次 GET 访问；不表示所有笔记示例已运行或商品业务实践完成。MySQL 连接及测试尚未验证。

## 示例约定

### 转入 BizVision 实践：环境核对（2026-11-04）

- 本次重新检查确认 Java 工程已位于 `/Users/mac/disk2/www/keep-learning/BizVision`（单层 BizVision 目录），以此路径为准。此前检查的外部同名 Node.js + React 项目与本次 Java 实践无关。
- 本工程已具备 pom.xml、启动类、示例 Controller 和启动测试源码。POM 指定 Java 1.8、Spring Boot 2.7.18、MyBatis starter 2.3.0；IDE 项目 SDK 为 zulu-1.8。测试源码存在不代表已运行通过。
- 静态检查发现：已引入 MyBatis，但没有配置数据源 URL 和 JDBC 驱动，也未提供内存数据库依赖，默认数据源自动配置预计会导致启动失败；尚未实际启动验证。Spring Boot Maven 插件还设置了 skip=true，后续制作可执行 jar 时需处理，不影响当前先从 IDE 启动的教学目标。
- 当前终端 `java -version` 与 `javac -version` 均成功返回 1.8.0_442（Zulu 8）；此前“尚未安装 Java 环境”应作为历史状态。`/usr/libexec/java_home -V` 未识别 Java Runtime，环境发现配置仍待核对。
- 已通过完整路径验证 `/Users/mac/.sdkman/candidates/maven/3.8.8/bin/mvn`：Maven 3.8.8、Java 1.8.0_442。直接执行 mvn 仍提示找不到命令，属于当前终端 PATH 未加载；IDE 自带 Maven 版本另为 3.9.16，学习工程应选已安装的 3.8.8。
- 导师已执行 Maven compile，成功编译 4 个主源码文件；用户随后确认编译通过，并反馈 GET /hello?name=Java 返回 Hello Java。概念学习有；编译运行有（仅限当前模板）；项目实践有入门操作，商品业务实践未完成。测试源码仍未验证执行。
- 已检查到 application.properties 添加了临时排除 DataSourceAutoConfiguration 的配置；接 MySQL 时删除排除项并配置驱动和数据源。本次运行成功是在临时排除数据源的前提下，不代表数据库可用。
- 用户已反馈 name=BizVision 与不传 name 两次访问结果，查询参数和默认值实践通过。第一条反馈写成小写 hello，现有代码实际应返回 Hello BizVision，已提醒大小写；第二条为 Hello unknown user。
- 学习者已创建 ProductController 并运行：反馈 /products/1 返回“保温杯，库存 20”，/products/2 报错。导师检查源码并通过 HTTP 请求验证 /products/1 为 200 和固定文本，/products/2 为 404 Not Found。本接口的代码创建、编译运行和最小接口实践已有证据；不是数据库查询或完整商品模块实践。
- 本次 404 含义题初答错误，已解释路径未匹配不等于数据库商品不存在。随后正确回答“返回库存 20 不能说明数据库库存为 20，因为当前没有数据库”，本次固定响应与数据库数据的区别复答通过。
- 商品 JSON 实践完成：学习者已创建普通 ProductResponse 类并修改 Controller，反馈响应为 {"name":"保温杯","stock":20}，正确回答修改第二个构造参数会改变 stock。导师核对源码并请求验证 HTTP 200、Content-Type 为 application/json、响应内容一致；尚未实际改为 8 验证。
- Service 分层实践通过：学习者已创建 ProductService 并完成 Controller 构造器注入，反馈 JSON 不变且由 Spring 注入；导师核对源码并实际请求得到 HTTP 200 与一致 JSON。Controller 方法取名 findProduct 不影响路径映射。概念、答题、该功能编译运行和最小分层实践有；数据仍为模拟值。
- 路径参数实践通过：用户反馈 /products/7 返回 {"name":"模拟商品7","stock":20}，正确判断 abc 无法转换为 long，不进入 Service。导师核对源码和请求：/products/7 为 200，/products/abc 为 400。实际名称无空格，符合用户代码。
- Optional 与 ResponseEntity 实践已运行：学习者正确修改代码，但怀疑服务未启动。导师验证 Maven compile 为 BUILD SUCCESS（7 个主源码文件）、Java 进程监听 8080，/products/7 为 200 和商品 JSON，/products/8 为 404 且 Content-Length 为 0。代码无需修复；本次是预期的业务响应，不是编译或启动失败。该功能编译运行和最小实践有，404 空响应与连接失败的区别已解释、待确认理解。数据库、CRUD、校验、分页、事务及测试仍待实践。

默认示例优先使用 Java 8/17 都能理解的写法。商品价格统一以整数“分”保存，字段名为 `priceInCents`，读取方法为 `getPriceInCents()`。

最新答题反馈：用户已报告 /products/7 的商品 JSON 与 /products/8 的 404，导师再次请求验证前者 200、后者 404 且空响应体。执行链路题初答错误，纠正后复答 /products/9 会进入 Service，并正确指出 ResponseEntity.notFound().build() 是构造 404 的语句。尚未直接回答所在类名，已补充：由 ProductController 调用 ResponseEntity 构造响应。该功能已有编译运行与最小实践，数据库实践没有。用户明确要求跳过调试，标记为未完成、后补，不再要求本阶段反馈断点结果。下一步实践商品列表 GET /products（模拟数据），代码与运行待完成。

## JDK 版本与实际项目

2026-11-04 用户确认 Java 8 工作项目版本：JDK 8、Spring Boot 2.7.18、Maven 3.8.8。后续 Java 8 教学以此为基线；参数校验使用 `javax.validation.*`，Servlet 使用 `javax.servlet.*`。Spring/MyBatis 自己的注解仍使用各自的包名，不是所有导入都改为 `javax.*`。此信息仅确认项目技术栈，不代表本机已安装环境或运行项目。Java 17 项目的具体框架版本尚未确认。

本次复习：多个 Mapper 参数的 `@Param` 绑定已有文档与讲解，已答题；参数找不到属于 MyBatis 运行时错误的判断正确。查询结果题曾漏看 `AND stock >= 1`，解释后正确回答 `search("杯", 0)` 返回保温杯和咖啡杯，复答通过。参数命名题回答正确：`@Param("stock")` 对应 `#{stock}`。条件库存扣减题两题正确：初始库存各为 5，扣减 3 返回 1、剩余 2；扣减 8 返回 0、库存仍为 5。返回 0 的含义判断正确：商品不存在也会返回 0，不能仅据此认定库存不足。Service 异常流程题两题正确：rows 为 0 抛异常、不打印成功；rows 为 1 不抛该异常、打印成功。事务回滚对照题结果均正确：事务回滚后库存为 5；无事务且扣减已自动提交时，后续异常不会撤销扣减，库存为 3。已讲解 throw 中断执行与事务回滚的区别；尚未独立口述原因。事务边界题回答正确：扣库存事务已提交后，不能再通过该事务回滚。全局异常响应题回答正确：状态码 409、响应体为异常消息。自定义业务异常两道判断题回答正确，详见第 11 篇末尾；抛出业务异常的单行语句经讲解与纠正后已写对；数量校验条件 quantity <= 0 填写正确；数量校验与库存扣减综合阅读题两题结果正确；下一步：try-catch 对事务回滚的影响（尚未讲解或答题）。概念学习：有；编译运行：没有；项目实践：没有。

学习示例原先按 JDK 21 编写，但实际项目使用 Java 8 和 Java 17，后续按兼容性分层：

| 版本 | 教学策略 |
|---|---|
| Java 8 | 作为业务代码的兼容基线；使用普通类代替 `record`，使用 `Collectors.toList()` 代替 `Stream.toList()`，不使用文本块、`var` 和新版本 `switch` 语法 |
| Java 17 | 可以使用 `record`、文本块、`Stream.toList()` 等当前示例中的语法；Spring Boot 3 通常要求至少 Java 17 |
| Java 21 | 用于学习和后续实践环境；额外的新特性单独标注，不默认带入 Java 8 项目 |

之后每段代码会标注适用版本。涉及商品业务、Spring Boot、MyBatis 的核心思想会优先用 Java 8 也能理解的写法解释，再补充 Java 17/21 的简写。实际项目应以项目的 Maven 编译版本和 Spring Boot 版本为准：Java 8 项目通常使用支持 Java 8 的 Spring Boot 版本，Java 17 项目可以使用 Spring Boot 3。

示例是教学代码，默认集合参数不为 null、商品名称不为 null，且不考虑整数溢出和多线程竞争；不是生产级库存实现。

除特别说明外，各段练习独立执行。商品库存会随着操作变化，不要把不同练习的中间状态混用。现有 `demo.java` 未修改。

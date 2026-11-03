# Java 学习笔记

更新日期：2026-11-03。

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
10. [Spring Boot 启动类、Controller 与 GET 接口](10-Spring Boot启动类Controller与GET接口.md)
11. [MyBatis：Mapper 与 SQL 的对应关系](11-MyBatis-Mapper与SQL对应关系.md)

基础语法不熟悉时，可先阅读第 8 篇的第 1～6 节，再回到商品案例。新增资料用于补充学习，不代表已完成实践。

## 当前进度

| 知识点 | 文档/概念学习 | 编译、运行或项目实践 |
|---|---|---|
| Java 类型、类与对象、封装、引用、方法 | 有 | 没有 |
| List、Map、空值判断、布尔值与短路求值 | 有 | 没有 |
| Stream 的 `filter`、`map`、统计、排序 | 有 | 没有 |
| 异常基础、`throw`、`try-catch` | 有 | 没有；练习曾跳过 |
| 接口、多态、构造器注入、多个实现、`default` | 有 | 没有 |
| 泛型、泛型方法、`? extends` | 有 | 没有 |
| enum、record、Optional | 有 | 没有 |
| Maven | 有；本阶段不重复讲 | 没有完整后端项目实践 |
| Spring Boot 启动类、Controller、GET/POST、Service、构造器注入 | 有 | 没有；尚未启动项目 |
| JSON、路径参数、查询参数、`@RequestBody`、HTTP 状态码 | 有 | 没有真实 HTTP 请求 |
| DTO：Request DTO、Response DTO、Entity 的区别 | 有；已通过示例学习 | 没有独立 DTO 实践 |
| MyBatis Mapper、SQL 参数绑定、结果映射、CRUD | 有 | 没有真实数据库执行 |
| MySQL 数据源、JDBC 驱动、表结构、分页 | 有部分概念 | 没有连接 MySQL 或创建表 |
| 参数校验、全局异常、事务、库存扣减 | 有部分概念 | 没有真实验证 |

结论：目前已经完成 Spring Boot 和 MyBatis 的主要概念阅读与答题，尚未完成任何 Java 编译、Spring Boot 启动、MySQL 连接和接口运行。后续状态只有在实际验证后，才把“编译、运行或项目实践”改为“有”。

## 示例约定

使用 JDK 21。商品价格统一以整数“分”保存，字段名为 `priceInCents`，读取方法为 `getPriceInCents()`。

## JDK 版本与实际项目

学习示例原先按 JDK 21 编写，但实际项目使用 Java 8 和 Java 17，后续按兼容性分层：

| 版本 | 教学策略 |
|---|---|
| Java 8 | 作为业务代码的兼容基线；使用普通类代替 `record`，使用 `Collectors.toList()` 代替 `Stream.toList()`，不使用文本块、`var` 和新版本 `switch` 语法 |
| Java 17 | 可以使用 `record`、文本块、`Stream.toList()` 等当前示例中的语法；Spring Boot 3 通常要求至少 Java 17 |
| Java 21 | 用于学习和后续实践环境；额外的新特性单独标注，不默认带入 Java 8 项目 |

之后每段代码会标注适用版本。涉及商品业务、Spring Boot、MyBatis 的核心思想会优先用 Java 8 也能理解的写法解释，再补充 Java 17/21 的简写。实际项目应以项目的 Maven 编译版本和 Spring Boot 版本为准：Java 8 项目通常使用支持 Java 8 的 Spring Boot 版本，Java 17 项目可以使用 Spring Boot 3。

示例是教学代码，默认集合参数不为 null、商品名称不为 null，且不考虑整数溢出和多线程竞争；不是生产级库存实现。

除特别说明外，各段练习独立执行。商品库存会随着操作变化，不要把不同练习的中间状态混用。现有 `demo.java` 未修改。

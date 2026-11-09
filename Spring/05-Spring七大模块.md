# 05 · Spring 七大模块

> 上一篇：[04 SOLID 五大设计原则](04-SOLID五大设计原则.md) · 下一篇：[06 响应式流与背压](06-响应式流与背压.md)

---

## 目录

1. [模块全景图](#1-模块全景图)
2. [七大模块逐个说明](#2-七大模块逐个说明)
3. [模块与 JAR 包的对应关系](#3-模块与-jar-包的对应关系)
4. [为什么说 Spring 是"分层架构"](#4-为什么说-spring-是分层架构)

---

## 1. 模块全景图

```
                    ┌──────────────────────────────────────────────┐
                    │              Spring Framework                 │
                    ├──────────────────────────────────────────────┤
                    │                                              │
                    │   ┌──────────────┐    ┌──────────────┐       │
                    │   │ Core         │    │ AOP          │       │
                    │   │ Container    │    │ (Aspects)    │       │
                    │   └──────┬───────┘    └──────────────┘       │
                    │          │                                   │
                    │   ┌──────┴───────┐    ┌──────────────┐       │
                    │   │ Context      │    │ Instrument-  │       │
                    │   │ (Bean管理)    │    │ ation        │       │
                    │   └──────────────┘    └──────────────┘       │
                    │                                              │
                    │   ┌──────────────┐    ┌──────────────┐       │
                    │   │ DAO/JDBC     │    │ ORM          │       │
                    │   │ (数据访问)     │    │ (JPA/Hibernate)│    │
                    │   └──────────────┘    └──────────────┘       │
                    │                                              │
                    │   ┌──────────────┐    ┌──────────────┐       │
                    │   │ Web (MVC)    │    │ WebFlux      │       │
                    │   │ (Web层)       │    │ (响应式Web)   │       │
                    │   └──────────────┘    └──────────────┘       │
                    │                                              │
                    └──────────────────────────────────────────────┘
```

---

## 2. 七大模块逐个说明

| 模块 | 说明 |
|------|------|
| **Core Container**（核心容器） | Spring 的核心，提供 IoC/DI 功能。包含 `spring-core`、`spring-beans`、`spring-context`、`spring-expression`（SpEL）。是其他所有模块的基础。 |
| **AOP & Aspects**（面向切面编程） | 提供面向切面编程能力，支持横切关注点（日志、事务、安全等）的模块化。`spring-aop` 提供基于代理的 AOP，`spring-aspects` 集成 AspectJ。 |
| **Data Access / Integration**（数据访问/集成） | 简化与数据库的交互，包含 JDBC、ORM（JPA/Hibernate/MyBatis 集成）、OXM、JMS、事务管理。对应 `spring-jdbc`、`spring-tx`、`spring-orm`、`spring-oxm`、`spring-jms`。 |
| **Web**（Web 层） | 提供 Web 应用支持。`spring-web` 提供基础 Web 集成功能，`spring-webmvc`（Spring MVC）提供基于 Servlet 的传统同步 Web 框架，`spring-webflux` 提供响应式 Web 框架。 |
| **Messaging**（消息） | Spring 4 引入，提供基于消息的编程模型，支持 STOMP 协议等 WebSocket 通信。 |
| **Instrumentation**（监测） | 提供类检测和类加载器实现，用于在应用服务器中使用，支持 JPA 等场景下的类织入。 |
| **Test**（测试） | 提供对 JUnit / TestNG 等测试框架的集成支持，支持通过 Spring 容器进行集成测试（`spring-test`）。 |

> **说明：** 部分资料将 Spring 模块归为七大核心模块时，可能会将 DAO/JDBC 与 ORM 合并为"数据访问层"，将 Web MVC 与 WebFlux 合并为"Web 层"。但总体上 Spring 官方文档将其划分为以上功能模块。

---

## 3. 模块与 JAR 包的对应关系

```
spring-core ──────────┐
spring-beans ─────────┤ Core Container
spring-context ───────┤   （IoC 就在这里）
spring-expression ────┘

spring-aop ───────────┐ AOP
spring-aspects ───────┘

spring-jdbc ──────────┐
spring-tx ────────────┤ Data Access
spring-orm ───────────┤
spring-oxm ───────────┤
spring-jms ───────────┘

spring-web ───────────┐
spring-webmvc ────────┤ Web
spring-webflux ───────┤
spring-websocket ─────┘

spring-messaging ─────  Messaging
spring-instrument ────  Instrumentation
spring-test ──────────  Test
```

各 JAR 的详细职责见 [09 篇](09-Spring常用JAR包.md)。

---

## 4. 为什么说 Spring 是"分层架构"

三个角度：

**① 依赖方向是单向的**

```
Web / Data Access / Messaging  ──依赖──▶  AOP / Instrumentation  ──依赖──▶  Core Container
```

上层可以依赖下层，下层绝不依赖上层。所以只引 `spring-context` 就能用 IoC，不需要任何 Web 相关的东西。

**② 可以只引需要的部分**

```xml
<!-- 只要 IoC：一个依赖搞定，不到 2MB -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
</dependency>

<!-- 要 Web 再加 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
</dependency>
```

**③ 每层都能单独替换实现**

- Core Container 里：`BeanFactory` → `ApplicationContext` 是增强，不是替换
- Data Access 里：JDBC / JPA / MyBatis 三种实现可以换
- Web 里：`spring-webmvc`（Servlet 阻塞）和 `spring-webflux`（Reactive 非阻塞）可以选

---

> 下一篇：[06 响应式流与背压](06-响应式流与背压.md)

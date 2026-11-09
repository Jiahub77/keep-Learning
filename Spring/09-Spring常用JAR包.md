# 09 · Spring 常用 JAR 包

> 上一篇：[08 Spring 框架特点](08-Spring框架特点.md) · 下一篇：[10 实战：手写迷你 IoC 容器](10-实战-手写迷你IoC容器.md)

---

## 目录

1. [核心模块 JAR 包](#1-核心模块-jar-包)
2. [Maven 依赖示例](#2-maven-依赖示例)
3. [Spring Boot 常用 Starter 对照](#3-spring-boot-常用-starter-对照)
4. [怎么判断该引哪个包](#4-怎么判断该引哪个包)

---

## 1. 核心模块 JAR 包

| JAR 包 | 所属模块 | 说明 |
|--------|---------|------|
| `spring-core` | Core Container | Spring 核心工具类，IoC 的基础依赖。包含核心工具类、反射、资源加载等。**必须引入**。 |
| `spring-beans` | Core Container | Bean 定义、创建、管理。IoC 容器核心。**必须引入**。 |
| `spring-context` | Core Container | Spring 上下文，扩展了 BeanFactory，提供 `ApplicationContext`、事件传播、国际化等。**必须引入**。 |
| `spring-expression` | Core Container | Spring 表达式语言（SpEL），支持运行时动态表达式求值。 |
| `spring-aop` | AOP | AOP 功能支持，基于 JDK 动态代理和 CGLIB。使用 Spring AOP 时引入。 |
| `spring-aspects` | AOP | 与 AspectJ 的集成，提供更强大的切面功能。 |
| `spring-jdbc` | Data Access | Spring 对 JDBC 的封装，提供 `JdbcTemplate` 等简化数据库操作的工具。 |
| `spring-tx` | Data Access | 事务管理，提供声明式和编程式事务支持。使用事务时必须引入。 |
| `spring-orm` | Data Access | 与 ORM 框架（Hibernate / JPA / MyBatis 等）的集成支持。 |
| `spring-oxm` | Data Access | Object/XML 映射支持（JAXB、XStream 等）。 |
| `spring-jms` | Data Access / Messaging | 对 JMS（Java 消息服务）的封装和简化。 |
| `spring-web` | Web | Web 基础支持，提供文件上传、WebSocket、REST 客户端等基础 Web 功能。 |
| `spring-webmvc` | Web | Spring MVC，基于 Servlet 的传统 Web 框架（同步阻塞式）。 |
| `spring-webflux` | Web | Spring WebFlux，基于 Reactive Streams 的响应式 Web 框架（异步非阻塞式）。 |
| `spring-websocket` | Web | 对 WebSocket 协议的支持，实现全双工通信。 |
| `spring-messaging` | Messaging | 消息传递基础支持，STOMP 协议支持。 |
| `spring-test` | Test | 测试支持，提供 `MockMvc`、TestContext 框架等。 |
| `spring-context-support` | Core Container | 对第三方库的集成支持（如 EhCache、Quartz 调度、邮件等）。 |
| `spring-instrument` | Instrumentation | 类检测和类加载器实现，用于应用服务器中的类织入。 |

### 1.1 依赖传递关系

```
spring-context
   ├── spring-aop
   │     └── spring-beans
   │           └── spring-core
   └── spring-expression
         └── spring-core
```

> 所以写 IoC 只需要引 `spring-context`，另外四个会自动带进来。这也是为什么很多老项目 pom 里只有一个 `spring-context` 却能跑。

---

## 2. Maven 依赖示例

### 2.1 Spring 6.x（JDK 17+）

```xml
<!-- Spring Core（必须） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>6.1.14</version>
</dependency>

<!-- Spring AOP -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
    <version>6.1.14</version>
</dependency>

<!-- Spring JDBC + 事务 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>6.1.14</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
    <version>6.1.14</version>
</dependency>

<!-- Spring MVC -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>6.1.14</version>
</dependency>

<!-- Spring Test -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>6.1.14</version>
    <scope>test</scope>
</dependency>
```

### 2.2 版本差异提醒

| | Spring 5.x | Spring 6.x |
|---|---|---|
| 最低 JDK | JDK 8 | **JDK 17** |
| Servlet API | `javax.servlet.*` | **`jakarta.servlet.*`** |
| 参数名发现 | 可用 LocalVariableTable | **必须 `-parameters`** |
| `web.xml` 命名空间 | `http://xmlns.jcp.org/xml/ns/javaee` | `https://jakarta.ee/xml/ns/jakartaee` |

> 抄老教程的 XML 时，最容易踩的就是 `javax` → `jakarta` 这一刀。

### 2.3 编译插件别忘了加参数名

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>17</source>
        <target>17</target>
        <parameters>true</parameters>   <!-- 让 <constructor-arg name=".."> 能用 -->
    </configuration>
</plugin>
```

---

## 3. Spring Boot 常用 Starter 对照

| Starter | 包含的核心依赖 | 用途 |
|---------|--------------|------|
| `spring-boot-starter` | spring-core, spring-context | 基础启动器 |
| `spring-boot-starter-web` | spring-web, spring-webmvc, Tomcat | 构建 Web 应用（同步） |
| `spring-boot-starter-webflux` | spring-web, spring-webflux, Netty | 构建响应式 Web 应用（异步） |
| `spring-boot-starter-aop` | spring-aop, spring-aspects, AspectJ | AOP 支持 |
| `spring-boot-starter-data-jpa` | spring-jdbc, spring-orm, spring-tx, Hibernate | JPA 数据访问 |
| `spring-boot-starter-jdbc` | spring-jdbc, spring-tx, HikariCP | JDBC 数据访问 |
| `spring-boot-starter-test` | spring-test, JUnit, Mockito, AssertJ | 单元测试与集成测试 |
| `spring-boot-starter-data-redis` | spring-data-redis, Lettuce | Redis 数据访问 |
| `spring-boot-starter-data-r2dbc` | spring-data-r2dbc, R2DBC 驱动 | 响应式数据库访问（配 WebFlux） |

> **提示：** 实际开发中通常用 Spring Boot 简化依赖管理。但**学习阶段建议先手写 XML**——Spring Boot 的自动配置把 `<bean>` 藏起来了，反而看不清 IoC 到底做了什么。

---

## 4. 怎么判断该引哪个包

```
我要做什么？
│
├─ 只想用 IoC / DI ────────────────────▶ spring-context（自动带 core/beans/aop/expression）
│
├─ 要用 AOP 切面 ──────────────────────▶ spring-aop（+ spring-aspects 若要 AspectJ 语法）
│
├─ 要操作数据库
│    ├─ 手写 SQL ──────────────────────▶ spring-jdbc
│    ├─ 用 MyBatis ────────────────────▶ mybatis-spring（内部依赖 spring-jdbc）
│    └─ 用 JPA / Hibernate ────────────▶ spring-orm + hibernate-core
│    凡是写库都要事务 ──────────────────▶ spring-tx
│
├─ 要做 Web（同步）────────────────────▶ spring-webmvc（自动带 spring-web）
├─ 要做 Web（响应式）──────────────────▶ spring-webflux + reactor-core
├─ 要用 WebSocket ─────────────────────▶ spring-websocket
│
└─ 要写测试 ───────────────────────────▶ spring-test（test scope）
```

---

> 下一篇：[10 实战：手写迷你 IoC 容器](10-实战-手写迷你IoC容器.md) —— 把 Set 注入和构造注入的原理亲手跑一遍。

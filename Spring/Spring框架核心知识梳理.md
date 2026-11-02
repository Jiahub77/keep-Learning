# Spring 框架核心知识梳理

> Spring：轻量级的控制反转（IoC）和面向切面（AOP）的容器框架。

---

## 目录

1. [OCP 开闭原则](#1-ocp-开闭原则)
2. [依赖倒置原则 DIP](#2-依赖倒置原则-dip)
3. [控制反转 IoC](#3-控制反转-ioc)
4. [SOLID 原则与 OOD 五大原则](#4-solid-原则与-ood-五大原则)
5. [Spring 七大模块](#5-spring-七大模块)
6. [响应式流与背压](#6-响应式流与背压)
7. [Web 同步与 Web 异步的区别（Spring Web MVC vs Spring WebFlux）](#7-web-同步与-web-异步的区别spring-web-mvc-vs-spring-webflux)
8. [Spring 框架特点](#8-spring-框架特点)
9. [Spring 常用的 JAR 包](#9-spring-常用的-jar-包)

---

## 1. OCP 开闭原则

**定义：** 一个软件实体（类、模块、函数等）应该**对扩展开放，对修改关闭**。

在程序开发中，代码通常呈上下依赖的流程，例如：

```
Controller → Service → Dao
```

- **对扩展开放：** 当需要增加新功能时，应该通过新增代码来实现（如新增类、新增方法），而不是修改已有代码。
- **对修改关闭：** 避免修改已经测试通过、稳定运行的老代码，因为修改可能引入 bug，并且导致模块之间的耦合度升高。

**核心价值：** 降低因需求变更带来的风险，使系统更稳定、更易维护。

**实现方式：** 通常通过抽象（抽象类 / 接口）和多态来实现。上层模块依赖抽象接口，当需要扩展时，只需新增接口的实现类即可，无需修改上层代码。

---

## 2. 依赖倒置原则 DIP

**定义：**

- 高层模块不应该依赖低层模块，二者都应该依赖其**抽象**。
- 抽象不应该依赖细节，细节应该依赖抽象。

**核心思想：** 提倡**面向抽象编程、面向接口编程**，不要面向具体编程，让上层不再直接依赖下层，从而降低耦合度。

**对比示例：**

```java
// ❌ 反面：面向具体编程，高层直接依赖低层
public class UserController {
    private UserDao userDao = new UserDao(); // 直接依赖具体类，耦合度高
}

// ✅ 正面：面向接口编程，高层依赖抽象
public class UserController {
    private UserDao userDao; // 依赖接口
    public void setUserDao(UserDao userDao) { // 通过注入获得具体实现
        this.userDao = userDao;
    }
}
```

---

## 3. 控制反转 IoC

### 3.1 定义

将对象的**创建权**交出去，将对象与对象之间的**管理权（依赖关系）**交出去，由**第三方容器（Spring IoC 容器）**来负责创建和维护。

> "控制"指的是对象创建和依赖关系维护的权力；"反转"指这种权力从程序代码内部转移到了外部容器。

### 3.2 实现：依赖注入（DI）

依赖注入是 IoC 的具体实现方式，即由容器在运行时将依赖关系注入到对象中。常见的方式有三种：

| 注入方式 | 说明 | 特点 |
|---------|------|------|
| **Set 注入** | 通过 setter 方法注入依赖 | 灵活，可选依赖；Spring 推荐 |
| **构造方法注入** | 通过构造方法注入依赖 | 依赖不可变，强制依赖；保证对象创建时即完整 |
| **字段注入** | 直接给 Field 赋值（通过反射机制，如 `@Autowired` 标注在字段上） | 简洁但不推荐，难以单元测试 |

**代码示例：**

```java
// 1. Set 注入
public class UserService {
    private UserDao userDao;
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}

// 2. 构造方法注入
public class UserService {
    private final UserDao userDao;
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
}

// 3. 字段注入（不推荐）
public class UserService {
    @Autowired
    private UserDao userDao;
}
```

### 3.3 IoC 与 DI 的关系

- **IoC** 是一种**思想**（控制反转）。
- **DI** 是 IoC 的**具体实现方式**（依赖注入）。
- IoC 容器是承载这一思想的**基础设施**（如 Spring 的 `BeanFactory`、`ApplicationContext`）。

---

## 4. SOLID 原则与 OOD 五大原则

### SOLID 是面向对象设计（OOD）五大原则的首字母缩写：

| 原则 | 全称 | 核心思想 |
|------|------|---------|
| **SRP** | Single Responsibility Principle（单一职责原则） | 一个类只干一件事，应该只有一个引起它变化的原因 |
| **OCP** | Open-Closed Principle（开闭原则） | 加功能时尽量新增代码，而不是修改老代码——对扩展开放，对修改关闭 |
| **LSP** | Liskov Substitution Principle（里氏替换原则） | 子类要能当父类用——所有引用父类的地方必须能透明地使用子类对象 |
| **ISP** | Interface Segregation Principle（接口隔离原则） | 接口要专一——客户端不应该被迫依赖它不使用的方法，避免"胖接口" |
| **DIP** | Dependency Inversion Principle（依赖倒置原则） | 依赖抽象而不是具体实现——高层不依赖低层，二者都依赖抽象 |

### 各原则之间的关系

```
        ┌──────────────────────────────────┐
        │          SOLID 五大原则            │
        ├──────┬──────┬──────┬──────┬──────┤
        │ SRP  │ OCP  │ LSP  │ ISP  │ DIP  │
        └──────┴──────┴──────┴──────┴──────┘
         │      │      │      │      │
         │      │      │      │      └──→ IoC/DI 的理论基础
         │      │      │      └──→ 接口要小而专
         │      │      └──→ 多态安全替换
         │      └──→ 扩展优于修改
         └──→ 职责单一
```

> **注意：** OCP（开闭原则）是 SOLID 的**核心目标**，其余四个原则都是实现 OCP 的手段。DIP（依赖倒置原则）是 Spring IoC 的理论基础。

---

## 5. Spring 七大模块

Spring 框架采用分层架构，由七大核心模块组成：

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

## 6. 响应式流与背压

### 6.1 响应式流（Reactive Streams）

**定义：** 响应式流是一种**异步数据流处理**的规范（标准），旨在处理异步序列中的元素，同时提供**非阻塞**的背压机制。

**核心目标：** 解决异步编程中数据生产者和消费者之间速度不匹配的问题。

**四大接口（Java 9+ 已纳入 `java.util.concurrent.Flow`）：**

| 接口 | 作用 |
|------|------|
| `Publisher` | 数据发布者——生产数据并发送给订阅者 |
| `Subscriber` | 数据订阅者——接收并处理数据 |
| `Subscription` | 订阅关系——连接 Publisher 和 Subscriber，可控制数据请求量 |
| `Processor` | 既是 Publisher 又是 Subscriber——数据处理中间环节 |

**处理流程：**

```
Publisher ──→ Processor ──→ Processor ──→ Subscriber
 (发布)       (转换)          (过滤)        (消费)
     ↑                                          │
     └──────────── request(n) ──────────────────┘
              （Subscriber 主动拉取，实现背压）
```

### 6.2 背压（Backpressure）

**定义：** 当数据**生产者的速度 > 消费者的处理速度**时，消费者通过**反向通知**机制告诉生产者"慢一点"或"我只要 n 个数据"，防止消费者被压垮。

**本质：** 背压是一种**流控机制**，让消费者**主动拉取（Pull）**而非被**被动推送（Push）**。

**示例：**

```
Publisher（每秒产生 1000 条数据）
        │
        ▼
Subscriber（每秒只能处理 100 条数据）

// 没有背压 → Subscriber 被淹没，OOM 或 崩溃
// 有背压  → Subscriber 告诉 Publisher："我只要 100 条"，Publisher 按需发送
```

**主流响应式框架/库：**

- **Project Reactor**（Spring WebFlux 默认使用）：提供 `Flux`（0..N 个元素）和 `Mono`（0..1 个元素）
- **RxJava**：Netflix 出品的响应式扩展库
- **Akka Streams**：基于 Actor 模型的响应式流实现

---

## 7. Web 同步与 Web 异步的区别（Spring Web MVC vs Spring WebFlux）

### 7.1 对比总览

| 维度 | Spring Web MVC（同步） | Spring WebFlux（异步/响应式） |
|------|----------------------|---------------------------|
| **编程模型** | 基于 Servlet API，阻塞式（命令式编程） | 基于 Reactive Streams，非阻塞式（响应式编程） |
| **底层容器** | Servlet 容器（Tomcat / Jetty / Undertow） | 支持 Reactive 的容器（Netty / Undertow / Tomcat 等） |
| **线程模型** | 一个请求占用一个线程（线程被 I/O 阻塞） | 少量线程处理大量请求（线程不被 I/O 阻塞，事件循环） |
| **并发能力** | 受线程池大小限制 | 用少量线程支撑高并发（吞吐量更高） |
| **返回类型** | `String` / `ModelAndView` / `ResponseEntity<T>` 等 | `Mono<T>` / `Flux<T>` 等 Publisher 类型 |
| **数据流** | 普通 Java 对象 | 响应式流（Reactor `Flux` / `Mono`） |
| **背压支持** | ❌ 不支持 | ✅ 支持（消费者按需拉取） |
| **学习曲线** | 低，成熟稳定，生态完善 | 高，响应式编程思维方式转变大 |
| **适用场景** | 传统 CRUD 应用，I/O 操作较少或可控 | 高并发、I/O 密集型（微服务网关、流式数据处理等） |

### 7.2 线程模型对比

**Spring Web MVC（同步阻塞）：**
```
请求1 → [线程1] ──── DB查询(阻塞等待) ────→ 返回响应
请求2 → [线程2] ──── 远程调用(阻塞等待) ──→ 返回响应
请求3 → [线程3] ──── ...

// 线程在 I/O 时被阻塞，无法处理其他请求
// 并发量 = 线程池大小
```

**Spring WebFlux（异步非阻塞）：**
```
请求1 → [事件线程] ──→ 发起DB查询 ──→ 线程释放，处理请求2 ──→ 查询结果回来 → 回调处理请求1
请求2 → [事件线程] ──→ 发起远程调用 ──→ 线程释放，处理请求3 ──→ ...
请求3 → [事件线程] ──→ ...

// 线程不被 I/O 阻塞，少量线程即可处理大量请求
// 并发量 >> 线程数
```

### 7.3 选择建议

- **用 Web MVC**：如果团队熟悉命令式编程、应用以 CRUD 为主、并发量不是特别高。
- **用 WebFlux**：如果系统是 I/O 密集型、需要极高并发吞吐量、需要背压控制、或已经在使用响应式技术栈。
- 两者可以在同一个应用中共存（Spring Boot 支持）。

---

## 8. Spring 框架特点

| 特点 | 说明 |
|------|------|
| **轻量级** | Spring 核心模块体积小、依赖少。基本版本只需少量 JAR 包即可运行，非侵入式设计（不强制实现 Spring 接口）。 |
| **非侵入式** | 业务对象不需要实现 Spring 的特定接口，可以脱离 Spring 框架独立使用。Spring 通过配置和注解来管理 Bean，而不是通过继承或实现。 |
| **控制反转（IoC）** | 将对象创建和依赖管理交给容器，降低组件间耦合度，使代码更易测试和维护。 |
| **面向切面（AOP）** | 将横切关注点（日志、事务、安全等）从业务逻辑中分离出来，通过切面统一管理，减少重复代码。 |
| **容器** | Spring 是一个容器，管理对象的生命周期和依赖关系（从创建到销毁的全过程）。 |
| **一站式框架** | Spring 提供了从核心容器到 Web、数据访问、消息、测试等全方位的解决方案，可以整合各种第三方框架（ORM、MQ 等）。 |
| **声明式事务** | 通过 `@Transactional` 注解或 XML 配置即可管理事务，无需手写事务代码。 |
| **模块化设计** | 各模块松散耦合，可按需引入（只用 IoC 就只引 Core，不需要全部引入）。 |
| **良好的集成能力** | 与 MyBatis、Hibernate、JPA、Struts、Quartz、Redis、RabbitMQ 等主流框架无缝集成。 |
| **方便测试** | `spring-test` 模块提供完善的单元测试和集成测试支持，可轻松 mock 依赖。 |

---

## 9. Spring 常用的 JAR 包

### 9.1 核心模块 JAR 包

| JAR 包 | 所属模块 | 说明 |
|--------|---------|------|
| `spring-core` | Core Container | Spring 核心工具类，IoC 的基础依赖。包含核心工具类、反射、资源加载等。**必须引入**。 |
| `spring-beans` | Core Container | Bean 定义、创建、管理。IoC 容器核心。**必须引入**。 |
| `spring-context` | Core Container | Spring 上下文，扩展了 BeanFactory，提供 `ApplicationContext`、事件传播、国际化等。**必须引入**。 |
| `spring-expression` | Core Container | Spring 表达式语言（SpEL），支持运行时动态表达式求值。 |
| `spring-aop` | AOP | AOP 功能支持，基于 JDK 动态代理和 CGLIB。使用 Spring AOP 时引入。 |
| `spring-aspects` | AOP | 与 AspectJ 的集成，提供更强大的切面功能（如注解式切面）。 |
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
| `spring-test` | Test | 测试支持，提供 `SpringJUnit4ClassRunner`、MockMvc、TestContext 框架等。 |
| `spring-context-support` | Core Container | 对第三方库的集成支持（如 EhCache、Quartz 调度、邮件等）。 |
| `spring-instrument` | Instrumentation | 类检测和类加载器实现，用于应用服务器中的类织入。 |

### 9.2 Maven 依赖示例（Spring 6.x / Spring Boot 3.x）

```xml
<!-- Spring Core（必须） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>6.1.x</version>
</dependency>

<!-- Spring AOP -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
    <version>6.1.x</version>
</dependency>

<!-- Spring JDBC + 事务 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>6.1.x</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
    <version>6.1.x</version>
</dependency>

<!-- Spring MVC -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>6.1.x</version>
</dependency>

<!-- Spring Test -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>6.1.x</version>
    <scope>test</scope>
</dependency>
```

> **提示：** 实际开发中，通常使用 **Spring Boot** 来简化依赖管理。Spring Boot 通过 starter 自动引入所需 JAR 包，如 `spring-boot-starter-web`（包含 web + mvc）、`spring-boot-starter-data-jpa`（包含 jdbc + orm + tx）等，无需手动逐个引入。

### 9.3 Spring Boot 常用 Starter 对照

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

---

## 附录：Spring 知识体系全景图

```
Spring 生态全景
│
├── 核心理论
│   ├── SOLID 原则（OCP / DIP / SRP / LSP / ISP）
│   ├── IoC（控制反转）→ DI（依赖注入：Set / 构造 / 字段）
│   └── AOP（面向切面编程）
│
├── 核心模块
│   ├── Core Container（Core / Beans / Context / SpEL）
│   ├── AOP & Aspects
│   ├── Data Access（JDBC / ORM / TX / JMS）
│   ├── Web（MVC / WebFlux / WebSocket）
│   ├── Messaging
│   ├── Instrumentation
│   └── Test
│
├── 编程范式
│   ├── 命令式（同步阻塞）→ Spring Web MVC + Servlet
│   └── 响应式（异步非阻塞）→ Spring WebFlux + Reactor + 背压
│
└── 生态扩展
    ├── Spring Boot（自动配置、起步依赖、内嵌容器）
    ├── Spring Cloud（微服务治理）
    ├── Spring Security（安全认证）
    ├── Spring Batch（批处理）
    └── Spring Data（数据访问统一抽象）
```

---

*本文档基于 Spring Framework 6.x 版本整理，持续更新中。*

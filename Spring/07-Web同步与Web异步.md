# 07 · Web 同步与 Web 异步

> 上一篇：[06 响应式流与背压](06-响应式流与背压.md) · 下一篇：[08 Spring 框架特点](08-Spring框架特点.md)
>
> 本篇的 Spring MVC 示例**全部用 XML 配置**（`web.xml` + `spring-mvc.xml` + 实现 `Controller` 接口）。
> WebFlux 是个例外——它**不支持 XML 配置**，只能用 Java 配置或注解，文中会明确标注。

---

## 目录

1. [对比总览](#1-对比总览)
2. [线程模型对比](#2-线程模型对比)
3. [代码对比：MVC（XML）vs WebFlux](#3-代码对比mvcxmlvs-webflux)
4. [同步框架里的"异步"：Servlet 3.0 异步支持](#4-同步框架里的异步servlet-30-异步支持)
5. [底层原理：Servlet 线程模型 vs Netty 事件循环](#5-底层原理servlet-线程模型-vs-netty-事件循环)
6. [选型建议与常见误区](#6-选型建议与常见误区)

---

## 1. 对比总览

| 维度 | Spring Web MVC（同步） | Spring WebFlux（异步/响应式） |
|------|----------------------|---------------------------|
| **编程模型** | 基于 Servlet API，阻塞式（命令式编程） | 基于 Reactive Streams，非阻塞式（响应式编程） |
| **底层容器** | Servlet 容器（Tomcat / Jetty / Undertow） | 支持 Reactive 的容器（Netty / Undertow / Tomcat 等） |
| **线程模型** | 一个请求占用一个线程（线程被 I/O 阻塞） | 少量线程处理大量请求（线程不被 I/O 阻塞，事件循环） |
| **并发能力** | 受线程池大小限制 | 用少量线程支撑高并发（吞吐量更高） |
| **返回类型** | `ModelAndView` / `String` / `ResponseEntity<T>` 等 | `Mono<T>` / `Flux<T>` 等 Publisher 类型 |
| **配置方式** | **XML（`web.xml` + `spring-mvc.xml`）** 或注解 | **只能 Java 配置或注解**（无 XML 支持） |
| **数据流** | 普通 Java 对象 | 响应式流（Reactor `Flux` / `Mono`） |
| **背压支持** | ❌ 不支持 | ✅ 支持（消费者按需拉取） |
| **学习曲线** | 低，成熟稳定，生态完善 | 高，响应式编程思维方式转变大 |
| **调试难度** | 低（同步调用栈清晰） | 高（异步链路，堆栈不连续） |
| **生态兼容** | 完整（JPA/MyBatis、Spring Security 全支持） | 部分受限（阻塞式 JDBC 会毁掉事件循环，需 R2DBC） |
| **适用场景** | 传统 CRUD 应用，I/O 操作较少或可控 | 高并发、I/O 密集型（微服务网关、流式数据处理等） |

---

## 2. 线程模型对比

**Spring Web MVC（同步阻塞）：**

```
请求1 → [线程1] ──── DB查询(阻塞等待) ────→ 返回响应
请求2 → [线程2] ──── 远程调用(阻塞等待) ──→ 返回响应
请求3 → [线程3] ──── ...

// 线程在 I/O 时被阻塞，无法处理其他请求
// 并发量 = 线程池大小（Tomcat 默认 max-threads=200）
```

**Spring WebFlux（异步非阻塞）：**

```
请求1 → [事件线程] ──→ 发起DB查询 ──→ 线程释放，处理请求2 ──→ 查询结果回来 → 回调处理请求1
请求2 → [事件线程] ──→ 发起远程调用 ──→ 线程释放，处理请求3 ──→ ...
请求3 → [事件线程] ──→ ...

// 线程不被 I/O 阻塞，少量线程即可处理大量请求
// 并发量 >> 线程数（Netty 默认 worker 线程数 = CPU 核数）
```

**为什么"少线程"能扛高并发？** 因为瓶颈从来不是"线程不够快"，而是"线程在等 I/O 时被白白占着"。一个线程等 50ms 的 DB 响应，这 50ms 里它什么也干不了；WebFlux 把这 50ms 还给了事件循环，同一个线程可以继续处理别的请求。

---

## 3. 代码对比：MVC（XML）vs WebFlux

### 3.1 Spring MVC：完整 XML 配置

**① 业务层（和 IoC 那篇一样，纯 XML 注入）**

```java
public interface UserDao {
    List<User> findAll();
}

public class UserDaoMysqlImpl implements UserDao {
    @Override
    public List<User> findAll() {
        // JDBC 查询，阻塞
        return jdbcTemplate.query("select id, name from user", rowMapper);
    }
}

public class UserService {
    private UserDao userDao;
    public void setUserDao(UserDao userDao) { this.userDao = userDao; }

    public List<User> findAll() {
        return userDao.findAll();          // 阻塞调用
    }
}
```

**② 控制器：实现 `Controller` 接口（XML 时代的标配写法）**

```java
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserListController implements Controller {

    private UserService userService;

    public void setUserService(UserService userService) {   // Set 注入，和普通 Bean 一样
        this.userService = userService;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) {
        List<User> users = userService.findAll();      // ★ 阻塞：线程在此等待
        ModelAndView mv = new ModelAndView("userList");
        mv.addObject("users", users);
        return mv;
    }
}
```

**③ `web.xml`：注册 DispatcherServlet**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
         version="5.0">

    <!-- Spring 根容器：Service / Dao -->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:applicationContext.xml</param-value>
    </context-param>
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <!-- Spring MVC 子容器 -->
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:spring-mvc.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>
```

> **注意版本差异：** Spring 6 用 `jakarta.servlet.*`（Jakarta EE 9+），Spring 5 及以前用 `javax.servlet.*`。抄老代码时这里是第一个要改的地方。

**④ `spring-mvc.xml`：把 URL 映射到控制器**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           https://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- ① 处理器映射：把 Bean 的 name 当作 URL 路径 -->
    <bean class="org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping"/>

    <!-- ② 处理器适配器：认识实现了 Controller 接口的处理器 -->
    <bean class="org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter"/>

    <!-- ③ 业务 Bean -->
    <bean id="userDao" class="com.example.dao.UserDaoMysqlImpl"/>
    <bean id="userService" class="com.example.service.UserService">
        <property name="userDao" ref="userDao"/>
    </bean>

    <!-- ④ 控制器：name 就是访问路径 -->
    <bean name="/users" class="com.example.web.UserListController">
        <property name="userService" ref="userService"/>
    </bean>

    <!-- ⑤ 视图解析器：ModelAndView("userList") → /WEB-INF/views/userList.jsp -->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

</beans>
```

**⑤ 请求流程**

```
浏览器 GET /users
   │
   ▼
DispatcherServlet（web.xml 里映射了 /）
   │
   ├─ ① HandlerMapping：按 URL "/users" 找到 Bean "UserListController"
   ├─ ② HandlerAdapter：SimpleControllerHandlerAdapter 发现它实现了 Controller 接口
   ├─ ③ 调用 handleRequest(request, response)   ← 线程在这里阻塞等数据库
   ├─ ④ 拿到 ModelAndView("userList", {users: [...]})
   ├─ ⑤ ViewResolver：解析成 /WEB-INF/views/userList.jsp
   └─ ⑥ 渲染 JSP，写回响应
```

### 3.2 Spring WebFlux：只能 Java 配置

> **⚠️ 前置说明：** WebFlux 是 Spring 5 引入的，**没有 XML 配置方案**。它的两种写法是「注解式控制器」和「函数式端点（RouterFunction）」。下面用的是函数式端点，只需要一个 `@Configuration` 配置类——这是本篇唯一出现注解的地方，等学到注解再回看，现在重点看 `Mono` / `Flux` 和线程模型。

**① 响应式数据访问**

```java
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveUserRepository extends ReactiveCrudRepository<User, Long> {
    Flux<User> findAll();                 // 非阻塞，返回 Flux
    Mono<User> findById(Long id);         // 非阻塞，返回 Mono
}
```

**② 处理器：纯业务逻辑，不需要注解**

```java
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class UserHandler {

    private final ReactiveUserRepository repository;

    public UserHandler(ReactiveUserRepository repository) {   // 构造注入
        this.repository = repository;
    }

    // 0 或 1 个结果 → Mono
    public Mono<ServerResponse> getOne(ServerRequest request) {
        Long id = Long.valueOf(request.pathVariable("id"));
        return repository.findById(id)
                .flatMap(user -> ServerResponse.ok().bodyValue(user))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    // 0..N 个结果 → Flux
    public Mono<ServerResponse> list(ServerRequest request) {
        return ServerResponse.ok().body(repository.findAll(), User.class);
    }
}
```

**③ 路由配置：把 URL 映射到 handler（唯一的注解处）**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> routes(UserHandler handler) {
        return RouterFunctions.route()
                .GET("/users/{id}", handler::getOne)
                .GET("/users", handler::list)
                .build();
    }
}
```

### 3.3 关键差异：返回类型

| | 返回类型 | 含义 |
|---|---------|------|
| **MVC** | `ModelAndView` / `List<User>` | 方法**执行完**才返回结果，线程全程占用 |
| **WebFlux** | `Mono<User>` / `Flux<User>` | 方法**立即返回一个"未来才有数据"的容器**，线程立刻释放 |

```java
// MVC：这一行执行时，线程真的去查了数据库，等结果
List<User> users = userService.findAll();

// WebFlux：这一行只是组装了一条"流水线"，什么都没执行
Flux<User> users = repository.findAll();
// 真正的执行发生在 subscribe() 时（由框架调用）
```

> 这是响应式编程最容易懵的一点：**"组装"和"执行"是分离的**。不调 `subscribe()`，什么都不会发生。

---

## 4. 同步框架里的"异步"：Servlet 3.0 异步支持

Web MVC 虽然是同步模型，但 Spring 提供了三种"释放容器线程"的手段——**注意：它们只是把等待挪到了别的线程，不是非阻塞**。

> **⚠️ 前置说明：** 下面三种写法的返回值依赖注解式控制器（`@Controller` + `@GetMapping`），XML 时代的 `Controller` 接口只能同步返回 `ModelAndView`。这里先把结论记住，代码等学到注解再回看。

```java
// ① Callable：把耗时任务交给 Spring 的 TaskExecutor，容器线程立即释放
@GetMapping("/slow1")
public Callable<String> slow1() {
    return () -> {
        Thread.sleep(2000);          // 在 taskExecutor 线程里执行
        return "done";
    };
}

// ② DeferredResult：由业务线程在任意时刻调用 setResult()
@GetMapping("/slow2")
public DeferredResult<String> slow2() {
    DeferredResult<String> result = new DeferredResult<>(3000L, "timeout");
    asyncService.process(result::setResult);   // 回调式
    return result;
}

// ③ WebAsyncTask：带超时和回调的 Callable
@GetMapping("/slow3")
public WebAsyncTask<String> slow3() {
    return new WebAsyncTask<>(2000L, () -> asyncService.query());
}
```

| 方式 | 容器线程释放 | 真正非阻塞 | 说明 |
|------|------------|-----------|------|
| 同步返回（`Controller` 接口） | ❌ | ❌ | 线程全程占用 |
| `Callable` | ✅ | ❌ | 换了个线程池继续阻塞 |
| `DeferredResult` | ✅ | ❌ | 换了个线程继续等待 |
| WebFlux `Mono/Flux` | ✅ | ✅ | 无线程等待，事件驱动 |

> 这就是"**异步 ≠ 非阻塞**"的经典区别：异步是"不占着当前线程等结果"，非阻塞是"底层 I/O 根本不阻塞线程"。Servlet 异步解决了前者，WebFlux 才解决后者。

---

## 5. 底层原理：Servlet 线程模型 vs Netty 事件循环

```
【Spring Web MVC + Tomcat】
  连接 ──→ Acceptor ──→ Poller ──→ Worker 线程池（每请求一线程）
                                      │
                                      └─ 阻塞读 → 业务 → 阻塞写 → 线程归还

  并发上限 = maxThreads（默认 200）
  超过后请求排队，排队满了拒绝连接
```

```
【Spring WebFlux + Netty】
  连接 ──→ Boss EventLoop（accept）
              │
              ▼
          Worker EventLoop（每核 1 个，各管一批 Channel）
              │
              ├─ Channel 可读 → 读事件 → 触发 Reactor 链路
              ├─ 遇到 I/O → 注册回调，立即返回，处理下一个 Channel
              └─ I/O 完成 → 事件通知 → 继续执行后续操作符

  并发上限 ≈ 内存 + 连接数（与线程数无关）
```

**核心差异：** Tomcat 是"**一个请求一个线程**"，Netty 是"**一个线程管一批连接**"（Reactor 模式 / 事件循环）。前者靠加线程扩容（线程有栈内存开销，且上下文切换昂贵），后者靠事件驱动复用线程。

### 5.1 一个线程能扛多少请求

| 模型 | 单线程承载 | 原因 |
|------|-----------|------|
| 阻塞式（Servlet） | 1 个 | 线程卡在 I/O 上，干不了别的 |
| 非阻塞（Netty） | 数千个 | 线程只在"有事件"时被唤醒，其余时间在跑别的连接 |

---

## 6. 选型建议与常见误区

### 6.1 选型建议

| 场景 | 推荐 |
|------|------|
| 传统 CRUD、后台管理、团队不熟悉响应式 | **Web MVC** |
| 使用 JPA / MyBatis / 阻塞式 JDBC | **Web MVC**（阻塞驱动在 WebFlux 里是灾难） |
| API 网关、高并发 I/O 密集、大量下游调用 | **WebFlux** |
| 流式推送（SSE、大文件流、实时数据） | **WebFlux**（`Flux` 天然支持边算边发） |
| 需要端到端背压 | **WebFlux** |
| 已有 Web MVC 项目想渐进改造 | 保持 MVC，只在个别接口返回 `Mono` |

### 6.2 常见误区

| 误区 | 真相 |
|------|------|
| **"WebFlux 一定更快"** | 错。低并发下 WebFlux 甚至更慢（异步编排有开销）。它的优势在**高并发 + I/O 密集**下的资源效率 |
| **"WebFlux 里可以随便用阻塞代码"** | 致命错误。一个 `Thread.sleep()` 或阻塞 JDBC 调用会**卡死整个事件循环**，影响该 EventLoop 上的所有连接 |
| **"用了 WebFlux 就是非阻塞了"** | 错。只要链路里有一环是阻塞的（如 JPA），整条链路就是阻塞的，只是被包在 `Mono.fromCallable()` 里 |
| **"MVC 和 WebFlux 不能共存"** | 错。Spring Boot 支持共存，但要注意：一旦引入 `spring-boot-starter-webflux`，默认容器会变成 Netty |
| **"背压能防住慢客户端"** | 错。HTTP 层背压有限，防慢客户端要靠限流、超时、熔断（见 [06 篇](06-响应式流与背压.md#6-http-场景下背压的边界)） |
| **"Flux 不 subscribe 也会执行"** | 错。响应式是懒执行的，不 `subscribe()` 什么都不发生 |

### 6.3 如果非要在 WebFlux 里调用阻塞 API

```java
// 必须切到弹性线程池，绝不能留在事件循环线程上
public Mono<String> blocking() {
    return Mono.fromCallable(() -> legacyBlockingDao.query())     // 阻塞调用
               .subscribeOn(Schedulers.boundedElastic());          // 切到弹性线程池
}
```

---

## 附：本篇要点速查

```
MVC（同步）
  配置：web.xml 注册 DispatcherServlet + spring-mvc.xml 配 HandlerMapping/Controller/ViewResolver
  控制器：实现 org.springframework.web.servlet.mvc.Controller，返回 ModelAndView
  线程：一请求一线程，I/O 期间线程阻塞

WebFlux（异步）
  配置：无 XML 支持，只能 Java 配置或注解
  控制器：返回 Mono<T> / Flux<T>（组装与执行分离，subscribe 才执行）
  线程：EventLoop 复用，I/O 期间线程去处理别的连接

异步 ≠ 非阻塞：Callable/DeferredResult 只是换了线程等
```

---

> 下一篇：[08 Spring 框架特点](08-Spring框架特点.md)

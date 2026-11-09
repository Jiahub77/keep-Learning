# 08 · Spring 框架特点

> 上一篇：[07 Web 同步与 Web 异步](07-Web同步与Web异步.md) · 下一篇：[09 Spring 常用 JAR 包](09-Spring常用JAR包.md)

---

## 目录

1. [官方六大特点](#1-官方六大特点)
2. [特点详解](#2-特点详解)
3. [Spring 中体现的设计模式](#3-spring-中体现的设计模式)

---

## 1. 官方六大特点

Spring 官方文档对框架的定位可以概括为六点：**轻量级、非侵入式、IoC、AOP、容器、一站式**。

| 特点 | 说明 |
|------|------|
| **轻量级** | Spring 核心模块体积小、依赖少。基本版本只需少量 JAR 包即可运行，启动快、内存占用低。 |
| **非侵入式** | 业务对象不需要实现 Spring 的特定接口、也不需要继承 Spring 的基类，可以脱离 Spring 框架独立使用。 |
| **控制反转（IoC）** | 将对象创建和依赖管理交给容器，降低组件间耦合度，使代码更易测试和维护。 |
| **面向切面（AOP）** | 将横切关注点（日志、事务、安全等）从业务逻辑中分离出来，通过切面统一管理，减少重复代码。 |
| **容器** | Spring 是一个容器，管理对象的生命周期和依赖关系（从创建到销毁的全过程）。 |
| **一站式框架** | Spring 提供了从核心容器到 Web、数据访问、消息、测试等全方位的解决方案，可以整合各种第三方框架。 |

---

## 2. 特点详解

### 2.1 非侵入式——最容易被低估的一点

```java
// ❌ 侵入式：业务类被迫继承框架基类，脱离框架就无法编译
public class UserService extends FrameworkBaseService {
    @Override
    public void onInit() { ... }
}
```

```java
// ✅ 非侵入式：就是一个普通 Java 类，连注解都没有
public class UserService {
    private UserDao userDao;

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public User find(Long id) {
        return userDao.findById(id);
    }
}
```

```xml
<!-- Spring 只是在 XML 里"登记"了它，没有改造这个类 -->
<bean id="userService" class="com.example.service.UserService">
    <property name="userDao" ref="userDao"/>
</bean>
```

**意义在哪？**

```java
// ① 单元测试：完全不需要启动 Spring
@Test
public void testFind() {
    UserService service = new UserService();
    service.setUserDao(id -> new User(1L, "张三"));   // 手写一个假实现
    assertEquals("张三", service.find(1L).getName());
}

// ② 随时可以搬走：这个类拿到任何项目里都能直接用
```

> 用 XML 配置的另一个好处就在这里：**Spring 的存在感全在 XML 里，Java 代码干干净净**。这也是后来注解被诟病"侵入业务代码"的原因之一。

### 2.2 声明式编程——把"怎么做"变成"声明什么"

| 能力 | 传统写法 | Spring 声明式写法（XML） |
|------|---------|----------------------|
| 事务 | 手动 `begin / commit / rollback`，try-catch 包裹 | `<tx:advice>` + `<aop:config>` |
| 缓存 | 手动查缓存、写缓存、失效 | `<cache:advice>` + `<aop:config>` |
| 定时任务 | `ScheduledExecutorService` 手写调度 | `<task:scheduled-tasks>` |
| 异步 | 自己建线程池、提交任务 | `<task:executor>` + `<task:annotation-driven>` |
| 远程调用 | 手写 HTTP 客户端 + 序列化 | `RmiServiceExporter` / `HttpInvokerProxyFactoryBean` |

**XML 声明式事务的完整例子：**

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/tx
           https://www.springframework.org/schema/tx/spring-tx.xsd
           http://www.springframework.org/schema/aop
           https://www.springframework.org/schema/aop/spring-aop.xsd">

    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
        <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/demo"/>
        <property name="username" value="root"/>
        <property name="password" value="123456"/>
    </bean>

    <!-- ① 事务管理器 -->
    <bean id="txManager"
          class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!-- ② 通知：哪些方法名要开事务、什么传播行为 -->
    <tx:advice id="txAdvice" transaction-manager="txManager">
        <tx:attributes>
            <tx:method name="save*"   propagation="REQUIRED"/>
            <tx:method name="update*" propagation="REQUIRED"/>
            <tx:method name="delete*" propagation="REQUIRED"/>
            <tx:method name="find*"   read-only="true"/>
            <tx:method name="*"       propagation="SUPPORTS"/>
        </tx:attributes>
    </tx:advice>

    <!-- ③ 切面：把通知织入 service 包下的所有方法 -->
    <aop:config>
        <aop:pointcut id="servicePointcut"
                      expression="execution(* com.example.service.*.*(..))"/>
        <aop:advisor advice-ref="txAdvice" pointcut-ref="servicePointcut"/>
    </aop:config>

</beans>
```

**业务代码里一行事务代码都没有：**

```java
public class OrderService {
    private OrderDao orderDao;
    private StockDao stockDao;

    public void setOrderDao(OrderDao orderDao)   { this.orderDao = orderDao; }
    public void setStockDao(StockDao stockDao)   { this.stockDao = stockDao; }

    public void saveOrder(Order order) {      // 名字以 save 开头 → 自动带事务
        orderDao.insert(order);               // 抛异常 → 整体回滚
        stockDao.decrease(order.getSkuId(), order.getCount());
    }
}
```

### 2.3 模块化与按需引入

```xml
<!-- 只想要 IoC，不需要 Web/数据库：只引 context 即可，整个包不到 2MB -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
</dependency>
```

### 2.4 良好的集成能力与可扩展性

Spring 几乎所有的核心机制都留了扩展点，第三方框架不是"被 Spring 改造"，而是"通过扩展点接入 Spring"：

| 扩展点 | 用途 |
|--------|------|
| `BeanPostProcessor` | 在 Bean 初始化前后加工（AOP 代理、`@Autowired` 注入都靠它） |
| `BeanFactoryPostProcessor` | 修改 BeanDefinition（占位符替换、动态注册 Bean） |
| `HandlerInterceptor` | Web 请求拦截 |
| `HandlerMapping` / `HandlerAdapter` | 自定义 URL 到处理器的映射 |
| `Converter` / `Formatter` | 自定义类型转换 |
| `HandlerMethodArgumentResolver` | 自定义控制器参数解析 |

这也是 MyBatis、Hibernate、Quartz、Redis、RabbitMQ 都能无缝集成的原因。

### 2.5 容器：管理完整生命周期

```
实例化 → 属性注入 → 初始化（init-method）→ 使用 → 销毁（destroy-method）
```

```xml
<bean id="pool" class="com.example.ConnectionPool"
      init-method="init" destroy-method="close">
    <property name="size" value="10"/>
</bean>
```

容器关闭时（`ctx.close()` 或 Web 应用卸载）会自动调用 `close()`，不用手写 try-finally。

---

## 3. Spring 中体现的设计模式

| 设计模式 | Spring 中的体现 |
|---------|----------------|
| **工厂模式** | `BeanFactory` / `ApplicationContext` 就是 Bean 工厂；`FactoryBean` 定制复杂对象的创建 |
| **单例模式** | 默认作用域 `singleton`，实例存放在 `singletonObjects` |
| **代理模式** | AOP 底层：JDK 动态代理（接口）与 CGLIB（类） |
| **模板方法** | `JdbcTemplate`、`RestTemplate`、`TransactionTemplate`——固定流程，暴露回调 |
| **策略模式** | `InstantiationStrategy`（实例化策略）、`Resource` 的不同实现、`HandlerMapping` |
| **观察者模式** | `ApplicationEvent` + `ApplicationListener` 事件机制 |
| **适配器模式** | `HandlerAdapter`、`AdvisorAdapter` |
| **责任链模式** | 拦截器链、`Filter` 链、`BeanPostProcessor` 链 |
| **委派模式** | `BeanDefinitionParserDelegate`、`DispatcherServlet` 委派给 `HandlerMapping` |

---

## 附：一句话总结

> Spring 的价值不在"帮你 new 对象"，而在**用一套统一的容器把"对象怎么来、依赖怎么给、生命周期怎么管、横切逻辑怎么织"全包了**，同时让业务代码保持普通 Java 类的样子。

---

> 下一篇：[09 Spring 常用 JAR 包](09-Spring常用JAR包.md)

# 11 · Bean 的作用域（scope 配置）

> 上一篇：[03 Spring 对 IoC 的实现：DI 依赖注入](03-Spring对IoC的实现-DI依赖注入.md) · 下一篇：[12 GoF 之工厂模式](12-GoF之工厂模式.md)
>
> 本篇只用 XML 配置，所有示例都在 Spring 5.3 + JDK 8 上实际跑过，输出为真实结果（对象 hashCode 每次运行不同）。

---

## 目录

1. [作用域到底决定什么](#1-作用域到底决定什么)
2. [六种作用域一览](#2-六种作用域一览)
3. [singleton：默认的单例](#3-singleton默认的单例)
4. [prototype：每次都要新的](#4-prototype每次都要新的)
5. [singleton 与 prototype 对比](#5-singleton-与-prototype-对比)
6. [经典坑：单例里注入 prototype](#6-经典坑单例里注入-prototype)
7. [Web 作用域：request / session / application / websocket](#7-web-作用域request--session--application--websocket)
8. [自定义作用域（thread）](#8-自定义作用域thread)
9. [源码：scope 在 doGetBean 里怎么分支](#9-源码scope-在-dogetbean-里怎么分支)
10. [常见坑](#10-常见坑)

---

## 1. 作用域到底决定什么

`<bean>` 上的 `scope` 属性回答三个问题：

| 问题 | 举例 |
|------|------|
| **造几个？** | 整个容器 1 个？每次调用 1 个？每个请求 1 个？ |
| **什么时候造？** | 容器启动时就造？还是第一次 `getBean` 才造？ |
| **造完放哪儿？** | 放进单例池长期持有？还是用完就扔给 GC？ |

再加一个隐性的第四个问题：**什么时候销毁、谁来销毁**。这一条最容易被忽略，也是 prototype 最容易踩坑的地方。

> 一句话：`scope` 不是"对象的属性"，而是**容器管理这个对象的策略**。对象本身完全不知道自己是单例还是多例——这一点和设计模式里的单例模式（`private static INSTANCE`）有本质区别。

---

## 2. 六种作用域一览

| scope 值 | 实例数量 | 生效环境 | 说明 |
|----------|---------|---------|------|
| `singleton` | 每个容器 **1 个** | 所有环境 | **默认值**，不写就是这个 |
| `prototype` | 每次获取 **1 个新的** | 所有环境 | 容器不缓存，也不负责销毁 |
| `request` | 每个 HTTP 请求 1 个 | Web | 请求结束即销毁 |
| `session` | 每个 HTTP Session 1 个 | Web | Session 失效时销毁 |
| `application` | 每个 ServletContext 1 个 | Web | 和 singleton 的区别见 7.3 |
| `websocket` | 每个 WebSocket 会话 1 个 | Web | Spring 4.0+ |
| `thread` | 每个线程 1 个 | 需手动注册 | `SimpleThreadScope`，不注册会报错 |

### 2.1 配置写法

```xml
<bean id="cartService" class="com.example.cart.CartService" scope="singleton"/>
<bean id="task"       class="com.example.task.Task"       scope="prototype"/>
<bean id="loginUser"  class="com.example.user.LoginUser"  scope="session"/>
```

就一个 `scope` 属性，值是上表里的字符串，**大小写敏感**（`scope="Singleton"` 会被当成一个名叫 `Singleton` 的自定义作用域，然后报 `No Scope registered for scope name 'Singleton'`）。

> ⚠️ **老教程里的 `singleton="true|false"` 现在已经不能用了。**
>
> 那是 Spring 1.x/2.x 的写法。从 Spring 4 开始，`spring-beans.xsd` 里已经**删掉了 `singleton` 属性**，写了会直接在校验阶段报错（实测）：
>
> ```
> SAXParseException: cvc-complex-type.3.2.2:
> 元素 'bean' 中不允许出现属性 'singleton'。
> ```
>
> 所以看到 `singleton="false"` 的老代码，一律改成 `scope="prototype"`。

---

## 3. singleton：默认的单例

### 3.1 完整示例

```java
// ===== CartService.java =====
package com.example.scope;

public class CartService {
    private static int seq = 0;
    private final int id = ++seq;

    public CartService() {
        System.out.println("  [构造] CartService#" + id + "  @" + Integer.toHexString(hashCode()));
    }
    public void init()    { System.out.println("  [init-method]    CartService#" + id); }
    public void cleanup() { System.out.println("  [destroy-method] CartService#" + id); }
}
```

```xml
<!-- scope.xml -->
<bean id="singletonCart" class="com.example.scope.CartService"
      scope="singleton" init-method="init" destroy-method="cleanup"/>

<bean id="prototypeCart" class="com.example.scope.CartService"
      scope="prototype" init-method="init" destroy-method="cleanup"/>
```

```java
// ===== ScopeMain.java =====
public class ScopeMain {
    public static void main(String[] args) {
        System.out.println("---- 容器启动 ----");
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("scope.xml");
        System.out.println("---- 容器启动完成 ----");

        CartService s1 = ctx.getBean("singletonCart", CartService.class);
        CartService s2 = ctx.getBean("singletonCart", CartService.class);
        System.out.println("singleton 同一个对象? " + (s1 == s2));
    }
}
```

### 3.2 真实输出

```
---- 容器启动 ----
  [构造] CartService#1  @6276ae34
  [init-method]    CartService#1
---- 容器启动完成 ----
singleton 同一个对象? true
```

注意顺序：**构造和 init 都发生在「容器启动完成」之前**。这说明单例不是等你去 `getBean` 才创建的。

### 3.3 三个要点

**① 创建时机：容器启动时**

`ApplicationContext` 的 `refresh()` 最后一步 `finishBeanFactoryInitialization()` → `preInstantiateSingletons()`，会把所有**非懒加载的单例**一次性创建完。

```xml
<!-- 想推迟到第一次 getBean 才创建 -->
<bean id="x" class="..." scope="singleton" lazy-init="true"/>
```

但注意：**用 `BeanFactory` 而不是 `ApplicationContext` 时，单例默认就是懒加载的**，因为它不调用 `preInstantiateSingletons()`。

**② 存放位置：单例池**

```java
// DefaultSingletonBeanRegistry
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
```

key 是 beanName。第二次 `getBean` 直接从这里返回，**不会再走创建流程**。

**③ 销毁时机：容器关闭时**

`ctx.close()` 会触发 `destroySingletons()`，对每个登记过的单例回调 `destroy-method` / `DisposableBean#destroy`。

实测输出（`ctx.close()` 之后）：

```
---- 关闭容器 ----
  [destroy-method] CartService#1
```

> 只有单例才会被登记进 `disposableBeans`。原因见 4.3。

**④ 单例 ≠ 线程安全**

Spring 只保证"容器里只有一个实例"，**不保证这个实例是线程安全的**。单例 Bean 如果有可变成员变量，并发下必然出问题：

```java
// ❌ 危险：单例 + 可变状态
public class OrderService {
    private long currentOrderId;          // 所有线程共用这一个字段
    public void create() {
        currentOrderId = nextId();
        // ... 中间被别的线程改掉，数据就串了
    }
}

// ✅ 单例的正确写法：无状态
public class OrderService {
    private final OrderDao orderDao;      // 依赖不可变
    public void create(Order order) {     // 状态放在方法参数/局部变量里
        orderDao.insert(order);
    }
}
```

---

## 4. prototype：每次都要新的

### 4.1 示例

沿用上面的 `CartService`，只把 `scope` 改成 `prototype`：

```java
CartService p1 = ctx.getBean("prototypeCart", CartService.class);
CartService p2 = ctx.getBean("prototypeCart", CartService.class);
System.out.println("prototype 同一个对象? " + (p1 == p2));
```

### 4.2 真实输出

```
---- 容器启动 ----
  [构造] CartService#1  @6276ae34          ← 只有单例被创建
  [init-method]    CartService#1
---- 容器启动完成 ----
prototype 两次 getBean:
  [构造] CartService#2  @18bf3d14          ← 第一次 getBean 才创建
  [init-method]    CartService#2
  [构造] CartService#3  @4fb64261          ← 第二次 getBean 又创建一个
  [init-method]    CartService#3
  prototype 同一个对象? false
```

关键差异：**prototype 在容器启动时完全没有动静**，直到第一次 `getBean`。

### 4.3 三个要点

**① 创建时机：每次获取时**

每次 `getBean()`、每次被别的 Bean 注入、每次 `lookup-method` 调用，都会执行一遍完整的创建流程（实例化 → 属性填充 → 初始化）。

**② 不缓存**

创建完直接返回给调用方，**不放进 `singletonObjects`**。容器里没有它的引用，用完就只剩调用方持有。

**③ 销毁：Spring 不管**

这是最容易踩的坑。看 4.2 的输出，最后 `ctx.close()` 时只打印了：

```
---- 关闭容器 ----
  [destroy-method] CartService#1     ← 只有单例 #1
```

`CartService#2`、`#3` 的 `destroy-method` **一次都没执行**。

原因在 `AbstractBeanFactory#registerDisposableBeanIfNecessary`：

```java
if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
    if (mbd.isSingleton()) {
        registerDisposableBean(beanName, new DisposableBeanAdapter(...));
    } else {
        // 自定义作用域：交给 Scope 自己登记销毁回调
        Scope scope = this.scopes.get(mbd.getScope());
        scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(...));
    }
}
```

`mbd.isPrototype()` 为 true 时**直接跳过**，连登记都不登记。官方文档也明确写了：

> 客户端代码必须负责清理 prototype 作用域的对象并释放其占用的昂贵资源。

所以 prototype Bean 里**不要放需要关闭的资源**（连接、文件流、线程池）。如果非放不可，调用方必须自己 `try-finally` 释放。

---

## 5. singleton 与 prototype 对比

| 维度 | singleton | prototype |
|------|-----------|-----------|
| 实例数量 | 每个容器 1 个 | 每次获取 1 个 |
| 创建时机 | 容器启动（`lazy-init` 可推迟） | 每次 `getBean` |
| 是否缓存 | 放进 `singletonObjects` | 不缓存 |
| 销毁回调 | 容器关闭时执行 | **不执行** |
| 内存占用 | 常驻内存 | 用完等 GC |
| 创建开销 | 只付一次 | 每次都付（大对象要小心） |
| 线程安全 | **要自己保证**（写成无状态） | 天然隔离，每个线程各拿一份 |
| 典型用途 | Service、Dao、配置类、连接池、线程池 | 有状态对象：购物车、任务、Builder、非线程安全的工具类（`SimpleDateFormat`） |

> 判断口诀：**这个对象"有没有状态"**。无状态 → singleton；有状态且每个使用者需要独立副本 → prototype。

---

## 6. 经典坑：单例里注入 prototype

### 6.1 现象

```xml
<bean id="task" class="com.example.scope.Task" scope="prototype"/>

<bean id="taskHolder" class="com.example.scope.TaskHolder">
    <property name="task" ref="task"/>       <!-- 注入一个 prototype -->
</bean>
```

```java
public class TaskHolder {
    private Task task;
    public void setTask(Task task) { this.task = task; }
    public Task getTask() { return task; }
}
```

调用两次：

```java
System.out.println(holder.getTask() + " / " + holder.getTask());
```

真实输出：

```
Task#1 @42607a4f / Task#1 @42607a4f      ← 同一个对象！
```

`task` 明明是 `prototype`，为什么拿到的是同一个？

### 6.2 原因

**注入只发生一次，在 `TaskHolder` 创建的那一刻。**

```
容器启动
  └─ 创建 taskHolder（单例）
       └─ populateBean → getBean("task")  ← 只在这里要了一次
            └─ 创建 Task#1，塞进 taskHolder.task 字段
  ...
之后所有 holder.getTask() 都只是读那个字段，跟容器再无关系
```

`prototype` 的语义是"**每次向容器要**都给新的"，但单例 Bean 只向容器要了**一次**。字段里存的是那个旧引用，容器再怎么造新对象也不会回来改你的字段。

这就是很多资料说的"prototype 被单例吃掉，退化成了事实上的单例"。

### 6.3 解法一：`lookup-method`（纯 XML，推荐）

思路：不在创建时注入，而是**每次调用方法时去容器要**。

```java
public abstract class TaskService {
    private String name;
    public void setName(String name) { this.name = name; }

    // 抽象方法，没有实现——Spring 会生成子类覆盖它
    protected abstract Task createTask();

    public void run() {
        System.out.println(name + " 拿到 " + createTask());
    }
}
```

```xml
<bean id="taskService" class="com.example.scope.TaskService">
    <lookup-method name="createTask" bean="task"/>
    <property name="name" value="TaskService(lookup-method)"/>
</bean>
```

真实输出：

```
TaskService(lookup-method) 拿到 Task#2 @782663d3
TaskService(lookup-method) 拿到 Task#3 @1990a65e      ← 每次都是新的
```

原理：Spring 用 CGLIB 生成 `TaskService` 的子类，覆盖 `createTask()`，方法体大致是：

```java
protected Task createTask() {
    return (Task) beanFactory.getBean("task");   // 每次都问容器
}
```

所以类不能是 `final`，方法也不能是 `final`、`private`、`static`。

> 名字叫 `lookup-method`，本质是「方法注入」——用方法调用代替字段读取，把"取值时机"从创建时推迟到调用时。

### 6.4 解法二：`<aop:scoped-proxy/>`

思路：注入的不是真对象，而是一个**代理**；每次调用代理的方法，代理去容器取当前作用域的实例。

```xml
<beans xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="...
           http://www.springframework.org/schema/aop
           https://www.springframework.org/schema/aop/spring-aop.xsd">

    <bean id="task" class="com.example.scope.Task" scope="prototype">
        <aop:scoped-proxy proxy-target-class="true"/>
    </bean>
</beans>
```

- `proxy-target-class="true"`：用 CGLIB 代理类（目标没有接口时必须为 true，默认是 false 即 JDK 动态代理）
- 注入方代码完全不用改，`holder.getTask()` 拿到的其实是代理对象

### 6.5 解法三：主动向容器要

让 Bean 实现 `ApplicationContextAware`，每次用的时候自己 `getBean`：

```java
public class TaskService implements ApplicationContextAware {
    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.ctx = ctx;
    }
    public void run() {
        Task task = ctx.getBean("task", Task.class);   // 每次新的
    }
}
```

能解决问题，但**业务类直接依赖了 Spring 容器**，单元测试很难写，一般作为最后手段。

### 6.6 三种解法对比

| 方案 | 侵入性 | 需要 CGLIB | 适用场景 |
|------|--------|-----------|---------|
| `lookup-method` | 中（要加抽象方法） | 是 | 纯 XML 项目，方法本身有业务含义 |
| `<aop:scoped-proxy/>` | **无**（只改 XML） | 看配置 | 最省事，尤其 Web 作用域注入单例 |
| `ApplicationContextAware` | 高（耦合容器） | 否 | 临时救急、框架层代码 |

---

## 7. Web 作用域：request / session / application / websocket

### 7.1 配置

```xml
<!-- 每个请求一份：当前登录用户信息 -->
<bean id="loginUser" class="com.example.user.LoginUser" scope="request"/>

<!-- 每个会话一份：购物车 -->
<bean id="shoppingCart" class="com.example.cart.ShoppingCart" scope="session"/>

<!-- 每个应用一份：全局计数器 -->
<bean id="visitCounter" class="com.example.stat.VisitCounter" scope="application"/>
```

| scope | 生命周期 | 典型用途 |
|-------|---------|---------|
| `request` | 一次 HTTP 请求（进入 DispatcherServlet 到响应完成） | 请求参数封装、当前用户、请求级缓存 |
| `session` | 一个 HTTP Session（默认 30 分钟不活动失效） | 登录态、购物车、多步表单 |
| `application` | 一个 `ServletContext`（应用启动到关闭） | 全局配置、统计计数 |
| `websocket` | 一个 WebSocket 会话 | 推送会话上下文 |

### 7.2 生效条件（不满足直接报错）

必须同时满足两个条件：

**① 容器必须是 `WebApplicationContext`**

用 `ContextLoaderListener` 或 `DispatcherServlet` 初始化的容器，才会调用 `WebApplicationContextUtils.registerWebApplicationScopes()` 把 `request` / `session` / `application` 注册进去。

**② 当前线程必须绑定着请求**

```xml
<!-- web.xml：只配了 ContextLoaderListener、没有 DispatcherServlet 时，必须加这个 -->
<listener>
    <listener-class>org.springframework.web.context.request.RequestContextListener</listener-class>
</listener>
```

否则一取 Bean 就抛：

```
java.lang.IllegalStateException: No thread-bound request found:
Are you referring to request attributes outside of an actual web request,
or processing a request outside of the originally receiving thread?
```

用 `DispatcherServlet` 的项目不需要额外配，它自己会绑定。

### 7.3 为什么 Web 作用域 Bean 注入单例必须用代理

```xml
<!-- ❌ 启动就会炸 -->
<bean id="loginUser" class="com.example.user.LoginUser" scope="request"/>

<bean id="userService" class="com.example.service.UserService">
    <property name="loginUser" ref="loginUser"/>
</bean>
```

单例 `userService` 在**容器启动时**创建，那一刻根本没有 HTTP 请求，`request` 作用域拿不到当前请求 → `No thread-bound request found`。

即使能拿到，注入也只发生一次，之后所有请求共用同一个 `LoginUser`——逻辑全错。

正确写法是加代理：

```xml
<bean id="loginUser" class="com.example.user.LoginUser" scope="request">
    <aop:scoped-proxy proxy-target-class="true"/>
</bean>
```

这样单例里注入的是代理，每次调用方法时才去当前请求里取真正的实例。

> 顺带回答一个常见疑问：**`application` 和 `singleton` 有什么区别？**
> 单容器内基本等价。但一个应用可能有多个容器（`ContextLoaderListener` 的根容器 + 每个 `DispatcherServlet` 的子容器），这时 `singleton` 是"每个容器一个"，`application` 是"整个 ServletContext 一个"。另外 `application` 的 Bean 会存进 `ServletContext` 属性，可以被非 Spring 代码拿到。

---

## 8. 自定义作用域（thread）

Spring 提供了 `SimpleThreadScope`：每个线程一份实例。它**默认不注册**，直接写 `scope="thread"` 会报：

```
IllegalStateException: No Scope registered for scope name 'thread'
```

注册方式（纯 XML）：

```xml
<bean class="org.springframework.beans.factory.config.CustomScopeConfigurer">
    <property name="scopes">
        <map>
            <entry key="thread">
                <bean class="org.springframework.context.support.SimpleThreadScope"/>
            </entry>
        </map>
    </property>
</bean>

<bean id="ctx" class="com.example.ThreadContext" scope="thread"/>
```

或者 Java 代码里注册：

```java
ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) ctx.getAutowireCapableBeanFactory();
beanFactory.registerScope("thread", new SimpleThreadScope());
```

> ⚠️ `SimpleThreadScope` 用 `ThreadLocal` 存实例，但**不会在 线程结束时清理**。在线程池场景下，同一个线程复用会一直拿到旧对象，容易造成脏数据和内存泄漏。生产环境一般自己实现 `Scope` 接口，重写 `remove()`。

---

## 9. 源码：scope 在 doGetBean 里怎么分支

`AbstractBeanFactory#doGetBean` 拿到 BeanDefinition 后，第一件事就是按作用域分三条路：

```java
// 简化后的主干
if (mbd.isSingleton()) {
    // ① 单例：先查缓存，没有再创建，创建完放进单例池
    sharedInstance = getSingleton(beanName, () -> createBean(beanName, mbd, args));
    beanInstance = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
}
else if (mbd.isPrototype()) {
    // ② 多例：每次都创建，不缓存
    try {
        beforePrototypeCreation(beanName);          // 标记"正在创建 prototype"
        prototypeInstance = createBean(beanName, mbd, args);
    } finally {
        afterPrototypeCreation(beanName);
    }
    beanInstance = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
}
else {
    // ③ 其它作用域：委托给 Scope 实现
    String scopeName = mbd.getScope();
    Scope scope = this.scopes.get(scopeName);
    if (scope == null) {
        throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
    }
    Object scopedInstance = scope.get(beanName, () -> {
        beforePrototypeCreation(beanName);
        try { return createBean(beanName, mbd, args); }
        finally { afterPrototypeCreation(beanName); }
    });
    beanInstance = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
}
```

其中：

```java
// AbstractBeanDefinition
public boolean isSingleton() { return SCOPE_SINGLETON.equals(this.scope); }   // "singleton"
public boolean isPrototype() { return SCOPE_PROTOTYPE.equals(this.scope); }   // "prototype"
```

`Scope` 接口只有四个方法，自定义作用域实现它就行：

```java
public interface Scope {
    Object get(String name, ObjectFactory<?> objectFactory);   // 取实例
    Object remove(String name);                                // 移除
    void registerDestructionCallback(String name, Runnable callback);  // 销毁回调
    Object resolveContextualObject(String key);                // 上下文对象
    String getConversationId();                                // 会话标识
}
```

`request` / `session` 的实现分别是 `RequestScope` / `SessionScope`，底层就是 `RequestContextHolder` + `request.getAttribute/setAttribute`。

---

## 10. 常见坑

| 坑 | 现象 | 原因 / 解法 |
|----|------|------------|
| **单例注入 prototype 只生效一次** | 每次拿到的都是同一个对象 | 注入只发生一次。用 `lookup-method` / `<aop:scoped-proxy/>` / 主动 `getBean`（第 6 节） |
| **prototype 的 `destroy-method` 不执行** | 资源没释放，连接泄漏 | prototype 不登记销毁回调，调用方自己负责释放 |
| **单例里有可变成员变量** | 并发下数据错乱 | 单例必须无状态，状态放方法参数或 `ThreadLocal` |
| **写 `singleton="false"`** | `SAXParseException: 元素 'bean' 中不允许出现属性 'singleton'` | 该属性在 Spring 4 已从 XSD 移除，改用 `scope="prototype"` |
| **`scope` 值大小写写错** | `No Scope registered for scope name 'Singleton'` | 作用域名是精确匹配的小写字符串 |
| **Web 作用域报 `No thread-bound request found`** | 取 Bean 时抛异常 | 缺 `RequestContextListener`，或不在请求线程里调用 |
| **单例注入 `request`/`session` Bean** | 启动即报错 | 必须加 `<aop:scoped-proxy/>`，注入代理而非实例 |
| **`scope="thread"` 报错** | `No Scope registered for scope name 'thread'` | 要先注册 `SimpleThreadScope`（第 8 节） |
| **以为单例是 JVM 唯一** | 起了两个容器，出现两个"单例" | Spring 的单例是**每容器一个**，和 Java 单例模式不是一回事 |
| **给 prototype 配 `lazy-init`** | 看起来没效果 | prototype 本来就是用时才创建，`lazy-init` 只对单例有意义 |
| **在 `BeanFactory` 上期待启动即创建** | 单例迟迟不创建 | `BeanFactory` 不调 `preInstantiateSingletons()`，默认懒加载 |

---

## 附：本篇速查

```
scope 决定四件事：造几个 / 何时造 / 放哪儿 / 何时销毁

singleton（默认）
  创建：容器启动时（preInstantiateSingletons）
  存放：singletonObjects（单例池）
  销毁：ctx.close() 时回调 destroy-method
  注意：单例 ≠ 线程安全，必须无状态

prototype
  创建：每次 getBean 都新建
  存放：不缓存
  销毁：★ 不回调，Spring 完全不管

单例持有 prototype 的三种解法
  lookup-method（CGLIB 覆盖抽象方法，每次问容器）
  <aop:scoped-proxy/>（注入代理，调用时取）
  ApplicationContextAware（自己 getBean，耦合容器）

Web 作用域：request / session / application / websocket
  前提：WebApplicationContext + 请求绑定当前线程
  注入单例：必须加 scoped-proxy

源码分支（doGetBean）
  isSingleton() → getSingleton(...) 走缓存
  isPrototype() → createBean(...) 每次新建
  其它         → scopes.get(scopeName).get(...)
```

---

> 下一篇：[12 GoF 之工厂模式](12-GoF之工厂模式.md) · 回顾：[03 DI 依赖注入](03-Spring对IoC的实现-DI依赖注入.md)

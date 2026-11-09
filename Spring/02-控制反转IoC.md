# 02 · 控制反转 IoC

> 上一篇：[01 开闭原则与依赖倒置](01-开闭原则与依赖倒置.md) · 下一篇：[03 Spring 对 IoC 的实现](03-Spring对IoC的实现-DI依赖注入.md)

---

## 目录

1. [IoC 的定义](#1-ioc-的定义)
2. [传统写法 vs IoC 写法](#2-传统写法-vs-ioc-写法)
3. [第一个完整的 Spring 程序（纯 XML）](#3-第一个完整的-spring-程序纯-xml)
4. [IoC、DI、容器三者的关系](#4-iocdi容器三者的关系)
5. [几个容易混淆的问题](#5-几个容易混淆的问题)

---

## 1. IoC 的定义

将对象的**创建权**交出去，将对象与对象之间的**管理权（依赖关系）**交出去，由**第三方容器（Spring IoC 容器）**来负责创建和维护。

> "控制"指的是对象创建和依赖关系维护的权力；"反转"指这种权力从程序代码内部转移到了外部容器。

拆开看两件事：

| 交出去的权力 | 以前是谁干 | 现在是谁干 |
|-------------|-----------|-----------|
| **创建权** | 业务代码 `new` | IoC 容器（反射创建） |
| **依赖管理权** | 业务代码自己 `set` / 构造函数传 | IoC 容器（读配置后注入） |

---

## 2. 传统写法 vs IoC 写法

### 2.1 传统写法：自己 new，自己管

```java
public class UserDaoMysqlImpl implements UserDao {
    private DataSource dataSource = new DruidDataSource();   // 自己造
}

public class UserService {
    private UserDao userDao = new UserDaoMysqlImpl();        // 自己造
}

public class UserController {
    private UserService userService = new UserService();     // 自己造
}
```

问题：

1. **耦合死**：`UserService` 编译期就依赖 `UserDaoMysqlImpl`，想换实现必须改代码。
2. **没法单测**：`UserService` 造出来就带着真数据库连接，测试想 mock 都插不进去。
3. **重复创建**：每个用到处都 `new` 一遍，对象满地跑，无法统一管理生命周期。
4. **装配逻辑散落各处**：谁用谁 new，改一处配置要翻遍整个工程。

### 2.2 IoC 写法：只声明"我需要什么"

```java
// ===== 类：干干净净，只有业务逻辑 =====
public interface UserDao {
    String findName(long id);
}

public class UserDaoMysqlImpl implements UserDao {
    @Override
    public String findName(long id) { return "mysql-user-" + id; }
}

public class UserService {
    private UserDao userDao;                                  // 只声明依赖

    public void setUserDao(UserDao userDao) {                 // 留一个入口给容器
        this.userDao = userDao;
    }

    public String hello(long id) {
        return "Hello, " + userDao.findName(id);
    }
}
```

```xml
<!-- ===== 配置：装配关系写在 XML 里 ===== -->
<bean id="userDao" class="com.example.dao.UserDaoMysqlImpl"/>

<bean id="userService" class="com.example.service.UserService">
    <property name="userDao" ref="userDao"/>
</bean>
```

```java
// ===== 使用：从容器拿现成的 =====
ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
UserService userService = ctx.getBean("userService", UserService.class);
System.out.println(userService.hello(1));     // Hello, mysql-user-1
```

**变化点：**

| 项 | 传统 | IoC |
|----|------|-----|
| `UserService` 里有没有 `new` | 有 | 没有 |
| 换实现要改什么 | 改 Java 代码 | 改 XML 一行 |
| 对象谁创建 | 业务代码 | 容器 |
| 依赖谁塞 | 业务代码 | 容器 |
| 单测怎么办 | 难 | 手动 `new UserService()` + `setUserDao(mock)`，容器都不用启 |

---

## 3. 第一个完整的 Spring 程序（纯 XML）

### 3.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>6.1.14</version>
</dependency>
```

> `spring-context` 会传递引入 `spring-core`、`spring-beans`、`spring-aop`、`spring-expression`，写 IoC 只引这一个就够了。

### 3.2 目录结构

```
src/main/java/com/example/
├── dao/
│   ├── UserDao.java
│   └── UserDaoMysqlImpl.java
├── service/
│   └── UserService.java
└── Main.java

src/main/resources/
└── applicationContext.xml
```

### 3.3 XML 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           https://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- id：Bean 的唯一标识，getBean 时用它取 -->
    <!-- class：全限定类名，容器靠它反射创建对象 -->
    <bean id="userDao" class="com.example.dao.UserDaoMysqlImpl"/>

    <bean id="userService" class="com.example.service.UserService">
        <!-- name：对应 setUserDao 去掉 set、首字母小写 -->
        <!-- ref：要注入的另一个 Bean 的 id -->
        <property name="userDao" ref="userDao"/>
    </bean>

</beans>
```

### 3.4 启动

```java
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    public static void main(String[] args) {
        // 1. 启动容器：解析 XML → 生成 BeanDefinition → 创建所有单例 Bean
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");

        // 2. 按 id + 类型取 Bean
        UserService userService = ctx.getBean("userService", UserService.class);

        // 3. 用
        System.out.println(userService.hello(1));
    }
}
```

**输出：**

```
Hello, mysql-user-1
```

### 3.5 这几行背后发生了什么

```
new ClassPathXmlApplicationContext("applicationContext.xml")
   │
   ├─ ① 找到 classpath 下的 applicationContext.xml
   ├─ ② XmlBeanDefinitionReader 解析每个 <bean>
   │      把 id / class / <property> 存成 BeanDefinition
   │      放进 beanDefinitionMap（"图纸柜"）
   ├─ ③ refresh()：实例化所有非懒加载的单例
   │      userDao    → 反射 new UserDaoMysqlImpl()
   │      userService → 反射 new UserService()
   │                  → 反射调用 setUserDao(userDao)
   └─ ④ 容器就绪，等你 getBean
```

> 完整流程见 [03 篇第 2 节](03-Spring对IoC的实现-DI依赖注入.md#2-从容器启动到-bean-可用宏观流程)。

---

## 4. IoC、DI、容器三者的关系

| 概念 | 层次 | 说明 |
|------|------|------|
| **IoC** | 思想 / 原则 | 控制反转，把创建权与依赖管理权交给容器 |
| **DI** | 实现手段 | 依赖注入，容器在运行时把依赖"注入"进对象 |
| **IoC 容器** | 基础设施 | 承载 DI 的运行时环境，如 `BeanFactory`、`ApplicationContext` |

一句话：**IoC 是目标，DI 是手段，容器是载体。**

```
       ┌─────────────────────────────────────────┐
       │  思想层：IoC（控制反转）                  │
       │    "对象别自己造，交给别人"                │
       └──────────────────┬──────────────────────┘
                          │ 怎么落地？
                          ▼
       ┌─────────────────────────────────────────┐
       │  手段层：DI（依赖注入）                   │
       │    Set 注入：<property>                  │
       │    构造注入：<constructor-arg>           │
       └──────────────────┬──────────────────────┘
                          │ 谁来执行？
                          ▼
       ┌─────────────────────────────────────────┐
       │  载体层：IoC 容器                        │
       │    BeanFactory / ApplicationContext      │
       └─────────────────────────────────────────┘
```

---

## 5. 几个容易混淆的问题

| 问题 | 回答 |
|------|------|
| **IoC 就等于 DI 吗？** | 不等于。IoC 是思想，DI 是它最常见的实现方式。理论上还有"依赖查找（Dependency Lookup）"也算 IoC，比如 EJB 的 JNDI 查找、Spring 的 `getBean()` 主动拉取 |
| **用了 IoC 容器就不依赖具体类了吗？** | 不完全。Java 代码里确实只依赖接口了，但**依赖关系转移到了 XML**——XML 里还是写着具体类名。所以 IoC 没有消除依赖，只是把依赖"集中管理"了 |
| **IoC 容器就是 ApplicationContext 吗？** | 不是。`BeanFactory` 才是最底层的容器，`ApplicationContext` 是它的增强版（多了事件、国际化、资源加载、启动时预实例化） |
| **`getBean()` 算不算 IoC？** | 算"依赖查找"，是 IoC 的另一种形式，但它是**主动拉取**，和 DI 的**被动注入**方向相反。业务代码里应该尽量避免调 `getBean()` |
| **不用 Spring 能实现 IoC 吗？** | 能。任何"把对象创建交给工厂"的做法都是 IoC，比如简单工厂、服务定位器。[10 篇](10-实战-手写迷你IoC容器.md)就用 100 行手写了一个 |

---

> 下一篇：[03 Spring 对 IoC 的实现：DI 依赖注入](03-Spring对IoC的实现-DI依赖注入.md) —— 本系列的重点，讲透 `<property>` 和 `<constructor-arg>` 到底是怎么被执行的。

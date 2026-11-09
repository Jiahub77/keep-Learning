# 04 · SOLID 五大设计原则

> 上一篇：[03 Spring 对 IoC 的实现](03-Spring对IoC的实现-DI依赖注入.md) · 下一篇：[05 Spring 七大模块](05-Spring七大模块.md)

---

## 目录

1. [SOLID 是什么](#1-solid-是什么)
2. [五条原则逐条说明](#2-五条原则逐条说明)
3. [五条原则之间的关系](#3-五条原则之间的关系)
4. [Spring 中对应的体现](#4-spring-中对应的体现)

---

## 1. SOLID 是什么

SOLID 是面向对象设计（OOD）五大原则首字母的缩写，由 Robert C. Martin（Uncle Bob）提出：

| 原则 | 全称 | 核心思想 |
|------|------|---------|
| **SRP** | Single Responsibility Principle（单一职责原则） | 一个类只干一件事，应该只有一个引起它变化的原因 |
| **OCP** | Open-Closed Principle（开闭原则） | 加功能时尽量新增代码，而不是修改老代码——对扩展开放，对修改关闭 |
| **LSP** | Liskov Substitution Principle（里氏替换原则） | 子类要能当父类用——所有引用父类的地方必须能透明地使用子类对象 |
| **ISP** | Interface Segregation Principle（接口隔离原则） | 接口要专一——客户端不应该被迫依赖它不使用的方法，避免"胖接口" |
| **DIP** | Dependency Inversion Principle（依赖倒置原则） | 依赖抽象而不是具体实现——高层不依赖低层，二者都依赖抽象 |

---

## 2. 五条原则逐条说明

### 2.1 SRP 单一职责

**定义：** 一个类应该只有一个引起它变化的原因。

```java
// ❌ 反面：一个类干了三件事，改任何一件都可能碰到这个类
public class UserService {
    public void register(User user) { /* 校验 + 存库 + 发邮件 */ }
}

// ✅ 正面：拆开，各自只对一件事负责
public class UserValidator   { public void validate(User user) { ... } }
public class UserRepository  { public void save(User user) { ... } }
public class MailSender      { public void sendWelcome(User user) { ... } }
```

> "职责"不是"功能数量"，而是**"变化的原因"**。校验规则变了、存储方式变了、邮件模板变了——这是三个不同的变化原因，就该分三个类。

### 2.2 OCP 开闭原则

**定义：** 对扩展开放，对修改关闭。

```java
// ❌ 每加一种支付方式都要改这个方法
public class PayService {
    public void pay(String type) {
        if ("alipay".equals(type)) { ... }
        else if ("wechat".equals(type)) { ... }   // 每加一种就改这里
    }
}

// ✅ 面向接口：新增实现类即可，PayService 一行不动
public interface PayChannel { void pay(BigDecimal amount); }
public class AlipayChannel implements PayChannel { ... }
public class WechatChannel implements PayChannel { ... }
```

XML 配置正好是 OCP 的落地手段——换实现只改 `ref`：

```xml
<bean id="payService" class="com.example.PayService">
    <property name="channel" ref="alipayChannel"/>   <!-- 换成 wechatChannel 即可 -->
</bean>
```

详见 [01 篇](01-开闭原则与依赖倒置.md#1-ocp-开闭原则)。

### 2.3 LSP 里氏替换

**定义：** 所有引用父类（或接口）的地方，必须能透明地使用其子类对象，且程序行为不变。

```java
// ❌ 反面：子类改变了父类的语义
public class Rectangle {
    protected int width, height;
    public void setWidth(int w)  { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

public class Square extends Rectangle {
    @Override public void setWidth(int w)  { this.width = this.height = w; }   // 偷偷改了另一个字段
    @Override public void setHeight(int h) { this.width = this.height = h; }
}

// 调用方按 Rectangle 的契约写：
Rectangle r = new Square();
r.setWidth(5);
r.setHeight(4);
// 期望 area = 20，实际 = 16 —— 契约被破坏
```

**判断标准：** 子类不能做这些事——抛父类没有的异常、改变父类方法的语义、削弱前置条件/加强后置条件。

### 2.4 ISP 接口隔离

**定义：** 客户端不应该被迫依赖它不使用的方法。接口要小而专。

```java
// ❌ 胖接口：实现类被迫写一堆空方法
public interface Worker {
    void work();
    void eat();
    void sleep();
}

public class Robot implements Worker {
    public void work()  { ... }
    public void eat()   { throw new UnsupportedOperationException(); }   // 机器人不用吃饭
    public void sleep() { throw new UnsupportedOperationException(); }
}

// ✅ 拆成小接口，各取所需
public interface Workable { void work(); }
public interface Eatable  { void eat(); }
public interface Sleepable{ void sleep(); }

public class Robot implements Workable { public void work() { ... } }
public class Human implements Workable, Eatable, Sleepable { ... }
```

### 2.5 DIP 依赖倒置

**定义：** 高层模块不依赖低层模块，二者都依赖抽象；抽象不依赖细节，细节依赖抽象。

```java
// ❌ 高层直接依赖低层
public class OrderService {
    private UserDaoMysqlImpl userDao = new UserDaoMysqlImpl();
}

// ✅ 都依赖抽象，具体实现由 XML 装配
public class OrderService {
    private UserDao userDao;
    public void setUserDao(UserDao userDao) { this.userDao = userDao; }
}
```

详见 [01 篇](01-开闭原则与依赖倒置.md#2-依赖倒置原则-dip)。

---

## 3. 五条原则之间的关系

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

## 4. Spring 中对应的体现

| 原则 | Spring 里怎么体现 |
|------|------------------|
| **SRP** | 分层架构：Controller / Service / Dao 各司其职；`BeanFactory` 只管创建，`ApplicationContext` 管应用级能力 |
| **OCP** | XML 换 `ref` 即可切换实现；`BeanPostProcessor` 等扩展点允许不改源码增强功能 |
| **LSP** | 面向接口编程；JDK 动态代理要求实现接口，代理对象可替换原对象 |
| **ISP** | `BeanFactory` / `ListableBeanFactory` / `ApplicationContext` 层层细化，按需实现 |
| **DIP** | 整个 IoC 容器就是 DIP 的终极实现：高层只写接口，实现由容器注入 |

---

> 下一篇：[05 Spring 七大模块](05-Spring七大模块.md)

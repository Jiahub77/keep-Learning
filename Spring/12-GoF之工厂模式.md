# 12 · GoF 之工厂模式（简单工厂 / 工厂方法 / 抽象工厂）

> 上一篇：[11 Bean 的作用域](11-Bean的作用域.md) · 下一篇：[13 Bean 的实例化方式](13-Bean的实例化方式.md)
>
> 本篇是设计模式篇，代码是纯 Java（不依赖 Spring），三个示例都已编译运行验证。

---

## 目录

1. [问题从一行 `new` 开始](#1-问题从一行-new-开始)
2. [简单工厂（Simple Factory）](#2-简单工厂simple-factory)
3. [工厂方法（Factory Method）](#3-工厂方法factory-method)
4. [抽象工厂（Abstract Factory）](#4-抽象工厂abstract-factory)
5. [三者对比](#5-三者对比)
6. [Spring 里的工厂模式](#6-spring-里的工厂模式)
7. [常见误区](#7-常见误区)

---

## 1. 问题从一行 `new` 开始

```java
public class OrderService {
    public void pay(long amount, String type) {
        PayChannel channel;
        if ("alipay".equals(type)) {
            channel = new AlipayChannel();        // ← 依赖了具体类
        } else if ("wechat".equals(type)) {
            channel = new WechatChannel();        // ← 再加渠道就要改这里
        } else {
            throw new IllegalArgumentException("不支持的渠道");
        }
        channel.pay(amount);
    }
}
```

这段代码有三个毛病：

| 毛病 | 说明 |
|------|------|
| **依赖具体类** | 业务类编译期就知道 `AlipayChannel` 的存在，违背「依赖倒置」 |
| **改一处、动全身** | 新增一个渠道要改 `OrderService`，违背「开闭原则」 |
| **创建逻辑散落** | 构造过程复杂时（读配置、设参数、加缓存），每个调用点都要重复一遍 |

工厂模式就是把「**创建对象**」这件事从业务代码里抽出来，交给一个专门的角色负责。

> 注意区分：工厂模式解决的是「**创建**的耦合」，依赖注入解决的是「**使用**的耦合」。Spring 两件事都做了——工厂负责造，容器负责塞。

---

## 2. 简单工厂（Simple Factory）

### 2.1 结构

```
        Client
          │  只认识 Product 和 Factory
          ▼
   ┌─────────────┐        ┌──────────────┐
   │   Factory   │───────▶│   Product    │  «interface»
   │  + create() │        └──────────────┘
   └─────────────┘          △         △
          │                 │         │
          │ new             │         │
          ├─────────────────┘         │
          │        ConcreteProductA    │
          └───────────────────────────┘
                     ConcreteProductB
```

一个工厂类，一个（通常是静态的）创建方法，内部用 `if/else` 或 `switch` 决定 `new` 哪个实现。

### 2.2 代码

```java
// ===== PayChannel.java =====
public interface PayChannel {
    void pay(long amount);
}

// ===== AlipayChannel.java =====
public class AlipayChannel implements PayChannel {
    @Override
    public void pay(long amount) { System.out.println("支付宝支付 " + amount + " 分"); }
}

// ===== WechatChannel.java =====
public class WechatChannel implements PayChannel {
    @Override
    public void pay(long amount) { System.out.println("微信支付 " + amount + " 分"); }
}

// ===== PayChannelFactory.java：唯一知道所有实现的地方 =====
public class PayChannelFactory {

    public static PayChannel create(String type) {
        if ("alipay".equalsIgnoreCase(type)) {
            return new AlipayChannel();
        } else if ("wechat".equalsIgnoreCase(type)) {
            return new WechatChannel();
        }
        throw new IllegalArgumentException("不支持的支付渠道: " + type);
    }
}
```

```java
// ===== 使用方 =====
PayChannel c1 = PayChannelFactory.create("alipay");
PayChannel c2 = PayChannelFactory.create("wechat");
c1.pay(100);
c2.pay(200);
```

真实输出：

```
支付宝支付 100 分
微信支付 200 分
```

### 2.3 优点

1. **客户端只依赖接口**：`OrderService` 里不再出现 `AlipayChannel` 这个类名，编译期解耦。
2. **创建逻辑集中**：要加缓存、打日志、做参数校验，只改工厂一处。
3. **语义清晰**：`PayChannelFactory.create("alipay")` 比 `new AlipayChannel()` 更能表达"我要一个支付渠道"这个意图。
4. **实现类可以藏起来**：`AlipayChannel` 可以包级私有，外部只能通过工厂拿。

### 2.4 缺点

| 缺点 | 说明 |
|------|------|
| **违反开闭原则** | 新增渠道必须改 `create()`，加一个 `else if`。产品越多，这个方法越臃肿 |
| **工厂职责过重** | 所有产品的创建细节都压在一个类里，产品多了就是"上帝类" |
| **静态方法不可扩展** | `static` 不能被继承重写，想换一套创建逻辑只能改源码 |
| **不好 mock** | 静态方法在单元测试里没法替换（除非用 PowerMock 之类） |
| **无法延迟到子类决定** | 产品类型在编译期就写死在工厂里了 |

> **它不是 GoF 23 种模式之一**。GoF 书里没有"简单工厂"，它是工厂方法的一种简化写法（也有人叫"静态工厂方法"）。但实际项目里它的出场率比另外两个都高。

---

## 3. 工厂方法（Factory Method）

### 3.1 结构

```
   «interface» Product          Creator  «abstract»
        △                          + templateMethod()
        │                          # factoryMethod()  «abstract»
   ┌────┴────┐                          △
ConcreteA  ConcreteB                      │
     △         △              ┌───────────┴───────────┐
     │         │        ConcreteCreatorA       ConcreteCreatorB
     └─────────┴───────────────│                       │
        （各自创建自己的产品）    └───────────────────────┘
```

核心变化：**把 `create()` 从「一个具体类里的 if/else」变成「抽象类里的抽象方法」，由子类决定造什么。**

这就是 GoF 说的「**将实例化延迟到子类**」。

### 3.2 代码

```java
// ===== 产品 =====
public interface Logger {
    void log(String msg);
}

public class ConsoleLogger implements Logger {
    @Override
    public void log(String msg) { System.out.println("[console] " + msg); }
}

public class FileLogger implements Logger {
    @Override
    public void log(String msg) { System.out.println("[file]    " + msg); }
}
```

```java
// ===== 创建者（抽象） =====
public abstract class LoggerFactory {

    // ★ 工厂方法：只声明，不实现
    protected abstract Logger createLogger();

    // 模板方法：业务流程固定，产品由子类提供
    public void write(String msg) {
        Logger logger = createLogger();     // 调子类的实现
        logger.log(msg);
    }
}

// ===== 具体创建者 =====
public class ConsoleLoggerFactory extends LoggerFactory {
    @Override
    protected Logger createLogger() { return new ConsoleLogger(); }
}

public class FileLoggerFactory extends LoggerFactory {
    @Override
    protected Logger createLogger() { return new FileLogger(); }
}
```

```java
// ===== 使用方 =====
LoggerFactory f1 = new ConsoleLoggerFactory();
LoggerFactory f2 = new FileLoggerFactory();
f1.write("工厂方法：控制台");
f2.write("工厂方法：文件");
```

真实输出：

```
[console] 工厂方法：控制台
[file]    工厂方法：文件
```

### 3.3 优点

1. **符合开闭原则**：新增一种日志 = 新增 `XxxLogger` + `XxxLoggerFactory` 两个类，**已有代码一行不改**。
2. **单一职责**：每个工厂只负责一种产品的创建，不会出现巨型 `switch`。
3. **可配合模板方法**：父类固定业务流程（`write`），子类只决定"用哪个产品"，扩展点非常干净。
4. **客户端依赖抽象**：`write()` 的调用方只认识 `LoggerFactory` 和 `Logger`。

### 3.4 缺点

| 缺点 | 说明 |
|------|------|
| **类数量膨胀** | n 个产品要 2n 个类。产品多时项目里全是 Factory |
| **选择问题转移了** | 工厂自己不选，改由**调用方**选（`new ConsoleLoggerFactory()`），调用方还是要认识具体工厂 |
| **只能造一个产品等级** | `LoggerFactory` 只能造 `Logger`，造不了别的类型 |
| **抽象层成本** | 产品只有一两个时，这套结构显得啰嗦 |

> 第 2 条是重点：工厂方法只是把「`if/else` 选产品」变成了「选工厂」，选择动作并没有消失。真正消灭选择的是**配置化 + 反射**——这正是 Spring 做的事（第 6 节）。

---

## 4. 抽象工厂（Abstract Factory）

### 4.1 先分清两个概念

| 概念 | 含义 | 例子 |
|------|------|------|
| **产品等级结构** | 同一类产品的继承体系 | `Button` → `LightButton` / `DarkButton` |
| **产品族** | 同一个具体工厂造出来的、互相配套的一组产品 | `LightButton` + `LightCheckbox` |

抽象工厂管的是**产品族**：一个工厂负责造"一整套风格一致的东西"。

### 4.2 结构

```
   «interface» Button     «interface» Checkbox
        △                       △
   ┌────┴────┐             ┌────┴────┐
LightBtn  DarkBtn      LightCb   DarkCb
   △         △             △         △
   └────┬────┘             └────┬────┘
        │ 创建                  │ 创建
   ┌────┴───────────────────────┴────┐
   │        «interface» UiFactory     │
   │  + createButton()                │
   │  + createCheckbox()              │
   └──────────────────────────────────┘
              △               △
      LightFactory        DarkFactory
   （造一整套浅色）      （造一整套深色）
```

### 4.3 代码

```java
// ===== 产品等级一：按钮 =====
public interface Button { void render(); }

public class LightButton implements Button {
    @Override public void render() { System.out.println("浅色按钮"); }
}
public class DarkButton implements Button {
    @Override public void render() { System.out.println("深色按钮"); }
}

// ===== 产品等级二：复选框 =====
public interface Checkbox { void render(); }

public class LightCheckbox implements Checkbox {
    @Override public void render() { System.out.println("浅色复选框"); }
}
public class DarkCheckbox implements Checkbox {
    @Override public void render() { System.out.println("深色复选框"); }
}
```

```java
// ===== 抽象工厂：一个工厂负责一整个产品族 =====
public interface UiFactory {
    Button createButton();
    Checkbox createCheckbox();
}

// ===== 具体工厂：浅色族 =====
public class LightFactory implements UiFactory {
    @Override public Button createButton()     { return new LightButton(); }
    @Override public Checkbox createCheckbox() { return new LightCheckbox(); }
}

// ===== 具体工厂：深色族 =====
public class DarkFactory implements UiFactory {
    @Override public Button createButton()     { return new DarkButton(); }
    @Override public Checkbox createCheckbox() { return new DarkCheckbox(); }
}
```

```java
// ===== 使用方 =====
UiFactory factory = new DarkFactory();   // ★ 只换这一行，整个界面风格跟着换
Button button = factory.createButton();
Checkbox checkbox = factory.createCheckbox();
button.render();
checkbox.render();
```

真实输出：

```
深色按钮
深色复选框
```

### 4.4 优点

1. **保证产品族一致性**：同一个工厂造出来的组件天然配套，不会出现"深色按钮配浅色复选框"。
2. **切换族成本极低**：换一个工厂实现，整套产品全换，业务代码不动。
3. **对「新增产品族」符合开闭原则**：加一套 `HighContrastFactory` + 两个新产品类，已有工厂不用改。
4. **客户端与具体实现彻底解耦**：客户端只依赖 `UiFactory` 和两个产品接口。

### 4.5 缺点

**最大的硬伤：对「新增产品等级」违反开闭原则。**

如果现在要加一个 `Scrollbar`：

```java
public interface UiFactory {
    Button createButton();
    Checkbox createCheckbox();
    Scrollbar createScrollbar();     // ← 接口一改
}
```

`LightFactory`、`DarkFactory`、以后所有的工厂**全部要改**，编译直接报错。

这就是常说的「**开闭原则的倾斜性**」：

| 扩展方向 | 简单工厂 | 工厂方法 | 抽象工厂 |
|---------|---------|---------|---------|
| 新增产品（同一等级） | ❌ 改工厂 | ✅ 加类 | ✅ 加类（新族） |
| 新增产品等级 | — | — | ❌ 改所有工厂 |

其他缺点：

- 类数量最多：m 个产品族 × n 个产品等级 = m×n 个产品类，再加 m 个工厂
- 结构复杂，小项目里属于过度设计
- 客户端仍然要知道 `new DarkFactory()`（选择问题同样没消失）

---

## 5. 三者对比

| 维度 | 简单工厂 | 工厂方法 | 抽象工厂 |
|------|---------|---------|---------|
| 是否 GoF 23 种 | ❌ 不是 | ✅ 创建型 | ✅ 创建型 |
| 产品维度 | 单一产品等级 | 单一产品等级 | **多个产品等级（产品族）** |
| 谁决定造什么 | 工厂内部的 `if/else` | 子类（抽象方法） | 具体工厂类 |
| 新增一个产品 | 改工厂（❌ OCP） | 加 2 个类（✅ OCP） | 加 2 个类（✅ OCP） |
| 新增一个产品族 | — | — | 加 n+1 个类（✅ OCP） |
| 新增一个产品等级 | — | — | 改所有工厂（❌ OCP） |
| 类数量 | 1 + n | 2n | m + m×n |
| 客户端依赖 | 工厂类 + 产品接口 | 具体工厂 + 产品接口 | 工厂接口 + 产品接口 |
| 复杂度 | 低 | 中 | 高 |
| 典型场景 | 产品少且稳定、工具类创建 | 框架扩展点、日志、解析器 | 换皮肤、换数据库、跨平台 UI |

**怎么选：**

```
产品只有一两种，短期不会扩展？
  → 直接 new，连工厂都不要

产品会不断增加，但都是同一类东西（都是 PayChannel / 都是 Logger）？
  → 简单工厂（产品少）或工厂方法（产品多、要严守 OCP）

要成套地换（一套 UI、一套数据库驱动、一套支付 SDK）？
  → 抽象工厂
```

---

## 6. Spring 里的工厂模式

### 6.1 `BeanFactory` = 简单工厂 + 配置化

```java
// 典型简单工厂
PayChannel channel = PayChannelFactory.create("alipay");

// Spring 的"工厂"
UserService userService = ctx.getBean("userService", UserService.class);
```

结构一模一样：**你给一个标识，工厂返回对象，你不需要 `new`**。

区别在于 Spring 把 `if/else` 换成了 `beanDefinitionMap` 查表：

```java
// 伪代码：DefaultListableBeanFactory#getBean
BeanDefinition bd = this.beanDefinitionMap.get(beanName);   // 查表，不是 if/else
if (bd == null) throw new NoSuchBeanDefinitionException(beanName);
return createBean(beanName, bd, args);
```

这就绕开了简单工厂"新增产品要改工厂"的死穴：**新增一个产品只要加一行 XML，工厂代码零改动**。

> 所以 `BeanFactory` 常被称为「**可配置的简单工厂**」——它用配置（BeanDefinition）替代了硬编码的分支。

### 6.2 `FactoryBean` = 工厂方法思想的落地

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
</bean>
```

`SqlSessionFactoryBean` 本身是个 Bean，但它 `getObject()` 返回的才是你要的 `SqlSessionFactory`。容器里存的是"工厂"，`getBean` 拿到的是"产品"。

这正是工厂方法的核心：**把"怎么造"封装在一个专门的类里，调用方只要结果**。

### 6.3 `ObjectFactory<T>` = 延迟创建

```java
public interface ObjectFactory<T> {
    T getObject() throws BeansException;
}
```

只在真正需要时才创建对象，Spring 内部大量用它做延迟解析（比如 `@Autowired` 注入 `ObjectProvider`）。

### 6.4 三者在 Spring 中的对应

| 设计模式 | Spring 中的体现 |
|---------|----------------|
| 简单工厂 | `BeanFactory#getBean(String)`、`ApplicationContext#getBean` |
| 工厂方法 | `FactoryBean#getObject()`、各种 `XxxFactoryBean` |
| 抽象工厂 | `ApplicationContext` 可以看作"一整套相关 Bean 的工厂"（同一容器里拿到的 Bean 天然配套） |

---

## 7. 常见误区

| 误区 | 纠正 |
|------|------|
| **简单工厂是 GoF 23 种之一** | 不是。GoF 书里只有工厂方法、抽象工厂（都属于创建型） |
| **有个 `XxxFactory` 类就是工厂模式** | 如果只是把 `new` 包一层、没有抽象产品，那只是个"创建工具类" |
| **工厂方法 = 静态工厂方法** | 静态工厂方法是**简单工厂**的实现手段；工厂方法强调"子类决定" |
| **抽象工厂 = 工厂方法叠加** | 关键区别是**产品族约束**：抽象工厂的一个工厂要造多个产品等级，且保证配套 |
| **用了工厂就没有 `new` 了** | 工厂内部照样 `new`，只是把 `new` 集中到了一处。`new` 本身不可能消失 |
| **工厂能解决所有耦合** | 工厂只解决"创建"的耦合；对象之间的依赖关系要靠 DI |
| **模式越复杂越好** | 产品只有一个时用抽象工厂是纯负担。**先写简单代码，出现变化点再重构** |

---

## 附：本篇速查

```
简单工厂（不是 GoF 23）
  一个工厂类 + if/else
  ✅ 客户端不依赖具体类、创建逻辑集中
  ❌ 新增产品改工厂（违反 OCP）、静态方法难 mock

工厂方法（GoF 创建型）
  抽象方法 createXxx()，子类决定造什么
  ✅ 新增产品只加类（OCP）、可配模板方法
  ❌ 类数量 2n、选择问题转移给调用方、只能造一个产品等级

抽象工厂（GoF 创建型）
  一个工厂造一整套产品族
  ✅ 族内一致、换族只改一行、新增族符合 OCP
  ❌ 新增产品等级要改所有工厂（OCP 倾斜性）

Spring
  BeanFactory#getBean  = 简单工厂 + BeanDefinition 查表（配置化）
  FactoryBean#getObject = 工厂方法
  新增 Bean 只加配置 → 绕开了简单工厂的 OCP 缺陷
```

---

> 下一篇：[13 Bean 的实例化方式](13-Bean的实例化方式.md) · 回顾：[01 开闭原则与依赖倒置](01-开闭原则与依赖倒置.md)

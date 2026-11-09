# 13 · Bean 的实例化方式

> 上一篇：[12 GoF 之工厂模式](12-GoF之工厂模式.md) · 返回 [索引](00-学习路线与索引.md)
>
> 本篇只用 XML 配置。所有示例都在 Spring 5.3 + JDK 8 上实际跑过，输出为真实结果。

---

## 目录

1. [四种方式总览](#1-四种方式总览)
2. [方式一：构造方法实例化](#2-方式一构造方法实例化)
3. [方式二：静态工厂 factory-method](#3-方式二静态工厂-factory-method)
4. [方式三：实例工厂 factory-bean](#4-方式三实例工厂-factory-bean)
5. [方式四：FactoryBean 接口](#5-方式四factorybean-接口)
6. [BeanFactory 与 FactoryBean 的区别](#6-beanfactory-与-factorybean-的区别)
7. [实战：注入自定义 Date](#7-实战注入自定义-date)
8. [四种方式对比与选型](#8-四种方式对比与选型)
9. [源码：createBeanInstance 的三个分支](#9-源码createbeaninstance-的三个分支)
10. [常见坑](#10-常见坑)

---

## 1. 四种方式总览

`<bean>` 标签只是"声明"，真正把一个对象造出来有四种途径：

| 方式 | XML 关键属性 | 谁负责 `new` | 典型场景 |
|------|-------------|-------------|---------|
| **构造方法** | `class`（+ `<constructor-arg>`） | Spring 反射调构造器 | 99% 的普通 Bean |
| **静态工厂** | `class` + `factory-method` | 你自己的 `static` 方法 | 复用已有静态工厂（`Calendar.getInstance()`） |
| **实例工厂** | `factory-bean` + `factory-method` | 另一个 Bean 的实例方法 | 工厂对象本身需要配置/依赖 |
| **FactoryBean** | `class` 实现 `FactoryBean` | 你写的 `getObject()` | 复杂初始化、代理对象、第三方类改不了 |

一句话区分：

- 前三种：**Spring 决定怎么造，你告诉它用哪个方法**
- 第四种：**你告诉 Spring"别自己造，来问我"**

---

## 2. 方式一：构造方法实例化

最普通的方式，`<bean>` 里只写 `class`，Spring 反射调用构造器。

```java
public class UserServiceImpl implements UserService {
    private String source = "构造方法";

    public UserServiceImpl() {
        System.out.println("  [new] UserServiceImpl() 无参构造被调用");
    }
    public void setSource(String source) { this.source = source; }

    @Override
    public String hello(long id) {
        return "hello " + id + " ← 由【" + source + "】创建";
    }
}
```

```xml
<bean id="byConstructor" class="com.example.inst.UserServiceImpl">
    <property name="source" value="构造方法（默认）"/>
</bean>
```

内部走的是：

```java
// AbstractAutowireCapableBeanFactory#instantiateBean
Object beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, this);
// → SimpleInstantiationStrategy#instantiate
//     → ctor.newInstance(args)     ★ 反射
```

带参构造的 `<constructor-arg>` 写法见 [03 篇第 6 节](03-Spring对IoC的实现-DI依赖注入.md#6-构造方法注入实现方式)。

**前提**：类必须有可访问的构造器（public 或至少包级且 Spring 能访问），并且要么是无参的，要么参数能被 `<constructor-arg>` 完全匹配上。

---

## 3. 方式二：静态工厂 `factory-method`

### 3.1 代码

```java
public class UserServiceFactory {

    // 静态工厂方法：static
    public static UserService createByStatic() {
        System.out.println("  [static-factory] UserServiceFactory.createByStatic()");
        UserServiceImpl s = new UserServiceImpl();
        s.setSource("静态工厂 factory-method");
        return s;
    }
}
```

```xml
<!-- 注意：class 写的是「工厂类」，不是产品类 -->
<bean id="byStaticFactory"
      class="com.example.inst.UserServiceFactory"
      factory-method="createByStatic"/>
```

运行后：

```
  [static-factory] UserServiceFactory.createByStatic()
  [new] UserServiceImpl() 无参构造被调用
hello 2 ← 由【静态工厂 factory-method】创建
```

### 3.2 三个容易看错的点

**① `class` 是工厂类，不是产品类**

```xml
<!-- ❌ 写成产品类，找不到这个方法 -->
<bean id="x" class="com.example.inst.UserServiceImpl" factory-method="createByStatic"/>
<!-- 报错：No matching factory method found on class [...UserServiceImpl] -->
```

**② `<constructor-arg>` 传给的是工厂方法，不是产品构造器**

```xml
<bean id="calendar" class="java.util.Calendar" factory-method="getInstance"/>
<!-- getInstance() 无参，所以这里不能写 constructor-arg -->

<bean id="birthday" class="com.example.date.DateUtils" factory-method="parse">
    <constructor-arg value="yyyy-MM-dd"/>    <!-- 这两个参数给的是 parse() -->
    <constructor-arg value="2000-01-01"/>
</bean>
```

**③ 工厂方法可以是任意已有的静态方法**

不要求这个类是你写的。`java.util.Calendar#getInstance`、`java.time.LocalDate#now`、`java.util.UUID#randomUUID` 都能直接用：

```xml
<bean id="uuid" class="java.util.UUID" factory-method="randomUUID"/>
<bean id="now"  class="java.time.LocalDate" factory-method="now"/>
```

### 3.3 优缺点

| 优点 | 缺点 |
|------|------|
| 可以返回接口，隐藏实现类 | `class` 语义有歧义（看起来像产品类型，实际是工厂） |
| 能复用第三方类的静态工厂方法，不用包一层 | 静态方法不能访问实例状态，也难 mock |
| 可以返回缓存/单例对象（不必每次 new） | 方法必须是 `static`，否则报错（见第 10 节） |

---

## 4. 方式三：实例工厂 `factory-bean`

### 4.1 代码

```java
public class UserServiceFactory {

    // 实例工厂方法：非 static，必须先有一个工厂对象
    public UserService createByInstance() {
        System.out.println("  [instance-factory] factory.createByInstance()");
        UserServiceImpl s = new UserServiceImpl();
        s.setSource("实例工厂 factory-bean");
        return s;
    }
}
```

```xml
<!-- ① 工厂本身先成为一个 Bean -->
<bean id="userServiceFactory" class="com.example.inst.UserServiceFactory"/>

<!-- ② 产品由「那个 Bean 的实例方法」创建 -->
<bean id="byInstanceFactory"
      factory-bean="userServiceFactory"
      factory-method="createByInstance"/>
```

运行后：

```
  [instance-factory] factory.createByInstance()
  [new] UserServiceImpl() 无参构造被调用
hello 3 ← 由【实例工厂 factory-bean】创建
```

### 4.2 为什么需要它

静态工厂只能调 `static` 方法。但真实场景里，工厂往往**自己需要依赖**：

```java
public class SqlSessionFactoryBuilder {
    private DataSource dataSource;                 // 工厂需要数据源
    private String mapperLocations;

    public void setDataSource(DataSource ds) { this.dataSource = ds; }

    public SqlSessionFactory build() {             // 非静态
        // 用 this.dataSource 组装
    }
}
```

```xml
<bean id="sqlSessionFactoryBuilder" class="com.example.orm.SqlSessionFactoryBuilder">
    <property name="dataSource" ref="dataSource"/>
    <property name="mapperLocations" value="classpath:mapper/*.xml"/>
</bean>

<bean id="sqlSessionFactory"
      factory-bean="sqlSessionFactoryBuilder"
      factory-method="build"/>
```

工厂先被 Spring 装配好（依赖注入完成），再让它的实例方法去造产品。这是静态工厂做不到的。

### 4.3 和静态工厂的区别

| | 静态工厂 | 实例工厂 |
|---|---------|---------|
| 方法修饰符 | 必须 `static` | 非 `static` |
| XML 属性 | `class` + `factory-method` | `factory-bean` + `factory-method` |
| 工厂能否有状态/依赖 | 不能 | **能** |
| Bean 数量 | 1 个（产品） | 2 个（工厂 + 产品） |

---

## 5. 方式四：`FactoryBean` 接口

### 5.1 代码

```java
public class UserServiceFactoryBean implements FactoryBean<UserService> {

    public UserServiceFactoryBean() {
        System.out.println("  [FactoryBean] 工厂 Bean 自己被创建");
    }

    @Override
    public UserService getObject() {              // ★ 真正造产品的地方
        System.out.println("  [FactoryBean] getObject() 被调用");
        UserServiceImpl s = new UserServiceImpl();
        s.setSource("FactoryBean 接口");
        return s;
    }

    @Override
    public Class<?> getObjectType() {             // 产品类型，用于类型匹配
        return UserService.class;
    }

    @Override
    public boolean isSingleton() {                // 产品是否单例
        return true;
    }
}
```

```xml
<bean id="byFactoryBean" class="com.example.inst.UserServiceFactoryBean"/>
```

用起来和普通 Bean 一模一样：

```java
UserService us = ctx.getBean("byFactoryBean", UserService.class);
```

### 5.2 真实输出（注意日志顺序）

```
  [new] UserServiceImpl() 无参构造被调用           ← ① 构造方法方式，启动时创建
  [static-factory] UserServiceFactory.createByStatic()
  [new] UserServiceImpl() 无参构造被调用
  [instance-factory] factory.createByInstance()
  [new] UserServiceImpl() 无参构造被调用
  [FactoryBean] 工厂 Bean 自己被创建               ← ② 只有「工厂」被创建了
---- getBean 结果 ----
hello 1 ← 由【构造方法（默认）】创建
hello 2 ← 由【静态工厂 factory-method】创建
hello 3 ← 由【实例工厂 factory-bean】创建
  [FactoryBean] getObject() 被调用                 ← ③ 第一次要产品时才调 getObject()
  [new] UserServiceImpl() 无参构造被调用
hello 4 ← 由【FactoryBean 接口】创建

---- FactoryBean 的 & 前缀 ----
  getBean("byFactoryBean")  -> com.example.inst.UserServiceImpl
  getBean("&byFactoryBean") -> com.example.inst.UserServiceFactoryBean
```

**两个关键事实：**

1. **`FactoryBean` 本身在容器启动时就被创建了**（日志②），但 `getObject()` 直到第一次 `getBean` 才调用（日志③）。所以产品的创建是**延迟**的。
   （例外：如果实现 `SmartFactoryBean#isEagerInit()` 返回 `true`，容器启动时就会调 `getObject()`。）
2. **`getBean("id")` 拿产品，`getBean("&id")` 拿工厂本身**。`&` 是 Spring 约定的"解引用"前缀，常量是 `BeanFactory.FACTORY_BEAN_PREFIX`。

### 5.3 `isSingleton()` 的影响

| 返回值 | 行为 |
|--------|------|
| `true`（默认） | 产品被缓存进 `factoryBeanObjectCache`，多次 `getBean` 返回同一个 |
| `false` | 每次 `getBean` 都调一次 `getObject()`，返回新对象 |

实测 `isSingleton() == false` 时，连续两次 `getBean("fb")` 得到的对象 `==` 为 `false`，并且控制台打印了两次 `getObject()`。

### 5.4 Spring 内部的经典应用

| 类 | 作用 |
|----|------|
| `SqlSessionFactoryBean` | MyBatis 的 `SqlSessionFactory`（组装 mybatis-config + mapper） |
| `MapperFactoryBean` | 为 Mapper 接口生成动态代理实现 |
| `ProxyFactoryBean` | AOP 代理对象 |
| `LocalSessionFactoryBean` | Hibernate 的 `SessionFactory` |
| `JndiObjectFactoryBean` | 从 JNDI 查找对象 |

这些场景的共同点：**目标对象的创建过程很复杂，而且目标类往往是第三方库的、你没法给它加构造参数**。用 `FactoryBean` 把这段"组装逻辑"封装起来，对外只暴露一个 `getObject()`。

---

## 6. `BeanFactory` 与 `FactoryBean` 的区别

这是面试高频题，名字只差一个词，含义完全不同。

| 对比项 | `BeanFactory` | `FactoryBean` |
|--------|--------------|---------------|
| 中文 | Bean **工厂**（容器） | **工厂** Bean |
| 本质 | IoC 容器的**顶层接口** | 容器里的**一个普通 Bean**，只是实现了特殊接口 |
| 作用范围 | 管理容器里**所有** Bean | 定制**某一个** Bean 的创建 |
| 它本身是不是 Bean | ❌ 不是，它就是容器 | ✅ 是，被 Spring 实例化并管理 |
| 典型实现 | `DefaultListableBeanFactory`、`ClassPathXmlApplicationContext` | `SqlSessionFactoryBean`、`MapperFactoryBean` |
| 怎么拿到 | 直接持有容器引用 | `getBean("x")` 拿产品；`getBean("&x")` 拿工厂 |
| 一句话 | 造**所有** Bean 的工厂 | 造**某一个** Bean 的工厂 |

记忆口诀：**`BeanFactory` 是工厂，`FactoryBean` 是产品（只不过这个产品自己也是个小工厂）。**

### 6.1 源码：`&` 是怎么生效的

```java
// AbstractBeanFactory#getObjectForBeanInstance
if (BeanFactoryUtils.isFactoryDereference(name)) {   // 名字以 & 开头
    return beanInstance;                              // 直接返回工厂本身
}
if (!(beanInstance instanceof FactoryBean)) {
    return beanInstance;                              // 普通 Bean，原样返回
}
// 是 FactoryBean 且名字不带 & → 取出产品
object = getObjectFromFactoryBean((FactoryBean<?>) beanInstance, beanName, !synthetic);
```

```java
// FactoryBeanRegistrySupport#getObjectFromFactoryBean（简化）
if (factory.isSingleton() && containsSingleton(beanName)) {
    synchronized (getSingletonMutex()) {
        Object object = this.factoryBeanObjectCache.get(beanName);
        if (object == null) {
            object = doGetObjectFromFactoryBean(factory, beanName);   // factory.getObject()
            this.factoryBeanObjectCache.put(beanName, object);        // 产品缓存在这里
        }
        return object;
    }
}
return doGetObjectFromFactoryBean(factory, beanName);                 // 非单例：每次都造
```

注意最后一行：**产品的缓存不在 `singletonObjects` 里，而在 `factoryBeanObjectCache` 里**。这也是为什么 `getBean("&x")` 能直接拿到工厂——工厂在 `singletonObjects`，产品在另一个缓存。

### 6.2 一个重要推论

`FactoryBean` 的"产品"**根本不经过 `createBeanInstance()`**。

```
getBean("byFactoryBean")
  └─ doGetBean
       └─ 创建的是 UserServiceFactoryBean 本身（走 createBeanInstance）
            └─ getObjectForBeanInstance  ← 在这里拦一道
                 └─ factory.getObject()  ← 产品在这里诞生，与反射无关
```

所以 `FactoryBean#getObject()` 里可以 `new`、可以查数据库、可以返回动态代理、甚至返回 `null`——容器都不管，它只负责把返回值交出去。

---

## 7. 实战：注入自定义 `Date`

这是 XML 时代非常经典的一个问题：**怎么往 Bean 里注入一个"指定日期"，而不是 `new Date()` 的当前时间？**

### 7.1 为什么直接写 `value` 不行

```xml
<bean id="holder" class="com.example.date.DateHolder">
    <property name="birthday" value="2000-01-01"/>   <!-- ❌ -->
</bean>
```

启动直接失败：

```
org.springframework.beans.ConversionNotSupportedException:
Failed to convert property value of type 'java.lang.String' to required type 'java.util.Date'
for property 'birthday';
nested exception is java.lang.IllegalStateException:
Cannot convert value of type 'java.lang.String' to required type 'java.util.Date'
for property 'birthday': no matching editors or conversion strategy found
```

原因：`value` 永远是字符串，Spring 内置的转换器覆盖了基本类型、枚举、`String`、`Class`、数组集合等，**但没有 String → Date**——因为日期格式没有统一约定（`2020-01-01`？`2020/01/01`？`01/01/2020`？）。

所以必须**告诉 Spring 怎么转**，有下面四种办法。

### 7.2 方案 A：实例工厂 + `SimpleDateFormat#parse`（最常用）

`SimpleDateFormat` 有个 `parse(String)` 方法，返回 `Date`——这正好是一个现成的"实例工厂"。

```xml
<!-- ① 格式化器本身作为 Bean，构造参数是日期格式 -->
<bean id="dateFormat" class="java.text.SimpleDateFormat">
    <constructor-arg value="yyyy-MM-dd"/>
</bean>

<!-- ② 让 dateFormat 这个 Bean 调 parse()，参数就是要注入的日期字符串 -->
<bean id="birthday" factory-bean="dateFormat" factory-method="parse">
    <constructor-arg value="2000-01-01"/>
</bean>
```

```xml
<!-- ③ 正常注入 -->
<bean id="holder" class="com.example.date.DateHolder">
    <property name="birthday" ref="birthday"/>
</bean>
```

真实输出：

```
birthday  = 2000-01-01 00:00:00
holder    = 2000-01-01 00:00:00
```

优点：**不用写任何 Java 代码**，纯 XML 就能搞定。
缺点：每个日期格式要配一个 `SimpleDateFormat` Bean；而且 `SimpleDateFormat` 不是线程安全的，如果这个 Bean 被多线程共用要小心（只在启动期解析的话没问题）。

> `parse` 还有一个重载 `parse(String, ParsePosition)`，Spring 会按 `<constructor-arg>` 的**个数和类型**去匹配，只写一个参数就精确命中单参版本。

### 7.3 方案 B：自定义静态工厂

```java
public class DateUtils {
    public static Date parse(String pattern, String text) throws ParseException {
        return new SimpleDateFormat(pattern).parse(text);
    }
}
```

```xml
<bean id="birthday2" class="com.example.date.DateUtils" factory-method="parse">
    <constructor-arg value="yyyy/MM/dd"/>
    <constructor-arg value="1999/12/31"/>
</bean>
```

真实输出：

```
birthday2 = 1999-12-31 00:00:00
```

优点：格式和值都写在 XML 里，一个 Bean 搞定；可以自己加缓存、处理异常、支持多格式。
缺点：要写一个工具类。

### 7.4 方案 C：`setTime` 直接给毫秒（能跑但别用）

`java.util.Date` 有 `setTime(long)`，所以：

```xml
<bean id="fixed" class="java.util.Date">
    <property name="time" value="946656000000"/>
</bean>
```

真实输出：

```
fixed = 2000-01-01 00:00:00
```

能跑，但 `946656000000` 这种数字**没人看得懂**，改起来还要换算，只适合临时验证。

### 7.5 方案 D：全局注册 `PropertyEditor`（一处配置，处处可用）

如果项目里到处都要写日期，最优雅的是注册一个**自定义类型转换器**，让 `value="2001-02-03"` 直接生效。

```java
public class DateEditorRegistrar implements PropertyEditorRegistrar {
    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor(Date.class,
                new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"), true));
    }
}
```

```xml
<!-- 全局注册，容器启动时生效 -->
<bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
    <property name="propertyEditorRegistrars">
        <list>
            <bean class="com.example.date.DateEditorRegistrar"/>
        </list>
    </property>
</bean>

<!-- 之后所有 Date 属性都能直接写字符串 -->
<bean id="holder" class="com.example.date.DateHolder">
    <property name="birthday" value="2001-02-03"/>
</bean>
```

真实输出：

```
注册编辑器后 holder = 2001-02-03 00:00:00
```

> ⚠️ **一个网上流传的错误写法**：把 `CustomDateEditor` 直接配在 `customEditors` 里：
>
> ```xml
> <property name="customEditors">
>     <map>
>         <entry key="java.util.Date" value="org.springframework.beans.propertyeditors.CustomDateEditor"/>
>     </map>
> </property>
> ```
>
> 启动会报 `NoSuchMethodException: CustomDateEditor.<init>()`。原因是 `setCustomEditors` 的签名是
> `Map<Class<?>, Class<? extends PropertyEditor>>`——它收的是 **Class**，Spring 会用**无参构造**去实例化它，而 `CustomDateEditor` 只有 `(DateFormat, boolean)` 这样的有参构造。
> 所以要传"已经配置好的实例"，只能走 `propertyEditorRegistrars`。

### 7.6 四种方案对比

| 方案 | 配置量 | 可读性 | 复用性 | 适用 |
|------|--------|--------|--------|------|
| A 实例工厂 + `parse` | 2 个 Bean | 高 | 每个日期一组 | 个别日期 |
| B 自定义静态工厂 | 1 个 Bean | 高 | 好 | 项目里少量日期 |
| C `setTime` 毫秒 | 1 个 Bean | **低** | 差 | 不推荐 |
| D 全局 PropertyEditor | 全局一次 | **最高** | 全局 | 大量日期配置 |

---

## 8. 四种方式对比与选型

| 方式 | 容器里存的是什么 | 谁 `new` 的产品 | 适合 |
|------|----------------|----------------|------|
| 构造方法 | 产品本身 | Spring（反射） | 普通类，能直接 `new` |
| 静态工厂 | 产品本身 | 你的 `static` 方法 | 已有静态工厂、想隐藏实现 |
| 实例工厂 | 产品本身 | 另一个 Bean 的实例方法 | 工厂需要注入依赖 |
| `FactoryBean` | **工厂**（产品在 `factoryBeanObjectCache`） | 你的 `getObject()` | 复杂初始化、代理对象、第三方类 |

**选型顺序：**

```
能直接 new 吗？
  ├─ 能 → 构造方法（99% 的情况，别再折腾）
  └─ 不能 / 不想
       ├─ 已有 static 工厂方法 → factory-method
       ├─ 工厂需要配置/依赖     → factory-bean
       └─ 组装逻辑复杂、要返回代理、类改不了 → FactoryBean
```

---

## 9. 源码：`createBeanInstance` 的三个分支

前三种方式最终都收敛到 `createBeanInstance()`：

```java
// AbstractAutowireCapableBeanFactory#createBeanInstance（简化）
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {

    // ① 有 factory-method → 交给工厂
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // ② 构造器已经解析过（缓存命中）→ 直接用上次的构造器和参数
    boolean resolved = false;
    if (mbd.resolvedConstructorOrFactoryMethod != null) { ... }

    // ③ 默认：无参构造
    return instantiateBean(beanName, mbd);
}
```

`instantiateUsingFactoryMethod` 的实际工作在 `ConstructorResolver` 里：

```java
// ConstructorResolver#instantiateUsingFactoryMethod（步骤简化）
// 1. 确定「工厂实例」
Object factoryBean = null;
if (mbd.getFactoryBeanName() != null) {
    factoryBean = this.beanFactory.getBean(mbd.getFactoryBeanName());   // 实例工厂：先拿工厂 Bean
} else {
    factoryBean = null;                                                 // 静态工厂：不需要实例
}

// 2. 找方法：按方法名 + 参数个数 + 参数类型匹配
Method factoryMethod = ClassUtils.getMethodIfAvailable(factoryClass, methodName, paramTypes);

// 3. 解析参数（和构造器注入共用一套 index / type / name 逻辑）
Object[] args = resolveArgs(...);

// 4. 反射调用
Object bean = factoryMethod.invoke(factoryBean, args);   // ★ 静态方法时 factoryBean 传 null
```

三个要点：

1. **`factory-bean` 非空时，会先 `getBean(factoryBeanName)`**——所以工厂 Bean 的依赖注入一定发生在产品创建之前。
2. **静态方法时 `invoke` 的第一个参数传 `null`**，这是 `Method.invoke` 对静态方法的约定。
3. **参数解析逻辑和 `<constructor-arg>` 完全一样**，所以 `index` / `type` / `name` 三种匹配方式在工厂方法上同样适用。

而 `FactoryBean` **不走这里**，它在 `getObjectForBeanInstance` 那一层被拦截（见 6.2）。

---

## 10. 常见坑

| 坑 | 现象 | 原因 / 解法 |
|----|------|------------|
| **静态工厂方法忘了写 `static`** | `No matching factory method found on class [...]: factory method 'createByInstance()'. Check that a method with the specified name exists and that it is static.` | 静态工厂要求 `static`；非静态的请改用 `factory-bean` |
| **`class` 写成了产品类** | 找不到 `factory-method` | `class` 必须是**工厂类** |
| **工厂方法有参数却没写 `<constructor-arg>`** | `UnsatisfiedDependencyException` / 参数个数不匹配 | 参数写全，注意顺序 |
| **以为 `<constructor-arg>` 是给产品构造器的** | 产品构造器没收到参数 | 有 `factory-method` 时，参数是给**工厂方法**的 |
| **`factory-bean` 的 id 写错** | `NoSuchBeanDefinitionException: No bean named 'xxx' available` | 检查 id 拼写，确认工厂 Bean 已注册 |
| **想拿工厂却忘了 `&`** | `ClassCastException` | `getBean("&x")` 才是工厂本身 |
| **给 `FactoryBean` 写 `<property>` 以为是配产品** | 产品没变化，工厂字段变了 | `<property>` 作用在**工厂**上（实测 `setPrefix` 在工厂里被调用） |
| **`FactoryBean` 的 `isSingleton()` 返回 `false`** | 每次 `getBean` 都是新对象 | 产品不缓存，按需返回；要复用就返回 `true` |
| **工厂方法抛异常** | `BeanCreationException`，根因被包在里面 | 看 `getCause()`，通常是工厂方法内部的业务异常 |
| **给 `FactoryBean` 的产品做类型强转** | `ClassCastException: ... cannot be cast to ...FactoryBean` | `getBean` 返回的是产品，不是工厂 |

---

## 附：本篇速查

```
四种实例化方式

① 构造方法（默认）
   <bean id="x" class="产品类"/>
   源码：createBeanInstance → instantiateBean → ctor.newInstance

② 静态工厂
   <bean id="x" class="工厂类" factory-method="静态方法"/>
   <constructor-arg> 传给的是「工厂方法」

③ 实例工厂
   <bean id="factory" class="工厂类"/>
   <bean id="x" factory-bean="factory" factory-method="实例方法"/>
   工厂可以先被注入依赖

④ FactoryBean
   <bean id="x" class="实现了FactoryBean的类"/>
   getBean("x")  → 产品（getObject 的返回）
   getBean("&x") → 工厂本身
   产品缓存在 factoryBeanObjectCache，不在 singletonObjects
   工厂启动时创建，getObject 首次 getBean 才调用

BeanFactory vs FactoryBean
   BeanFactory  = 容器（造所有 Bean 的工厂），本身不是 Bean
   FactoryBean  = 容器里的一个 Bean（造某一个 Bean 的工厂）

自定义 Date
   ❌ value="2000-01-01" → no matching editors or conversion strategy found
   ✅ factory-bean + SimpleDateFormat#parse
   ✅ 自定义静态工厂 DateUtils.parse
   ✅ 全局 PropertyEditorRegistrar（customEditors 收的是 Class，不能用有参构造的 CustomDateEditor）
```

---

> 回顾：[12 GoF 之工厂模式](12-GoF之工厂模式.md) · 返回 [索引](00-学习路线与索引.md)

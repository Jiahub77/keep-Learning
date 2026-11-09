# 03 · Spring 对 IoC 的实现：DI 依赖注入

> 上一篇：[02 控制反转 IoC](02-控制反转IoC.md) · 下一篇：[04 SOLID 五大设计原则](04-SOLID五大设计原则.md)
>
> **本篇只用 XML 配置**，不出现任何注解。注解只是把同样的信息换个地方写，底层机制完全一致。

---

## 目录

1. [IoC 容器的两个层次](#1-ioc-容器的两个层次)
2. [从容器启动到 Bean 可用：宏观流程](#2-从容器启动到-bean-可用宏观流程)
3. [XML 里的注入方式总览](#3-xml-里的注入方式总览)
4. [Set 注入：实现方式](#4-set-注入实现方式)
5. [Set 注入：原理](#5-set-注入原理)
6. [构造方法注入：实现方式](#6-构造方法注入实现方式)
7. [构造方法注入：原理](#7-构造方法注入原理)
8. [XML 参数匹配规则（index / type / name）](#8-xml-参数匹配规则index--type--name)
9. [自动装配 autowire](#9-自动装配-autowire)
10. [三级缓存与循环依赖](#10-三级缓存与循环依赖)
11. [两种注入方式对比与选型](#11-两种注入方式对比与选型)
12. [XML 配置的常见坑](#12-xml-配置的常见坑)

---

## 1. IoC 容器的两个层次

Spring 的 IoC 容器本质上是**一个管理 Bean 定义（BeanDefinition）和 Bean 实例的工厂**，分两个层次：

| 容器 | 接口 | 特点 |
|------|------|------|
| **BeanFactory** | `org.springframework.beans.factory.BeanFactory` | 最底层的容器，**懒加载**：调用 `getBean()` 时才实例化。功能最小，是"基础设施" |
| **ApplicationContext** | `org.springframework.context.ApplicationContext` | 面向应用，继承 BeanFactory，**启动时预实例化所有单例**，额外提供事件发布、国际化、资源加载、环境抽象等 |

### 1.1 常用实现类

```java
// ① 从 classpath 找 XML（最常用）
ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");

// ② 从磁盘路径找 XML
ApplicationContext ctx = new FileSystemXmlApplicationContext("/opt/config/applicationContext.xml");

// ③ 一次加载多个 XML（可变参数）
ApplicationContext ctx = new ClassPathXmlApplicationContext(
        "applicationContext-dao.xml",
        "applicationContext-service.xml");
```

### 1.2 容器内部最关键的两个数据结构

```java
// Bean 定义：Bean 的"图纸"，描述类名、作用域、是否懒加载、构造参数、属性值等
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

// 单例池：Bean 的"成品仓库"，一级缓存
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
```

> 理解这一点很重要：**Spring 并不是"帮你 new 对象"，而是"先读图纸（BeanDefinition），再按图纸造对象（反射），最后把成品放进单例池"**。所谓 DI，就是造对象的过程中按图纸去别的仓库里取零件。

### 1.3 一个 `<bean>` 标签被解析成了什么

```xml
<bean id="userService" class="com.example.service.UserService" scope="singleton" lazy-init="false">
    <property name="userDao" ref="userDao"/>
    <property name="timeout" value="3000"/>
</bean>
```

```java
// 解析后大致等价于（伪代码）
RootBeanDefinition bd = new RootBeanDefinition(UserService.class);
bd.setScope("singleton");              // 作用域
bd.setLazyInit(false);                 // 是否懒加载

MutablePropertyValues pvs = new MutablePropertyValues();
pvs.add("userDao", new RuntimeBeanReference("userDao"));   // ref → 引用，先存 id
pvs.add("timeout", new TypedStringValue("3000"));          // value → 字符串，稍后转换
bd.setPropertyValues(pvs);

beanDefinitionMap.put("userService", bd);
```

注意两个细节：

- `ref` 存的是 **`RuntimeBeanReference`（Bean 的 id）**，不是真实对象——真正解析要等到创建 Bean 时。
- `value="3000"` 存的是 **字符串**，转换成 `int` 也是等到注入时才做。

---

## 2. 从容器启动到 Bean 可用：宏观流程

```
① 加载配置
   applicationContext.xml
        │
        ▼
② 解析为 BeanDefinition，注册进 beanDefinitionMap
   XmlBeanDefinitionReader 解析 <bean> / <property> / <constructor-arg>
        │
        ▼
③ BeanFactoryPostProcessor 介入：修改 BeanDefinition
   （占位符替换、PropertySourcesPlaceholderConfigurer 等）
        │
        ▼
④ 实例化 instantiate ──→ createBeanInstance()      ← ★ 构造方法注入在这里发生
        │
        ▼
⑤ 暴露早期引用（三级缓存，用于解决循环依赖）
        │
        ▼
⑥ 属性填充 populate ───→ populateBean()            ← ★ Set 注入在这里发生
        │
        ▼
⑦ 初始化 initialize ───→ initializeBean()
   Aware 回调 → BeanPostProcessor#postProcessBeforeInitialization
   → InitializingBean#afterPropertiesSet / init-method
   → BeanPostProcessor#postProcessAfterInitialization（AOP 代理在此生成）
        │
        ▼
⑧ 放入单例池 singletonObjects，可被使用
        │
        ▼
⑨ 容器关闭：DisposableBean#destroy / destroy-method
```

对应的源码主干（`AbstractAutowireCapableBeanFactory#doCreateBean`，简化版）：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
    // ① 实例化：反射调用构造器 —— 构造方法注入在此完成
    BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);
    Object bean = instanceWrapper.getWrappedInstance();

    // ② 提前暴露"半成品"引用，供循环依赖时其它 Bean 引用（三级缓存）
    boolean earlySingletonExposure = mbd.isSingleton() && this.allowCircularReferences
            && isSingletonCurrentlyInCreation(beanName);
    if (earlySingletonExposure) {
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // ③ 属性填充：XML 的 <property> 在此完成
    populateBean(beanName, mbd, instanceWrapper);

    // ④ 初始化：Aware、初始化方法、BeanPostProcessor、AOP 代理
    Object exposedObject = initializeBean(beanName, bean, mbd);
    return exposedObject;
}
```

**记住这条主线，Set 注入和构造注入的区别就一目了然了：**

| 注入方式 | 发生在哪一步 | 顺序 |
|---------|-------------|------|
| **构造方法注入** | 步骤 ④ 实例化（`createBeanInstance`） | **先解析依赖，再创建对象** |
| **Set 注入** | 步骤 ⑥ 属性填充（`populateBean`） | **先创建对象（无参构造），再反射塞入依赖** |

### 2.1 什么时候创建 Bean

| 配置 | 创建时机 |
|------|---------|
| `scope="singleton"`（默认）+ 非懒加载 | 容器 `refresh()` 时**全部创建**（`preInstantiateSingletons`） |
| `lazy-init="true"` | 第一次 `getBean()` 时创建 |
| `<beans default-lazy-init="true">` | 全局懒加载，所有 Bean 都改成按需创建 |
| `scope="prototype"` | 每次 `getBean()` 都新建一个 |

```xml
<!-- 这个 Bean 到第一次 getBean 时才创建 -->
<bean id="heavyService" class="com.example.HeavyService" lazy-init="true"/>
```

---

## 3. XML 里的注入方式总览

```xml
<beans>
    <!-- ① Set 注入：无参构造 + setter -->
    <bean id="a" class="com.example.A">
        <property name="userDao" ref="userDao"/>
    </bean>

    <!-- ② 构造方法注入：带参构造 -->
    <bean id="b" class="com.example.B">
        <constructor-arg name="userDao" ref="userDao"/>
    </bean>

    <!-- ③ 自动装配：不写 property，容器自己找 -->
    <bean id="c" class="com.example.C" autowire="byName"/>

    <!-- ④ 工厂方法（不是注入，但也是创建 Bean 的方式） -->
    <bean id="d" class="com.example.Factory" factory-method="create"/>
</beans>
```

| 方式 | 标签 / 属性 | 前提 | 注入时机 |
|------|------------|------|---------|
| **Set 注入** | `<property name=".." ref/value=".."/>` | 无参构造 + `setXxx` 方法 | `populateBean`（实例化之后） |
| **构造方法注入** | `<constructor-arg index/name/type=".." ref/value=".."/>` | 对应的带参构造器 | `createBeanInstance`（实例化时） |
| **自动装配** | `<bean autowire="byName｜byType｜constructor"/>` | 无需 setter/构造参数声明，容器按规则匹配 | 取决于装配模式 |
| **工厂方法** | `<bean factory-method="..">` | 静态工厂 / 实例工厂 | 实例化时 |

---

## 4. Set 注入：实现方式

**思路：** 对象先用**无参构造**创建出来，再通过调用 **setter 方法**把依赖塞进去。

所以有两个硬性前提：

1. 类必须有**无参构造器**（不写构造器时编译器默认给一个）；
2. 每个要注入的属性必须有 **`setXxx` 方法**。

### 4.1 完整可运行示例

```java
// ===== UserDao.java =====
public interface UserDao {
    String findName(long id);
}

// ===== UserDaoMysqlImpl.java =====
public class UserDaoMysqlImpl implements UserDao {
    @Override
    public String findName(long id) { return "mysql-user-" + id; }
}

// ===== UserService.java：Set 注入的两个前提 =====
public class UserService {
    private UserDao userDao;
    private int timeout;

    public UserService() { }                          // 前提①：必须有无参构造

    public void setUserDao(UserDao userDao) {         // 前提②：必须有 setXxx 方法
        this.userDao = userDao;
    }
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String hello(long id) {
        return userDao.findName(id) + ", timeout=" + timeout;
    }
}
```

```xml
<!-- applicationContext.xml -->
<bean id="userDao" class="com.example.dao.UserDaoMysqlImpl"/>

<bean id="userService" class="com.example.service.UserService">
    <property name="userDao" ref="userDao"/>     <!-- ref：注入另一个 Bean -->
    <property name="timeout" value="3000"/>      <!-- value：注入字面量，自动转 int -->
</bean>
```

```java
public class Main {
    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
        UserService userService = ctx.getBean("userService", UserService.class);
        System.out.println(userService.hello(1L));   // 输出：mysql-user-1, timeout=3000
    }
}
```

### 4.2 `<property>` 能注入的所有类型

**① 字面量（字符串 / 数字 / 布尔）**

```xml
<property name="timeout" value="3000"/>
<property name="enabled" value="true"/>
<property name="rate" value="0.85"/>
<property name="name" value="张三"/>
```

`value` 永远是**字符串**，由 Spring 的 `TypeConverter` 在注入时转成目标类型。转不了就抛 `TypeMismatchException`。

**② 引用另一个 Bean**

```xml
<property name="userDao" ref="userDao"/>
<!-- 等价写法，老项目里常见 -->
<property name="userDao"><ref bean="userDao"/></property>
```

**③ null 值**

```xml
<property name="remark"><null/></property>
```

> 注意：只写 `<property name="remark"/>`（没有 value/ref）表示"不动这个属性"，而不是"设为 null"。Spring 4 之前还有 `value=""` 和 `null` 的混淆问题，现在明确用 `<null/>`。

**④ 内部 Bean（匿名，不能被外部引用）**

```xml
<bean id="orderService" class="com.example.service.OrderService">
    <property name="payClient">
        <bean class="com.example.pay.AlipayClient">
            <property name="appId" value="2021000000"/>
        </bean>
    </property>
</bean>
```

**⑤ 集合：list / set / map / props / array**

```xml
<property name="handlers">
    <list>
        <ref bean="smsHandler"/>
        <ref bean="emailHandler"/>
        <bean class="com.example.handler.LogHandler"/>
    </list>
</property>

<property name="tags">
    <set>
        <value>vip</value>
        <value>new</value>
    </set>
</property>

<property name="config">
    <map>
        <entry key="timeout" value="3000"/>
        <entry key="retry" value="3"/>
        <entry key-ref="userDao" value-ref="userService"/>
    </map>
</property>

<property name="jdbcProps">
    <props>
        <prop key="driver">com.mysql.cj.jdbc.Driver</prop>
        <prop key="url">jdbc:mysql://localhost:3306/demo</prop>
    </props>
</property>

<property name="ports">
    <array>
        <value>8080</value>
        <value>8081</value>
    </array>
</property>
```

> `<props>` 的 key 和 value 都只能是 String，对应 `java.util.Properties`；`<map>` 的 key/value 可以是任意类型。

**⑥ 级联属性（注入到属性的属性里）**

```xml
<bean id="address" class="com.example.Address"/>

<bean id="user" class="com.example.User">
    <!-- 等价于 user.getAddress().setCity("杭州") -->
    <property name="address.city" value="杭州"/>
</bean>
```

> 级联属性有个坑：`address` 必须是**已经初始化好的对象**（上面 `user` 里的 `address` 字段得先 `new` 出来），否则报 `NullValueInNestedPathException`。

**⑦ p 命名空间（纯 XML 时代的简洁写法）**

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:p="http://www.springframework.org/schema/p"
       xsi:schemaLocation="...">

    <!-- p:属性名="字面量"        p:属性名-ref="Bean的id" -->
    <bean id="userService" class="com.example.service.UserService"
          p:userDao-ref="userDao"
          p:timeout="3000"/>
</beans>
```

---

### 4.3 util 命名空间：把集合抽成可复用的 Bean

前面 ⑤ 里的 `<list>`、`<map>` 都是**内联**的——只能给当前这个 `<property>` 用。如果两个 Bean 需要同一个集合，就得把内容复制两遍，改一处还得改两处。

`util` 命名空间解决的就是这个问题：**把集合提升为一个独立的 Bean，然后到处 `ref` 它**。

**① 声明命名空间**

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:util="http://www.springframework.org/schema/util"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.springframework.org/schema/util
           https://www.springframework.org/schema/util/spring-util.xsd">
```

**② util 命名空间的六个标签**

| 标签 | 产出的 Bean 类型 | 说明 |
|------|----------------|------|
| `<util:list>` | `java.util.List` | 可指定实现类 `list-class` |
| `<util:set>` | `java.util.Set` | 可指定实现类 `set-class` |
| `<util:map>` | `java.util.Map` | 可指定实现类 `map-class` |
| `<util:properties>` | `java.util.Properties` | 从外部 `.properties` 文件加载 |
| `<util:constant>` | 任意常量值 | 引用某个类的 `static final` 字段 |
| `<util:property-path>` | 某个 Bean 的属性值 | 把别的 Bean 的属性单独暴露成一个 Bean |

**③ 可复用集合：一次定义，多处引用**

```xml
<!-- 定义一次 -->
<util:list id="allHandlers" list-class="java.util.LinkedList">
    <ref bean="smsHandler"/>
    <ref bean="emailHandler"/>
    <value>fallback</value>
</util:list>

<util:map id="retryConfig" map-class="java.util.TreeMap">
    <entry key="max" value="3"/>
    <entry key="interval" value="1000"/>
</util:map>

<util:set id="whiteList" set-class="java.util.LinkedHashSet">
    <value>127.0.0.1</value>
    <value>192.168.1.1</value>
</util:set>

<!-- 多处复用 -->
<bean id="notifyService" class="com.example.NotifyService">
    <property name="handlers" ref="allHandlers"/>
</bean>

<bean id="auditService" class="com.example.AuditService">
    <property name="handlers" ref="allHandlers"/>   <!-- 同一个集合 Bean -->
</bean>
```

**内联 vs util 的区别：**

```
内联 <list>
    <property name="handlers">
        <list>…</list>          ← 写死在 property 里，匿名
    </property>
    只能用一次，想复用只能复制内容，还拿不到集合本身的引用

<util:list id="allHandlers">
    提升为独立 Bean，有 id
    ✅ 可以 ref 给任意多个 Bean
    ✅ 可以被 getBean("allHandlers", List.class) 取到
    ✅ 可以指定实现类（LinkedList / TreeMap / LinkedHashSet）
    ✅ 可以加 depends-on / lazy-init 等 Bean 属性
```

> 注意：`<util:list>` 里的元素写法和内联 `<list>` 完全一样——`<value>`、`<ref bean>`、`<bean>`、`<null/>` 都能用。

**④ `<util:properties>`：读外部配置文件（XML 时代最常用的一个）**

`jdbc.properties`：

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/demo
jdbc.username=root
jdbc.password=123456
```

```xml
<!-- 把整个 properties 文件读成一个 Properties Bean -->
<util:properties id="jdbcProps" location="classpath:jdbc.properties"/>
```

取值有两种写法，**机制完全不同**：

```xml
<!-- 写法一：SpEL 表达式 #{...}，直接从 Bean 的属性/键取值 -->
<bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
    <property name="driverClassName" value="#{jdbcProps['jdbc.driver']}"/>
    <property name="jdbcUrl"         value="#{jdbcProps['jdbc.url']}"/>
    <property name="username"        value="#{jdbcProps['jdbc.username']}"/>
    <property name="password"        value="#{jdbcProps['jdbc.password']}"/>
</bean>
```

```xml
<!-- 写法二：占位符 ${...}，需要先注册占位符解析器（context 命名空间） -->
<context:property-placeholder location="classpath:jdbc.properties"/>

<bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
    <property name="driverClassName" value="${jdbc.driver}"/>
    <property name="jdbcUrl"         value="${jdbc.url}"/>
</bean>
```

| | `#{...}` SpEL | `${...}` 占位符 |
|---|---|---|
| 本质 | 运行时**表达式求值**，可以调方法、做运算 | 解析 BeanDefinition 阶段**文本替换** |
| 依赖 | `spring-expression`（引 context 时自带） | `PropertySourcesPlaceholderConfigurer` |
| 能否引用 Bean | ✅ `#{userService.userDao}` | ❌ 只能读配置 |
| 能否做运算 | ✅ `#{${pool.size} * 2}` | ❌ |
| 典型用途 | 取某个 Bean 的属性、简单计算 | 数据库连接、开关配置 |

> 两者可以嵌套：`value="#{${pool.max} * 2}"` —— 先替换占位符，再算 SpEL。

**⑤ `<util:constant>`：引用静态常量**

```xml
<!-- 等价于 Constants.MAX_SIZE -->
<util:constant id="maxSize" static-field="com.example.Constants.MAX_SIZE"/>

<bean id="pool" class="com.example.Pool">
    <property name="size" ref="maxSize"/>
</bean>
```

好处：常量改了，XML 不用跟着改。

**⑥ `<util:property-path>`：把别的 Bean 的属性单独暴露出来**

```xml
<bean id="userService" class="com.example.service.UserService">
    <property name="userDao" ref="userDao"/>
</bean>

<!-- 等价于 userService.getUserDao()，作为一个独立 Bean -->
<util:property-path id="daoFromService" path="userService.userDao"/>

<bean id="reportService" class="com.example.ReportService">
    <property name="userDao" ref="daoFromService"/>
</bean>
```

用得不多，但看老项目代码时会遇到。

**⑦ 什么时候该用 util**

| 场景 | 选择 |
|------|------|
| 集合只给一个 Bean 用 | 内联 `<list>` 就够了，别多写一个 Bean |
| 集合要给多个 Bean 共用 | `<util:list>` / `<util:map>` |
| 需要指定集合实现类（`LinkedList` / `TreeMap` / `LinkedHashSet`） | `<util:*>` 的 `list-class` / `map-class` / `set-class` |
| 需要从配置文件加载键值对 | `<util:properties>` |
| 要引用别的类的静态常量 | `<util:constant>` |
| 要把别的 Bean 的属性当 Bean 用 | `<util:property-path>` |

---

## 5. Set 注入：原理

**一句话原理：先造空壳，再反射填坑。**

```
第 1 步：反射无参构造，造出一个"空壳"        第 2 步：反射调用 setter，把依赖塞进去
┌────────────────────────┐                  ┌────────────────────────┐
│ new UserService()      │                  │ setUserDao(dao)        │
│ ┌────────────────────┐ │   ──────────→    │ ┌────────────────────┐ │
│ │ userDao = null     │ │                  │ │ userDao = dao      │ │
│ └────────────────────┘ │                  │ └────────────────────┘ │
└────────────────────────┘                  └────────────────────────┘
   发生在 createBeanInstance()                 发生在 populateBean()
```

### 5.1 源码链路

```
AbstractAutowireCapableBeanFactory#populateBean(beanName, mbd, bw)
    │
    ├─ 0. 前置检查：bw 是否为 InstantiationAwareBeanPostProcessor
    │      （XML 场景下一般不返回 false，继续往下）
    │
    ├─ 1. 取出 BeanDefinition 里的 PropertyValues（就是 XML 的 <property> 集合）
    │      MutablePropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
    │
    ├─ 2. 若 BeanDefinition 的 autowireMode 是 BY_NAME / BY_TYPE
    │      → autowireByName() / autowireByType() 先把自动装配的属性补进 pvs
    │
    ├─ 3. 若有属性要注入 → applyPropertyValues(beanName, mbd, bw, pvs)   ★ 核心
    │      │
    │      ├─ 3.1 遍历每个 PropertyValue，用 BeanDefinitionValueResolver 解析原始值
    │      │      resolveValueIfNecessary(beanName, mbd, pv.getName(), pv.getValue())
    │      │        · value="3000"        → TypedStringValue → 仍是字符串，待转换
    │      │        · ref="userDao"       → RuntimeBeanReference
    │      │                                → getBean("userDao")        ← 递归创建依赖！
    │      │        · <list>…</list>      → ManagedList → 逐个解析成 List<Object>
    │      │        · 内部 <bean>         → 先创建这个匿名 Bean
    │      │
    │      ├─ 3.2 注意：这一步只是"把 ref 解析成对象"，类型转换还没做
    │      │      （"3000" 仍是 String，转换发生在 3.3 的 processLocalProperty 里）
    │      │
    │      └─ 3.3 bw.setPropertyValues(pvs)      // bw 是 BeanWrapperImpl
    │             └─ AbstractPropertyAccessor#setPropertyValues
    │                  └─ AbstractNestablePropertyAccessor#setPropertyValue(pv)
    │                       └─ processLocalProperty(tokens, pv)
    │                            │  tokens = "userDao"（支持 "address.city" 这种嵌套路径）
    │                            ├─ getPropertyHandler(name)
    │                            │     └─ CachedIntrospectionResults（java.beans.Introspector）
    │                            │          把 UserService 的 PropertyDescriptor 缓存起来
    │                            │          → 找到 writeMethod = setUserDao(UserDao)
    │                            ├─ convertForProperty(value)   类型转换
    │                            └─ ph.setValue(value)
    │                                  └─ Method.invoke(getWrappedInstance(), value)   ★ 反射调用
    │
    └─ 4. 注册依赖关系（供销毁顺序、循环依赖检测用）
           registerDependentBeans / registerDisposableBeanIfNecessary
```

### 5.2 三个必须记住的点

**① `name="userDao"` 凭什么能对上 `setUserDao`？**

靠的是 **JavaBeans 命名约定**，不是注解、也不是字符串硬匹配：

```java
// java.beans.Introspector 把 setUserDao 识别成属性 "userDao"
BeanInfo info = Introspector.getBeanInfo(UserService.class);
// PropertyDescriptor: name = "userDao", writeMethod = setUserDao(UserDao)
```

所以：

- `name="userDao"` ✅ 对应 `setUserDao`
- `name="UserDao"` ❌ 大小写敏感，报 `NotWritablePropertyException`
- `name="userdao"` ❌ 同上
- 没有 `setUserDao` 方法 ❌ 同样报 `NotWritablePropertyException`

**② `ref` 的解析是"递归 getBean"，不是提前注入**

`applyPropertyValues` 遇到 `ref="userDao"` 时会去调 `getBean("userDao")`。如果 `userDao` 还没创建，就现场创建——这解释了为什么 Set 注入能解决循环依赖（见第 10 节）。

**③ 注入时机在"实例化之后、初始化之前"**

```java
public class DemoService {
    private UserDao userDao;

    public void setUserDao(UserDao userDao) { this.userDao = userDao; }

    public DemoService() {
        System.out.println(userDao);      // null —— 构造时属性还没填充
    }

    // init-method 里就有值了，因为 populateBean 已经跑完
    public void init() {
        System.out.println(userDao);      // 有值
    }
}
```

```xml
<bean id="demoService" class="com.example.DemoService" init-method="init">
    <property name="userDao" ref="userDao"/>
</bean>
```

**两种注入的时间轴对比（记住这张图就够了）：**

```
Set 注入：
   [无参实例化] ──→ [populateBean 注入] ──→ [initializeBean] ──→ 可用
        │                  │
        │                  └─ 依赖在这里才被塞进去
        └─ 此刻对象已存在，但 userDao = null（"空壳"状态）

构造注入：
   [解析依赖 getBean] ──→ [newInstance] ──→ [initializeBean] ──→ 可用
            │                  │
            │                  └─ 对象一出生就是完整的，没有空壳阶段
            └─ 依赖先于对象存在（所以解不了循环依赖）
```

---

## 6. 构造方法注入：实现方式

**思路：** 在**创建对象的那一刻**就把依赖作为构造参数传进去。对象一旦创建出来就是**完整可用**的。

### 6.1 完整可运行示例

```java
// ===== 只有带参构造，没有 setter，字段可以 final =====
public class OrderService {
    private final UserDao userDao;
    private final PayClient payClient;

    public OrderService(UserDao userDao, PayClient payClient) {
        this.userDao = userDao;         // 构造器执行时依赖一定非 null
        this.payClient = payClient;
    }

    public String submit(long userId, int cents) {
        return userDao.findName(userId) + " | " + payClient.pay(userId, cents);
    }
}
```

```xml
<bean id="userDao" class="com.example.dao.UserDaoMysqlImpl"/>
<bean id="payClient" class="com.example.pay.AlipayClient"/>

<bean id="orderService" class="com.example.service.OrderService">
    <!-- 按参数名匹配 -->
    <constructor-arg name="userDao" ref="userDao"/>
    <constructor-arg name="payClient" ref="payClient"/>
</bean>
```

```java
ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
OrderService orderService = ctx.getBean("orderService", OrderService.class);
System.out.println(orderService.submit(1L, 9900));   // 输出：mysql-user-1 | alipay#1:9900
```

### 6.2 `<constructor-arg>` 的三种匹配方式

```xml
<!-- ① 按参数名（可读性最好，但依赖编译时的参数名信息，见第 8 节） -->
<constructor-arg name="userDao" ref="userDao"/>
<constructor-arg name="payClient" ref="payClient"/>

<!-- ② 按位置下标（从 0 开始，最可靠） -->
<constructor-arg index="0" ref="userDao"/>
<constructor-arg index="1" ref="payClient"/>

<!-- ③ 按参数类型（同类型有多个参数时不可靠） -->
<constructor-arg type="com.example.dao.UserDao" ref="userDao"/>
<constructor-arg type="com.example.pay.PayClient" ref="payClient"/>
```

### 6.3 字面量参数

```xml
<bean id="dataSource" class="com.example.DataSource">
    <constructor-arg index="0" value="jdbc:mysql://localhost:3306/demo"/>
    <constructor-arg index="1" value="root"/>
    <constructor-arg index="2" value="123456"/>
    <constructor-arg index="3" value="5"/>       <!-- String "5" → int 5，自动转换 -->
</bean>
```

### 6.4 集合参数

```xml
<bean id="batchJob" class="com.example.BatchJob">
    <constructor-arg index="0">
        <list>
            <value>taskA</value>
            <value>taskB</value>
        </list>
    </constructor-arg>
</bean>
```

### 6.5 c 命名空间（构造注入的简洁写法）

```xml
<beans xmlns:c="http://www.springframework.org/schema/c" ...>

    <!-- c:参数名="字面量"   c:参数名-ref="Bean的id" -->
    <bean id="orderService" class="com.example.service.OrderService"
          c:userDao-ref="userDao"
          c:payClient-ref="payClient"/>

    <!-- 用下标时写 c:_0-ref，因为 XML 属性名不能以数字开头 -->
    <bean id="dataSource" class="com.example.DataSource"
          c:_0="jdbc:mysql://localhost:3306/demo"
          c:_1="root"
          c:_2="123456"/>
</beans>
```

### 6.6 特点

- 依赖**强制**：不传就创建不出来，天然避免"依赖漏注入"；
- 字段可以声明为 `final`，**不可变**，线程安全友好；
- 对象**创建即完整**，不存在中间状态；
- 缺点：构造参数多时 XML 冗长；**无法解决构造器层面的循环依赖**（见第 10 节）。

---

## 7. 构造方法注入：原理

构造注入发生在 `createBeanInstance()`，核心是 **`ConstructorResolver` 挑选构造器 + 逐个解析参数 + 反射 `newInstance`**。

### 7.1 先看分支条件：什么时候走构造注入

```java
// AbstractAutowireCapableBeanFactory#createBeanInstance（简化）
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    // ① 有 Supplier 配置 → 直接调 Supplier.get()
    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }

    // ② 有 factory-method（静态/实例工厂）→ 走工厂路线，不是构造注入
    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // ③ 关键判断：以下任一条件成立就走 autowireConstructor
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null
            || mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR
            || mbd.hasConstructorArgumentValues()          // ★ XML 配了 <constructor-arg>
            || !ObjectUtils.isEmpty(args)) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // ④ 都不满足 → 无参反射实例化（Set 注入走的就是这里）
    return instantiateBean(beanName, mbd);
}
```

> **XML 场景下走的就是 `mbd.hasConstructorArgumentValues()` 这个条件**——只要写了 `<constructor-arg>`，就进 `autowireConstructor`。
> （`determineConstructorsFromBeanPostProcessors` 是为注解 `@Autowired` 构造器准备的，没有注解时返回 `null`，不影响。）

### 7.2 源码链路

```
ConstructorResolver#autowireConstructor(beanName, mbd, chosenCtors, explicitArgs)
    │
    ├─ 1. 准备构造参数
    │      ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
    │      · indexedArgumentValues：Map<Integer, ValueHolder>   ← 来自 index="0"/"1"
    │      · genericArgumentValues：List<ValueHolder>           ← 来自不带 index 的
    │      minNrOfArgs = cargs.getArgumentCount() + explicitArgs.length
    │
    ├─ 2. 确定候选构造器列表
    │      · 若 chosenCtors 为空（XML 场景）→ beanClass.getDeclaredConstructors()
    │      · 排序：public 优先，参数多者优先（"贪婪"策略）
    │
    ├─ 3. 逐个构造器尝试匹配（从上到下，第一个成功的胜出）
    │      for (Constructor<?> candidate : candidates) {
    │          ├─ 参数个数必须 >= minNrOfArgs，否则跳过
    │          ├─ 参数个数 > 5 → 用 SmartInstantiationAwareBeanPostProcessor 精确匹配
    │          └─ resolveConstructorArguments(beanName, mbd, bw, cargs, candidate)
    │               for (每个参数 i) {
    │                   ValueHolder holder = cargs.getArgumentValue(i, paramType, ...)
    │                     · 先按 index=i 找（indexedArgumentValues）
    │                     · 再按 type 找（genericArgumentValues 里类型匹配的）
    │                   if (holder == null && autowireMode == AUTOWIRE_CONSTRUCTOR)
    │                       → resolveAutowiredArgument(...)  ← 自动装配模式才按类型找 Bean
    │                   args[i] = resolveValueIfNecessary(...)   // ref → getBean()，递归
    │                   类型转换：TypeConverter.convertIfNecessary(...)
    │               }
    │               若某参数匹配不上 → 换下一个构造器
    │      }
    │
    ├─ 4. 全部参数就绪 → 得到一个 ArgumentsHolder（含 args 数组）
    │
    └─ 5. instantiate(beanName, mbd, constructorToUse, args)
           → InstantiationStrategy#instantiate
              （默认 CglibSubclassingInstantiationStrategy，无方法注入时退化为 SimpleInstantiationStrategy）
              → BeanUtils.instantiateClass(ctor, args)
                 → ReflectionUtils.makeAccessible(ctor)     // 私有构造器也能调
                 → ctor.newInstance(args)                   ★ 反射创建实例
```

### 7.3 三个容易踩坑的细节

**① XML 里 `name="userDao"` 要能匹配，编译时必须有参数名**

`ConstructorResolver` 要按名匹配，就必须知道每个参数"叫什么"。名字来源：

| 来源 | 说明 |
|------|------|
| 编译参数 `-parameters` | `StandardReflectionParameterNameDiscoverer` 读取；**Maven 的 `maven-compiler-plugin` 需显式加 `<parameters>true</parameters>`** |
| 字节码 LocalVariableTable（`-g`） | Spring 5 可用；**Spring 6 已删除 `LocalVariableTableParameterNameDiscoverer`**，只能靠 `-parameters` |

> **实践建议：XML 里优先用 `index`，别用 `name`。** `index` 不依赖任何编译参数，永远不会因为"编译环境换了"而报错。

**② 依赖链是"由内向外"逐层完成的**

```java
// A 构造注入 B，B 构造注入 C，C 无依赖
getBean("A")
 └─ createBeanInstance(A)                        ← A 还没造出来
      └─ 解析构造参数 B → getBean("B")            ← 先去造 B
           └─ createBeanInstance(B)              ← B 也还没造出来
                └─ 解析构造参数 C → getBean("C")
                     └─ createBeanInstance(C)    ← C 无依赖，直接实例化
                     └─ populateBean(C) / initializeBean(C) → 进单例池
                └─ ctor.newInstance(c)           ← C 就绪后，才构造 B
           └─ populateBean(B) / initializeBean(B) → 进单例池
      └─ ctor.newInstance(b)                     ← B 就绪后，才构造 A
 └─ populateBean(A) / initializeBean(A) → 进单例池
```

链的末端必须是**无依赖的 Bean**（或已存在于单例池的 Bean）。如果末端绕回来指向自己，就是循环依赖——见第 10 节。

**③ 最终是谁 new 的？`InstantiationStrategy`**

`instantiate()` 并不直接反射，而是委托给实例化策略：

```
instantiateBean(beanName, mbd)
   └─ getInstantiationStrategy()                 // 默认 CglibSubclassingInstantiationStrategy
        └─ instantiate(mbd, beanName, owner)
             ├─ 没有 lookup-method / replace-method 覆盖
             │    → SimpleInstantiationStrategy
             │       → BeanUtils.instantiateClass(ctor, args)
             │          → ReflectionUtils.makeAccessible(ctor)
             │          → ctor.newInstance(args)                ★ 反射
             │
             └─ 有方法注入（lookup-method / replace-method）
                  → CGLIB 生成子类，重写被注入的方法
```

所以"构造注入 = 反射 `newInstance`"在绝大多数场景成立，只有方法注入（`lookup-method`）才会切换成 CGLIB 子类化。

---

## 8. XML 参数匹配规则（index / type / name）

### 8.1 三种方式怎么选

| 方式 | 写法 | 优点 | 缺点 |
|------|------|------|------|
| **index** | `<constructor-arg index="0" .../>` | 精确、不依赖编译参数、顺序明确 | 参数一多要数下标 |
| **type** | `<constructor-arg type="java.lang.String" .../>` | 能区分同名不同类型的参数 | **同类型多个参数时无法区分** |
| **name** | `<constructor-arg name="userDao" .../>` | 可读性最好 | 依赖 `-parameters` 编译参数；Spring 6 无调试信息兜底 |

**优先级：** `index` > `type` > `name`。实际匹配时 `ConstructorArgumentValues#getArgumentValue` 先按 index 找，找不到再按 type 在 generic 列表里找。

### 8.2 歧义场景演示

```java
public class ReportService {
    private final String title;
    private final String author;

    public ReportService(String title, String author) {
        this.title = title;
        this.author = author;
    }
}
```

```xml
<!-- ❌ 两个都是 String，用 type 区分不了，只能按顺序猜 -->
<bean id="reportService" class="com.example.ReportService">
    <constructor-arg type="java.lang.String" value="月报"/>
    <constructor-arg type="java.lang.String" value="张三"/>
</bean>
<!-- 这种写法能跑，但完全依赖书写顺序，很容易写反 -->

<!-- ✅ 用 index 明确顺序 -->
<bean id="reportService" class="com.example.ReportService">
    <constructor-arg index="0" value="月报"/>
    <constructor-arg index="1" value="张三"/>
</bean>

<!-- ✅ 或者用 name（前提：编译时加了 -parameters） -->
<bean id="reportService" class="com.example.ReportService">
    <constructor-arg name="title" value="月报"/>
    <constructor-arg name="author" value="张三"/>
</bean>
```

### 8.3 匹配不上会怎样

| 情况 | 报错 |
|------|------|
| 参数个数对不上 | `UnsatisfiedDependencyException`（提示 Could not resolve matching constructor） |
| 类型转不过去 | `UnsatisfiedDependencyException`，cause 是 `TypeMismatchException` |
| `ref` 指向不存在的 Bean | `NoSuchBeanDefinitionException` |
| 构造器里有集合参数但没配 | 参数为 `null`（集合类型不会自动给空集合） |

---

## 9. 自动装配 autowire

XML 时代还有个"偷懒"的办法：**不写 `<property>`，让容器按规则自己找**。

```xml
<bean id="userService" class="com.example.service.UserService" autowire="byName"/>
```

### 9.1 四种模式

| 模式 | 规则 | 找不到时 | 多个候选时 |
|------|------|---------|-----------|
| **no**（默认） | 不自动装配，必须显式写 `<property>` | — | — |
| **byName** | 按属性名找同名 Bean（`setUserDao` → 找 id 为 `userDao` 的 Bean） | **静默跳过**，属性保持 null | 不存在此问题（名字唯一） |
| **byType** | 按属性的类型找 Bean | 抛 `NoSuchBeanDefinitionException` | 抛 `NoUniqueBeanDefinitionException` |
| **constructor** | 对构造参数按类型自动装配（类似 byType，但作用于构造器） | 抛异常 | 抛异常 |

```xml
<!-- byName：属性名 userDao → 找 id="userDao" -->
<bean id="userDao" class="com.example.dao.UserDaoMysqlImpl"/>
<bean id="userService" class="com.example.service.UserService" autowire="byName"/>

<!-- byType：按 UserDao 类型找，容器里只能有一个 UserDao 实现 -->
<bean id="userService2" class="com.example.service.UserService" autowire="byType"/>

<!-- constructor：自动装配构造参数 -->
<bean id="orderService" class="com.example.service.OrderService" autowire="constructor"/>
```

### 9.2 全局默认

```xml
<!-- 整个 XML 默认按名字自动装配，个别 Bean 可覆盖 -->
<beans default-autowire="byName">
    <bean id="userDao" class="com.example.dao.UserDaoMysqlImpl"/>
    <bean id="userService" class="com.example.service.UserService"/>

    <!-- 这个 Bean 不用自动装配 -->
    <bean id="specialService" class="com.example.SpecialService" autowire="no"/>
</beans>
```

### 9.3 为什么实际项目很少用 autowire

| 问题 | 说明 |
|------|------|
| **依赖关系不直观** | 看 XML 完全不知道 `UserService` 依赖了谁，得去翻 Java 代码 |
| **byName 静默失败** | 名字写错（`userdao` vs `userDao`）不报错，运行时才 NPE，非常难查 |
| **byType 太脆** | 一旦多了一个同类型实现（比如加了 Oracle 版），全工程启动失败 |
| **无法注入字面量** | `timeout="3000"` 这种只能显式写 `<property>` |

> **结论：** 学习和中小项目用**显式 `<property>` / `<constructor-arg>`**，依赖关系一目了然。`autowire` 知道有这回事就行，遇到老项目能看懂即可。

---

## 10. 三级缓存与循环依赖

**什么是循环依赖：** A 依赖 B，B 又依赖 A。

```java
public class AService {
    private BService bService;
    public void setBService(BService bService) { this.bService = bService; }   // A → B
}

public class BService {
    private AService aService;
    public void setAService(AService aService) { this.aService = aService; }   // B → A
}
```

```xml
<!-- Set 注入的循环依赖：能启动成功 -->
<bean id="aService" class="com.example.AService">
    <property name="bService" ref="bService"/>
</bean>
<bean id="bService" class="com.example.BService">
    <property name="aService" ref="aService"/>
</bean>
```

### 10.1 三级缓存

| 缓存 | 字段 | 存放内容 | 作用 |
|------|------|---------|------|
| 一级 | `singletonObjects` | 完整初始化好的单例 Bean | 日常 `getBean` 直接命中 |
| 二级 | `earlySingletonObjects` | 已实例化但未完成属性填充的**早期引用** | 避免重复执行三级缓存的工厂、保证同一引用 |
| 三级 | `singletonFactories` | `ObjectFactory`（Lambda，可生成早期引用/AOP 代理） | 延迟决定是否需要提前生成代理对象 |

### 10.2 为什么 Set 注入能解决

```
创建 A：
  ① createBeanInstance(A)      → A 的无参实例 a（bService 字段还是 null）
  ② addSingletonFactory(A, () -> getEarlyBeanReference(A))   ← 把 a 的"工厂"放进三级缓存
  ③ populateBean(A) → 遇到 ref="bService" → getBean(B)
        │
        └─ 创建 B：
             ① 实例化 B
             ② B 进三级缓存
             ③ populateBean(B) → 遇到 ref="aService" → getSingleton(A, allowEarlyReference=true)
                   → 一级没有 → 二级没有 → 三级命中工厂
                   → 执行工厂拿到 a 的早期引用 → 放入二级缓存 → 返回给 B
             ④ B 初始化完成，进一级缓存
        │
  ④ B 拿到后，注入 A 的 bService 字段
  ⑤ A 初始化完成，进一级缓存（同时清理二、三级缓存）
```

关键点：**A 能"提前暴露引用"，是因为它已经实例化完成（对象在堆上有了地址），只是属性还没填**。B 拿到的就是这个"半成品"引用，等 A 后续填充完属性，B 持有的引用自然也就完整了（同一个对象）。

### 10.3 为什么构造方法注入解决不了

```xml
<!-- 构造注入的循环依赖：启动直接失败 -->
<bean id="aService" class="com.example.AService">
    <constructor-arg ref="bService"/>
</bean>
<bean id="bService" class="com.example.BService">
    <constructor-arg ref="aService"/>
</bean>
```

```
创建 A：
  ① createBeanInstance(A) → 需要先解析构造参数 B → getBean(B)
        └─ 创建 B：
             ① createBeanInstance(B) → 需要先解析构造参数 A → getBean(A)
                   → A 还在创建中，且 A 尚未实例化（连半成品都没有）
                   → 无法提前暴露引用
                   → 抛 BeanCurrentlyInCreationException
```

一句话：**构造注入要求"依赖先于对象存在"，而循环依赖要求"对象先于依赖存在"，逻辑上直接矛盾。**

### 10.4 三种解法

```xml
<!-- 解法 1：改成 Set 注入（能跑，但不推荐——只是绕开了问题） -->
<bean id="aService" class="com.example.AService">
    <property name="bService" ref="bService"/>
</bean>

<!-- 解法 2：用 lookup-method / 改为依赖注入到方法参数（Spring 的方法注入） -->
<!-- 解法 3：重构设计（正解）—— 抽第三层、用事件解耦、把双向依赖改成单向 -->
```

> **注意：** 循环依赖能解只是容错，不代表设计正确。看到循环依赖，第一反应应该是"这两个类是不是职责切错了"。

---

## 11. 两种注入方式对比与选型

| 维度 | Set 注入 | 构造方法注入 |
|------|---------|-------------|
| **XML 标签** | `<property name=".." ref/value=".."/>` | `<constructor-arg index/name/type=".." ref/value=".."/>` |
| **类的要求** | 无参构造 + `setXxx` | 带参构造器（可无 setter） |
| **依赖强制性** | 可选，可以不注入 | 强制，缺一不可 |
| **对象完整性** | 创建后可能处于中间状态 | 创建即完整 |
| **不可变性** | 字段无法 `final` | 字段可 `final`，线程安全 |
| **循环依赖** | ✅ 可解决（三级缓存） | ❌ 不可解决 |
| **注入时机** | 实例化后、初始化前（`populateBean`） | 实例化时（`createBeanInstance`） |
| **底层动作** | 反射 `Method.invoke` | 反射 `Constructor.newInstance` |
| **可测试性** | 需要 setter 或反射，较麻烦 | 直接 `new` 传 mock，最好测 |
| **参数多时** | XML 清爽（一个个 property） | XML 冗长（一堆 constructor-arg） |
| **官方推荐** | 用于**可选依赖** | 用于**强制依赖**（Spring 官方明确推荐） |

### 11.1 官方立场

> "Since you can mix constructor-based and setter-based DI, it is a good rule of thumb to use constructors for **mandatory dependencies** and setter methods or configuration methods for **optional dependencies**."
> —— Spring Framework Reference

### 11.2 实践建议

1. **强制依赖一律用构造方法注入**，字段加 `final`；
2. **可选依赖用 Set 注入**（比如可插拔的插件、可选的缓存客户端）；
3. XML 里构造参数**优先用 `index`**，别依赖参数名；
4. 一个类同时有带参构造和 setter 时，**两种可以混用**：

```java
public class UserService {
    private final UserDao userDao;      // 强制依赖
    private int timeout = 3000;         // 可选，有默认值

    public UserService(UserDao userDao) { this.userDao = userDao; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}
```

```xml
<bean id="userService" class="com.example.service.UserService">
    <constructor-arg index="0" ref="userDao"/>   <!-- 强制依赖：构造注入 -->
    <property name="timeout" value="5000"/>      <!-- 可选依赖：Set 注入 -->
</bean>
```

---

## 12. XML 配置的常见坑

| 坑 | 现象 | 原因 / 解法 |
|----|------|------------|
| **`name` 与 setter 不匹配** | `NotWritablePropertyException: Invalid property 'userdao'` | JavaBeans 约定：属性名是 `setXxx` 去掉 `set` 后首字母小写。大小写敏感 |
| **忘了写 setter** | 同上 | Set 注入必须有 `setXxx`，只有字段不够 |
| **只有带参构造却用 `<property>`** | `BeanInstantiationException: No default constructor found` | Set 注入要求无参构造。要么补无参构造，要么改 `<constructor-arg>` |
| **构造参数顺序写反** | 不报错但数据错乱 | 两个同类型参数（如两个 String）时，`type` 无法区分，必须用 `index` |
| **`name` 匹配不上构造参数** | `UnsatisfiedDependencyException` | 编译时没加 `-parameters`，Spring 6 拿不到参数名。改用 `index` |
| **类型转换失败** | `TypeMismatchException: Failed to convert value of type 'String' to required type 'int'` | `value="abc"` 转 int。检查字面量格式 |
| **`ref` 指向不存在的 id** | `NoSuchBeanDefinitionException: No bean named 'xxx' available` | 检查 id 拼写；确认那个 Bean 真的注册了 |
| **内部 Bean 想复用** | 外部 `ref` 拿不到 | 内部 `<bean>` 是匿名的，只能在当前 property 里用。要复用就提到外面给它 `id` |
| **`<property name="remark"/>` 想设 null** | 属性保持原值 | 空标签表示"不处理"，设 null 要写 `<null/>` |
| **级联属性 NPE** | `NullValueInNestedPathException` | `address.city` 要求 `address` 字段已初始化（先 `new Address()`） |
| **构造注入 + 循环依赖** | `BeanCurrentlyInCreationException` | 改成 Set 注入或重构（第 10 节） |
| **单例注入 prototype 只生效一次** | prototype Bean 变成"事实上的单例" | 单例 Bean 只在创建时注入一次。要每次拿新的，用 `lookup-method` 或 `scoped-proxy` |
| **XML 头写错** | `SAXParseException` / 找不到 schema | `xsi:schemaLocation` 必须成对出现（命名空间 + XSD 地址） |
| **`lazy-init` 掩盖了启动期错误** | 启动正常，运行时报错 | 懒加载把创建推迟到第一次 `getBean`。排错时先设 `default-lazy-init="false"` |

---

## 附：本篇要点速查

```
Set 注入
  写法：<property name="userDao" ref="userDao"/> + 无参构造 + setXxx
  原理：createBeanInstance（无参反射造空壳）
        → populateBean → applyPropertyValues
        → BeanDefinitionValueResolver（ref → getBean 递归）
        → BeanWrapperImpl → Introspector 找 setUserDao
        → Method.invoke  ★
  时机：实例化之后、初始化之前

构造注入
  写法：<constructor-arg index="0" ref="userDao"/> + 带参构造
  原理：createBeanInstance → hasConstructorArgumentValues() == true
        → ConstructorResolver#autowireConstructor
        → 构造器排序（public + 参数多优先）→ 逐个匹配参数
        → TypeConverter 类型转换 → ctor.newInstance  ★
  时机：实例化时（依赖先于对象）

共同点：都用 resolveValueIfNecessary 解析 ref/value，最终都是反射
```

---

> 下一篇：[04 SOLID 五大设计原则](04-SOLID五大设计原则.md) · 想动手：[10 实战：手写迷你 IoC 容器](10-实战-手写迷你IoC容器.md)

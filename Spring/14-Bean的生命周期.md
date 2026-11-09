# 14 · Bean 的生命周期

> 上一篇：[13 Bean 的实例化方式](13-Bean的实例化方式.md) · 返回 [索引](00-学习路线与索引.md)
>
> 本篇只用 XML 配置。回调顺序在 Spring 5.3 + JDK 8 上实测，输出为真实结果。

---

## 目录

1. [两种记法：5 步与 7 步](#1-两种记法5-步与-7-步)
2. [5 步版：最小模型](#2-5-步版最小模型)
3. [7 步版：加上 BeanPostProcessor](#3-7-步版加上-beanpostprocessor)
4. [完整版：真实 Spring 的 13 个回调点](#4-完整版真实-spring-的-13-个回调点)
5. [每个阶段在源码里的位置](#5-每个阶段在源码里的位置)
6. [BeanPostProcessor 详解](#6-beanpostprocessor-详解)
7. [各阶段能做什么](#7-各阶段能做什么)
8. [和其它知识点的关系](#8-和其它知识点的关系)
9. [常见坑](#9-常见坑)

---

## 1. 两种记法：5 步与 7 步

| 5 步版 | 7 步版 | 说明 |
|--------|--------|------|
| ① 实例化 Bean | ① 实例化 Bean | 反射调构造器，得到一个"空壳" |
| ② Bean 属性赋值 | ② Bean 属性赋值 | `<property>` 注入依赖 |
| — | **③ BeanPostProcessor before** | 初始化**之前**的扩展点 |
| ③ 初始化 Bean | ④ 初始化 Bean | `afterPropertiesSet` + `init-method` |
| — | **⑤ BeanPostProcessor after** | 初始化**之后**的扩展点 |
| ④ 使用 Bean | ⑥ 使用 Bean | `getBean` 返回后正常调用 |
| ⑤ 销毁 Bean | ⑦ 销毁 Bean | 容器 `close()` 时 |

**关键理解：BeanPostProcessor 不是"一个步骤"，而是两个插入点，把"初始化"这一步夹在了中间。**

```
        初始化
          ▲
   ┌──────┴──────┐
   │  BPP before │  ← 初始化前
   ├─────────────┤
   │  afterPropertiesSet / init-method  │
   ├─────────────┤
   │  BPP after  │  ← 初始化后（AOP 代理在这里生成）
   └─────────────┘
```

所以 5 步是"骨架"，7 步是"骨架 + 扩展点"。面试说 5 步不算错，但能说清 7 步说明你知道 Spring 在哪插手。

---

## 2. 5 步版：最小模型

| 步骤 | 做什么 | 谁来做 | 源码方法 |
|------|--------|--------|---------|
| ① 实例化 | 反射调构造器，`new` 出一个对象（此时字段全是默认值） | Spring | `createBeanInstance()` |
| ② 属性赋值 | 把 `<property>` 里的值/引用注入进去 | Spring | `populateBean()` |
| ③ 初始化 | 执行你写的初始化逻辑 | 你的代码 | `initializeBean()` |
| ④ 使用 | 从容器拿出来用 | 你的代码 | — |
| ⑤ 销毁 | 释放资源 | 你的代码 | `destroy()` |

### 2.1 一个最容易搞混的点

**构造注入的参数在 ① 就解析完了，`<property>` 才在 ② 注入。**

```
<constructor-arg>  → createBeanInstance（①，先 getBean 拿依赖，再 newInstance）
<property>         → populateBean        （②，对象已存在，反射调 setter）
```

所以：

```java
public class UserService {
    private UserDao userDao;

    public UserService() {
        // ❌ 这里 userDao 一定是 null
        //    构造器执行时，② 还没发生
        System.out.println(userDao);
    }

    public void setUserDao(UserDao userDao) { this.userDao = userDao; }
}
```

而构造注入没有这个问题，因为依赖是在 `newInstance` **之前**就解析好的（见 [03 篇第 7 节](03-Spring对IoC的实现-DI依赖注入.md#7-构造方法注入原理)）。

### 2.2 "初始化"这一步到底做了什么

```java
// AbstractAutowireCapableBeanFactory#invokeInitMethods（简化）
if (bean instanceof InitializingBean) {
    ((InitializingBean) bean).afterPropertiesSet();       // 先接口
}
if (mbd.getInitMethodName() != null) {
    invokeCustomInitMethod(beanName, bean, mbd);          // 再 init-method
}
```

两个都配的话：**`afterPropertiesSet()` 先执行，`init-method` 后执行**。

```xml
<bean id="x" class="com.example.X" init-method="initMethod"/>
```

### 2.3 "销毁"这一步的前提

```java
// AbstractBeanFactory#registerDisposableBeanIfNecessary
if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
    if (mbd.isSingleton()) {
        registerDisposableBean(beanName, new DisposableBeanAdapter(...));
    }
}
```

三个条件同时满足才会有销毁回调：

1. **不是 prototype**（prototype 的销毁 Spring 完全不管，见 [11 篇 4.3](11-Bean的作用域.md#43-三个要点)）
2. 实现了 `DisposableBean` 或配了 `destroy-method`
3. **容器正常 `close()`**（`kill -9`、进程崩溃都不会执行）

---

## 3. 7 步版：加上 BeanPostProcessor

`BeanPostProcessor` 是 Spring 最重要的扩展点，接口只有两个方法：

```java
public interface BeanPostProcessor {

    // 初始化之前调用
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    // 初始化之后调用
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

```java
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof LifeCycleBean) {
            System.out.println("⑥ BPP before -> " + beanName);
        }
        return bean;                       // ★ 必须返回，返回 null 后面就拿不到了
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof LifeCycleBean) {
            System.out.println("⑩ BPP after -> " + beanName);
        }
        return bean;
    }
}
```

```xml
<!-- 只要注册成 Bean，容器就会自动识别，不需要额外配置 -->
<bean class="com.example.life.MyBeanPostProcessor"/>
```

三个必须知道的事实：

1. **它对容器里所有 Bean 生效**，所以基本都要 `instanceof` 或按 `beanName` 过滤，否则日志会刷屏。
2. **返回值可以是别的对象**。`postProcessAfterInitialization` 返回代理对象，调用方拿到的就是代理——**AOP 就是在这里做的**。
3. **它自己也是 Bean**，但由 Spring 提前创建（早于普通 Bean），因为要给普通 Bean 用。

---

## 4. 完整版：真实 Spring 的 13 个回调点

7 步是简化模型。真实执行时，Aware 接口、`afterPropertiesSet`、`init-method` 会把"初始化"撑开成好几步。

### 4.1 代码

```java
public class LifeCycleBean implements BeanNameAware, BeanFactoryAware,
        ApplicationContextAware, InitializingBean, DisposableBean {

    private String name;

    public LifeCycleBean() {
        System.out.println("① 构造方法（实例化）");
    }

    public void setName(String name) {
        this.name = name;
        System.out.println("② setter 注入 name=" + name + "（属性赋值）");
    }

    @Override
    public void setBeanName(String beanName) {
        System.out.println("③ BeanNameAware#setBeanName -> " + beanName);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        System.out.println("④ BeanFactoryAware#setBeanFactory");
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        System.out.println("⑤ ApplicationContextAware#setApplicationContext");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("⑧ InitializingBean#afterPropertiesSet");
    }

    public void initMethod() {
        System.out.println("⑨ init-method");
    }

    public void use() {
        System.out.println("⑪ 使用 Bean：name=" + name);
    }

    @Override
    public void destroy() {
        System.out.println("⑫ DisposableBean#destroy");
    }

    public void destroyMethod() {
        System.out.println("⑬ destroy-method");
    }
}
```

```xml
<bean class="com.example.life.MyBeanPostProcessor"/>

<bean id="lifeCycleBean" class="com.example.life.LifeCycleBean"
      init-method="initMethod" destroy-method="destroyMethod">
    <property name="name" value="张三"/>
</bean>
```

### 4.2 真实输出

```
==== 容器启动 ====
  [BeanPostProcessor 自己被创建]
① 构造方法（实例化）
② setter 注入 name=张三（属性赋值）
③ BeanNameAware#setBeanName -> lifeCycleBean
④ BeanFactoryAware#setBeanFactory
⑤ ApplicationContextAware#setApplicationContext
⑥ BeanPostProcessor#postProcessBeforeInitialization
⑧ InitializingBean#afterPropertiesSet
⑨ init-method
⑩ BeanPostProcessor#postProcessAfterInitialization
==== 容器启动完成 ====
⑪ 使用 Bean：name=张三
==== 关闭容器 ====
⑫ DisposableBean#destroy
⑬ destroy-method
```

对照着看：

| 编号 | 回调 | 属于哪一步 |
|------|------|-----------|
| ① | 构造方法 | 实例化 |
| ② | `setName` | 属性赋值 |
| ③ | `BeanNameAware` | 初始化（Aware 回调） |
| ④ | `BeanFactoryAware` | 初始化（Aware 回调） |
| ⑤ | `ApplicationContextAware` | 初始化（Aware 回调） |
| ⑥ | `BeanPostProcessor#before` | 7 步里的第 ③ 步 |
| ⑧ | `InitializingBean#afterPropertiesSet` | 初始化 |
| ⑨ | `init-method` | 初始化 |
| ⑩ | `BeanPostProcessor#after` | 7 步里的第 ⑤ 步 |
| ⑪ | 业务方法 | 使用 |
| ⑫ | `DisposableBean#destroy` | 销毁 |
| ⑬ | `destroy-method` | 销毁 |

注意几个细节：

- **③④⑤ 在 ⑥ 之前**。因为 `ApplicationContextAware` 是由 `ApplicationContextAwareProcessor` 这个 **BeanPostProcessor** 在 `postProcessBeforeInitialization` 里调用的，而它注册得比你的 BPP 早，所以先执行。
- **⑧ 在 ⑨ 之前**。`afterPropertiesSet()` 总是先于 `init-method`。
- **⑫ 在 ⑬ 之前**。`destroy()` 总是先于 `destroy-method`。
- 整个 ①~⑩ 都发生在"容器启动完成"之前（单例默认预实例化）。

### 4.3 完整顺序图

```
resolveBeforeInstantiation
  └─ InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation   (a)
       │  返回非 null 就直接用它，后面全跳过
       ▼
createBeanInstance                                                   ① 实例化
  └─ ctor.newInstance()
       │
       ▼
populateBean                                                         ② 属性赋值
  ├─ InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation  (b)
  ├─ InstantiationAwareBeanPostProcessor#postProcessProperties          (c)
  └─ applyPropertyValues → setter
       │
       ▼
initializeBean
  ├─ invokeAwareMethods          → BeanNameAware / BeanClassLoaderAware / BeanFactoryAware  ③④
  ├─ Aware 类 BPP（ApplicationContextAwareProcessor）                                       ⑤
  ├─ BPP#postProcessBeforeInitialization                                                    ⑥
  ├─ invokeInitMethods
  │    ├─ InitializingBean#afterPropertiesSet                                               ⑧
  │    └─ init-method                                                                       ⑨
  └─ BPP#postProcessAfterInitialization                                                     ⑩
       │
       ▼
   使用 Bean                                                                                ⑪
       │
       ▼
destroySingletons（容器 close）
  └─ DisposableBeanAdapter#destroy
       ├─ DisposableBean#destroy                                                            ⑫
       └─ destroy-method                                                                    ⑬
```

### 4.4 验证 InstantiationAwareBeanPostProcessor 的三个插入点

实现 `InstantiationAwareBeanPostProcessor`（它是 `BeanPostProcessor` 的子接口）能多拿到三个插入点：

```java
public class MyInstantiationAwareBPP implements InstantiationAwareBeanPostProcessor {

    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        System.out.println("(a) 构造之前");
        return null;          // null = 不接管创建；返回对象则跳过后续所有步骤
    }

    public boolean postProcessAfterInstantiation(Object bean, String beanName) {
        System.out.println("(b) 构造之后、赋值之前");
        return true;          // false = 跳过属性赋值
    }

    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        System.out.println("(c) 属性赋值时");
        return pvs;           // 可以在这里改 pvs，甚至手工塞值
    }
}
```

真实输出：

```
  (a) postProcessBeforeInstantiation   ← 构造之前
① 构造方法（实例化）
  (b) postProcessAfterInstantiation    ← 构造之后、赋值之前
  (c) postProcessProperties            ← 属性赋值时
② setter 注入 name=李四（属性赋值）
③ BeanNameAware#setBeanName -> lifeCycleBean
④ BeanFactoryAware#setBeanFactory
⑤ ApplicationContextAware#setApplicationContext
  (d) postProcessBeforeInitialization  ← 初始化之前
⑧ InitializingBean#afterPropertiesSet
  (e) postProcessAfterInitialization   ← 初始化之后
⑪ 使用 Bean：name=李四
⑫ DisposableBean#destroy
```

> **(c) `postProcessProperties` 这个点是关键**：注解式的依赖注入（`@Autowired` / `@Resource`）就是在这里完成的——它在 `setter` 之前执行，直接往字段里塞值。你现在用 XML，走的是 `applyPropertyValues`；等以后学注解，会发现"注入"换了个入口，但生命周期骨架完全一样。

---

## 5. 每个阶段在源码里的位置

`doCreateBean` 是整条链的主干：

```java
// AbstractAutowireCapableBeanFactory#doCreateBean（简化）
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {

    // ① 实例化
    BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);
    Object bean = instanceWrapper.getWrappedInstance();

    // 循环依赖：提前暴露工厂（三级缓存）
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences);
    if (earlySingletonExposure) {
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // ② 属性赋值
    populateBean(beanName, mbd, instanceWrapper);

    // ③ 初始化（Aware + BPP + init-method）
    Object exposedObject = initializeBean(beanName, bean, mbd);

    return exposedObject;
}
```

```java
// AbstractAutowireCapableBeanFactory#initializeBean（简化）
protected Object initializeBean(String beanName, Object bean, RootBeanDefinition mbd) {

    invokeAwareMethods(beanName, bean);                        // BeanNameAware / BeanClassLoaderAware / BeanFactoryAware

    Object wrappedBean = bean;
    wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);   // ⑥

    invokeInitMethods(beanName, wrappedBean, mbd);              // ⑧⑨

    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);    // ⑩
    return wrappedBean;
}
```

销毁阶段：

```java
// DisposableBeanAdapter#destroy（简化）
public void destroy() {
    if (this.bean instanceof DisposableBean) {
        ((DisposableBean) this.bean).destroy();                 // ⑫
    }
    if (this.destroyMethodName != null) {
        invokeCustomDestroyMethod(this.destroyMethodName);      // ⑬
    }
}
```

---

## 6. BeanPostProcessor 详解

### 6.1 怎么注册

```xml
<!-- 唯一要求：让它成为一个 Bean。容器启动时会自动收集所有 BeanPostProcessor 类型的 Bean -->
<bean class="com.example.life.MyBeanPostProcessor"/>
```

内部由 `PostProcessorRegistrationDelegate#registerBeanPostProcessors` 在 `refresh()` 里统一注册，**早于普通单例的创建**。

### 6.2 执行顺序怎么控制

多个 BPP 同时存在时，按这个优先级排序：

```
PriorityOrdered  →  Ordered  →  没实现任何排序接口（按注册顺序）
```

```java
public class MyBPP implements BeanPostProcessor, Ordered {
    @Override
    public int getOrder() { return 0; }   // 数字越小越先执行
}
```

### 6.3 它和 BeanFactoryPostProcessor 的区别

这是另一个高频对比题：

| | `BeanFactoryPostProcessor` | `BeanPostProcessor` |
|---|---|---|
| 作用对象 | **BeanDefinition**（图纸） | **Bean 实例**（成品） |
| 执行时机 | 所有 BeanDefinition 加载完、**Bean 实例化之前** | **每个 Bean** 初始化前后 |
| 执行次数 | 一次 | 每个 Bean 两次（before + after） |
| 典型实现 | `PropertySourcesPlaceholderConfigurer`（`${}` 替换）、`CustomEditorConfigurer` | `ApplicationContextAwareProcessor`、AOP 的 `AnnotationAwareAspectJAutoProxyCreator` |
| 能不能改 Bean | 不能，只能改图纸 | 能，能替换成代理对象 |

一句话：**前者改配置，后者改对象**。

### 6.4 两个容易忽略的限制

**① BPP 对 `FactoryBean` 的"产品"不生效**

`FactoryBean#getObject()` 返回的对象是在 `getObjectForBeanInstance` 里拿的，**根本不经过 `initializeBean`**，所以你的 BPP 看不到它（见 [13 篇 6.2](13-Bean的实例化方式.md#62-一个重要推论)）。

**② BPP 自己不应该依赖业务 Bean**

如果 BPP 的构造器/属性里注入了某个业务 Bean，那个 Bean 会被迫**提前创建**，而此时后面的 BPP 还没注册完，它就拿不到完整的后置处理（比如不会被 AOP 代理）。Spring 会打印警告：

```
Bean 'xxx' of type [...] is not eligible for getting processed by all BeanPostProcessors
(for example: not eligible for auto-proxying)
```

看到这条日志，基本就是"某个 BPP 提前拉了业务 Bean"。

---

## 7. 各阶段能做什么

| 阶段 | 能做什么 | 典型用法 |
|------|---------|---------|
| 构造方法 | 初始化字段 | 尽量只做赋值，别做重活 |
| 属性赋值 | 注入依赖 | `<property>` / `<constructor-arg>` |
| Aware 回调 | 拿到容器、Bean 名字 | 按名字取资源、打日志 |
| BPP before | 改属性、校验、返回包装对象 | 自定义注解处理、字段注入 |
| `afterPropertiesSet` | 校验必填属性、打开资源 | `if (dataSource == null) throw ...` |
| `init-method` | 业务初始化 | 加载缓存、预热连接、启动定时任务 |
| BPP after | 生成代理 | AOP、事务、`@Async` |
| `destroy` / `destroy-method` | 释放资源 | 关连接池、停线程、flush 缓冲 |

**一个实践建议**：需要"属性都注入完才能做"的事情，放 `afterPropertiesSet` 或 `init-method`，**不要放构造方法**。构造方法执行时依赖还是 null。

---

## 8. 和其它知识点的关系

| 关联点 | 说明 | 去哪看 |
|--------|------|--------|
| 循环依赖 | ① 之后、② 之前会把"早期引用"放进三级缓存，所以 Set 注入能解循环依赖 | [03 篇第 10 节](03-Spring对IoC的实现-DI依赖注入.md#10-三级缓存与循环依赖) |
| 构造注入解不了循环依赖 | 构造注入发生在 ① 内部，此时对象还没造出来，没法提前暴露 | [03 篇 10.3](03-Spring对IoC的实现-DI依赖注入.md#103-为什么构造方法注入解决不了) |
| prototype 没有销毁 | 销毁回调只给单例登记 | [11 篇 4.3](11-Bean的作用域.md#43-三个要点) |
| FactoryBean 产品不经过生命周期 | 产品在 `getObjectForBeanInstance` 里产生，绕过 `initializeBean` | [13 篇 6.2](13-Bean的实例化方式.md#62-一个重要推论) |
| AOP 在哪生效 | `postProcessAfterInitialization` 返回代理 | [08 篇](08-Spring框架特点.md) |

---

## 9. 常见坑

| 坑 | 现象 | 原因 / 解法 |
|----|------|------------|
| **构造方法里用注入的字段** | 拿到 `null` | ② 属性赋值在 ① 之后。改用 `afterPropertiesSet` / `init-method` |
| **`afterPropertiesSet` 里抛异常** | 容器启动失败，`BeanCreationException` | 初始化异常会中断整个 `refresh()`，要确保校验逻辑不误伤 |
| **`init-method` 名字写错** | 外层 `BeanCreationException`，根因 `BeanDefinitionValidationException: Could not find an init method named 'initMethodTypo' on bean with name 'lifeCycleBean'` | 方法必须**无参**、非 static，名字大小写一致 |
| **`destroy-method` 没执行** | 资源没释放 | prototype 不回调；或容器没 `close()`（`main` 里要显式 `ctx.close()`） |
| **BPP 里 `return null`** | 后续 NPE 或 Bean 变成 null | 两个方法都必须 `return bean` |
| **BPP 对 FactoryBean 产品不生效** | 产品没被代理/没被处理 | 产品不经过 `initializeBean`，要处理得包一层 |
| **BPP 里注入了业务 Bean** | 日志警告 `not eligible for getting processed by all BeanPostProcessors` | 业务 Bean 被提前创建。BPP 只依赖基础设施类 |
| **`new` 出来的对象没有回调** | 不执行 init/destroy | 生命周期由容器驱动，`new` 出来的归你管 |
| **以为 5 步里没有 BPP** | 面试答不全 | 5 步是骨架，7 步是骨架 + 两个 BPP 插入点 |
| **`ApplicationContextAware` 顺序搞反** | 以为在自定义 BPP 之后 | 它由 `ApplicationContextAwareProcessor` 提前调用，在自定义 BPP 之前 |

---

## 附：本篇速查

```
5 步（骨架）
  ① 实例化      createBeanInstance  → 反射调构造器，得到空壳
  ② 属性赋值    populateBean        → 反射调 setter
  ③ 初始化      initializeBean      → afterPropertiesSet → init-method
  ④ 使用
  ⑤ 销毁        ctx.close()         → destroy() → destroy-method

7 步（骨架 + BPP）
  ① 实例化 → ② 属性赋值 → ③ BPP before → ④ 初始化 → ⑤ BPP after → ⑥ 使用 → ⑦ 销毁

完整 13 个回调点（实测顺序）
  ① 构造
  ② setter 注入
  ③ BeanNameAware#setBeanName
  ④ BeanFactoryAware#setBeanFactory
  ⑤ ApplicationContextAware#setApplicationContext
  ⑥ BeanPostProcessor#postProcessBeforeInitialization
  ⑧ InitializingBean#afterPropertiesSet
  ⑨ init-method
  ⑩ BeanPostProcessor#postProcessAfterInitialization
  ⑪ 使用
  ⑫ DisposableBean#destroy
  ⑬ destroy-method

InstantiationAwareBeanPostProcessor 的额外插入点
  (a) postProcessBeforeInstantiation   构造之前（返回非 null 直接接管）
  (b) postProcessAfterInstantiation    构造之后、赋值之前（false 跳过赋值）
  (c) postProcessProperties            属性赋值时（注解注入在这里）

源码主干
  doCreateBean
    ├─ createBeanInstance   ①
    ├─ addSingletonFactory  三级缓存（循环依赖）
    ├─ populateBean         ②
    └─ initializeBean       ③（Aware → BPP before → init → BPP after）

两个顺序别记反
  afterPropertiesSet  → init-method
  destroy()           → destroy-method
```

---

> 回顾：[13 Bean 的实例化方式](13-Bean的实例化方式.md) · 返回 [索引](00-学习路线与索引.md)

# 10 · 实战：手写迷你 IoC 容器

> 上一篇：[09 Spring 常用 JAR 包](09-Spring常用JAR包.md) · 返回 [索引](00-学习路线与索引.md)
>
> 这一篇把 [03 篇](03-Spring对IoC的实现-DI依赖注入.md) 讲的原理亲手跑一遍。代码不到 100 行，**纯 JDK，不依赖 Spring**，`javac` 直接编译就能跑。

---

## 目录

1. [目标：复刻 Spring 的核心动作](#1-目标复刻-spring-的核心动作)
2. [完整代码](#2-完整代码)
3. [运行输出与解读](#3-运行输出与解读)
4. [迷你容器 ↔ Spring 源码对照](#4-迷你容器--spring-源码对照)
5. [故意省略了什么](#5-故意省略了什么)
6. [进阶练习](#6-进阶练习)

---

## 1. 目标：复刻 Spring 的核心动作

```
读配置（BeanDefinition）→ 按需创建 → 递归解析依赖 → 反射注入 → 存入单例池
```

对照 Spring：

| 迷你容器要做的事 | Spring 里对应的东西 |
|----------------|-------------------|
| 存"Bean 的图纸" | `BeanDefinition` + `beanDefinitionMap` |
| 存"造好的对象" | `singletonObjects`（一级缓存） |
| Set 注入 | `populateBean` → `Method.invoke` |
| 构造注入 | `createBeanInstance` → `Constructor.newInstance` |
| 按名字找 setter | `java.beans.Introspector` / `PropertyDescriptor` |
| 递归拿依赖 | `resolveDependency` → `getBean` |

---

## 2. 完整代码

保存为 `MiniIocDemo.java`，直接 `javac MiniIocDemo.java && java MiniIocDemo` 即可运行。

```java
import java.lang.reflect.*;
import java.util.*;

public class MiniIocDemo {

    // ================= 业务类 =================
    interface UserDao { String findName(long id); }

    static class UserDaoMysqlImpl implements UserDao {
        public String findName(long id) { return "mysql-user-" + id; }
    }

    /** Set 注入的类：必须有无参构造 + setter */
    static class UserServiceBySetter {
        private UserDao userDao;

        public UserServiceBySetter() {
            System.out.println("  [1] 无参构造完成，userDao = " + userDao);   // 一定是 null
        }
        public void setUserDao(UserDao userDao) {
            System.out.println("  [2] setUserDao 被调用，注入 " + userDao.getClass().getSimpleName());
            this.userDao = userDao;
        }
        public String hello(long id) { return "setter 注入 -> " + userDao.findName(id); }
    }

    /** 构造注入的类：只有带参构造，字段 final */
    static class UserServiceByCtor {
        private final UserDao userDao;

        public UserServiceByCtor(UserDao userDao) {
            System.out.println("  [1] 构造器收到 " + userDao.getClass().getSimpleName());
            this.userDao = userDao;
        }
        public String hello(long id) { return "ctor 注入 -> " + userDao.findName(id); }
    }

    // ================= 迷你容器 =================
    /** 对应 Spring 的 BeanDefinition：Bean 的"图纸" */
    static class BeanDefinition {
        String id;
        Class<?> type;
        Map<String, String> properties = new LinkedHashMap<>();   // 对应 <property name ref>
        List<String> constructorArgs = new ArrayList<>();         // 对应 <constructor-arg ref>

        BeanDefinition(String id, Class<?> type) { this.id = id; this.type = type; }
    }

    static class MiniContainer {
        private final Map<String, BeanDefinition> definitionMap = new LinkedHashMap<>(); // 图纸柜
        private final Map<String, Object> singletonObjects = new HashMap<>();            // 成品仓库（一级缓存）

        void register(BeanDefinition def) { definitionMap.put(def.id, def); }

        Object getBean(String id) {
            Object cached = singletonObjects.get(id);
            if (cached != null) return cached;                        // ① 先查单例池

            BeanDefinition def = definitionMap.get(id);
            if (def == null) throw new IllegalArgumentException("No bean named '" + id + "'");

            Object bean = def.constructorArgs.isEmpty()
                    ? instantiateBySetter(def)                        // ② Set 注入路线
                    : instantiateByConstructor(def);                  // ② 构造注入路线

            singletonObjects.put(id, bean);                           // ③ 放入单例池
            return bean;
        }

        /** Set 注入：先无参实例化造空壳，再反射调用 setter 填依赖 */
        private Object instantiateBySetter(BeanDefinition def) {
            try {
                Object bean = def.type.getDeclaredConstructor().newInstance();   // 造空壳
                for (Map.Entry<String, String> e : def.properties.entrySet()) {
                    Object dep = getBean(e.getValue());                          // 递归解析 ref
                    Method setter = findSetter(def.type, e.getKey());            // 按 JavaBeans 约定找 setter
                    setter.invoke(bean, dep);                                    // ★ 反射调用
                }
                return bean;
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /** 构造注入：先解析全部构造参数，再反射 newInstance */
        private Object instantiateByConstructor(BeanDefinition def) {
            try {
                Constructor<?> ctor = def.type.getDeclaredConstructors()[0];
                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = getBean(def.constructorArgs.get(i));               // 依赖必须先就绪
                }
                ctor.setAccessible(true);
                return ctor.newInstance(args);                                   // ★ 反射创建
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /** JavaBeans 命名约定：userDao → setUserDao（等价于 Spring 的 Introspector 内省） */
        private Method findSetter(Class<?> type, String property) throws NoSuchMethodException {
            String name = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
            for (Method m : type.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1) return m;
            }
            throw new NoSuchMethodException(name);
        }
    }

    // ================= 测试 =================
    public static void main(String[] args) {
        MiniContainer ctx = new MiniContainer();

        ctx.register(new BeanDefinition("userDao", UserDaoMysqlImpl.class));

        BeanDefinition bySetter = new BeanDefinition("bySetter", UserServiceBySetter.class);
        bySetter.properties.put("userDao", "userDao");     // 等价于 <property name="userDao" ref="userDao"/>
        ctx.register(bySetter);

        BeanDefinition byCtor = new BeanDefinition("byCtor", UserServiceByCtor.class);
        byCtor.constructorArgs.add("userDao");             // 等价于 <constructor-arg ref="userDao"/>
        ctx.register(byCtor);

        System.out.println("== 创建 bySetter ==");
        System.out.println(((UserServiceBySetter) ctx.getBean("bySetter")).hello(1));

        System.out.println("== 创建 byCtor ==");
        System.out.println(((UserServiceByCtor) ctx.getBean("byCtor")).hello(2));
    }
}
```

---

## 3. 运行输出与解读

```
== 创建 bySetter ==
  [1] 无参构造完成，userDao = null          ← 空壳阶段，依赖还没注入
  [2] setUserDao 被调用，注入 UserDaoMysqlImpl  ← 属性填充阶段
setter 注入 -> mysql-user-1
== 创建 byCtor ==
  [1] 构造器收到 UserDaoMysqlImpl            ← 构造器执行时依赖已就绪
ctor 注入 -> mysql-user-2
```

**三个必须注意的顺序细节：**

**① `[1]` 打印的是 `null`**

因为 `instantiateBySetter` 先 `newInstance()`（无参构造）再 `invoke` setter。构造器执行时 `userDao` 字段还是默认值——这正是 Spring 里"构造方法里拿不到注入依赖"的原因。

**② Set 注入的 `[1]` 和 `[2]` 之间，还偷偷创建了 `userDao`**

```
getBean("bySetter")
  ├─ newInstance()          → 打印 [1]，userDao = null
  ├─ 遍历 properties：
  │    getBean("userDao")   → 现场创建 UserDaoMysqlImpl（递归！）
  │    setUserDao(dao)      → 打印 [2]
  └─ 放入单例池
```

如果 `userDao` 依赖了别人，那就在这一步继续递归下去——和 Spring 的 `applyPropertyValues` 完全一致。

**③ 构造注入的 `[1]` 打印时依赖已经有值**

因为 `instantiateByConstructor` 是先把 `args[i]` 全部解析好，最后才 `ctor.newInstance(args)`。依赖必须先于对象存在——这也解释了为什么构造注入解决不了循环依赖。

---

## 4. 迷你容器 ↔ Spring 源码对照

| 迷你容器 | Spring 中的对应物 |
|---------|-----------------|
| `BeanDefinition` | `org.springframework.beans.factory.config.BeanDefinition` |
| `definitionMap` | `DefaultListableBeanFactory#beanDefinitionMap` |
| `singletonObjects` | `DefaultSingletonBeanRegistry#singletonObjects`（一级缓存） |
| `getBean()` 里的缓存判断 | `doGetBean()` → `getSingleton(beanName)` |
| `instantiateBySetter()` | `populateBean()` → `applyPropertyValues()` |
| `properties` Map | `MutablePropertyValues` |
| `findSetter()` | `CachedIntrospectionResults`（`java.beans.Introspector`）+ `PropertyDescriptor` |
| `setter.invoke(bean, dep)` | `AbstractNestablePropertyAccessor#processLocalProperty` → `Method.invoke` |
| `instantiateByConstructor()` | `createBeanInstance()` → `ConstructorResolver#autowireConstructor()` |
| `constructorArgs` List | `ConstructorArgumentValues` |
| `ctor.setAccessible(true)` | `ReflectionUtils.makeAccessible(ctor)` |
| `ctor.newInstance(args)` | `BeanUtils.instantiateClass()` → `Constructor.newInstance` |
| `getBean()` 递归调用 | `doResolveDependency()` → `getBean()` |

---

## 5. 故意省略了什么

这些才是 Spring 真正的复杂度所在：

| 省略项 | Spring 里的作用 | 在哪篇讲 |
|--------|---------------|---------|
| **三级缓存** | 解决循环依赖 | [03 篇第 10 节](03-Spring对IoC的实现-DI依赖注入.md#10-三级缓存与循环依赖) |
| **按类型匹配 + @Primary/@Qualifier** | 本示例是显式 `ref`，Spring 要按类型自动找 | [03 篇第 9 节](03-Spring对IoC的实现-DI依赖注入.md#9-自动装配-autowire) |
| **BeanPostProcessor / AOP 代理** | `initializeBean` 阶段生成代理对象 | [08 篇](08-Spring框架特点.md#3-spring-中体现的设计模式) |
| **生命周期回调** | `init-method`、`InitializingBean`、`destroy-method` | [08 篇](08-Spring框架特点.md#25-容器管理完整生命周期) |
| **作用域** | `singleton` / `prototype` | [03 篇第 2 节](03-Spring对IoC的实现-DI依赖注入.md#21-什么时候创建-bean) |
| **类型转换** | `<property value="3000">` 到 `int` 的自动转换 | [03 篇第 5 节](03-Spring对IoC的实现-DI依赖注入.md#5-set-注入原理) |
| **懒加载 / 循环引用检测** | `lazy-init`、`BeanCurrentlyInCreationException` | [03 篇](03-Spring对IoC的实现-DI依赖注入.md) |

---

## 6. 进阶练习

按顺序做，每完成一个就离 Spring 更近一步：

**① 加 XML 解析（把硬编码的 `register` 换成读文件）**

用 `javax.xml.parsers.DocumentBuilder` 读一个简化版 `beans.xml`：

```xml
<beans>
    <bean id="userDao" class="com.example.UserDaoMysqlImpl"/>
    <bean id="userService" class="com.example.UserService">
        <property name="userDao" ref="userDao"/>
    </bean>
</beans>
```

提示：`DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(...)`，遍历 `getElementsByTagName("bean")`，把 `class` 用 `Class.forName()` 变成 `Class<?>`。

**② 加作用域**

给 `BeanDefinition` 加 `scope` 字段：`singleton` 走缓存，`prototype` 每次新建。

**③ 加三级缓存**

写两个互相依赖的 Bean（都用 Set 注入），观察不加缓存时无限递归 → `StackOverflowError`，然后照 [03 篇第 10 节](03-Spring对IoC的实现-DI依赖注入.md#10-三级缓存与循环依赖) 加 `singletonFactories` 解决。

**④ 加 byType 自动装配**

当 `properties` 里只给了属性名、没给 `ref` 时，按 setter 的参数类型去 `definitionMap` 里找唯一匹配的 Bean；找不到或找到多个就抛异常——这就是 Spring `autowire="byType"` 的雏形。

**⑤ 加 init-method**

在 `getBean` 返回前，如果 `BeanDefinition` 配了 `initMethod`，反射调用它。观察它和 Set 注入的先后顺序（应该是先注入、后 init）。

---

## 附：一句话总结

> **Spring 的 DI 没有任何魔法，就是"读配置 → 反射造对象 → 反射填依赖 → 存起来"。**
> 所有的复杂度都在"填依赖时怎么找到正确的那个 Bean"以及"什么时候填、填完还要做什么"上。

---

> 返回 [索引](00-学习路线与索引.md) · 回到重点篇：[03 Spring 对 IoC 的实现](03-Spring对IoC的实现-DI依赖注入.md)

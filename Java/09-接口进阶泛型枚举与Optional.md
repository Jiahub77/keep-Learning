# 接口进阶、泛型、枚举、record 与 Optional

本篇记录接口入门之后新增的学习内容。示例以商品通知和商品摘要为背景，代码片段独立使用。

## 1. 接口与构造器注入

业务类依赖接口，而不是依赖某个具体通知实现：

```java
public class StockService {
    private final Notifier notifier;

    public StockService(Notifier notifier) {
        this.notifier = notifier;
    }

    public void notifyLowStock(String productName) {
        notifier.send(productName + " 库存不足");
    }
}
```

创建时可以自由选择实现：

```java
StockService emailService = new StockService(new EmailNotifier());
emailService.notifyLowStock("保温杯");

StockService smsService = new StockService(new SmsNotifier());
smsService.notifyLowStock("炒锅");
```

增加 `WechatNotifier` 时不需要修改 `StockService`，只需传入新实现。这是构造器注入，也是 Spring 依赖注入的基础思想。

## 2. 声明类型与实际对象

```java
Notifier notifier = new WechatNotifier();
notifier.send("库存不足");
```

编译时看声明类型 `Notifier`，所以只能调用接口中声明的方法；运行时看实际对象 `WechatNotifier`，因此执行微信实现的 `send()`。

如果 `WechatNotifier` 还有 `checkConnection()`，下面第一行可以，第二行不能：

```java
WechatNotifier notifier = new WechatNotifier();
notifier.checkConnection();

Notifier notifier2 = new WechatNotifier();
notifier2.checkConnection(); // 编译错误
```

## 3. 多个实现与 default 方法

不同实现可以放进同一个接口列表：

```java
List<Notifier> notifiers = new ArrayList<>();
notifiers.add(new EmailNotifier());
notifiers.add(new SmsNotifier());

for (Notifier notifier : notifiers) {
    notifier.send("炒锅库存不足");
}
```

接口可以提供默认实现：

```java
public interface Notifier {
    void send(String message);

    default boolean isEnabled() {
        return true;
    }
}
```

实现类可重写它：

```java
@Override
public boolean isEnabled() {
    return false;
}
```

`false` 不会自动阻止发送，业务代码必须主动判断：

```java
for (Notifier notifier : notifiers) {
    if (notifier.isEnabled()) {
        notifier.send("库存不足");
    }
}
```

空列表可以正常遍历，循环执行 0 次，不会报错，也不会发送通知。

## 4. 泛型：使用时确定类型

泛型在复用代码的同时提供编译期类型检查：

```java
public class Box<T> {
    private T value;

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }
}
```

使用时指定类型：

```java
Box<String> nameBox = new Box<>();
nameBox.set("保温杯");
String name = nameBox.get();

Box<Product> productBox = new Box<>();
productBox.set(new Product("炒锅", 15900, 5));
Product product = productBox.get();
```

`Box<String>` 的 `set()` 只能接收 `String`，不能接收整数。数字的泛型参数使用包装类型：`Box<Integer>`，不能写 `Box<int>`。

### 泛型方法

```java
public static <T> T firstOrNull(List<T> items) {
    if (items.isEmpty()) {
        return null;
    }
    return items.get(0);
}
```

`<T>` 声明方法的类型参数，返回值和列表元素类型保持一致。`List<String>` 调用后返回 `String`，不能把它赋给 `Integer`。

### `extends` 通配符

```java
public static void sendAll(List<? extends Notifier> notifiers) {
    for (Notifier notifier : notifiers) {
        notifier.send("库存不足");
    }
}
```

`List<EmailNotifier>` 和 `List<SmsNotifier>` 都可以传入，因为元素一定是 `Notifier` 的子类型。读取时可以当作 `Notifier`，但不能向其中添加具体的邮件或短信对象，因为真实列表的具体类型未知。

需要同时添加不同实现时，声明为：

```java
List<Notifier> notifiers = new ArrayList<>();
```

## 5. 枚举 enum

枚举表示固定的一组合法值：

```java
public enum ProductStatus {
    DRAFT,
    ON_SALE,
    OFF_SHELF
}
```

使用和比较：

```java
ProductStatus status = ProductStatus.ON_SALE;

if (status == ProductStatus.ON_SALE) {
    System.out.println("商品正在销售");
}
```

枚举值使用 `==` 比较；字符串不能直接赋给枚举，拼写错误会在编译期暴露。可以用 `switch` 转换为展示文本：

```java
public static String describe(ProductStatus status) {
    return switch (status) {
        case DRAFT -> "草稿";
        case ON_SALE -> "在售";
        case OFF_SHELF -> "已下架";
    };
}
```

枚举与 null 使用 `status == ProductStatus.ON_SALE` 比较是安全的，结果为 false；但 `switch (status)` 不能直接处理 null，需要先判断。

## 6. record 数据对象

`record` 适合表示创建后不修改的 DTO 或返回结果：

```java
public record ProductSummary(String name, int priceInCents) {
}
```

使用：

```java
ProductSummary summary = new ProductSummary("保温杯", 9900);
System.out.println(summary.name());
System.out.println(summary.priceInCents());
System.out.println(summary);
```

Java 会自动生成构造器、访问方法、`equals()`、`hashCode()` 和 `toString()`。访问方法是 `name()`，不是 `getName()`；record 字段不能重新赋值。

`equals()` 比较字段内容，`==` 比较是否为同一个对象，因此两个相同内容的 record 通常 `equals()` 为 true，但 `==` 为 false。

可以用紧凑构造器校验参数：

```java
public record ProductSummary(String name, int priceInCents) {
    public ProductSummary {
        if (priceInCents < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
    }
}
```

创建负价格对象时，代码可编译，但运行时抛出 `IllegalArgumentException`。

## 7. Optional

`Optional<T>` 明确表示返回值可能存在，也可能不存在，适合替代“用 null 表示查找失败”的返回值：

```java
public static Optional<Product> findByName(
        List<Product> products, String name) {
    for (Product product : products) {
        if (product.getName().equals(name)) {
            return Optional.of(product);
        }
    }
    return Optional.empty();
}
```

常用处理方式：

```java
findByName(products, "炒锅")
        .ifPresent(Product::printInfo);

String text = Optional.<String>empty().orElse("默认值");
```

| 方法 | 作用 |
|---|---|
| `of(value)` | 创建有值 Optional，value 不能为 null |
| `ofNullable(value)` | null 转为空 Optional |
| `empty()` | 创建空 Optional |
| `isPresent()` | 判断是否有值 |
| `orElse(default)` | 没值时使用默认值 |
| `orElseGet(...)` | 没值时延迟计算默认值 |
| `ifPresent(...)` | 有值时执行操作 |
| `get()` | 取值；空时抛 `NoSuchElementException` |

不要在不确定有值时直接调用 `get()`。Optional 主要用于方法返回值，通常不要把它作为实体字段。

## 8. 当前学习状态

- 接口、多态、构造器注入、default 方法：已通过阅读和答题练习。
- 泛型、通配符、枚举、record、Optional：已完成概念练习。
- Java 环境仍未安装，以上代码尚未编译、运行或调试。
- 后续应先安装 JDK 21，实际运行这些示例，再进入 Maven 和 Spring Boot。

# Java 基础知识

本篇补齐前面商品案例中未系统展开的基础，使用 JDK 21。可先读第 1～6 节，再结合 [商品对象](02-Java基础与商品对象.md) 学习面向对象。

除标注为完整程序外，代码片段放在 `main` 方法内；方法定义与 `main` 并列，`import` 放在文件顶部。各片段独立使用，不代表已完成练习。

## 1. JDK、JVM 与程序入口

- JDK：开发工具包，包含 `javac` 编译器、`java` 启动器及运行所需组件。
- JVM：Java 虚拟机，执行编译生成的字节码。
- JRE：Java 运行环境这一概念，包含 JVM 与运行类库；学习时安装 JDK 即可，不必另装 JRE。

先用 `java -version` 和 `javac -version` 确认两者为 JDK 21。仅找到命令路径，不代表 JDK 已安装成功。

将下面的完整程序保存为 `Main.java`：

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("你好，Java！");
    }
}
```

在文件所在目录执行：

```bash
javac -encoding UTF-8 Main.java
java Main
```

第一步生成 `Main.class`，第二步输出 `你好，Java！`。运行时写类名 `Main`，不写 `Main.class`。JDK 21 也支持用 `java Main.java` 直接运行这个单文件程序。

Java 区分大小写；普通语句以分号结束，花括号定义代码块。`//` 是单行注释，`/* ... */` 是多行注释，`/** ... */` 用于文档注释。

## 2. 变量、基本类型与包装类型

变量声明形式为 `类型 名称 = 初始值;`。局部变量必须先赋值再使用；对象字段和数组元素有默认值，例如整数为 0、布尔值为 false、引用为 null。

| 基本类型 | 含义 | 示例 |
|---|---|---|
| byte | 8 位有符号整数，-128～127 | `byte level = 1;` |
| short | 16 位有符号整数 | `short count = 100;` |
| int | 32 位有符号整数，常用 | `int stock = 20;` |
| long | 64 位有符号整数 | `long id = 1001L;` |
| float | 32 位浮点数 | `float ratio = 0.5F;` |
| double | 64 位浮点数 | `double weight = 1.5;` |
| char | 一个 UTF-16 代码单元 | `char grade = 'A';` |
| boolean | 只能为 true 或 false | `boolean enabled = true;` |

`String`、数组和自定义类都是引用类型。`char` 用单引号，字符串用双引号；部分字符（如许多 emoji）需要两个 `char` 表示。

```java
int quantity = 3;
long wide = quantity;          // 自动拓宽
double average = 5 / 2.0;      // 2.5
int truncated = (int) 3.9;     // 3：截去小数部分
var name = "保温杯";           // 编译器推断为 String，此后不能赋值为整数
```

`var` 用于有初始化值的局部变量，不能用于字段、方法参数，也不能写 `var value = null`。数值转换不总是无损，例如 long 转 double 可能损失精度；强制缩窄转换也可能丢失数据。

基本类型对应包装类型：`Byte`、`Short`、`Integer`、`Long`、`Float`、`Double`、`Character`、`Boolean`。集合泛型只能使用引用类型，例如 `List<Integer>`。

```java
Integer boxed = 10;                  // 自动装箱
int value = boxed;                   // 自动拆箱
int parsed = Integer.parseInt("42"); // 字符串转 int；非法文本会抛异常
String text = String.valueOf(parsed);
```

包装类型可以为 null，拆箱 null 会抛出 `NullPointerException`。比较包装对象的值使用 `equals` 或 `Objects.equals`，不要依赖 `==` 和整数缓存。

## 3. 运算符与条件判断

| 类别 | 常用运算符 | 说明 |
|---|---|---|
| 算术 | `+ - * / %` | `%` 求余；整数相除仍为整数 |
| 比较 | `== != > >= < <=` | 结果为 boolean |
| 逻辑 | `&&`、｜｜、`!` | 与、或、非；前两个会短路 |
| 赋值 | `= += -= *= /=` | `=` 是赋值，不是比较 |
| 自增自减 | `++ --` | 初学时尽量单独成句 |
| 三元 | `条件 ? 值1 : 值2` | 按条件选择结果 |

```java
int priceInCents = 9900;
int quantity = 3;
long totalInCents = (long) priceInCents * quantity;
System.out.println(5 / 2);           // 2
System.out.println(5 % 2);           // 1
System.out.println("金额：" + 1 + 2); // 金额：12
System.out.println("金额：" + (1 + 2)); // 金额：3
String label = quantity > 0 ? "可购买" : "数量不合法";
```

先把乘法的一个操作数转为 long，乘法才按 long 计算；`long total = int值 * int值` 可能先在 int 范围内溢出。普通整数溢出不会自动报错；需要检查时可用 `Math.addExact`、`Math.multiplyExact`。

Java 的 `if` 必须接收 boolean，没有 JavaScript 的 truthy/falsy 转换。混合逻辑运算时用括号表达分组，空值判断放在访问对象之前。

## 4. 分支与循环

```java
int stock = 5;
if (stock == 0) {
    System.out.println("售罄");
} else if (stock < 10) {
    System.out.println("库存偏低");
} else {
    System.out.println("库存充足");
}

String status = "ON_SALE";
String label = switch (status) {
    case "ON_SALE" -> "在售";
    case "OFF_SALE" -> "下架";
    default -> "未知状态";
};
```

这里使用 switch 表达式，箭头分支不会落入下一个分支，必须覆盖所有可能输入或提供 default。本例 status 不能为 null；default 不会捕获 null。

```java
for (int i = 0; i < 3; i++) {
    System.out.println(i); // 0、1、2
}

int remaining = 2;
while (remaining > 0) {
    remaining--;
}

int attempts = 0;
do {
    attempts++;
} while (attempts < 1); // do-while 至少执行一次
```

- `break`：结束最近的一层循环；也可结束传统 switch 分支。
- `continue`：跳过本次循环剩余语句，进入下一轮。
- `return`：结束整个方法，不只是当前循环。

变量的作用域通常限制在声明所在的代码块内，例如上面 for 中的 i 不能在循环外使用。

## 5. 数组与字符串

### 数组

```java
int[] stocks = {10, 5, 0};
int total = 0;
for (int stock : stocks) {
    total += stock;
}
System.out.println(stocks.length); // 3，数组长度不带括号
System.out.println(total);         // 15
stocks[0] = 8;
int[] emptySlots = new int[3];      // 三个元素均为 0
```

数组长度创建后固定，下标范围为 0 到 length - 1；动态增删使用 List。增强 for 中给基本类型循环变量重新赋值，不会改变原数组元素。

数组赋值也只是复制引用。需要独立的一层数组时，可使用 `Arrays.copyOf(stocks, stocks.length)`（导入 `java.util.Arrays`）；若为对象数组，元素指向的对象仍然共享。

### 字符串

```java
String name = "  保温杯  ";
String cleaned = name.strip();
System.out.println(cleaned.equals("保温杯")); // true
System.out.println(cleaned.length());        // 3，字符串长度带括号
System.out.println(cleaned.contains("杯"));  // true
System.out.println(cleaned.substring(0, 2)); // 保温，起点包含、终点不包含
System.out.println("".isEmpty());            // true
System.out.println("  ".isBlank());          // true
```

String 不可变：`strip`、`replace` 等操作返回结果，不会修改原字符串。`length()` 统计 UTF-16 代码单元数量，不一定等于人眼看到的字符数。

引用类型的 `==` 判断是否指向同一个对象。字符串内容比较使用 `equals`；变量可能为 null 时可写 `"保温杯".equals(name)`，或导入 `java.util.Objects` 后写 `Objects.equals(a, b)`。

循环中大量拼接文本可使用 StringBuilder：

```java
StringBuilder builder = new StringBuilder();
for (int i = 1; i <= 3; i++) {
    builder.append("商品").append(i).append('\n');
}
String result = builder.toString();
```

## 6. 方法、重载与值传递

方法定义放在类内，与 main 并列：

```java
static long calculateTotal(int priceInCents, int quantity) {
    if (priceInCents < 0 || quantity <= 0) {
        throw new IllegalArgumentException("价格或数量不合法");
    }
    return (long) priceInCents * quantity;
}
```

在 main 中用 `long total = calculateTotal(9900, 3);` 调用，结果为 29700。返回类型为 void 的方法没有返回值；非 void 方法所有正常结束的路径都必须返回兼容类型的值。

同一个类中，方法同名、参数列表不同称为重载，例如 `print(int value)` 和 `print(String value)`。仅返回类型不同不能构成重载。

**Java 始终是值传递。**基本类型复制数值，引用类型复制引用值。下面是一个可单独保存为 `Main.java` 的完整程序：

```java
public class Main {
    static void change(int count, int[] stocks) {
        count = 99;
        stocks[0] = 8;
        stocks = new int[]{100};
    }

    public static void main(String[] args) {
        int count = 3;
        int[] stocks = {10};
        change(count, stocks);
        System.out.println(count);     // 3
        System.out.println(stocks[0]); // 8
    }
}
```

方法能通过复制的引用修改原数组，但将参数重新指向新数组，不会让调用者的变量也改为指向新数组。

## 7. 面向对象、访问控制与常用关键字

| 概念 | 含义 |
|---|---|
| 类与对象 | 类定义结构和行为，对象是具体实例 |
| 封装 | 隐藏内部状态，通过方法控制合法操作 |
| 继承 | `extends` 复用父类能力；类只能直接继承一个父类 |
| 接口 | `implements` 声明实现约定；类可实现多个接口 |
| 多态 | 通过父类或接口引用调用实例方法时，执行实际对象的重写实现 |
| 抽象类 | `abstract class` 可包含字段、构造器、普通方法和抽象方法，不能直接 new |

重写是子类提供父类实例方法的新实现，与重载不同。使用 `@Override` 让编译器检查。构造器不能被继承；子类构造器可用 `super(...)` 调用父类构造器，在 JDK 21 中该调用须放在构造器第一条语句。

### 访问控制

| 修饰符 | 访问范围 |
|---|---|
| private | 所属顶层类及其嵌套类型内部；普通业务代码可先理解为本类内部 |
| 不写修饰符 | 同一个包内 |
| protected | 同包，以及其他包中的子类（跨包访问还有接收者类型限制） |
| public | 类型及模块可访问的前提下，对外公开 |

顶层类只能用 public 或不写访问修饰符，不能声明为 private 或 protected。

### static、final、this

- `static` 成员属于类。静态方法没有 this，不能直接访问实例字段，必须先取得对象。
- `final` 变量只能赋值一次；final 引用不能改指向，但引用对象的内容仍可能变化。
- `final` 方法不能被重写，final 类不能被继承。
- `this` 表示当前实例，常用于区分同名字段与参数。

例如 `final int[] stocks = {10};` 允许 `stocks[0] = 8;`，但不允许 `stocks = new int[]{8};`。常量通常写成 `static final int MAX_PAGE_SIZE = 100;`，定义在类体中。

### package 与 import

`package learning;` 放在源文件开头，声明类型所属的包；import 写在 package 之后、类定义之前。`java.lang` 中的 String、System 等无需显式导入，`java.util.List` 等需要导入或使用全限定名。

在常规 Maven 项目中，`learning.Product` 对应 `src/main/java/learning/Product.java`。import 只是让源码可以使用短类型名，不会下载依赖；第三方依赖由 Maven 等构建工具管理。

## 8. 集合、泛型与对象相等

| 类型 | 主要特点 | 常用实现 |
|---|---|---|
| List | 有顺序，允许重复元素 | ArrayList |
| Set | 元素不重复 | HashSet |
| Map | 键唯一，键映射到值 | HashMap |

HashSet、HashMap 不保证遍历顺序。需要按插入顺序遍历时可用 LinkedHashSet、LinkedHashMap。详细操作见 [List](03-List集合与方法返回值.md)、[Map](04-Map查询与条件判断.md) 和 [Stream](05-Stream筛选统计与排序.md)。

泛型让编译器检查元素类型，例如 `List<String>` 不能添加 Integer。`List<Integer>` 也不是 `List<Number>` 的子类型，不能直接相互赋值。

```java
// 文件顶部导入 java.util.List、java.util.ArrayList
List<String> fixed = List.of("保温杯", "餐盘");
List<String> editable = new ArrayList<>(fixed);
editable.add("炒锅");
editable.removeIf(name -> name.equals("餐盘"));
```

`List.of` 返回不可修改的列表，且不允许 null 元素；复制到 ArrayList 后可以增删。不要在增强 for 中直接对原列表执行结构性增删，可使用 removeIf 或迭代器支持的删除方式。

自定义对象放入 HashSet 或作为 HashMap 键时，如果希望按业务字段判等，应同时实现 equals 和 hashCode。相等对象必须有相同哈希值，哈希值相同不代表对象相等。对象作为键后，不要修改参与判等和哈希计算的字段。

## 9. 异常与资源关闭

- `throw`：实际抛出异常对象。
- `throws`：在方法签名中声明可能向外传播的异常。
- 受检异常：编译器要求捕获或声明，例如 IOException。
- RuntimeException 及其子类：不强制捕获或声明，例如 IllegalArgumentException、NullPointerException。
- `finally`：用于清理，正常控制流下即使 try 中 return 也会执行；进程被强制终止等情况下不能保证执行。不要在 finally 中 return，以免覆盖结果或异常。

读取文件时可用 try-with-resources 自动关闭资源。下面的方法定义在类体内，文件顶部导入 `java.io.BufferedReader`、`java.io.IOException`、`java.nio.file.Files`、`java.nio.file.Path`、`java.nio.charset.StandardCharsets`：

```java
static String readFirstLine(Path path) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        return reader.readLine(); // 空文件返回 null
    }
}
```

调用者仍需捕获或继续声明 IOException。try-with-resources 管理实现 AutoCloseable 的资源，即使读取失败也会尝试关闭。不要用空 catch 隐藏错误；基础示例见 [异常处理与接口入门](06-异常处理与接口入门.md)。

## 10. 金额、日期与枚举

### 金额计算

前面的商品案例使用整数分。本节独立演示 BigDecimal，不改变 Product 的字段约定。文件顶部导入 `java.math.BigDecimal`、`java.math.RoundingMode`：

```java
BigDecimal price = new BigDecimal("99.00");
BigDecimal total = price.multiply(BigDecimal.valueOf(3));
BigDecimal average = total.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
System.out.println(total);   // 297.00
System.out.println(average); // 42.43
```

避免 `new BigDecimal(0.1)` 引入二进制浮点误差，优先使用字符串构造或合适的 valueOf。除法可能出现无限小数，应按业务要求指定精度与舍入方式。

`new BigDecimal("1.0").equals(new BigDecimal("1.00"))` 为 false，因为 equals 还比较 scale；只比较数值大小时使用 compareTo，返回 0 表示数值相等。

### 日期与时间

| 类型 | 用途 |
|---|---|
| LocalDate | 日期，例如生日，不含时区 |
| LocalTime | 时间，不含日期或时区 |
| LocalDateTime | 日期与时间，不含时区，不能单独确定全球唯一时刻 |
| Instant | 时间线上的时刻 |
| ZonedDateTime | 带时区的日期时间 |

```java
// 文件顶部导入 java.time.LocalDate、java.time.Instant、java.time.ZoneId
LocalDate date = LocalDate.parse("2026-10-28");
System.out.println(date.plusDays(7)); // 2026-11-04；原 date 不变
Instant instant = Instant.parse("2026-10-28T00:00:00Z");
System.out.println(instant.atZone(ZoneId.of("Asia/Shanghai")));
// 2026-10-28T08:00+08:00[Asia/Shanghai]
```

### 枚举

状态取值固定时可定义 `enum ProductStatus { ON_SALE, OFF_SALE }`，放在类体内或独立文件中。在方法内使用 `ProductStatus status = ProductStatus.ON_SALE;`。枚举可以安全地用 `==` 比较，能减少拼错状态字符串的问题。

## 11. 自测与实践

先解释原因，再运行验证：

1. `double value = 5 / 2;` 得到多少？如何得到 2.5？
2. String 的 `==` 与 equals 有什么区别？null 应怎样比较？
3. 方法修改数组元素和重新给数组参数赋值，对调用者有什么不同影响？
4. final 修饰 List 引用后，能否调用 add？
5. 重载、重写、static 方法分别解决什么问题？
6. 为什么 BigDecimal 比较金额大小通常使用 compareTo？

参考答案：① 2.0，改为 `5 / 2.0`；② 身份与内容的区别，空值安全比较可用 Objects.equals；③ 元素修改可见，参数重新赋值不改变调用者的变量；④ 取决于列表实现是否允许修改，final 本身不禁止 add；⑤ 重载提供不同参数入口，重写定制继承的方法行为，static 方法无需当前实例；⑥ compareTo 比较数值，不要求 scale 相同。

综合练习：声明库存数组 `{10, 0, 5, 3}`，编写静态方法统计库存总量、有库存商品种类数和最大库存。预期结果为 18、3、10；再验证空数组，约定三项都返回 0。最后将循环版本改写为 Stream 版本，对照结果。

建议顺序：能独立编译运行 → 掌握类型、条件和循环 → 理解对象引用与方法调用 → 完成集合和异常练习 → 再进入 Maven、Spring Boot。

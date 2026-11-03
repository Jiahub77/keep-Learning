# Stream 筛选、统计与排序

## 1. 示例数据

以下片段均从这份数据独立开始：

```java
List<Product> products = new ArrayList<>();
products.add(new Product("保温杯", 9900, 10));
products.add(new Product("餐盘", 3000, 0));
products.add(new Product("炒锅", 15900, 5));
```

Stream 类似 JavaScript 数组的链式处理。中间操作描述处理步骤，count、sum、toList 等终止操作触发处理；同一个 Stream 不能重复消费，需要重新调用 products.stream()。

## 2. 筛选与转换

```java
List<String> names = products.stream()
        .filter(product -> product.getStock() >= 5)
        .map(Product::getName)
        .toList();
```

结果为 `[保温杯, 炒锅]`，原列表仍有三个元素。

- filter：按布尔条件保留元素。
- map：转换每个元素。
- toList：收集为不可增删替换的列表；不是深拷贝，其中对象仍可能被修改。

## 3. Lambda 与方法引用

```java
product -> product.getName()
Product::getName
```

在此场景中两者作用相同。方法引用不加调用括号。

```java
.filter(product -> product.getPriceInCents() < 10000)
.filter(product -> product.getStock() > 0)
.map(Product::getName)
```

有比较等额外逻辑时使用 Lambda，不能写 `Product::getStock() > 0`。注意 filter 的拼写，不是 fliter。

## 4. 数量与求和

```java
long count = products.stream()
        .filter(product -> product.getStock() >= 5)
        .count(); // 2

int totalStock = products.stream()
        .filter(product -> product.getStock() >= 5)
        .mapToInt(Product::getStock)
        .sum(); // 15
```

count 回答“有几个”，sum 回答“数值相加是多少”。普通 Stream<Product> 不能直接调用 sum；先转成数值流。

count 的 API 返回类型固定为 long，即使结果很小也不能直接赋给 int。int 最大值为 2,147,483,647，long 最大值为 9,223,372,036,854,775,807。

确实需要 int 时：

```java
int count = Math.toIntExact(products.stream().count());
```

超出 int 范围会抛出异常。mapToInt 得到 IntStream，其 sum 返回 int；大数值累加需考虑溢出，可以改用 mapToLong，仍需注意 long 的范围。

## 5. 排序与截取

```java
import java.util.Comparator;

List<String> names = products.stream()
        .filter(product -> product.getStock() > 0)
        .sorted(Comparator.comparingInt(Product::getPriceInCents))
        .limit(1)
        .map(Product::getName)
        .toList();
```

结果为 `[保温杯]`：有库存商品中价格最低的一款。

- comparingInt 默认升序。
- 比较器后加 reversed() 改为降序。
- limit(n) 保留前 n 个，不足 n 个时保留全部。
- 本例 sorted 不改变原列表顺序。

综合练习：有库存商品按库存降序，取前两个名称。

```java
List<String> names = products.stream()
        .filter(product -> product.getStock() > 0)
        .sorted(Comparator.comparingInt(Product::getStock).reversed())
        .limit(2)
        .map(Product::getName)
        .toList();
```

结果为 `[保温杯, 炒锅]`。如果按价格降序，结果则是 `[炒锅, 保温杯]`。

## 6. 操作速查

| 操作 | 用途 |
|---|---|
| filter | 筛选 |
| map | 转换 |
| count | 计数，返回 long |
| mapToInt + sum | 整数求和，返回 int |
| sorted | 排序 |
| limit | 截取前若干个 |
| toList | 收集结果列表 |

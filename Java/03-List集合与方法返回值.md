# List 集合与方法返回值

## 1. 创建列表

```java
import java.util.ArrayList;
import java.util.List;

// 放在 main 方法中
List<Product> products = new ArrayList<>();
products.add(new Product("保温杯", 9900, 10));
products.add(new Product("餐盘", 3000, 5));
```

`List` 是接口，`ArrayList` 是常用实现。`List<Product>` 限定元素类型，右侧 `<>` 让编译器推断类型。

| 操作 | JavaScript | Java |
|---|---|---|
| 添加 | push(item) | add(item) |
| 长度 | length | size() |
| 取元素 | items[0] | get(0) |
| 替换 | items[0] = item | set(0, item) |
| 按下标删除 | splice(0, 1) | remove(0) |

下标从 0 开始，越界访问抛出异常。删除第一个元素后，后面元素的下标前移。

`get()` 返回对象引用：`products.get(0).purchase(3)` 会修改列表中那个商品的库存，不是修改副本。

## 2. 遍历与查找

以下静态方法定义在 Main 类中，与 main 方法并列。

```java
public static Product findByName(List<Product> products, String name) {
    for (Product product : products) {
        if (product.getName().equals(name)) {
            return product;
        }
    }
    return null;
}
```

字符串内容比较使用 `.equals()`；引用类型的 `==` 比较是否指向同一个对象。此示例假定商品名称不为 null。

```java
Product found = findByName(products, "餐盘");
if (found == null) {
    System.out.println("没有找到商品");
} else if (found.purchase(4)) {
    System.out.println("购买成功");
} else {
    System.out.println("数量不合法或库存不足");
}
```

找不到商品返回 null；对 null 调用方法会抛出 `NullPointerException`。库存不足则由 purchase 返回 false，两者不同。

## 3. 统计库存与商品种类

```java
int totalStock = 0;
for (Product product : products) {
    totalStock += product.getStock();
}
```

统计有库存的商品种类数，而不是库存总量：

```java
public static int countInStock(List<Product> products) {
    int hasStockGoods = 0;
    for (Product product : products) {
        if (product.getStock() > 0) {
            hasStockGoods += 1;
        }
    }
    return hasStockGoods;
}
```

计数变量放在循环外。`+= 1` 可以写为 `++`。库存 10、5、0 对应总库存 15，有库存种类数 2。

## 4. 空列表与 null

- `countInStock(new ArrayList<>())` 返回 0：列表存在，但没有元素。
- `countInStock(null)` 抛出空指针异常：没有可遍历的列表对象。

`System.out.println(product)` 可以打印值为 null 的 Product 引用；但 `product.printInfo()` 不能在 product 为 null 时调用。

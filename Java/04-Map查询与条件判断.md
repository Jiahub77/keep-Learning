# Map 查询与条件判断

## 1. 按编号保存商品

```java
import java.util.HashMap;
import java.util.Map;

Map<Long, Product> productsById = new HashMap<>();
productsById.put(1001L, new Product("保温杯", 9900, 10));
productsById.put(1002L, new Product("餐盘", 3000, 5));
```

泛型使用包装类型 `Long`，不能写基本类型 `long`。数字后的 L 表示 long 字面量；传入 long 参数时可自动装箱为 Long。

| 方法 | 作用 |
|---|---|
| put(key, value) | 添加或替换键对应的值 |
| get(key) | 取值，键不存在时返回 null |
| remove(key) | 删除键对应的记录 |
| containsKey(key) | 判断键是否存在，返回 boolean |
| size() | 记录数量 |

再次 put 相同键会替换该键的值，不会清空其他记录。将 1001 替换为炒锅后，Map 仍有两条记录。HashMap 不保证遍历顺序。

## 2. 查询后执行业务

```java
public static boolean purchaseById(
        Map<Long, Product> productsById, long id, int quantity) {
    Product product = productsById.get(id);
    if (product == null) {
        return false;
    }
    return product.purchase(quantity);
}

public static boolean restockById(
        Map<Long, Product> productsById, long id, int quantity) {
    Product product = productsById.get(id);
    if (product != null) {
        return product.restock(quantity);
    }
    return false;
}
```

商品存在不等于操作成功，必须返回 purchase/restock 的真实结果，不能调用后无条件返回 true。

例如炒锅库存 8：买 10 件返回 false；再买 3 件返回 true，库存 5；查询不存在的编号后购买返回 false，而不是 null。

HashMap 允许 null 值，因此 containsKey 为 true 也不绝对保证取出的对象非 null。上述方法将取到 null 统一视为不可操作。

## 3. 只检查，不修改库存

```java
public static boolean hasEnoughStock(
        Map<Long, Product> productsById, long id, int quantity) {
    Product product = productsById.get(id);
    if (product == null || quantity <= 0) {
        return false;
    }
    return product.getStock() >= quantity;
}
```

库存 5 时，检查数量 5、6、0 分别返回 true、false、false，库存不变。

## 4. 布尔返回值与短路

Java 的 if 条件必须是 boolean，不能像 JavaScript 那样写 `if (product)`，应该写 `if (product != null)`。

基本类型 boolean 不能返回 int 或 null。非 void 方法所有正常结束的路径都必须有返回值：

```java
public static boolean exists(Map<Long, Product> productsById, long id) {
    return productsById.containsKey(id);
}
```

条件从左到右计算：

- `A || B`：A 为 true 时，不再计算 B。
- `A && B`：A 为 false 时，不再计算 B。

安全写法：

```java
return product != null
        && quantity > 0
        && product.getStock() >= quantity;
```

错误顺序：

```java
return product.getStock() >= quantity && product != null;
```

后者在 product 为 null 时，先访问对象，立即抛出空指针异常。

```java
return product != null && product.restock(quantity);
```

这也是合法的短路表达式，但调用 restock 会改变库存，不是纯检查。

# Java 基础与商品对象

## 1. 与 JavaScript 的区别

Java 变量有明确类型，不能像 JavaScript 变量那样任意更换值的类型。

```java
int stock = 20;
long productId = 1001L;
double weight = 1.5;
boolean enabled = true;
String name = "保温杯";
```

`String` 是引用类型，其余上述示例是基本类型。金额不直接用 `double` 做精确计算：本课用整数分，后续可学 `BigDecimal`。

## 2. 完整商品示例

以下代码可保存为 `Main.java`。公开顶层类名应与文件名一致；这里的非公开 `Product` 可以放在同一文件中。

```java
public class Main {
    public static void main(String[] args) {
        Product product = new Product("保温杯", 9900, 10);
        product.printInfo();
        System.out.println("购买 3 件：" + product.purchase(3));
        product.printInfo();
        System.out.println("购买 20 件：" + product.purchase(20));
    }
}

class Product {
    private String name;
    private int priceInCents;
    private int stock;

    public Product(String name, int priceInCents, int stock) {
        if (priceInCents < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("库存不能为负数");
        }
        this.name = name;
        this.priceInCents = priceInCents;
        this.stock = stock;
    }

    public boolean purchase(int quantity) {
        if (quantity <= 0 || quantity > stock) {
            return false;
        }
        stock -= quantity;
        return true;
    }

    public boolean restock(int quantity) {
        if (quantity <= 0) {
            return false;
        }
        stock += quantity;
        return true;
    }

    public String getName() {
        return name;
    }

    public int getPriceInCents() {
        return priceInCents;
    }

    public int getStock() {
        return stock;
    }

    public void printInfo() {
        System.out.println(name + "，价格：" + priceInCents + " 分，库存：" + stock);
    }
}
```

预期输出：

```text
保温杯，价格：9900 分，库存：10
购买 3 件：true
保温杯，价格：9900 分，库存：7
购买 20 件：false
```

## 3. 关键概念

- 类定义结构与行为，`new` 创建对象。
- 构造器与类同名，没有返回类型，用来初始化对象。
- `this.stock = stock`：左边是当前对象字段，右边是参数。
- `private` 限制直接访问；`Main` 不能直接写 `product.stock = -100`。
- 封装不自动保证数据合法，构造器和业务方法仍需校验。
- `boolean` 方法返回 true 或 false，`void` 表示没有返回值。
- `return` 立即结束当前方法；后面的语句不再执行。
- `main` 是本课使用的程序入口，`static` 表示调用它不需要先创建 Main 对象。

## 4. 对象引用

```java
Product cup = new Product("保温杯", 9900, 10);
Product another = cup;
another.purchase(3);
```

只创建了一个对象；两个变量都指向它，因此两边看到的库存都是 7。赋值引用不会复制商品。

```java
another = new Product("餐盘", 3000, 5);
another.purchase(2);
```

现在累计创建了两个对象。`cup` 仍指向保温杯，库存为 7；`another` 指向餐盘，库存为 3。

## 5. 调试练习（待实际完成）

在 `stock += quantity` 处打断点并 Debug。假设调用前库存 7、补货量 5，暂停在赋值语句前时，`quantity` 为 5，`this.stock` 为 7；Step Over 后分别为 5 和 12。

补充验证：负价格或负库存创建失败，零价格和零库存允许；购买 0、负数、超出库存均失败，购买全部库存成功。

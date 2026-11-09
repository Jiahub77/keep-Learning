package StringExplanation;/* String 和 StringBuffer、 StringBuilder 类
* `String`、`StringBuffer` 和 StringBuilder 都能处理字符串，
* 主要区别是：内容能不能修改，以及是否提供线程安全保证。
* 日常文本用 String，循环拼接用 StringBuilder
* */

public class StringExplanation {
    public static void main(String[] args) {
        // 原来的 "小明" 没有被修改。拼接产生了新的字符串 "小明同学"，变量 name 改为引用新字符串
        String name = "小明";
        name = name + "同学";
        System.out.println(name); // 小明同学

        // toUpperCase() 返回转换后的字符串，原字符串保持原样，需要接收返回值。
        String text = "hello";
        text.toUpperCase();
        System.out.println(text); // hello
        text = text.toUpperCase();
        System.out.println(text); // HELLO

        // 字符串比较内容时使用 equals()
        String a = new String("Java");
        String b = new String("Java");
        System.out.println(a == b); // false
        System.out.println(a.equals(b)); // true

        // StringBuilder 可以理解为一个能够不断添加、删除和修改文字的容器。
        StringBuilder builder = new StringBuilder("Hello");

        // 插入
        builder.append(" Java");
        System.out.println(builder); // Hello Java

        // 在下标0的位置插入
        builder.insert(0, "开始 ");
        System.out.println(builder); // 开始 Hello Java

        // 删除下标[0, 2)的内容
        builder.delete(0, 3);
        System.out.println(builder); // Hello Java

        // 替换下标[0, 4)的内容
        builder.replace(0, 5, "Python");
        System.out.println(builder); // Python Java

        builder.setCharAt(0, 'A');
        System.out.println(builder); // Aython Java

        // 反转
        builder.reverse();
        System.out.println(builder); // avaJ nohtyA

        // 得到一个普通字符串
        String result = builder.toString();

        // 为什么循环拼接推荐 StringBuilder
        // 每次循环都会形成新的字符串结果。循环次数多、内容长时，会产生额外的复制和分配开销
        String strResult = "";
        for (int i = 1; i <= 5; i++) {
            strResult += i;
        }
        System.out.println(strResult); // 12345

        // 使用 StringBuilder:它在同一个可变容器中累积内容，适合这种反复拼接的场景。
        StringBuilder StrBuilder = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            builder.append(i);
        }
        String Str = StrBuilder.toString();
        System.out.println(Str); // 12345

        /* StringBuffer与 StringBuilder 基本一致，例如 append()、`insert()`、`delete()` 和 toString()
        * `StringBuffer` 的主要方法带有同步保护，多个线程共享同一个对象时，单次方法操作有线程安全保证；多个操作组合起来的业务逻辑，仍可能需要额外同步。
        * 现在写普通入门程序，通常选择 StringBuilder。
        * */
        StringBuffer buffer = new StringBuffer("Hello");
        buffer.append(" Java");
        buffer.insert(0, "你好，");
        System.out.println(buffer); // 你好，Hello Java

        // 注意两个坑
        // StringBuilder 和 StringBuffer 的 equals() 不会像 String 那样比较文本内容
        StringBuilder c = new StringBuilder("Java");
        StringBuilder d = new StringBuilder("Java");
        System.out.println(c.equals(d)); // false
        System.out.println(c.toString().equals(d.toString())); // true

        // length() 和 capacity() 含义不同
        StringBuilder e = new StringBuilder(100);
        System.out.println(e.length());   // 0：当前内容的长度
        System.out.println(e.capacity()); // 100：当前内部容量


        // 练习：用 StringBuilder 将数组 {"Java", "MySQL", "Spring"} 拼接成 Java, MySQL, Spring，注意最后一个元素后面不要多出逗号
        StringBuilder f = new StringBuilder();
        String[] arr = {"Java", "MySQL", "Spring"};
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                f.append(", ");
            }
            f.append(arr[i]);
        }
        System.out.println(f);
    }
}

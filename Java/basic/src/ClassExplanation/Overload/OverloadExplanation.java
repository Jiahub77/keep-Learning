package ClassExplanation.Overload;

// 重载是同名方法接收不同参数；重写是子类重新实现继承来的实例方法。
public class OverloadExplanation {
    // 方法重载：同名，不同参数。构造方法可以重载，但不能被继承或重写。
    static int add(int a, int b) {
        return a + b;
    }

    static double add(double a, double b) {
        return a + b;
    }

    static int add(int a, int b, int c) {
        return a + b + c;
    }

    public static void main(String[] args) {
        System.out.println(add(2, 3));       // 5
        System.out.println(add(2.5, 3.5));   // 6.0
        System.out.println(add(2, 3, 4));    // 9
    }
}

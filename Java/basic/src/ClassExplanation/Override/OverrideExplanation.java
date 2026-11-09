package ClassExplanation.Override;

// 重写是子类重新实现继承来的实例方法。
/*
* 方法名和参数列表必须相同。
* 返回类型必须相同，或是允许的更具体引用类型；基本类型返回值必须一致。
* 访问权限不能缩小：父类是 public，子类也必须是 public。
* final 方法不能重写。
* private 方法不被子类继承，因此不能重写。
* static 方法属于类，同名同参数时属于隐藏，不属于重写。
* 重写时也不能声明比父类更宽泛的受检异常
* */
class Animal {
    public void speak() {
        System.out.println("动物发出声音");
    }
}

class Dog extends Animal {
    @Override
    public void speak() {
        super.speak();
        System.out.println("汪汪");
    }
}

public class OverrideExplanation {
    public static void main(String[] args) {
        Animal animal = new Animal();
        Dog dog = new Dog();

        animal.speak(); // 动物发出声音
        dog.speak();    // 汪汪


        /* 练习
        * 创建 Course 类，提供 public void study()，输出 "学习课程"。
        * 在 Course 中重载 public void study(int minutes)，输出 "学习了 X 分钟"。
        * 创建 JavaCourse extends Course，重写无参 study()，输出 "学习 Java"。
        * 在 main 中运行下面的代码，先预测结果，再验证：*/
//        JavaCourse course = new JavaCourse();
//        course.study();
//        course.study(30);
//
//        Course other = new JavaCourse();
//        other.study();
    }
}
//class Course {
//    public void study() {
//        System.out.println("学习课程");
//    }
//
//    public void study(int minutes) {
//        System.out.println("学习了 " + minutes + " 分钟");
//    }
//}
//
//class JavaCourse extends  Course {
//    @Override
//    public void study() {
//        System.out.println("学习 Java");
//    }
//}

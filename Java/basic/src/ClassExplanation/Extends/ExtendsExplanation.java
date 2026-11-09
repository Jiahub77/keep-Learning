package ClassExplanation.Extends;

class Course {
    private String name;
    private int minutes;

    Course() {

    }
    Course(String name, int minutes) {
        this.name = name;
        this.minutes = minutes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("学习时长不能为负数");
        }

        this.minutes = minutes;
    }
}

class JavaCourse extends Course {
    JavaCourse(int minutes) {
        super("Java", minutes);
    }

    public void practice() {
        System.out.println("编写 Java 代码");
    }
}

/*
* 一个类只能直接继承一个父类。
* 构造方法不会被继承，子类需要定义自己的构造方法。
* 子类不能直接访问父类的 private 字段，可以通过父类提供的方法操作。
* final 类不能被继承，`final` 方法不能被重写。
* */
public class ExtendsExplanation {
    public static void main(String[] args) {
        JavaCourse course = new JavaCourse(30);

        System.out.println(course.getName()); // 继承的方法
        System.out.println(course.getMinutes());
        course.practice();                   // 子类新增的方法
    }
}

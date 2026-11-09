package ClassExplanation.Interface;

// 接口：约定一种能力
interface Practice {
    // 隐含为 public abstract
    void practice();

    // 提供默认行为，可以直接使用 prepare()，也可以重写它。
    default void prepare() {
        System.out.println("准备练习环境");
    }
}

abstract class Course {
    private String name;

    Course(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // 抽象方法，只有声明，没有方法体
    public abstract void study();
}

class JavaCourse extends Course implements Practice {

    JavaCourse(String name) {
        super(name);
    }

    @Override
    public void study() {
        System.out.println("学习 Java 类和对象");
    }

    @Override
    public void practice() {
        System.out.println("编写 Java 代码");
    }
}

public class InterfaceExplanation {
    public static void main(String[] args) {
        JavaCourse javaCourse = new JavaCourse("Java");
        Course course = javaCourse;
        course.study();

        Practice practice = javaCourse;
        practice.practice();
        practice.prepare();
    }
}

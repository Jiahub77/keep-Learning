package ClassExplanation.Abstract;

/* 抽象类：提取共同部分，留下实现要求
* 普通字段和普通方法。
* 构造方法
* 抽象方法，也可以没有抽象方法
* 不能直接创建抽象类对象
* */
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

class JavaCourse extends Course {
    JavaCourse(String name) {
        super(name);
    }

    @Override
    public void study() {
        System.out.println("学习 Java 类和对象");
    }
}

class MySqlCourse extends Course {
    MySqlCourse() {
        super("MySQL");
    }

    @Override
    public void study() {
        System.out.println("练习 SQL 查询");
    }
}

public class AbstractExplanation {
    public static void main(String[] args) {
        Course[] courses = {
                new JavaCourse("Java"),
                new MySqlCourse()
        };

        for (Course course : courses) {
            course.study();
        }
    }
}

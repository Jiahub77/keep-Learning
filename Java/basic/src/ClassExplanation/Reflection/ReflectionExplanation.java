package ClassExplanation.Reflection;

import java.lang.reflect.Method;

// 反射让程序在运行时，根据类名、方法名等信息，查看类、创建对象、调用方法
public class JavaCourse {
    public void study() {
        System.out.println("学习 Java");
    }
}
public class ReflectionExplanation {
    public static void main(String[] args) {
        JavaCourse course = new JavaCourse();

        Class<?> type = course.getClass();
        Method method = type.getMethod("study");
        method.invoke(course);
    }
}

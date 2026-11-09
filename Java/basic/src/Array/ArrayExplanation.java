package Array;

import java.util.Arrays;

public class ArrayExplanation {
    public static void main(String[] args) {
        String[] courses = {"Java", "MySQL", "Spring"};

        System.out.println(courses[0]); // Java

        courses[1] = "Redis";
        System.out.println(courses[1]); // Redis

        System.out.println(courses.length); // 3

        // 先指定长度，再赋值
        int[] scores = new int[3];

        scores[0] = 85;
        scores[1] = 90;

        System.out.println(scores[2]); // 0

        // 打印内容
        System.out.println(Arrays.toString(scores)); // [85, 90, 0]

        // 排序：会修改原数组
        Arrays.sort(scores);
        System.out.println(Arrays.toString(scores)); // [0, 85, 90]

        // 复制成一个独立数组
        int[] copy = Arrays.copyOf(scores, scores.length);

        // 比较元素内容
        System.out.println(Arrays.equals(scores, copy)); // true

        // 直接赋值不会复制数组
        int[] a = {10, 20};
        int[] b = a;

        b[0] = 99;
        System.out.println(a[0]); // 99

        // 数组越界会报错
        int[] nums = {10, 20, 30};
        System.out.println(nums[3]); // ArrayIndexOutOfBoundsException
    }
}

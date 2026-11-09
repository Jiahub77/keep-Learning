package DateObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateExplanation {
    public static void main(String[] args) {
        /*
        * LocalDate:日期 2026-11-09
        * LocalTime：时间 14:30:00
        * LocalDateTime：日期和时间 2026-11-09T14:30:00
        * ZonedDateTime：带时区的日期时间 上海时区的某个日期时间
        * Instant：时间线上的一个时刻 日志或事件发生的时刻
        * */
        // 当前日期时间，按运行环境的默认时区获取
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        LocalDateTime current = LocalDateTime.now();

        System.out.println(date);    // 2026-11-09
        System.out.println(time);    // 15:23:43.461
        System.out.println(current); // 2026-11-09T15:23:43.461

        // 指定日期时间
        LocalDate date1 = LocalDate.of(2026, 11, 9);
        LocalTime time1 = LocalTime.of(14, 30);
        LocalDateTime meeting = LocalDateTime.of(2026, 11, 9, 14, 30);

        System.out.println(date1);    // 2026-11-09
        System.out.println(time1);    // 14:30
        System.out.println(meeting); // 2026-11-09T14:30

        // 读取日期中的信息
        System.out.println(date.getYear());       // 2026
        System.out.println(date.getMonthValue()); // 11
        System.out.println(date.getDayOfMonth()); // 9
        System.out.println(date.getDayOfWeek());  // MONDAY

        // 日期加减与比较,日期时间对象不可变，修改操作会返回新对象，这一点与 String 类似。
        LocalDate date2 = LocalDate.of(2026, 11, 9);
        LocalDate tomorrow = date2.plusDays(1);
        LocalDate nextWeek = date.plusWeeks(1);
        LocalDate lastMonth = date.minusMonths(1);

        System.out.println(date2);     // 2026-11-09，原日期没有变化
        System.out.println(tomorrow); // 2026-11-10
        System.out.println(nextWeek); // 2026-11-16
        System.out.println(lastMonth);// 2026-10-09

        // 比较日期
        LocalDate start = LocalDate.of(2026, 11, 9);
        LocalDate end = LocalDate.of(2026, 11, 12);

        System.out.println(start.isBefore(end)); // true
        System.out.println(start.isAfter(end));  // false
        System.out.println(start.equals(end));   // false

        // 计算相隔多少天
        long days = ChronoUnit.DAYS.between(start, end);
        System.out.println(days); // 3

        // 字符串与日期互相转换
        LocalDate date3 = LocalDate.of(2026, 11, 9);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        String text = date3.format(formatter);
        System.out.println(text); // 2026年11月09日

        LocalDate date4 = LocalDate.parse("2026-11-09");
        System.out.println(date4);

        // 默认支持上面的 ISO 日期格式。其他格式可以指定规则：
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate date5 = LocalDate.parse("2026/11/09", formatter1);
        System.out.println(date5.format(formatter1));


        // 练习
        String[] courses = {"Java", "MySQL", "Spring"};
        LocalDate start1 = LocalDate.of(2026, 11, 9);
        for (int i = 0; i < courses.length; i++) {
            System.out.println(start1.plusDays(i) + ": " + courses[i]);
        }
    }
}

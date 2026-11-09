package ClassExplanation.Enum;

enum StudyStatus {
    NOT_STARTED("未开始"),
    LEARNING("学习中"),
    COMPLETED("已完成");

    private final String label;

    StudyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

public class EnumExplanation {
    public static void main(String[] args) {
        StudyStatus status = StudyStatus.LEARNING;

        if (status == StudyStatus.LEARNING) {
            System.out.println("正在学习");
        }

        System.out.println(StudyStatus.LEARNING.getLabel()); // 学习中
    }
}

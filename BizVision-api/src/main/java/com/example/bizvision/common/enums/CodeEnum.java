package com.example.bizvision.common.enums;

public interface CodeEnum {

    int getCode();

    String getLabel();

    /** 按 code 反查枚举，查不到直接报错 */
    static <E extends Enum<E>> E of(Class<E> type, int code) {
        for (E constant : type.getEnumConstants()) {
            if (((CodeEnum) constant).getCode() == code) {
                return constant;
            }
        }
        throw new IllegalArgumentException("未知的枚举编码: " + code + "（" + type.getSimpleName() + "）");
    }
}

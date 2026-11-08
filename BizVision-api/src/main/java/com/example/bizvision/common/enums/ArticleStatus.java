package com.example.bizvision.common.enums;

/**
 * 文章状态，对应 articles.is_on 列：1 = 已发布，0 = 已下架。
 * 删除是软删除，所以没有单独的 DELETED，删了就是 OFFLINE。
 */
public enum ArticleStatus implements CodeEnum {

    PUBLISHED(1, "已发布"),
    OFFLINE(0, "已下架");

    private final int code;
    private final String label;

    ArticleStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /** 按数据库里的数字反查枚举，查不到直接报错，不要静默返回默认值 */
    public static ArticleStatus of(int code) {
        return CodeEnum.of(ArticleStatus.class, code);
    }
}

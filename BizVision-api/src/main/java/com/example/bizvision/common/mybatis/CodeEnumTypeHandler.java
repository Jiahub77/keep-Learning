package com.example.bizvision.common.mybatis;

import com.example.bizvision.common.enums.CodeEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CodeEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> type;
    /** 不是 CodeEnum 的普通枚举走这里，保持 MyBatis 原行为 */
    private final BaseTypeHandler<E> fallback;

    public CodeEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
        this.fallback = CodeEnum.class.isAssignableFrom(type) ? null : new EnumTypeHandler<E>(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
            throws SQLException {
        if (fallback != null) {
            fallback.setNonNullParameter(ps, i, parameter, jdbcType);
            return;
        }
        ps.setInt(i, ((CodeEnum) parameter).getCode());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        if (fallback != null) {
            return fallback.getNullableResult(rs, columnName);
        }
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : CodeEnum.of(type, code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        if (fallback != null) {
            return fallback.getNullableResult(rs, columnIndex);
        }
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : CodeEnum.of(type, code);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        if (fallback != null) {
            return fallback.getNullableResult(cs, columnIndex);
        }
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : CodeEnum.of(type, code);
    }
}

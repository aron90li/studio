package com.aron.studio.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MySQL 查询结果
 */
@Data
public class MysqlQueryResult {

    /** 查询结果记录数 */
    private int count;

    /** UPDATE/DELETE 影响的行数 */
    private int affectedRows;

    /** 查询数据 */
    private List<Map<String, Object>> data;

    /** 错误信息（查询成功时为 null） */
    private String error;

    /**
     * 构建成功结果
     */
    public static MysqlQueryResult success(List<Map<String, Object>> data) {
        MysqlQueryResult result = new MysqlQueryResult();
        result.setCount(data != null ? data.size() : 0);
        result.setData(data);
        return result;
    }

    /**
     * 构建更新结果
     */
    public static MysqlQueryResult updateSuccess(int affectedRows) {
        MysqlQueryResult result = new MysqlQueryResult();
        result.setAffectedRows(affectedRows);
        result.setData(List.of());
        return result;
    }

    /**
     * 构建错误结果
     */
    public static MysqlQueryResult error(String message) {
        MysqlQueryResult result = new MysqlQueryResult();
        result.setCount(0);
        result.setData(List.of());
        result.setError(message);
        return result;
    }
}
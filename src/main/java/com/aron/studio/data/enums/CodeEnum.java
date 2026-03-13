package com.aron.studio.data.enums;

public enum CodeEnum {
    // 通用成功失败
    SUCCESS(0, "成功"),
    FAIL(9999, "失败"),
    // 成功的code 1001 - 1999
    CREATE_SUCCESS(1001, "创建成功"),
    EDIT_SUCCESS(1002, "修改成功"),

    // 失败的 code 2001 - 2999
    GRAMMAR_CHECK_FAIL(2001, "语法检查失败"),
    SUBMIT_ERROR(2002, "提交失败"),
    UNKNOWN_ERROR(2999, "未知失败");

    private Integer code;
    private String msg;

    CodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}

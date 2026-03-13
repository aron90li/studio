package com.aron.studio.data;

import com.aron.studio.data.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class Response<T> implements Serializable {

    private T data;
    private Integer code; // 自定义的返回码，业务层面，和 http 响应码无关
    private String msg;   // 可以从 CodeEnum 的 msg 中得到
    private boolean success; // 必填项目，标识该调用是否成功

    // 通用成功, 不带数据
    public static <T> Response<T> success() {
        return new Response<>(null, CodeEnum.SUCCESS.getCode(), CodeEnum.SUCCESS.getMsg(), true);
    }

    // 通用成功, 带数据
    public static <T> Response<T> success(T data) {
        return new Response<>(data, CodeEnum.SUCCESS.getCode(), CodeEnum.SUCCESS.getMsg(), true);
    }

    // 通用失败
    public static <T> Response<T> fail() {
        return new Response<>(null, CodeEnum.FAIL.getCode(), CodeEnum.FAIL.getMsg(), false);
    }

    // 自定义失败信息
    public static <T> Response<T> fail(String msg) {
        return new Response<>(null, CodeEnum.FAIL.getCode(), msg, false);
    }

    // 采用指定的失败信息，注意要选择对
    public static <T> Response<T> fail(CodeEnum code) {
        return new Response<>(null, code.getCode(), code.getMsg(), false);
    }


}

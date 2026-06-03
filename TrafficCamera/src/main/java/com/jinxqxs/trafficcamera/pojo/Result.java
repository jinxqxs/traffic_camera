package com.jinxqxs.trafficcamera.pojo;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success() { return new Result<>(200, "操作成功", null); }
    public static <T> Result<T> success(T data) { return new Result<>(200, "操作成功", data); }
    public static <T> Result<T> error(String msg) { return new Result<>(500, msg, null); }

    // 替代若依的 toAjax
    public static Result<Void> toAjax(int rows) { return rows > 0 ? success() : error("操作失败"); }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
    // 省略 getter 和 setter
}
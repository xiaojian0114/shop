package org.example.shop.common;



import lombok.Data;

@Data
public class Result {
    private int code;
    private String msg;
    private Object data;

    public Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result ok() { return new Result(200, "成功", null); }
    public static Result ok(Object data) { return new Result(200, "成功", data); }
    public static Result fail(String msg) { return new Result(500, msg, null); }
}
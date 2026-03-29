package com.cs2trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 * 用于规范所有API接口的返回格式
 * 
 * @param <T> 响应数据的类型
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     * 200表示成功，其他表示失败
     */
    private Integer code;

    /**
     * 响应消息
     * 成功时为"success"，失败时为错误描述
     */
    private String message;

    /**
     * 响应数据
     * 成功时返回具体数据，失败时可能为null
     */
    private T data;

    /**
     * 时间戳
     * 服务器响应时间
     */
    private Long timestamp;

    /**
     * 私有构造方法，强制使用静态工厂方法创建实例
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应结果(无数据)
     * 
     * @param <T> 数据类型
     * @return Result 响应结果对象
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        return result;
    }

    /**
     * 创建成功响应结果(带数据)
     * 
     * @param data 响应数据
     * @param <T> 数据类型
     * @return Result 响应结果对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 创建成功响应结果(带自定义消息和数据)
     * 
     * @param message 自定义消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return Result 响应结果对象
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 创建失败响应结果
     * 
     * @param message 错误消息
     * @param <T> 数据类型
     * @return Result 响应结果对象
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 创建失败响应结果(带状态码)
     * 
     * @param code 错误状态码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return Result 响应结果对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 创建失败响应结果(带状态码、消息和数据)
     * 
     * @param code 错误状态码
     * @param message 错误消息
     * @param data 错误相关数据
     * @param <T> 数据类型
     * @return Result 响应结果对象
     */
    public static <T> Result<T> error(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 判断响应是否成功
     * 
     * @return boolean 是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
}

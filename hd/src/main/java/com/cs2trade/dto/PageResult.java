package com.cs2trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 * 用于封装分页查询的结果数据
 *
 * @param <T> 数据类型
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 创建分页结果
     *
     * @param page  当前页码
     * @param size  每页数量
     * @param total 总记录数
     * @param list  数据列表
     * @param <T>   数据类型
     * @return PageResult 分页结果
     */
    public static <T> PageResult<T> of(Integer page, Integer size, Long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(total);
        result.setList(list);

        // 计算总页数
        result.setTotalPages((int) Math.ceil((double) total / size));

        // 计算是否有下一页和上一页
        result.setHasNext(page < result.getTotalPages());
        result.setHasPrevious(page > 1);

        return result;
    }
}

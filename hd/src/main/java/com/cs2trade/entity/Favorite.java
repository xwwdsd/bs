package com.cs2trade.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收藏实体类
 */
@Data
public class Favorite implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long itemId;
    private Long newsId;
    private Integer type; // 1: 饰品, 2: 资讯
    private LocalDateTime createdAt;
    
    // 关联属性
    private Item item;
    private News news;
}

package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Favorite entity.
 */
@Data
public class Favorite implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long itemId;
    private Long newsId;
    private Integer type; // 1: item, 2: news
    private LocalDateTime createdAt;

    private Item item;
    private News news;
}

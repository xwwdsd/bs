package com.cs2trade.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资讯实体类
 */
@Data
public class News implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String category;
    private String author;
    private Long userId;
    private String source;
    private Integer views;
    private Integer status;
    private String rejectReason;
    private LocalDateTime createdAt;
}

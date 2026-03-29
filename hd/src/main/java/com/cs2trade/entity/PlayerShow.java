package com.cs2trade.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 玩家秀实体类
 */
@Data
public class PlayerShow implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String imageUrl;
    private String description;
    private Integer likes;
    private LocalDateTime createdAt;
    
    // 关联用户信息
    private String username;
    private String avatar;
}

package com.cs2trade.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PlayerShowLike implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long showId;
    private Long userId;
    private LocalDateTime createdAt;
}

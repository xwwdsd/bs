package com.cs2trade.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PasswordResetToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String token;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private Integer used;

    public static final int USED_NO = 0;
    public static final int USED_YES = 1;
}

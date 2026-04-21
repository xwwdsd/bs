package com.cs2trade.dto.market;

import lombok.Data;

import java.io.Serializable;

@Data
public class ItemIconCandidate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String iconUrl;
}

package com.cs2trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SteamSyncTask {

    private Long id;
    private String taskType;
    private String status;
    private Integer totalCount;
    private Integer totalPages;
    private Integer plannedPages;
    private Integer syncedPages;
    private Integer currentPage;
    private Integer processedCount;
    private Integer savedCount;
    private Integer updatedCount;
    private Integer skippedCount;
    private Integer remainingPages;
    private Integer remainingItems;
    private Integer maxPagesLimit;
    private Integer failedPage;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

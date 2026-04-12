package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.config.SteamMarketSyncProperties;
import com.cs2trade.dto.PageResult;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.SteamSyncTask;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.SteamSyncTaskMapper;
import com.cs2trade.service.ItemService;
import com.cs2trade.util.SteamApiClient;
import com.cs2trade.util.SteamMarketPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private static final String TASK_TYPE_STEAM_ITEM_SYNC = "STEAM_ITEM_SYNC";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COOLDOWN = "COOLDOWN";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_PARTIAL = "PARTIAL";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STEAM_REFERENCE_CURRENCY = "CNY";
    private static final String STEAM_REFERENCE_PRICE_SOURCE = "steam_market_search";
    private static final Pattern PRICE_TEXT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");

    private static final int LEGACY_STEAM_PAGE_SIZE = 10;
    private static final DateTimeFormatter COOLDOWN_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ItemMapper itemMapper;
    private final SteamApiClient steamApiClient;
    private final SteamSyncTaskMapper steamSyncTaskMapper;
    private final SteamMarketSyncProperties syncProperties;

    private final AtomicBoolean steamSyncRunning = new AtomicBoolean(false);

    @Override
    public Item getItemById(Long id) {
        return itemMapper.selectById(id);
    }

    @Override
    public PageResult<Item> getItemList(String category, String exterior, String quality,
                                        String keyword, String sortField, String sortOrder,
                                        Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        sortField = normalizeItemSortField(sortField);
        sortOrder = normalizeSortOrder(sortOrder);

        int offset = (page - 1) * size;
        long total = itemMapper.selectCount(category, exterior, quality, keyword);
        List<Item> list = itemMapper.selectList(category, exterior, quality, keyword,
                sortField, sortOrder, offset, size);

        return PageResult.of(page, size, total, list);
    }

    private String normalizeItemSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return "created_at";
        }

        return switch (sortField.trim()) {
            case "id", "name", "name_cn", "created_at", "updated_at", "buff_price", "steam_reference_price" -> sortField.trim();
            case "price", "buffPrice", "marketPrice", "steamReferencePrice" -> "reference_price";
            default -> "created_at";
        };
    }

    private String normalizeSortOrder(String sortOrder) {
        return "ASC".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
    }

    @Override
    public List<Item> getAllActiveItems() {
        return itemMapper.selectAllActive();
    }

    @Override
    public List<Item> getItemsByCategory(String category) {
        return itemMapper.selectByCategory(category);
    }

    @Override
    public List<Item> searchItems(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveItems();
        }
        return itemMapper.searchByName(keyword.trim());
    }

    @Override
    public List<String> getAllCategories() {
        return itemMapper.selectAllCategories();
    }

    @Override
    public List<String> getAllExteriors() {
        return itemMapper.selectAllExteriors();
    }

    @Override
    public List<String> getAllQualities() {
        return itemMapper.selectAllQualities();
    }

    @Override
    public Item addItem(Item item) {
        if (item.getIsActive() == null) {
            item.setIsActive(Item.STATUS_ENABLED);
        }
        itemMapper.insert(item);
        return item;
    }

    @Override
    public Item updateItem(Item item) {
        itemMapper.updateById(item);
        return itemMapper.selectById(item.getId());
    }

    @Override
    public void deleteItem(Long id) {
        itemMapper.deleteById(id);
    }

    @Override
    @org.springframework.scheduling.annotation.Async("steamSyncExecutor")
    public CompletableFuture<Integer> syncItemsFromSteamAsync() {
        if (!steamSyncRunning.compareAndSet(false, true)) {
            throw new RuntimeException("Steam 饰品同步任务正在运行，请稍后再试");
        }

        SteamSyncTask task = prepareTaskForRun();
        log.info("Start Steam item sync task: taskId={}, syncedPages={}, plannedPages={}, pageSize={}",
                task.getId(), task.getSyncedPages(), task.getPlannedPages(), task.getPageSize());

        try {
            return CompletableFuture.completedFuture(runSteamSync(task));
        } finally {
            steamSyncRunning.set(false);
        }
    }

    @Override
    public int syncItemsFromSteam() {
        if (!steamSyncRunning.compareAndSet(false, true)) {
            throw new RuntimeException("Steam 饰品同步任务正在运行，请稍后再试");
        }

        SteamSyncTask task = prepareTaskForRun();
        try {
            return runSteamSync(task);
        } finally {
            steamSyncRunning.set(false);
        }
    }

    @Override
    public Map<String, Object> getSteamSyncStatus() {
        SteamSyncTask latestTask = steamSyncTaskMapper.selectLatestByTaskType(TASK_TYPE_STEAM_ITEM_SYNC);
        Map<String, Object> result = createDefaultStatus();
        if (latestTask == null) {
            result.put("running", steamSyncRunning.get());
            return result;
        }

        int totalPages = defaultInt(latestTask.getTotalPages());
        int plannedPages = defaultInt(latestTask.getPlannedPages());
        int syncedPages = defaultInt(latestTask.getSyncedPages());
        boolean runningLike = STATUS_RUNNING.equalsIgnoreCase(latestTask.getStatus())
                || STATUS_COOLDOWN.equalsIgnoreCase(latestTask.getStatus());
        int nextStartPage = totalPages > syncedPages ? syncedPages + 1 : 0;
        int nextEndPage = nextStartPage > 0
                ? Math.min(totalPages, syncedPages + syncProperties.getMaxPagesPerRun())
                : 0;
        boolean canContinue = !runningLike && totalPages > syncedPages;
        boolean resumableFailed = STATUS_FAILED.equalsIgnoreCase(latestTask.getStatus()) && canContinue;

        result.put("taskId", latestTask.getId());
        result.put("running", steamSyncRunning.get() && runningLike);
        result.put("phase", resumableFailed ? "partial" : mapStatusToPhase(latestTask.getStatus()));
        result.put("message", buildStatusMessage(latestTask, canContinue, nextStartPage, nextEndPage));
        result.put("startedAt", toIsoString(latestTask.getStartedAt()));
        result.put("finishedAt", toIsoString(latestTask.getFinishedAt()));
        result.put("totalCount", defaultInt(latestTask.getTotalCount()));
        result.put("totalPages", totalPages);
        result.put("plannedPages", plannedPages);
        result.put("syncedPages", syncedPages);
        result.put("currentPage", defaultInt(latestTask.getCurrentPage()));
        result.put("processedCount", defaultInt(latestTask.getProcessedCount()));
        result.put("savedCount", defaultInt(latestTask.getSavedCount()));
        result.put("updatedCount", defaultInt(latestTask.getUpdatedCount()));
        result.put("skippedCount", defaultInt(latestTask.getSkippedCount()));
        result.put("remainingPages", defaultInt(latestTask.getRemainingPages()));
        result.put("remainingItems", defaultInt(latestTask.getRemainingItems()));
        result.put("maxPagesLimit", defaultInt(latestTask.getMaxPagesLimit()));
        result.put("pageSize", positiveOrDefault(latestTask.getPageSize(), syncProperties.getPageSize()));
        result.put("retryCount", defaultInt(latestTask.getRetryCount()));
        result.put("lastHttpStatus", latestTask.getLastHttpStatus());
        result.put("cooldownUntil", toIsoString(latestTask.getCooldownUntil()));
        result.put("autoRetrying", STATUS_COOLDOWN.equalsIgnoreCase(latestTask.getStatus()));
        result.put("failedPage", latestTask.getFailedPage());
        result.put("error", resumableFailed ? null : latestTask.getErrorMessage());
        result.put("lastDurationSeconds", calculateDurationSeconds(latestTask));
        result.put("capped", totalPages > plannedPages);
        result.put("canContinue", canContinue);
        result.put("nextStartPage", nextStartPage);
        result.put("nextEndPage", nextEndPage);
        result.put("remainingPageRange", totalPages > syncedPages ? nextStartPage + "-" + totalPages : "");

        return result;
    }

    private SteamSyncTask prepareTaskForRun() {
        SteamSyncTask latestTask = steamSyncTaskMapper.selectLatestByTaskType(TASK_TYPE_STEAM_ITEM_SYNC);
        if (isResumableTask(latestTask)) {
            return resumeTask(latestTask);
        }
        return createTask();
    }

    private boolean isResumableTask(SteamSyncTask task) {
        if (task == null) {
            return false;
        }

        int totalPages = defaultInt(task.getTotalPages());
        int syncedPages = defaultInt(task.getSyncedPages());
        boolean hasRemainingPages = totalPages > syncedPages;
        String status = firstNonBlank(task.getStatus()).toUpperCase();

        return hasRemainingPages && (STATUS_PARTIAL.equals(status)
                || STATUS_FAILED.equals(status)
                || STATUS_COOLDOWN.equals(status));
    }

    private SteamSyncTask resumeTask(SteamSyncTask task) {
        migrateTaskPageSizeIfNeeded(task);
        int totalPages = defaultInt(task.getTotalPages());
        int syncedPages = defaultInt(task.getSyncedPages());
        int plannedPages = Math.min(totalPages, syncedPages + syncProperties.getMaxPagesPerRun());

        task.setStatus(STATUS_RUNNING);
        task.setPlannedPages(plannedPages);
        task.setCurrentPage(Math.max(syncedPages, 0));
        task.setRemainingPages(Math.max(totalPages - syncedPages, 0));
        task.setRemainingItems(Math.max(defaultInt(task.getTotalCount()) - defaultInt(task.getProcessedCount()), 0));
        task.setMaxPagesLimit(syncProperties.getMaxPagesPerRun());
        task.setFailedPage(null);
        task.setErrorMessage(null);
        task.setCooldownUntil(null);
        task.setRetryCount(0);
        task.setLastHttpStatus(null);
        task.setStartedAt(LocalDateTime.now());
        task.setFinishedAt(null);
        persistTask(task);
        return task;
    }

    private void migrateTaskPageSizeIfNeeded(SteamSyncTask task) {
        int taskPageSize = positiveOrDefault(task.getPageSize(), LEGACY_STEAM_PAGE_SIZE);
        int configuredPageSize = syncProperties.getPageSize();
        task.setMaxPagesLimit(syncProperties.getMaxPagesPerRun());

        if (taskPageSize == configuredPageSize) {
            task.setPageSize(configuredPageSize);
            return;
        }

        int totalCount = defaultInt(task.getTotalCount());
        int syncedPages = defaultInt(task.getSyncedPages());
        int processedCount = Math.max(defaultInt(task.getProcessedCount()), syncedPages * taskPageSize);
        int translatedSyncedPages = processedCount / configuredPageSize;
        int translatedTotalPages = totalCount <= 0 ? 0 : (totalCount + configuredPageSize - 1) / configuredPageSize;
        int translatedCurrentPage = Math.min(defaultInt(task.getCurrentPage()), translatedSyncedPages);
        int plannedPages = translatedTotalPages == 0
                ? 0
                : Math.min(translatedTotalPages, translatedSyncedPages + syncProperties.getMaxPagesPerRun());

        task.setPageSize(configuredPageSize);
        task.setSyncedPages(translatedSyncedPages);
        task.setCurrentPage(Math.max(translatedCurrentPage, translatedSyncedPages));
        task.setProcessedCount(processedCount);
        task.setTotalPages(translatedTotalPages);
        task.setPlannedPages(plannedPages);
        task.setRemainingPages(Math.max(translatedTotalPages - translatedSyncedPages, 0));
        task.setRemainingItems(Math.max(totalCount - processedCount, 0));
    }

    private SteamSyncTask createTask() {
        SteamSyncTask task = new SteamSyncTask();
        task.setTaskType(TASK_TYPE_STEAM_ITEM_SYNC);
        task.setStatus(STATUS_RUNNING);
        task.setTotalCount(0);
        task.setTotalPages(0);
        task.setPlannedPages(0);
        task.setSyncedPages(0);
        task.setCurrentPage(0);
        task.setProcessedCount(0);
        task.setSavedCount(0);
        task.setUpdatedCount(0);
        task.setSkippedCount(0);
        task.setRemainingPages(0);
        task.setRemainingItems(0);
        task.setMaxPagesLimit(syncProperties.getMaxPagesPerRun());
        task.setPageSize(syncProperties.getPageSize());
        task.setRetryCount(0);
        task.setLastHttpStatus(null);
        task.setCooldownUntil(null);
        task.setStartedAt(LocalDateTime.now());
        steamSyncTaskMapper.insert(task);
        return task;
    }

    private int runSteamSync(SteamSyncTask task) {
        try {
            Map<String, Item> existingItemMap = buildExistingItemMap();

            int startPage = Math.max(defaultInt(task.getSyncedPages()), 0);
            int pageSize = positiveOrDefault(task.getPageSize(), syncProperties.getPageSize());
            SteamMarketPageResult firstPageResult = null;
            int totalCount;
            int totalPages;

            if (startPage == 0 || defaultInt(task.getTotalCount()) <= 0 || defaultInt(task.getTotalPages()) <= 0) {
                firstPageResult = fetchSteamPageWithRetry(task, 0, pageSize);
                if (!firstPageResult.isSuccess()) {
                    if (STATUS_PARTIAL.equalsIgnoreCase(task.getStatus())) {
                        return 0;
                    }
                    return failTask(task, null, firstPageResult.getStatusCode(),
                            firstNonBlank(firstPageResult.getErrorMessage(), "从 Steam API 获取第一页数据失败"));
                }
                JSONObject firstPageData = firstPageResult.getPayload();
                totalCount = firstPageData.getIntValue("total_count", 0);
                totalPages = (totalCount + pageSize - 1) / pageSize;
            } else {
                totalCount = defaultInt(task.getTotalCount());
                totalPages = defaultInt(task.getTotalPages());
                log.info("Resume Steam sync task from page {} with total pages {}", startPage + 1, totalPages);
            }

            int plannedPages = Math.min(
                    totalPages,
                    Math.max(defaultInt(task.getPlannedPages()), startPage + syncProperties.getMaxPagesPerRun())
            );

            task.setStatus(STATUS_RUNNING);
            task.setTotalCount(totalCount);
            task.setTotalPages(totalPages);
            task.setPlannedPages(plannedPages);
            task.setCurrentPage(startPage);
            task.setPageSize(pageSize);
            task.setMaxPagesLimit(syncProperties.getMaxPagesPerRun());
            task.setRetryCount(0);
            task.setCooldownUntil(null);
            Integer initialStatusCode = firstPageResult != null
                    ? Integer.valueOf(firstPageResult.getStatusCode())
                    : task.getLastHttpStatus();
            task.setLastHttpStatus(initialStatusCode);
            task.setRemainingPages(Math.max(totalPages - startPage, 0));
            task.setRemainingItems(Math.max(totalCount - defaultInt(task.getProcessedCount()), 0));
            persistTask(task);

            if (startPage >= totalPages) {
                task.setStatus(STATUS_COMPLETED);
                task.setFinishedAt(LocalDateTime.now());
                task.setCurrentPage(totalPages);
                task.setProcessedCount(totalCount);
                task.setRemainingPages(0);
                task.setRemainingItems(0);
                task.setRetryCount(0);
                task.setCooldownUntil(null);
                task.setLastHttpStatus(200);
                persistTask(task);
                return 0;
            }

            int batchSaved = 0;
            int batchUpdated = 0;
            int totalSaved = defaultInt(task.getSavedCount());
            int totalUpdated = defaultInt(task.getUpdatedCount());
            int totalSkipped = defaultInt(task.getSkippedCount());

            for (int page = startPage; page < plannedPages; page++) {
                task.setCurrentPage(page + 1);
                persistTask(task);

                SteamMarketPageResult steamPageResult = page == 0 && firstPageResult != null
                        ? firstPageResult
                        : fetchSteamPageWithRetry(task, page, pageSize);
                if (!steamPageResult.isSuccess()) {
                    if (STATUS_PARTIAL.equalsIgnoreCase(task.getStatus())) {
                        return 0;
                    }
                    return failTask(task, page + 1, steamPageResult.getStatusCode(),
                            firstNonBlank(steamPageResult.getErrorMessage(), "从 Steam API 获取第 " + (page + 1) + " 页数据失败"));
                }

                JSONObject steamData = steamPageResult.getPayload();
                JSONArray itemsArray = steamData.getJSONArray("results");
                if (itemsArray == null || itemsArray.isEmpty()) {
                    break;
                }

                int savedCount = 0;
                int updatedCount = 0;
                int skippedCount = 0;

                for (int i = 0; i < itemsArray.size(); i++) {
                    JSONObject itemJson = itemsArray.getJSONObject(i);
                    try {
                        Item item = parseSteamItem(itemJson);
                        if (item == null) {
                            skippedCount++;
                            continue;
                        }

                        Item existingItem = existingItemMap.get(item.getItemId());
                        if (existingItem != null) {
                            if (needsUpdate(existingItem, item)) {
                                item.setId(existingItem.getId());
                                item.setCreatedAt(existingItem.getCreatedAt());
                                itemMapper.updateById(item);
                                updatedCount++;
                                existingItemMap.put(item.getItemId(), item);
                            } else {
                                skippedCount++;
                            }
                        } else {
                            itemMapper.insert(item);
                            existingItemMap.put(item.getItemId(), item);
                            savedCount++;
                        }
                    } catch (Exception e) {
                        log.error("Parse Steam item failed: {}", e.getMessage(), e);
                        skippedCount++;
                    }
                }

                batchSaved += savedCount;
                batchUpdated += updatedCount;
                totalSaved += savedCount;
                totalUpdated += updatedCount;
                totalSkipped += skippedCount;

                int processedCount = Math.min(totalCount, (page * pageSize) + itemsArray.size());
                task.setSyncedPages(page + 1);
                task.setProcessedCount(processedCount);
                task.setSavedCount(totalSaved);
                task.setUpdatedCount(totalUpdated);
                task.setSkippedCount(totalSkipped);
                task.setRemainingPages(Math.max(totalPages - task.getSyncedPages(), 0));
                task.setRemainingItems(Math.max(totalCount - processedCount, 0));
                task.setRetryCount(0);
                task.setCooldownUntil(null);
                task.setLastHttpStatus(200);
                task.setFailedPage(null);
                task.setErrorMessage(null);
                persistTask(task);

                if (processedCount >= totalCount) {
                    break;
                }

                Thread.sleep(syncProperties.getPageDelayMillis());
            }

            task.setStatus(task.getRemainingPages() > 0 ? STATUS_PARTIAL : STATUS_COMPLETED);
            task.setFinishedAt(LocalDateTime.now());
            task.setCurrentPage(task.getSyncedPages());
            task.setRetryCount(0);
            task.setCooldownUntil(null);
            task.setLastHttpStatus(200);
            task.setFailedPage(null);
            task.setErrorMessage(null);
            persistTask(task);

            log.info("Steam item sync finished: taskId={}, syncedPages={}, totalPages={}, processed={}/{}",
                    task.getId(), task.getSyncedPages(), task.getTotalPages(), task.getProcessedCount(), task.getTotalCount());

            return batchSaved + batchUpdated;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failTask(task, task.getCurrentPage(), task.getLastHttpStatus(), "同步过程被中断");
        } catch (Exception e) {
            log.error("Sync Steam items failed: {}", e.getMessage(), e);
            return failTask(task, task.getCurrentPage(), task.getLastHttpStatus(), "同步失败: " + e.getMessage());
        }
    }

    private Map<String, Item> buildExistingItemMap() {
        List<Item> existingItems = itemMapper.selectAllActive();
        Map<String, Item> existingItemMap = new HashMap<>();
        for (Item item : existingItems) {
            if (item.getItemId() != null) {
                existingItemMap.put(item.getItemId(), item);
            }
        }
        return existingItemMap;
    }

    private SteamMarketPageResult fetchSteamPageWithRetry(SteamSyncTask task, int page, int pageSize) throws InterruptedException {
        long retryBudgetDeadline = System.currentTimeMillis()
                + (long) syncProperties.getAutoRetryBudgetMinutes() * 60_000L;
        int retryCount = defaultInt(task.getRetryCount());

        while (true) {
            SteamMarketPageResult steamResult = steamApiClient.getCsgoItems(page * pageSize, pageSize);
            task.setLastHttpStatus(steamResult.getStatusCode());

            if (steamResult.isSuccess()) {
                task.setStatus(STATUS_RUNNING);
                task.setRetryCount(0);
                task.setCooldownUntil(null);
                task.setLastHttpStatus(200);
                task.setFailedPage(null);
                task.setErrorMessage(null);
                persistTask(task);
                return steamResult;
            }

            if (steamResult.isRateLimited()) {
                retryCount++;
                int waitSeconds = resolveRateLimitWaitSeconds(steamResult.getRetryAfterSeconds(), retryCount);
                LocalDateTime cooldownUntil = LocalDateTime.now().plusSeconds(waitSeconds);

                if (System.currentTimeMillis() + (waitSeconds * 1000L) > retryBudgetDeadline) {
                    task.setStatus(STATUS_PARTIAL);
                    task.setRetryCount(retryCount);
                    task.setCooldownUntil(null);
                    task.setFailedPage(page + 1);
                    task.setErrorMessage("Steam 限流持续过久，已保留进度，可稍后继续同步");
                    task.setFinishedAt(LocalDateTime.now());
                    persistTask(task);
                    return SteamMarketPageResult.failure(steamResult.getStatusCode(), task.getErrorMessage());
                }

                task.setStatus(STATUS_COOLDOWN);
                task.setRetryCount(retryCount);
                task.setCooldownUntil(cooldownUntil);
                task.setFailedPage(null);
                task.setErrorMessage(null);
                persistTask(task);

                log.warn("Steam rate limited on page {}, retryCount={}, waitSeconds={}, cooldownUntil={}",
                        page + 1, retryCount, waitSeconds, cooldownUntil);
                Thread.sleep(waitSeconds * 1000L);

                task.setStatus(STATUS_RUNNING);
                task.setCooldownUntil(null);
                task.setStartedAt(task.getStartedAt() == null ? LocalDateTime.now() : task.getStartedAt());
                persistTask(task);
                continue;
            }

            return steamResult;
        }
    }

    private int resolveRateLimitWaitSeconds(Integer retryAfterSeconds, int retryCount) {
        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            return retryAfterSeconds;
        }

        List<Integer> backoffSeconds = syncProperties.getRateLimitBackoffSeconds();
        if (backoffSeconds == null || backoffSeconds.isEmpty()) {
            return 60;
        }

        int index = Math.min(Math.max(retryCount - 1, 0), backoffSeconds.size() - 1);
        Integer value = backoffSeconds.get(index);
        return value == null || value <= 0 ? 60 : value;
    }

    private int failTask(SteamSyncTask task, Integer failedPage, Integer statusCode, String message) {
        task.setStatus(STATUS_FAILED);
        task.setFailedPage(failedPage);
        task.setLastHttpStatus(statusCode);
        task.setCooldownUntil(null);
        task.setErrorMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        persistTask(task);
        return 0;
    }

    private void persistTask(SteamSyncTask task) {
        steamSyncTaskMapper.updateById(task);
    }

    private Map<String, Object> createDefaultStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("taskId", null);
        status.put("running", false);
        status.put("phase", "idle");
        status.put("message", "尚未开始同步");
        status.put("startedAt", null);
        status.put("finishedAt", null);
        status.put("totalCount", 0);
        status.put("totalPages", 0);
        status.put("plannedPages", 0);
        status.put("syncedPages", 0);
        status.put("currentPage", 0);
        status.put("processedCount", 0);
        status.put("savedCount", 0);
        status.put("updatedCount", 0);
        status.put("skippedCount", 0);
        status.put("remainingPages", 0);
        status.put("remainingItems", 0);
        status.put("maxPagesLimit", syncProperties.getMaxPagesPerRun());
        status.put("pageSize", syncProperties.getPageSize());
        status.put("retryCount", 0);
        status.put("lastHttpStatus", null);
        status.put("cooldownUntil", null);
        status.put("autoRetrying", false);
        status.put("failedPage", null);
        status.put("error", null);
        status.put("lastDurationSeconds", 0);
        status.put("remainingPageRange", "");
        status.put("capped", false);
        status.put("canContinue", false);
        status.put("nextStartPage", 0);
        status.put("nextEndPage", 0);
        return status;
    }

    private String mapStatusToPhase(String status) {
        if (status == null) {
            return "idle";
        }
        return switch (status.toUpperCase()) {
            case STATUS_RUNNING -> "syncing";
            case STATUS_COOLDOWN -> "cooldown";
            case STATUS_COMPLETED -> "completed";
            case STATUS_PARTIAL -> "partial";
            case STATUS_FAILED -> "failed";
            default -> "idle";
        };
    }

    private String buildStatusMessage(SteamSyncTask task, boolean canContinue, int nextStartPage, int nextEndPage) {
        if (task == null) {
            return "尚未开始同步";
        }

        int currentPage = defaultInt(task.getCurrentPage());
        int plannedPages = defaultInt(task.getPlannedPages());
        int totalPages = defaultInt(task.getTotalPages());
        int syncedPages = defaultInt(task.getSyncedPages());
        int remainingPages = defaultInt(task.getRemainingPages());

        return switch (firstNonBlank(task.getStatus()).toUpperCase()) {
            case STATUS_RUNNING -> "正在同步第 " + currentPage + " / " + plannedPages + " 页";
            case STATUS_COOLDOWN -> "Steam 限流中，系统将在 "
                    + formatCooldownTime(task.getCooldownUntil())
                    + " 自动重试第 " + currentPage + " 页";
            case STATUS_COMPLETED -> "Steam 饰品已全部同步完成";
            case STATUS_PARTIAL -> canContinue
                    ? "本轮已完成，可继续同步第 " + nextStartPage + " - " + nextEndPage + " 页，剩余 " + remainingPages + " 页"
                    : firstNonBlank(task.getErrorMessage(), "本轮同步已结束");
            case STATUS_FAILED -> firstNonBlank(task.getErrorMessage(), "同步失败");
            default -> totalPages > 0 && totalPages > syncedPages
                    ? "可继续同步剩余页面"
                    : "尚未开始同步";
        };
    }

    private String formatCooldownTime(LocalDateTime cooldownUntil) {
        if (cooldownUntil == null) {
            return "稍后";
        }
        return cooldownUntil.format(COOLDOWN_TIME_FORMATTER);
    }

    private long calculateDurationSeconds(SteamSyncTask task) {
        if (task == null || task.getStartedAt() == null) {
            return 0L;
        }

        LocalDateTime endTime = task.getFinishedAt() != null ? task.getFinishedAt() : LocalDateTime.now();
        return ChronoUnit.SECONDS.between(task.getStartedAt(), endTime);
    }

    private String toIsoString(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    boolean needsUpdate(Item existingItem, Item latestItem) {
        return !Objects.equals(existingItem.getName(), latestItem.getName())
                || !Objects.equals(existingItem.getNameCn(), latestItem.getNameCn())
                || !Objects.equals(existingItem.getIconUrl(), latestItem.getIconUrl())
                || !Objects.equals(existingItem.getCategory(), latestItem.getCategory())
                || hasNewSteamReferencePrice(existingItem, latestItem);
    }

    private boolean hasNewSteamReferencePrice(Item existingItem, Item latestItem) {
        return latestItem.getSteamReferencePrice() != null
                && (!Objects.equals(existingItem.getSteamReferencePrice(), latestItem.getSteamReferencePrice())
                || !Objects.equals(existingItem.getSteamReferenceCurrency(), latestItem.getSteamReferenceCurrency())
                || !Objects.equals(existingItem.getSteamReferencePriceSource(), latestItem.getSteamReferencePriceSource()));
    }

    Item parseSteamItem(JSONObject itemJson) {
        try {
            Item item = new Item();

            String name = itemJson.getString("name");
            String hashName = itemJson.getString("market_hash_name");
            if (name == null || name.isEmpty()) {
                return null;
            }

            item.setItemId(hashName != null ? hashName : name);
            item.setName(name);
            item.setNameCn(name);

            String iconUrl = itemJson.getString("icon_url");
            if (iconUrl != null && !iconUrl.isEmpty()) {
                item.setIconUrl(iconUrl);
            } else {
                String appIcon = itemJson.getString("app_icon");
                if (appIcon != null && !appIcon.isEmpty()) {
                    item.setIconUrl(appIcon);
                }
            }

            item.setCategory(determineCategory(name));
            item.setQuality("common");
            item.setRarity("common");
            item.setExterior(null);
            item.setIsActive(1);

            BigDecimal steamReferencePrice = parseSteamReferencePrice(itemJson);
            if (steamReferencePrice != null) {
                item.setSteamReferencePrice(steamReferencePrice);
                item.setSteamReferenceCurrency(STEAM_REFERENCE_CURRENCY);
                item.setSteamReferencePriceSource(STEAM_REFERENCE_PRICE_SOURCE);
                item.setSteamReferencePriceUpdatedAt(LocalDateTime.now());
            }

            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            return item;
        } catch (Exception e) {
            log.error("Parse Steam item failed: {}", e.getMessage(), e);
            return null;
        }
    }

    BigDecimal parseSteamReferencePrice(JSONObject itemJson) {
        Long priceInCents = firstPositiveLong(itemJson, "sell_price", "price_value");
        if (priceInCents != null) {
            return BigDecimal.valueOf(priceInCents, 2).setScale(2, RoundingMode.HALF_UP);
        }

        return parseSteamPriceText(
                itemJson.getString("sell_price_text"),
                itemJson.getString("price")
        );
    }

    private Long firstPositiveLong(JSONObject itemJson, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object rawValue = itemJson.get(fieldName);
            Long value = parsePositiveLong(rawValue);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long parsePositiveLong(Object rawValue) {
        if (rawValue instanceof Number number) {
            long value = number.longValue();
            return value > 0 ? value : null;
        }
        if (rawValue instanceof String text && !text.isBlank()) {
            try {
                long value = Long.parseLong(text.trim());
                return value > 0 ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal parseSteamPriceText(String... texts) {
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }

            Matcher matcher = PRICE_TEXT_PATTERN.matcher(text.replace(",", ""));
            if (matcher.find()) {
                BigDecimal value = new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
            }
        }
        return null;
    }

    private String determineCategory(String name) {
        if (name == null) {
            return "other";
        }

        String lowerName = name.toLowerCase();

        if (lowerName.contains("ak-47") || lowerName.contains("m4a1") || lowerName.contains("m4a4")
                || lowerName.contains("awp") || lowerName.contains("rifle")) {
            return "rifle";
        }
        if (lowerName.contains("pistol") || lowerName.contains("glock") || lowerName.contains("desert eagle")
                || lowerName.contains("p250") || lowerName.contains("tec-9") || lowerName.contains("cz75")
                || lowerName.contains("fn57") || lowerName.contains("usp") || lowerName.contains("r8")) {
            return "pistol";
        }
        if (lowerName.contains("mp9") || lowerName.contains("mp7") || lowerName.contains("mac-10")
                || lowerName.contains("p90") || lowerName.contains("bizon") || lowerName.contains("smg")) {
            return "smg";
        }
        if (lowerName.contains("shotgun") || lowerName.contains("mag-7") || lowerName.contains("nova")
                || lowerName.contains("xm1014") || lowerName.contains("sawed-off")) {
            return "shotgun";
        }
        if (lowerName.contains("sniper") || lowerName.contains("ssg 08") || lowerName.contains("g3sg1")
                || lowerName.contains("scar")) {
            return "sniper_rifle";
        }
        if (lowerName.contains("machinegun") || lowerName.contains("m249") || lowerName.contains("negev")) {
            return "machinegun";
        }
        if (lowerName.contains("knife") || lowerName.contains("bayonet") || lowerName.contains("karambit")
                || lowerName.contains("m9 bayonet") || lowerName.contains("butterfly") || lowerName.contains("huntsman")
                || lowerName.contains("falchion") || lowerName.contains("shadow daggers") || lowerName.contains("bowie")
                || lowerName.contains("survival") || lowerName.contains("ursus") || lowerName.contains("navaja")
                || lowerName.contains("stiletto") || lowerName.contains("talon") || lowerName.contains("paracord")
                || lowerName.contains("nomad") || lowerName.contains("skeleton")) {
            return "knife";
        }
        if (lowerName.contains("glove") || lowerName.contains("sport glove") || lowerName.contains("driver glove")
                || lowerName.contains("hand wraps") || lowerName.contains("motive glove")
                || lowerName.contains("specialist") || lowerName.contains("bloodhound")) {
            return "glove";
        }
        if (lowerName.contains("sticker")) {
            return "sticker";
        }
        if (lowerName.contains("souvenir")) {
            return "souvenir";
        }
        if (lowerName.contains("music kit")) {
            return "music";
        }
        if (lowerName.contains("agent")) {
            return "agent";
        }
        if (lowerName.contains("graffiti")) {
            return "graffiti";
        }
        if (lowerName.contains("case")) {
            return "case";
        }

        return "other";
    }
}

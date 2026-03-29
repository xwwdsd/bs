package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.Message;
import com.cs2trade.service.MessageService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    private Long getCurrentUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtils.getUserIdFromToken(token.substring(7));
        }
        throw new RuntimeException("未登录");
    }

    /**
     * 获取消息列表
     */
    @GetMapping
    public Result<Map<String, Object>> getMessages(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = getCurrentUserId();
        log.info("获取消息列表: userId={}, type={}, page={}", userId, type, page);
        
        List<Message> list = messageService.getMessages(userId, type, page, size);
        int total = messageService.countMessages(userId, type);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    /**
     * 标记单条消息已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.success(null);
    }

    /**
     * 批量标记已读
     */
    @PostMapping("/batch-read")
    public Result<Void> batchMarkAsRead(@RequestBody List<Long> ids) {
        log.info("批量标记已读: ids={}", ids);
        messageService.batchMarkAsRead(ids);
        return Result.success(null);
    }

    /**
     * 全部标记已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestBody(required = false) Map<String, Integer> params) {
        Long userId = getCurrentUserId();
        Integer type = params != null ? params.get("type") : null;
        log.info("全部标记已读: userId={}, type={}", userId, type);
        messageService.markAllRead(userId, type);
        return Result.success(null);
    }

    /**
     * 批量删除消息
     */
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        log.info("批量删除消息: ids={}", ids);
        messageService.batchDelete(ids);
        return Result.success(null);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount() {
        Long userId = getCurrentUserId();
        log.info("获取未读消息数量: userId={}", userId);
        
        Map<String, Integer> counts = new HashMap<>();
        counts.put("trade", messageService.countUnread(userId, 1));
        counts.put("system", messageService.countUnread(userId, 2));
        counts.put("bargain", messageService.countUnread(userId, 3));
        return Result.success(counts);
    }
}

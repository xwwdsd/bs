package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.Message;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.service.MessageService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        throw new RuntimeException("Not logged in");
    }

    @GetMapping
    public Result<Map<String, Object>> getMessages(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = getCurrentUserId();
        log.info("Fetch messages: userId={}, type={}, page={}", userId, type, page);

        List<Message> list = messageService.getMessages(userId, type, page, size);
        int total = messageService.countMessages(userId, type);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        return Result.success(data);
    }

    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        messageService.markAsRead(id);
        return Result.success(null);
    }

    @PostMapping("/batch-read")
    public Result<Void> batchMarkAsRead(@RequestBody List<Long> ids) {
        log.info("Batch mark read: ids={}", ids);
        messageService.batchMarkAsRead(ids);
        return Result.success(null);
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead(@RequestBody(required = false) Map<String, Integer> params) {
        Long userId = getCurrentUserId();
        Integer type = params != null ? params.get("type") : null;
        log.info("Mark all read: userId={}, type={}", userId, type);
        messageService.markAllRead(userId, type);
        return Result.success(null);
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        log.info("Batch delete messages: ids={}", ids);
        messageService.batchDelete(ids);
        return Result.success(null);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount() {
        Long userId = getCurrentUserId();
        log.info("Fetch unread count: userId={}", userId);

        Map<String, Integer> counts = new HashMap<>();
        counts.put("trade", messageService.countUnread(userId, Message.TYPE_TRADE));
        counts.put("system", messageService.countUnread(userId, Message.TYPE_SYSTEM));
        counts.put("bargain", messageService.countUnread(userId, Message.TYPE_BARGAIN));
        return Result.success(counts);
    }

    @PostMapping("/bargains")
    public Result<Message> createBargain(@RequestBody BargainRequest requestBody) {
        Long userId = getCurrentUserId();
        Message message = messageService.createBargainMessage(
                userId,
                requestBody.getSellOrderId(),
                requestBody.getBargainPrice()
        );
        return Result.success("Bargain sent", message);
    }

    @PostMapping("/{id}/bargain/accept")
    public Result<Map<String, Object>> acceptBargain(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        TradeOrder order = messageService.acceptBargain(userId, id);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getId());
        data.put("orderNo", order.getOrderNo());
        return Result.success("Bargain accepted", data);
    }

    @PostMapping("/{id}/bargain/reject")
    public Result<Message> rejectBargain(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Message message = messageService.rejectBargain(userId, id);
        return Result.success("Bargain rejected", message);
    }

    @Data
    public static class BargainRequest {
        private Long sellOrderId;
        private BigDecimal bargainPrice;
    }
}

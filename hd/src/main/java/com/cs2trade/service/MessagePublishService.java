package com.cs2trade.service;

import com.cs2trade.entity.Item;
import com.cs2trade.entity.Message;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.MessageMapper;
import com.cs2trade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class MessagePublishService {

    private final MessageMapper messageMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    public Message sendTradeMessage(Long userId,
                                    Integer subType,
                                    String title,
                                    String content,
                                    TradeOrder order) {
        return sendTradeMessage(userId, subType, title, content, order, null, null);
    }

    public Message sendTradeMessage(Long userId,
                                    Integer subType,
                                    String title,
                                    String content,
                                    TradeOrder order,
                                    Long senderId,
                                    String senderName) {
        Message message = new Message();
        message.setUserId(requireUserId(userId));
        message.setType(Message.TYPE_TRADE);
        message.setSubType(subType == null ? 1 : subType);
        message.setTitle(firstNonBlank(title, "交易消息"));
        message.setContent(requireContent(content));
        message.setSenderId(senderId);
        message.setSenderName(firstNonBlank(senderName, senderId == null ? "系统" : resolveUsername(senderId, "系统")));

        if (order != null) {
            message.setRelatedOrderNo(order.getOrderNo());
            message.setRelatedItemId(order.getItemId());
            message.setItemName(resolveItemName(order.getItemId()));
        }

        persist(message);
        return message;
    }

    public Message sendSystemMessage(Long userId, Integer subType, String title, String content) {
        return sendSystemMessage(userId, subType, title, content, null, "系统");
    }

    public Message sendSystemMessage(Long userId,
                                     Integer subType,
                                     String title,
                                     String content,
                                     Long senderId,
                                     String senderName) {
        Message message = new Message();
        message.setUserId(requireUserId(userId));
        message.setType(Message.TYPE_SYSTEM);
        message.setSubType(subType == null ? 3 : subType);
        message.setTitle(firstNonBlank(title, "系统通知"));
        message.setContent(requireContent(content));
        message.setSenderId(senderId);
        message.setSenderName(firstNonBlank(senderName, senderId == null ? "系统" : resolveUsername(senderId, "系统")));
        persist(message);
        return message;
    }

    public Message sendSystemMessage(Message message) {
        if (message == null) {
            throw new RuntimeException("消息不能为空");
        }

        message.setUserId(requireUserId(message.getUserId()));
        message.setType(Message.TYPE_SYSTEM);
        message.setSubType(message.getSubType() == null ? 3 : message.getSubType());
        message.setTitle(firstNonBlank(message.getTitle(), "系统通知"));
        message.setContent(requireContent(message.getContent()));
        message.setSenderName(firstNonBlank(message.getSenderName(), "系统"));
        persist(message);
        return message;
    }

    public String resolveItemName(Long itemId) {
        if (itemId == null) {
            return "这件饰品";
        }
        Item item = itemMapper.selectById(itemId);
        if (item == null) {
            return "饰品 #" + itemId;
        }
        return firstNonBlank(item.getNameCn(), item.getName(), "饰品 #" + itemId);
    }

    public String resolveUsername(Long userId, String fallback) {
        if (userId == null) {
            return firstNonBlank(fallback, "用户");
        }
        User user = userMapper.selectById(userId);
        return firstNonBlank(user == null ? null : user.getUsername(), fallback, "用户");
    }

    public String formatMoneyWithSymbol(BigDecimal amount) {
        return "¥" + formatMoney(amount);
    }

    private void persist(Message message) {
        message.setStatus(0);
        messageMapper.createMessage(message);
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new RuntimeException("接收用户不能为空");
        }
        return userId;
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("消息内容不能为空");
        }
        return content;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}

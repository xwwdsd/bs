package com.cs2trade.service;

import com.cs2trade.entity.Item;
import com.cs2trade.entity.Message;
import com.cs2trade.entity.SellOrder;
import com.cs2trade.entity.TradeOrder;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.MessageMapper;
import com.cs2trade.mapper.SellOrderMapper;
import com.cs2trade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final SellOrderMapper sellOrderMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final TradeOrderService tradeOrderService;

    public List<Message> getMessages(Long userId, Integer type, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return messageMapper.getMessages(userId, type, offset, size);
    }

    public int countMessages(Long userId, Integer type) {
        return messageMapper.countMessages(userId, type);
    }

    public void createMessage(Message message) {
        messageMapper.createMessage(message);
    }

    public Message getMessageById(Long id) {
        return messageMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Message createBargainMessage(Long buyerId, Long sellOrderId, BigDecimal bargainPrice) {
        if (sellOrderId == null) {
            throw new RuntimeException("\u8bf7\u5148\u9009\u62e9\u8981\u8fd8\u4ef7\u7684\u5356\u5355");
        }
        if (bargainPrice == null || bargainPrice.compareTo(new BigDecimal("0.01")) < 0) {
            throw new RuntimeException("\u8fd8\u4ef7\u91d1\u989d\u5fc5\u987b\u5927\u4e8e 0");
        }

        SellOrder sellOrder = sellOrderMapper.selectById(sellOrderId);
        if (sellOrder == null) {
            throw new RuntimeException("\u5356\u5355\u4e0d\u5b58\u5728");
        }
        if (sellOrder.getStatus() == null || sellOrder.getStatus() != SellOrder.STATUS_ON_SALE) {
            throw new RuntimeException("\u8fd9\u6761\u5356\u5355\u5f53\u524d\u4e0d\u652f\u6301\u8fd8\u4ef7");
        }
        if (sellOrder.getUserId().equals(buyerId)) {
            throw new RuntimeException("\u4e0d\u80fd\u5bf9\u81ea\u5df1\u7684\u5356\u5355\u8fd8\u4ef7");
        }
        if (sellOrder.getPrice() != null && bargainPrice.compareTo(sellOrder.getPrice()) >= 0) {
            throw new RuntimeException("\u8fd8\u4ef7\u91d1\u989d\u5fc5\u987b\u4f4e\u4e8e\u5f53\u524d\u552e\u4ef7");
        }
        if (messageMapper.countPendingBargains(buyerId, sellOrderId) > 0) {
            throw new RuntimeException("\u4f60\u5df2\u7ecf\u5bf9\u8fd9\u6761\u5356\u5355\u53d1\u8d77\u8fc7\u5f85\u5904\u7406\u7684\u8fd8\u4ef7");
        }

        User buyer = userMapper.selectById(buyerId);
        Item item = itemMapper.selectById(sellOrder.getItemId());
        String buyerName = firstNonBlank(buyer != null ? buyer.getUsername() : null, "\u4e70\u5bb6");
        String itemName = resolveItemName(item, sellOrder.getItemId());

        Message message = new Message();
        message.setUserId(sellOrder.getUserId());
        message.setType(Message.TYPE_BARGAIN);
        message.setSubType(Message.BARGAIN_STATUS_PENDING);
        message.setTitle("\u6536\u5230\u65b0\u7684\u8fd8\u4ef7");
        message.setContent(
                buyerName
                        + "\u5bf9\u300c" + itemName + "\u300d\u51fa\u4ef7 "
                        + formatMoneyWithSymbol(bargainPrice)
                        + "\uff0c\u5f53\u524d\u552e\u4ef7 "
                        + formatMoneyWithSymbol(sellOrder.getPrice())
                        + "\u3002"
        );
        message.setRelatedItemId(sellOrder.getItemId());
        message.setRelatedSellOrderId(sellOrder.getId());
        message.setItemName(itemName);
        message.setBargainPrice(bargainPrice);
        message.setSenderId(buyerId);
        message.setSenderName(buyerName);
        createMessage(message);
        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public TradeOrder acceptBargain(Long sellerId, Long messageId) {
        Message message = requirePendingBargainMessageForUser(messageId, sellerId);

        SellOrder sellOrder = sellOrderMapper.selectById(message.getRelatedSellOrderId());
        if (sellOrder == null) {
            throw new RuntimeException("\u5173\u8054\u5356\u5355\u4e0d\u5b58\u5728");
        }
        if (!sellerId.equals(sellOrder.getUserId())) {
            throw new RuntimeException("\u4f60\u65e0\u6743\u5904\u7406\u8fd9\u6761\u8fd8\u4ef7");
        }
        if (sellOrder.getStatus() == null || sellOrder.getStatus() != SellOrder.STATUS_ON_SALE) {
            throw new RuntimeException("\u8fd9\u6761\u5356\u5355\u5df2\u4e0d\u5728\u552e");
        }

        TradeOrder order = tradeOrderService.createOrder(
                message.getSenderId(),
                sellOrder.getId(),
                message.getBargainPrice()
        );

        String itemName = firstNonBlank(
                message.getItemName(),
                resolveItemName(itemMapper.selectById(sellOrder.getItemId()), sellOrder.getItemId())
        );
        String sellerName = resolveUsername(sellerId, "\u5356\u5bb6");
        String sellerContent =
                "\u4f60\u5df2\u63a5\u53d7"
                        + firstNonBlank(message.getSenderName(), "\u4e70\u5bb6")
                        + "\u5bf9\u300c" + itemName + "\u300d\u7684\u8fd8\u4ef7\uff0c\u7cfb\u7edf\u5df2\u6309 "
                        + formatMoneyWithSymbol(message.getBargainPrice())
                        + "\u521b\u5efa\u8ba2\u5355 "
                        + order.getOrderNo()
                        + "\u3002";

        messageMapper.updateBargainStatus(
                message.getId(),
                Message.BARGAIN_STATUS_ACCEPTED,
                1,
                order.getOrderNo(),
                sellerContent,
                "\u8fd8\u4ef7\u5df2\u63a5\u53d7"
        );

        Message buyerNotice = new Message();
        buyerNotice.setUserId(message.getSenderId());
        buyerNotice.setType(Message.TYPE_BARGAIN);
        buyerNotice.setSubType(Message.BARGAIN_STATUS_ACCEPTED);
        buyerNotice.setTitle("\u4f60\u7684\u8fd8\u4ef7\u5df2\u88ab\u63a5\u53d7");
        buyerNotice.setContent(
                "\u5356\u5bb6\u5df2\u63a5\u53d7\u4f60\u5bf9\u300c"
                        + itemName
                        + "\u300d\u7684\u8fd8\u4ef7\uff0c\u7cfb\u7edf\u5df2\u6309 "
                        + formatMoneyWithSymbol(message.getBargainPrice())
                        + "\u521b\u5efa\u8ba2\u5355 "
                        + order.getOrderNo()
                        + "\uff0c\u8bf7\u5c3d\u5feb\u5b8c\u6210\u652f\u4ed8\u3002"
        );
        buyerNotice.setRelatedOrderNo(order.getOrderNo());
        buyerNotice.setRelatedItemId(message.getRelatedItemId());
        buyerNotice.setRelatedSellOrderId(message.getRelatedSellOrderId());
        buyerNotice.setItemName(itemName);
        buyerNotice.setBargainPrice(message.getBargainPrice());
        buyerNotice.setSenderId(sellerId);
        buyerNotice.setSenderName(sellerName);
        createMessage(buyerNotice);

        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public Message rejectBargain(Long sellerId, Long messageId) {
        Message message = requirePendingBargainMessageForUser(messageId, sellerId);

        String sellerContent =
                "\u4f60\u5df2\u62d2\u7edd"
                        + firstNonBlank(message.getSenderName(), "\u4e70\u5bb6")
                        + "\u7684\u8fd8\u4ef7\u7533\u8bf7\u3002";

        messageMapper.updateBargainStatus(
                message.getId(),
                Message.BARGAIN_STATUS_REJECTED,
                1,
                message.getRelatedOrderNo(),
                sellerContent,
                "\u8fd8\u4ef7\u5df2\u62d2\u7edd"
        );

        Message buyerNotice = new Message();
        buyerNotice.setUserId(message.getSenderId());
        buyerNotice.setType(Message.TYPE_BARGAIN);
        buyerNotice.setSubType(Message.BARGAIN_STATUS_REJECTED);
        buyerNotice.setTitle("\u4f60\u7684\u8fd8\u4ef7\u5df2\u88ab\u62d2\u7edd");
        buyerNotice.setContent(
                "\u5356\u5bb6\u5df2\u62d2\u7edd\u4f60\u5bf9\u300c"
                        + firstNonBlank(message.getItemName(), "\u8fd9\u4ef6\u9970\u54c1")
                        + "\u300d\u7684\u8fd8\u4ef7\uff0c\u4f60\u53ef\u4ee5\u91cd\u65b0\u51fa\u4ef7\u6216\u76f4\u63a5\u8d2d\u4e70\u3002"
        );
        buyerNotice.setRelatedItemId(message.getRelatedItemId());
        buyerNotice.setRelatedSellOrderId(message.getRelatedSellOrderId());
        buyerNotice.setItemName(message.getItemName());
        buyerNotice.setBargainPrice(message.getBargainPrice());
        buyerNotice.setSenderId(sellerId);
        buyerNotice.setSenderName(resolveUsername(sellerId, "\u5356\u5bb6"));
        createMessage(buyerNotice);

        return messageMapper.selectById(messageId);
    }

    public void markAsRead(Long id) {
        messageMapper.markAsRead(id);
    }

    public void batchMarkAsRead(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            messageMapper.batchMarkAsRead(ids);
        }
    }

    public void markAllRead(Long userId, Integer type) {
        messageMapper.markAllRead(userId, type);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            messageMapper.batchDelete(ids);
        }
    }

    public int countUnread(Long userId, Integer type) {
        return messageMapper.countUnread(userId, type);
    }

    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId, null);
    }

    private Message requirePendingBargainMessageForUser(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("\u8fd8\u4ef7\u7559\u8a00\u4e0d\u5b58\u5728");
        }
        if (!userId.equals(message.getUserId())) {
            throw new RuntimeException("\u4f60\u65e0\u6743\u67e5\u770b\u6216\u5904\u7406\u8fd9\u6761\u8fd8\u4ef7");
        }
        if (message.getType() == null || message.getType() != Message.TYPE_BARGAIN) {
            throw new RuntimeException("\u8fd9\u4e0d\u662f\u4e00\u6761\u8fd8\u4ef7\u7559\u8a00");
        }
        if (message.getRelatedSellOrderId() == null) {
            throw new RuntimeException("\u8fd9\u6761\u8fd8\u4ef7\u7f3a\u5c11\u5173\u8054\u5356\u5355");
        }
        if (message.getSubType() != null && message.getSubType() != Message.BARGAIN_STATUS_PENDING) {
            throw new RuntimeException("\u8fd9\u6761\u8fd8\u4ef7\u5df2\u7ecf\u5904\u7406\u8fc7\u4e86");
        }
        return message;
    }

    private String resolveItemName(Item item, Long itemId) {
        if (item != null) {
            return firstNonBlank(item.getNameCn(), item.getName());
        }
        return itemId != null ? "\u9970\u54c1 #" + itemId : "\u8fd9\u4ef6\u9970\u54c1";
    }

    private String resolveUsername(Long userId, String fallback) {
        User user = userMapper.selectById(userId);
        return firstNonBlank(user != null ? user.getUsername() : null, fallback);
    }

    private String formatMoneyWithSymbol(BigDecimal amount) {
        return "\uFFE5" + formatMoney(amount);
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

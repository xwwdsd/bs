package com.cs2trade.service;

import com.cs2trade.entity.Message;
import com.cs2trade.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    /**
     * 获取消息列表
     */
    public List<Message> getMessages(Long userId, Integer type, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return messageMapper.getMessages(userId, type, offset, size);
    }

    /**
     * 统计消息数量
     */
    public int countMessages(Long userId, Integer type) {
        return messageMapper.countMessages(userId, type);
    }

    /**
     * 创建消息
     */
    public void createMessage(Message message) {
        messageMapper.createMessage(message);
    }

    /**
     * 标记单条消息已读
     */
    public void markAsRead(Long id) {
        messageMapper.markAsRead(id);
    }

    /**
     * 批量标记已读
     */
    public void batchMarkAsRead(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            messageMapper.batchMarkAsRead(ids);
        }
    }

    /**
     * 全部标记已读
     */
    public void markAllRead(Long userId, Integer type) {
        messageMapper.markAllRead(userId, type);
    }

    /**
     * 批量删除消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            messageMapper.batchDelete(ids);
        }
    }

    /**
     * 统计未读消息数量
     */
    public int countUnread(Long userId, Integer type) {
        return messageMapper.countUnread(userId, type);
    }

    /**
     * 统计所有未读消息数量
     */
    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId, null);
    }
}

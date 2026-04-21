package com.cs2trade.mapper;

import com.cs2trade.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息数据访问层
 */
@Mapper
public interface MessageMapper {
    Message selectById(@Param("id") Long id);
    
    /**
     * 获取消息列表
     */
    List<Message> getMessages(@Param("userId") Long userId, @Param("type") Integer type, 
                              @Param("offset") Integer offset, @Param("size") Integer size);

    List<Message> selectAllForAdmin(@Param("userId") Long userId,
                                    @Param("type") Integer type,
                                    @Param("status") Integer status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    long countAllForAdmin(@Param("userId") Long userId,
                          @Param("type") Integer type,
                          @Param("status") Integer status,
                          @Param("keyword") String keyword);
    
    /**
     * 统计消息数量
     */
    int countMessages(@Param("userId") Long userId, @Param("type") Integer type);
    
    /**
     * 创建消息
     */
    void createMessage(Message message);
    int countPendingBargains(@Param("senderId") Long senderId, @Param("relatedSellOrderId") Long relatedSellOrderId);
    void updateBargainStatus(@Param("id") Long id,
                             @Param("subType") Integer subType,
                             @Param("status") Integer status,
                             @Param("relatedOrderNo") String relatedOrderNo,
                             @Param("content") String content,
                             @Param("title") String title);
    
    /**
     * 标记单条消息已读
     */
    void markAsRead(@Param("id") Long id);
    
    /**
     * 批量标记已读
     */
    void batchMarkAsRead(@Param("ids") List<Long> ids);
    
    /**
     * 全部标记已读
     */
    void markAllRead(@Param("userId") Long userId, @Param("type") Integer type);
    
    /**
     * 批量删除消息
     */
    void batchDelete(@Param("ids") List<Long> ids);
    
    /**
     * 统计未读消息数量
     */
    int countUnread(@Param("userId") Long userId, @Param("type") Integer type);
}

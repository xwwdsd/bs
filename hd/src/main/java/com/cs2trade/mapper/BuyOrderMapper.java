package com.cs2trade.mapper;

import com.cs2trade.entity.BuyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BuyOrderMapper {

    BuyOrder selectById(@Param("id") Long id);

    List<BuyOrder> selectByUserId(@Param("userId") Long userId);

    List<BuyOrder> selectActiveByItemId(@Param("itemId") Long itemId);

    List<BuyOrder> selectMarketList(@Param("category") String category,
                                    @Param("exterior") String exterior,
                                    @Param("quality") String quality,
                                    @Param("keyword") String keyword,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("sortField") String sortField,
                                    @Param("sortOrder") String sortOrder,
                                    @Param("offset") Integer offset,
                                    @Param("size") Integer size);

    Long countMarketList(@Param("category") String category,
                         @Param("exterior") String exterior,
                         @Param("quality") String quality,
                         @Param("keyword") String keyword,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice);

    int insert(BuyOrder buyOrder);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int incrementFilledQuantity(@Param("id") Long id);

    int expireOldActiveOrders();
}

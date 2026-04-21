package com.cs2trade.mapper;

import com.cs2trade.dto.market.ItemIconCandidate;
import com.cs2trade.dto.market.RecentTradeRecord;
import com.cs2trade.dto.market.TradeDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MarketAnalyticsMapper {

    @Select("""
            SELECT COALESCE(MIN(price), 0)
            FROM sell_order
            WHERE item_id = #{itemId}
              AND status = 1
            """)
    BigDecimal selectLowestActiveSellPrice(@Param("itemId") Long itemId);

    @Select("""
            SELECT COALESCE(MAX(price), 0)
            FROM buy_order
            WHERE item_id = #{itemId}
              AND status = 1
              AND filled_quantity < quantity
            """)
    BigDecimal selectHighestActiveBuyPrice(@Param("itemId") Long itemId);

    @Select("""
            SELECT COUNT(*)
            FROM sell_order
            WHERE item_id = #{itemId}
              AND status = 1
            """)
    Integer countActiveSellOrders(@Param("itemId") Long itemId);

    @Select("""
            SELECT COUNT(*)
            FROM sys_favorite
            WHERE type = 1
              AND item_id = #{itemId}
            """)
    Integer countFavorites(@Param("itemId") Long itemId);

    @Select("""
            SELECT COALESCE(AVG(price), 0)
            FROM trade_order
            WHERE item_id = #{itemId}
              AND status = 4
              AND completed_at IS NOT NULL
              AND completed_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            """)
    BigDecimal selectAverageCompletedPriceWithinDays(@Param("itemId") Long itemId, @Param("days") int days);

    @Select("""
            SELECT COUNT(*)
            FROM trade_order
            WHERE item_id = #{itemId}
              AND status = 4
              AND completed_at IS NOT NULL
              AND completed_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            """)
    Integer countCompletedWithinDays(@Param("itemId") Long itemId, @Param("days") int days);

    @Select("""
            SELECT DATE(completed_at) AS tradeDate,
                   ROUND(AVG(price), 2) AS averagePrice,
                   COUNT(*) AS volume
            FROM trade_order
            WHERE item_id = #{itemId}
              AND status = 4
              AND completed_at IS NOT NULL
            GROUP BY DATE(completed_at)
            ORDER BY DATE(completed_at) ASC
            """)
    List<TradeDailyStat> selectDailyCompletedStats(@Param("itemId") Long itemId);

    @Select("""
            SELECT completed_at AS completedAt,
                   price,
                   'local_trade' AS source
            FROM trade_order
            WHERE item_id = #{itemId}
              AND status = 4
              AND completed_at IS NOT NULL
            ORDER BY completed_at DESC
            LIMIT #{limit}
            """)
    List<RecentTradeRecord> selectRecentCompletedTrades(@Param("itemId") Long itemId, @Param("limit") int limit);

    @Select("""
            SELECT iconUrl
            FROM (
                SELECT COALESCE(NULLIF(ui.icon_url_large, ''), NULLIF(ui.icon_url, '')) AS iconUrl,
                       CASE WHEN so.id IS NULL THEN 1 ELSE 0 END AS priority,
                       ui.updated_at AS updatedAt
                FROM user_inventory ui
                LEFT JOIN sell_order so ON so.inventory_id = ui.id
                  AND so.item_id = ui.item_id
                  AND so.status = 1
                WHERE ui.item_id = #{itemId}
            ) candidate
            WHERE iconUrl IS NOT NULL
            ORDER BY priority ASC, updatedAt DESC
            LIMIT 1
            """)
    String selectBestInventoryIconUrl(@Param("itemId") Long itemId);

    @Select("""
            <script>
            SELECT ui.item_id AS itemId,
                   COALESCE(NULLIF(ui.icon_url_large, ''), NULLIF(ui.icon_url, '')) AS iconUrl
            FROM user_inventory ui
            LEFT JOIN sell_order so ON so.inventory_id = ui.id
              AND so.item_id = ui.item_id
              AND so.status = 1
            WHERE ui.item_id IN
            <foreach collection="itemIds" item="itemId" open="(" separator="," close=")">
                #{itemId}
            </foreach>
              AND COALESCE(NULLIF(ui.icon_url_large, ''), NULLIF(ui.icon_url, '')) IS NOT NULL
            ORDER BY ui.item_id ASC,
                     CASE WHEN so.id IS NULL THEN 1 ELSE 0 END ASC,
                     ui.updated_at DESC
            </script>
            """)
    List<ItemIconCandidate> selectBestInventoryIconUrls(@Param("itemIds") List<Long> itemIds);
}

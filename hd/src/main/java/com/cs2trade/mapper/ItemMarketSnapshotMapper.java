package com.cs2trade.mapper;

import com.cs2trade.entity.ItemMarketSnapshot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ItemMarketSnapshotMapper {

    @Select("""
            SELECT id,
                   item_id AS itemId,
                   snapshot_date AS snapshotDate,
                   latest_price AS latestPrice,
                   reference_price AS referencePrice,
                   lowest_sell_price AS lowestSellPrice,
                   avg_trade_price_7d AS avgTradePrice7d,
                   trade_count_7d AS tradeCount7d,
                   trade_count_30d AS tradeCount30d,
                   active_sell_count AS activeSellCount,
                   favorite_count AS favoriteCount,
                   price_change_7d AS priceChange7d,
                   price_change_30d AS priceChange30d,
                   volatility_score AS volatilityScore,
                   liquidity_score AS liquidityScore,
                   heat_score AS heatScore,
                   suggested_buy_price AS suggestedBuyPrice,
                   suggested_sell_price AS suggestedSellPrice,
                   created_at AS createdAt
            FROM item_market_snapshot
            WHERE item_id = #{itemId}
            ORDER BY snapshot_date DESC
            LIMIT 1
            """)
    ItemMarketSnapshot selectLatestByItemId(@Param("itemId") Long itemId);

    @Select("""
            SELECT id,
                   item_id AS itemId,
                   snapshot_date AS snapshotDate,
                   latest_price AS latestPrice,
                   reference_price AS referencePrice,
                   lowest_sell_price AS lowestSellPrice,
                   avg_trade_price_7d AS avgTradePrice7d,
                   trade_count_7d AS tradeCount7d,
                   trade_count_30d AS tradeCount30d,
                   active_sell_count AS activeSellCount,
                   favorite_count AS favoriteCount,
                   price_change_7d AS priceChange7d,
                   price_change_30d AS priceChange30d,
                   volatility_score AS volatilityScore,
                   liquidity_score AS liquidityScore,
                   heat_score AS heatScore,
                   suggested_buy_price AS suggestedBuyPrice,
                   suggested_sell_price AS suggestedSellPrice,
                   created_at AS createdAt
            FROM item_market_snapshot
            WHERE snapshot_date = (SELECT MAX(snapshot_date) FROM item_market_snapshot)
            ORDER BY heat_score DESC, liquidity_score DESC, item_id ASC
            """)
    List<ItemMarketSnapshot> selectLatestAll();

    @Delete("""
            DELETE FROM item_market_snapshot
            WHERE item_id = #{itemId}
              AND snapshot_date = #{snapshotDate}
            """)
    int deleteByItemIdAndSnapshotDate(@Param("itemId") Long itemId, @Param("snapshotDate") LocalDate snapshotDate);

    @Insert("""
            INSERT INTO item_market_snapshot (
                item_id, snapshot_date, latest_price, reference_price, lowest_sell_price,
                avg_trade_price_7d, trade_count_7d, trade_count_30d, active_sell_count,
                favorite_count, price_change_7d, price_change_30d, volatility_score,
                liquidity_score, heat_score, suggested_buy_price, suggested_sell_price, created_at
            ) VALUES (
                #{itemId}, #{snapshotDate}, #{latestPrice}, #{referencePrice}, #{lowestSellPrice},
                #{avgTradePrice7d}, #{tradeCount7d}, #{tradeCount30d}, #{activeSellCount},
                #{favoriteCount}, #{priceChange7d}, #{priceChange30d}, #{volatilityScore},
                #{liquidityScore}, #{heatScore}, #{suggestedBuyPrice}, #{suggestedSellPrice}, NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ItemMarketSnapshot snapshot);
}

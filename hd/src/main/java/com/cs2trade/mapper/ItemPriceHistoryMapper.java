package com.cs2trade.mapper;

import com.cs2trade.entity.ItemPriceHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ItemPriceHistoryMapper {

    @Select("""
            SELECT id,
                   item_id AS itemId,
                   source,
                   price,
                   volume,
                   recorded_at AS recordedAt,
                   range_type AS rangeType,
                   created_at AS createdAt
            FROM item_price_history
            WHERE item_id = #{itemId}
              AND source = #{source}
            ORDER BY recorded_at ASC
            """)
    List<ItemPriceHistory> selectByItemIdAndSource(@Param("itemId") Long itemId, @Param("source") String source);

    @Delete("""
            DELETE FROM item_price_history
            WHERE item_id = #{itemId}
              AND source = #{source}
            """)
    int deleteByItemIdAndSource(@Param("itemId") Long itemId, @Param("source") String source);

    @Insert("""
            INSERT INTO item_price_history (
                item_id, source, price, volume, recorded_at, range_type, created_at
            ) VALUES (
                #{itemId}, #{source}, #{price}, #{volume}, #{recordedAt}, #{rangeType}, NOW()
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ItemPriceHistory history);
}

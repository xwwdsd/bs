package com.cs2trade.mapper;

import com.cs2trade.entity.PlayerShowLike;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PlayerShowLikeMapper {
    
    @Select("SELECT * FROM player_show_like WHERE show_id = #{showId} AND user_id = #{userId}")
    PlayerShowLike findByShowIdAndUserId(@Param("showId") Long showId, @Param("userId") Long userId);
    
    @Insert("INSERT INTO player_show_like (show_id, user_id, created_at) VALUES (#{showId}, #{userId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlayerShowLike like);
    
    @Delete("DELETE FROM player_show_like WHERE show_id = #{showId} AND user_id = #{userId}")
    int delete(@Param("showId") Long showId, @Param("userId") Long userId);
    
    @Select("SELECT COUNT(*) > 0 FROM player_show_like WHERE show_id = #{showId} AND user_id = #{userId}")
    boolean hasLiked(@Param("showId") Long showId, @Param("userId") Long userId);
}

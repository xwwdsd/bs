package com.cs2trade.mapper;

import com.cs2trade.entity.PlayerShowLike;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    @Select("""
            <script>
            SELECT *
            FROM player_show_like
            <where>
                <if test='showId != null'>AND show_id = #{showId}</if>
                <if test='userId != null'>AND user_id = #{userId}</if>
            </where>
            ORDER BY created_at DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<PlayerShowLike> selectAll(@Param("showId") Long showId,
                                   @Param("userId") Long userId,
                                   @Param("offset") Integer offset,
                                   @Param("size") Integer size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM player_show_like
            <where>
                <if test='showId != null'>AND show_id = #{showId}</if>
                <if test='userId != null'>AND user_id = #{userId}</if>
            </where>
            </script>
            """)
    long countAll(@Param("showId") Long showId, @Param("userId") Long userId);
}

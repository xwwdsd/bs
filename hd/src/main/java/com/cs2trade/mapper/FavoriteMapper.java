package com.cs2trade.mapper;

import com.cs2trade.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {
    List<Favorite> getFavorites(@Param("userId") Long userId, @Param("type") Integer type); // type: 1=Item, 2=News (Optional)
    List<Favorite> selectAll(@Param("userId") Long userId,
                             @Param("type") Integer type,
                             @Param("targetId") Long targetId,
                             @Param("offset") Integer offset,
                             @Param("size") Integer size);
    long countAll(@Param("userId") Long userId,
                  @Param("type") Integer type,
                  @Param("targetId") Long targetId);
    void addFavorite(Favorite favorite);
    void removeFavorite(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("type") Integer type);
    boolean isFavorite(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("type") Integer type);
    int deleteById(@Param("id") Long id);
}

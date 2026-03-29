package com.cs2trade.mapper;

import com.cs2trade.entity.PlayerShow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PlayerShowMapper {
    List<PlayerShow> selectAll();
    List<PlayerShow> selectByUserId(@Param("userId") Long userId);
    PlayerShow selectById(Long id);
    int insert(PlayerShow show);
    int incrementLikes(Long id);
    int deleteById(Long id);
}

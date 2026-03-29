package com.cs2trade.mapper;

import com.cs2trade.entity.PlayerShowComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PlayerShowCommentMapper {
    List<PlayerShowComment> selectByShowId(Long showId);
    int insert(PlayerShowComment comment);
}

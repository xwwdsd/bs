package com.cs2trade.mapper;

import com.cs2trade.entity.PlayerShowComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PlayerShowCommentMapper {
    List<PlayerShowComment> selectByShowId(Long showId);
    List<PlayerShowComment> selectAllForAdmin(@Param("showId") Long showId,
                                              @Param("userId") Long userId,
                                              @Param("keyword") String keyword,
                                              @Param("offset") Integer offset,
                                              @Param("size") Integer size);
    long countAllForAdmin(@Param("showId") Long showId,
                          @Param("userId") Long userId,
                          @Param("keyword") String keyword);
    int insert(PlayerShowComment comment);
    int deleteById(@Param("id") Long id);
}

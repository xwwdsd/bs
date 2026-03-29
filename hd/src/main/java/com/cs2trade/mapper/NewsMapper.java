package com.cs2trade.mapper;

import com.cs2trade.entity.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NewsMapper {
    List<News> selectAll();
    List<News> selectAllForAdmin();
    List<News> selectByCategory(@Param("category") String category);
    List<News> selectByUserId(@Param("userId") Long userId);
    News selectById(Long id);
    int insert(News news);
    int update(News news);
    int updateViews(Long id);
    int deleteById(Long id);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("rejectReason") String rejectReason);
}

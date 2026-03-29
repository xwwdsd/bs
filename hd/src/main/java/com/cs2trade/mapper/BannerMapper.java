package com.cs2trade.mapper;

import com.cs2trade.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BannerMapper {
    // 获取所有Banner（后台用）
    List<Banner> selectAll();
    
    // 获取启用的Banner（前台用）
    List<Banner> selectActive();
    
    // 根据ID获取
    Banner selectById(Long id);
    
    // 插入
    int insert(Banner banner);
    
    // 更新
    int update(Banner banner);
    
    // 删除
    int deleteById(Long id);
}

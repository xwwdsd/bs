package com.cs2trade.mapper;

import com.cs2trade.entity.SteamSyncTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SteamSyncTaskMapper {

    int insert(SteamSyncTask task);

    int updateById(SteamSyncTask task);

    SteamSyncTask selectById(@Param("id") Long id);

    SteamSyncTask selectLatestByTaskType(@Param("taskType") String taskType);

    List<SteamSyncTask> selectAll(@Param("taskType") String taskType,
                                  @Param("status") String status,
                                  @Param("offset") Integer offset,
                                  @Param("size") Integer size);

    long countAll(@Param("taskType") String taskType, @Param("status") String status);
}

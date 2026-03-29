package com.cs2trade.mapper;

import com.cs2trade.entity.Withdrawal;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WithdrawalMapper {

    @Select("SELECT w.*, u.username FROM withdrawal w LEFT JOIN sys_user u ON w.user_id = u.id WHERE #{status} IS NULL OR w.status = #{status} ORDER BY w.created_at DESC")
    List<Withdrawal> selectByStatus(@Param("status") Integer status);

    @Select("SELECT w.*, u.username FROM withdrawal w LEFT JOIN sys_user u ON w.user_id = u.id ORDER BY w.created_at DESC")
    List<Withdrawal> selectAll();

    @Select("SELECT w.*, u.username FROM withdrawal w LEFT JOIN sys_user u ON w.user_id = u.id WHERE w.id = #{id}")
    Withdrawal selectById(Long id);

    @Update("UPDATE withdrawal SET status = #{status}, reason = #{reason}, processed_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("reason") String reason);
}

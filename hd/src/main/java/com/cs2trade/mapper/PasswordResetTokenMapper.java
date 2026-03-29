package com.cs2trade.mapper;

import com.cs2trade.entity.PasswordResetToken;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PasswordResetTokenMapper {

    @Select("SELECT * FROM password_reset_token WHERE token = #{token}")
    PasswordResetToken selectByToken(@Param("token") String token);

    @Insert("INSERT INTO password_reset_token (user_id, token, expires_at, created_at, used) VALUES (#{userId}, #{token}, #{expiresAt}, #{createdAt}, #{used})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PasswordResetToken token);

    @Update("UPDATE password_reset_token SET used = #{used} WHERE id = #{id}")
    int updateById(PasswordResetToken token);

    @Update("UPDATE password_reset_token SET used = 1 WHERE user_id = #{userId}")
    void invalidateByUserId(@Param("userId") Long userId);
}

package com.cs2trade.mapper;

import com.cs2trade.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层接口
 * 定义对用户表(sys_user)的数据库操作方法
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     * 
     * @param id 用户ID
     * @return User 用户实体对象，不存在时返回null
     */
    User selectById(@Param("id") Long id);

    /**
     * 根据邮箱查询用户
     * 用于登录验证和检查邮箱是否已注册
     * 
     * @param email 用户邮箱
     * @return User 用户实体对象，不存在时返回null
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据用户名查询用户
     * 用于检查用户名是否已存在
     * 
     * @param username 用户名
     * @return User 用户实体对象，不存在时返回null
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据Steam ID查询用户
     * 用于Steam账号绑定验证
     * 
     * @param steamId Steam64ID
     * @return User 用户实体对象，不存在时返回null
     */
    User selectBySteamId(@Param("steamId") String steamId);

    /**
     * 插入新用户
     * 用于用户注册
     * 
     * @param user 用户实体对象
     * @return int 影响的行数
     */
    int insert(User user);

    /**
     * 根据ID更新用户信息
     * 用于修改用户资料
     * 
     * @param user 用户实体对象(包含ID)
     * @return int 影响的行数
     */
    int updateById(User user);

    /**
     * 根据ID删除用户(逻辑删除)
     * 实际项目中建议使用逻辑删除而非物理删除
     * 
     * @param id 用户ID
     * @return int 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询所有用户
     * 用于后台管理
     * 
     * @return List<User> 用户列表
     */
    List<User> selectAll();

    /**
     * 根据状态查询用户列表
     * 用于后台筛选用户
     * 
     * @param status 用户状态
     * @return List<User> 用户列表
     */
    List<User> selectByStatus(@Param("status") Integer status);

    /**
     * 更新用户最后登录信息
     * 用户登录成功后更新
     * 
     * @param id 用户ID
     * @param loginTime 登录时间
     * @param loginIp 登录IP
     * @return int 影响的行数
     */
    int updateLoginInfo(@Param("id") Long id, 
                       @Param("loginTime") java.time.LocalDateTime loginTime, 
                       @Param("loginIp") String loginIp);

    /**
     * 更新用户Steam绑定信息
     * 
     * @param id 用户ID
     * @param steamId Steam64ID
     * @param steamTradeUrl Steam交易链接
     * @param steamApiKey Steam API Key
     * @return int 影响的行数
     */
    int updateSteamInfo(@Param("id") Long id,
                       @Param("steamId") String steamId,
                       @Param("steamTradeUrl") String steamTradeUrl,
                       @Param("steamApiKey") String steamApiKey);

    /**
     * 检查邮箱是否已存在
     * 用于注册时验证
     * 
     * @param email 邮箱地址
     * @return int 存在的记录数
     */
    int countByEmail(@Param("email") String email);

    /**
     * 检查用户名是否已存在
     * 用于注册时验证
     * 
     * @param username 用户名
     * @return int 存在的记录数
     */
    int countByUsername(@Param("username") String username);

    /**
     * 根据手机号查询用户
     * 
     * @param phone 手机号
     * @return User 用户实体对象，不存在时返回null
     */
    User selectByPhone(@Param("phone") String phone);

    /**
     * 检查手机号是否已存在
     * 
     * @param phone 手机号
     * @return int 存在的记录数
     */
    int countByPhone(@Param("phone") String phone);

    /**
     * 更新用户手机号
     * 
     * @param id 用户ID
     * @param phone 手机号
     * @return int 影响的行数
     */
    int updatePhone(@Param("id") Long id, @Param("phone") String phone);

    /**
     * 查询所有用户（MyBatis-Plus兼容）
     * 用于后台管理
     * 
     * @return List<User> 用户列表
     */
    List<User> selectList(@Param("ew") Object wrapper);

    /**
     * 分页查询用户
     */
    List<User> selectByPage(@Param("keyword") String keyword, @Param("role") String role, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户数量
     */
    long countByCondition(@Param("keyword") String keyword, @Param("role") String role);
}

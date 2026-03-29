package com.cs2trade.service;

import com.cs2trade.entity.User;

/**
 * Steam服务接口
 * 处理Steam账号绑定、库存同步等功能
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
public interface SteamService {

    /**
     * 获取Steam登录URL
     * 用于引导用户到Steam进行OAuth授权
     *
     * @return String Steam登录URL
     */
    String getSteamLoginUrl();

    /**
     * 处理Steam回调
     * 验证Steam OpenID并获取用户SteamID
     *
     * @param userId  用户ID
     * @param openIdParams OpenID参数
     * @return User 更新后的用户信息
     */
    User handleSteamCallback(Long userId, java.util.Map<String, String> openIdParams);

    /**
     * 绑定Steam账号
     * 直接通过SteamID绑定（需要用户自行提供）
     *
     * @param userId      用户ID
     * @param steamId     Steam64位ID
     * @param tradeUrl    Steam交易链接
     * @return boolean 是否绑定成功
     */
    boolean bindSteamAccount(Long userId, String steamId, String tradeUrl);

    /**
     * 解绑Steam账号
     *
     * @param userId 用户ID
     * @return boolean 是否解绑成功
     */
    boolean unbindSteamAccount(Long userId);

    /**
     * 验证SteamID是否有效
     *
     * @param steamId Steam64位ID
     * @return boolean 是否有效
     */
    boolean validateSteamId(String steamId);

    /**
     * 验证交易链接是否有效
     *
     * @param tradeUrl Steam交易链接
     * @return boolean 是否有效
     */
    boolean validateTradeUrl(String tradeUrl);

    /**
     * 从交易链接中提取SteamID
     *
     * @param tradeUrl Steam交易链接
     * @return String Steam64位ID
     */
    String extractSteamIdFromTradeUrl(String tradeUrl);

    /**
     * 获取Steam用户信息
     *
     * @param steamId Steam64位ID
     * @return java.util.Map<String, Object> 用户信息
     */
    java.util.Map<String, Object> getSteamUserInfo(String steamId);

    /**
     * 保存Steam API Key
     *
     * @param userId 用户ID
     * @param apiKey Steam API Key
     * @return boolean 是否保存成功
     */
    boolean saveSteamApiKey(Long userId, String apiKey);

    /**
     * 验证Steam API Key是否有效
     *
     * @param apiKey Steam API Key
     * @return boolean 是否有效
     */
    boolean validateApiKey(String apiKey);
}

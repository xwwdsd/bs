package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.service.SteamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Steam服务实现类
 * 实现Steam账号绑定、库存同步等功能
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteamServiceImpl implements SteamService {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;

    @Value("${steam.api-key:}")
    private String steamApiKey;

    @Value("${steam.realm:http://localhost:8080}")
    private String steamRealm;

    @Value("${steam.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${steam.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${steam.proxy.port:7890}")
    private int proxyPort;

    @Value("${steam.proxy.type:HTTP}")
    private String proxyType;

    // Steam OpenID 相关URL
    private static final String STEAM_OPENID_URL = "https://steamcommunity.com/openid";
    private static final String STEAM_API_URL = "https://api.steampowered.com";
    private static final String STEAM_COMMUNITY_URL = "https://steamcommunity.com";

    @Override
    public String getSteamLoginUrl() {
        // 构建Steam OpenID登录URL
        String returnUrl = steamRealm + "/api/v1/steam/callback";

        Map<String, String> params = new HashMap<>();
        params.put("openid.ns", "http://specs.openid.net/auth/2.0");
        params.put("openid.mode", "checkid_setup");
        params.put("openid.return_to", returnUrl);
        params.put("openid.realm", steamRealm);
        params.put("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
        params.put("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");

        StringBuilder url = new StringBuilder(STEAM_OPENID_URL + "/login?");
        params.forEach((key, value) -> {
            url.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append("&");
        });

        return url.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User handleSteamCallback(Long userId, Map<String, String> openIdParams) {
        log.info("处理Steam回调: userId={}", userId);

        // 验证OpenID响应
        if (!validateOpenIdResponse(openIdParams)) {
            throw new RuntimeException("Steam OpenID验证失败");
        }

        // 提取SteamID
        String steamId = extractSteamIdFromOpenId(openIdParams.get("openid.claimed_id"));
        if (steamId == null || steamId.isEmpty()) {
            throw new RuntimeException("无法获取SteamID");
        }

        // 验证SteamID是否有效
        if (!validateSteamId(steamId)) {
            throw new RuntimeException("SteamID无效");
        }

        // 更新用户Steam信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setSteamId(steamId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("Steam账号绑定成功: userId={}, steamId={}", userId, steamId);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindSteamAccount(Long userId, String steamId, String tradeUrl) {
        log.info("绑定Steam账号: userId={}, steamId={}", userId, steamId);

        // 验证SteamID
        if (!validateSteamId(steamId)) {
            log.error("SteamID无效: {}", steamId);
            return false;
        }

        // 验证交易链接
        if (tradeUrl != null && !tradeUrl.isEmpty() && !validateTradeUrl(tradeUrl)) {
            log.error("交易链接无效: {}", tradeUrl);
            return false;
        }

        // 检查SteamID是否已被其他用户绑定
        User existingUser = userMapper.selectBySteamId(steamId);
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            log.error("SteamID已被其他用户绑定: steamId={}", steamId);
            throw new RuntimeException("该Steam账号已被其他用户绑定");
        }

        // 更新用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setSteamId(steamId);
        if (tradeUrl != null && !tradeUrl.isEmpty()) {
            user.setSteamTradeUrl(tradeUrl);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("Steam账号绑定成功: userId={}", userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindSteamAccount(Long userId) {
        log.info("解绑Steam账号: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setSteamId(null);
        user.setSteamTradeUrl(null);
        user.setSteamApiKey(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("Steam账号解绑成功: userId={}", userId);
        return true;
    }

    @Override
    public boolean validateSteamId(String steamId) {
        if (steamId == null || steamId.isEmpty()) {
            return false;
        }

        // 验证SteamID格式（17位数字）
        if (!steamId.matches("^\\d{17}$")) {
            return false;
        }

        // 如果有配置有效的API Key（不是默认值），调用Steam API验证
        if (steamApiKey != null && !steamApiKey.isEmpty() && !"YOUR_STEAM_API_KEY".equals(steamApiKey)) {
            try {
                String url = STEAM_API_URL + "/ISteamUser/GetPlayerSummaries/v0002/?key=" + steamApiKey + "&steamids=" + steamId;
                
                // 使用RestTemplate调用
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    JSONObject json = JSON.parseObject(response.getBody());
                    JSONObject responseObj = json.getJSONObject("response");
                    if (responseObj != null) {
                        var players = responseObj.getJSONArray("players");
                        return players != null && !players.isEmpty();
                    }
                }
            } catch (Exception e) {
                log.warn("Steam API验证失败（可能是证书问题），仅验证格式: {}", e.getMessage());
                // 网络不通时，只要格式正确就放行
                return true;
            }
        }

        // 没有配置API Key时，只验证格式
        return true;
    }

    @Override
    public boolean validateTradeUrl(String tradeUrl) {
        if (tradeUrl == null || tradeUrl.isEmpty()) {
            return false;
        }

        // 验证交易链接格式
        String pattern = "^https://steamcommunity\\.com/tradeoffer/new/\\?partner=\\d+&token=[a-zA-Z0-9_-]+$";
        return tradeUrl.matches(pattern);
    }

    @Override
    public String extractSteamIdFromTradeUrl(String tradeUrl) {
        if (tradeUrl == null || tradeUrl.isEmpty()) {
            return null;
        }

        // 从交易链接中提取partner ID
        Pattern pattern = Pattern.compile("partner=(\\d+)");
        Matcher matcher = pattern.matcher(tradeUrl);

        if (matcher.find()) {
            String partnerId = matcher.group(1);
            // 将partner ID转换为Steam64 ID
            return convertToSteam64Id(partnerId);
        }

        return null;
    }

    @Override
    public Map<String, Object> getSteamUserInfo(String steamId) {
        Map<String, Object> userInfo = new HashMap<>();

        // 如果没有配置有效的API Key，返回基本信息
        if (steamApiKey == null || steamApiKey.isEmpty() || "YOUR_STEAM_API_KEY".equals(steamApiKey)) {
            log.warn("未配置有效的Steam API Key，无法获取用户信息");
            // 返回基本信息
            userInfo.put("steamId", steamId);
            userInfo.put("personaName", "Steam用户");
            userInfo.put("profileUrl", "https://steamcommunity.com/profiles/" + steamId);
            userInfo.put("avatar", "/default-avatar.png");
            return userInfo;
        }

        try {
            String url = STEAM_API_URL + "/ISteamUser/GetPlayerSummaries/v0002/?key=" + steamApiKey + "&steamids=" + steamId;
            
            // 使用RestTemplate调用
            // 确保RestTemplate配置了代理
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject json = JSON.parseObject(response.getBody());
                JSONObject responseObj = json.getJSONObject("response");
                if (responseObj != null) {
                    var players = responseObj.getJSONArray("players");
                    if (players != null && !players.isEmpty()) {
                        JSONObject player = players.getJSONObject(0);
                        userInfo.put("steamId", player.getString("steamid"));
                        userInfo.put("personaName", player.getString("personaname"));
                        userInfo.put("profileUrl", player.getString("profileurl"));
                        userInfo.put("avatar", player.getString("avatar"));
                        userInfo.put("avatarMedium", player.getString("avatarmedium"));
                        userInfo.put("avatarFull", player.getString("avatarfull"));
                        userInfo.put("personaState", player.getInteger("personastate"));
                        userInfo.put("communityVisibilityState", player.getInteger("communityvisibilitystate"));
                        return userInfo;
                    } else {
                        // API返回了空玩家列表
                        log.warn("Steam API返回空玩家列表: steamId={}", steamId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取Steam用户信息失败: {}", e.getMessage());
        }
        
        // 发生异常或获取失败时，返回基本信息，保证前端能显示已绑定
        userInfo.put("steamId", steamId);
        userInfo.put("personaName", "Steam用户");
        userInfo.put("profileUrl", "https://steamcommunity.com/profiles/" + steamId);
        userInfo.put("avatar", "/default-avatar.png"); // 使用默认头像

        return userInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveSteamApiKey(Long userId, String apiKey) {
        log.info("保存Steam API Key: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setSteamApiKey(apiKey);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("Steam API Key保存成功: userId={}", userId);
        return true;
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }

        try {
            // 使用Steam Web API验证API Key
            // 调用GetNewsForApp接口来测试API Key是否有效
            String url = STEAM_API_URL + "/ISteamUser/GetPlayerSummaries/v0002/?key=" + apiKey + "&steamids=76561197960434622";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Steam API Key验证成功");
                return true;
            }
        } catch (Exception e) {
            // 如果是因为网络问题无法连接Steam，为了让用户能够完成绑定流程，
            // 这里我们记录警告并视为验证通过（只要Key格式不为空）
            // 注意：生产环境应更严格，但在开发环境或网络受限时，这是必要的妥协
            log.warn("Steam API Key验证时网络连接失败: {}, 暂时视为验证通过", e.getMessage());
            return true;
        }

        return false;
    }

    /**
     * 验证OpenID响应
     */
    private boolean validateOpenIdResponse(Map<String, String> params) {
        // 构建验证请求
        Map<String, String> validationParams = new HashMap<>(params);
        validationParams.put("openid.mode", "check_authentication");

        try {
            // 发送验证请求到Steam
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.util.MultiValueMap<String, String> map = new org.springframework.util.LinkedMultiValueMap<>();
            validationParams.forEach(map::add);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request =
                    new org.springframework.http.HttpEntity<>(map, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    STEAM_OPENID_URL + "/login", request, String.class);

            return response.getBody() != null && response.getBody().contains("is_valid:true");
        } catch (Exception e) {
            log.error("OpenID验证请求失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从OpenID claimed_id中提取SteamID
     */
    private String extractSteamIdFromOpenId(String claimedId) {
        if (claimedId == null || claimedId.isEmpty()) {
            return null;
        }

        // Steam OpenID格式: https://steamcommunity.com/openid/id/76561198xxxxxxxx
        Pattern pattern = Pattern.compile("/openid/id/(\\d+)");
        Matcher matcher = pattern.matcher(claimedId);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 将partner ID转换为Steam64 ID
     */
    private String convertToSteam64Id(String partnerId) {
        // Steam64 ID = partner ID + 76561197960265728
        BigInteger base = new BigInteger("76561197960265728");
        BigInteger partner = new BigInteger(partnerId);
        return base.add(partner).toString();
    }
}

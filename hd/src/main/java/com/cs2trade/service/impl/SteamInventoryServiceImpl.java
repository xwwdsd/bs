package com.cs2trade.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cs2trade.entity.User;
import com.cs2trade.entity.UserInventory;
import com.cs2trade.entity.Item;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.service.InspectMetadataService;
import com.cs2trade.service.SteamInventoryService;
import com.cs2trade.util.InventoryExteriorResolver;
import com.cs2trade.util.SteamApiClient;
import com.cs2trade.util.SteamItemIdentityUtils;
import com.cs2trade.util.SteamInspectLinkResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Steam库存服务实现类
 * 实现Steam库存同步和管理功能
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteamInventoryServiceImpl implements SteamInventoryService {

    private final UserInventoryMapper userInventoryMapper;
    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final SteamApiClient steamApiClient;
    private final InspectMetadataService inspectMetadataService;

    // Steam物品图片URL前缀
    private static final String STEAM_IMAGE_URL = "https://steamcommunity-a.akamaihd.net/economy/image/";
    private static final String STEAM_REFERENCE_CURRENCY = "CNY";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<UserInventory> syncInventory(Long userId) {
        log.info("同步用户Steam库存: userId={}", userId);

        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户是否绑定了Steam
        if (user.getSteamId() == null || user.getSteamId().isEmpty()) {
            log.warn("用户未绑定Steam账号: userId={}", userId);
            throw new RuntimeException("请先绑定Steam账号");
        }

        try {
            List<UserInventory> inventoryList = fetchSteamInventory(user.getSteamId(), userId);
            List<UserInventory> existingInventory = userInventoryMapper.selectByUserId(userId);
            log.info("同步前现有库存：userId={}, total={}", userId, existingInventory.size());

            int deletedCount = clearUserInventorySnapshot(userId);
            List<UserInventory> retainedInventory = userInventoryMapper.selectByUserId(userId);
            Map<String, UserInventory> existingMap = retainedInventory.stream()
                    .filter(inventory -> inventory.getAssetId() != null)
                    .collect(Collectors.toMap(UserInventory::getAssetId, inv -> inv, (left, right) -> left));

            int savedCount = 0;
            int updatedCount = 0;
            for (UserInventory inventory : inventoryList) {
                UserInventory existing = existingMap.get(inventory.getAssetId());
                if (existing != null) {
                    if (applySnapshotToExisting(existing, inventory)) {
                        userInventoryMapper.updateById(existing);
                        updatedCount++;
                    }
                    continue;
                }

                if (inventory.getItemId() != null) {
                    userInventoryMapper.insert(inventory);
                    savedCount++;
                } else {
                    log.debug("跳过未匹配物品：name={}, assetId={}", inventory.getName(), inventory.getAssetId());
                }
            }

            log.info("Steam 库存同步完成：userId={}, steamTotal={}, saved={}, updated={}, clearedSnapshot={}",
                    userId, inventoryList.size(), savedCount, updatedCount, deletedCount);

            return buildSyncedInventoryView(inventoryList, existingMap, false);

        } catch (Exception e) {
            log.error("同步Steam库存失败: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("同步Steam库存失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否需要更新物品信息
     */
    private boolean shouldUpdateInventory(UserInventory existing, UserInventory newInventory) {
        boolean needsUpdate = false;
        
        // 检查名称是否变化
        String existingName = String.valueOf(existing.getName());
        String newName = String.valueOf(newInventory.getName());
        if (!existingName.equals(newName)) {
            log.info("物品名称变化：assetId={}, old={}, new={}", newInventory.getAssetId(), existingName, newName);
            needsUpdate = true;
        }
        
        // 检查图标是否变化
        String existingIcon = String.valueOf(existing.getIconUrl());
        String newIcon = String.valueOf(newInventory.getIconUrl());
        if (!existingIcon.equals(newIcon)) {
            log.info("物品图标变化：assetId={}", newInventory.getAssetId());
            needsUpdate = true;
        }
        
        // 检查磨损是否变化
        if (existing.getPaintWear() == null || newInventory.getPaintWear() == null) {
            if (existing.getPaintWear() != newInventory.getPaintWear()) {
                log.info("物品磨损变化 (null 检查): assetId={}, old={}, new={}", 
                    newInventory.getAssetId(), existing.getPaintWear(), newInventory.getPaintWear());
                needsUpdate = true;
            }
        } else if (Math.abs(existing.getPaintWear().doubleValue() - newInventory.getPaintWear().doubleValue()) > 0.0000001) {
            log.info("物品磨损变化：assetId={}, old={}, new={}", 
                newInventory.getAssetId(), existing.getPaintWear(), newInventory.getPaintWear());
            needsUpdate = true;
        }
        
        // 检查外观是否变化
        String existingExterior = String.valueOf(existing.getExterior());
        String newExterior = String.valueOf(newInventory.getExterior());
        if (!existingExterior.equals(newExterior)) {
            log.info("物品外观变化：assetId={}, old={}, new={}", newInventory.getAssetId(), existingExterior, newExterior);
            needsUpdate = true;
        }
        
        // 检查图案模板是否变化
        if ((existing.getPaintSeed() == null && newInventory.getPaintSeed() != null) ||
            (existing.getPaintSeed() != null && !existing.getPaintSeed().equals(newInventory.getPaintSeed()))) {
            log.info("物品图案模板变化：assetId={}, old={}, new={}", 
                newInventory.getAssetId(), existing.getPaintSeed(), newInventory.getPaintSeed());
            needsUpdate = true;
        }
        
        if (!needsUpdate) {
            log.debug("物品无需更新：assetId={}", newInventory.getAssetId());
        }
        
        return needsUpdate;
    }

    /**
     * 更新物品信息
     */
    private void updateInventoryInfo(UserInventory existing, UserInventory newInventory) {
        existing.setName(newInventory.getName());
        existing.setIconUrl(newInventory.getIconUrl());
        existing.setIconUrlLarge(newInventory.getIconUrlLarge());
        existing.setExterior(newInventory.getExterior());
        existing.setPaintSeed(newInventory.getPaintSeed());
        existing.setPaintWear(newInventory.getPaintWear());
        existing.setUpdatedAt(java.time.LocalDateTime.now());
    }

    /**
     * 从Steam Community API获取库存
     *
     * @param steamId Steam64位ID
     * @param userId  用户ID
     * @return List<UserInventory> 库存列表
     */
    private List<UserInventory> fetchSteamInventory(String steamId, Long userId) {
        List<UserInventory> inventoryList = new ArrayList<>();

        // 使用SteamApiClient获取库存数据
        JSONObject json;
        try {
            json = steamApiClient.getInventory(steamId);
        } catch (Exception e) {
            log.error("调用Steam API失败: steamId={}, error={}", steamId, e.getMessage());
            throw new RuntimeException("无法连接Steam服务器，请检查网络或代理配置。详细错误: " + e.getMessage());
        }

        if (json == null) {
            throw new RuntimeException("无法获取Steam库存。可能原因：\n1. 网络连接失败(请检查代理配置)\n2. SteamID不正确\n3. 该账号没有CS2游戏\n4. Steam库存设置为私密\n请访问 https://steamcommunity.com/profiles/" + steamId + "/inventory/ 检查库存是否公开");
        }

        // 检查是否有错误
        if (json.containsKey("error")) {
            String error = json.getString("error");
            log.error("Steam API返回错误: {}", error);
            throw new RuntimeException("Steam API错误: " + error);
        }

        // 解析库存数据
        JSONArray assets = json.getJSONArray("assets");
        JSONArray descriptions = json.getJSONArray("descriptions");
        JSONArray assetProperties = json.getJSONArray("asset_properties");

        if (assets == null || descriptions == null) {
            log.warn("Steam库存为空或无法访问: steamId={}", steamId);
            return inventoryList;
        }

        // 构建description映射表，方便查找
        Map<String, JSONObject> descriptionMap = new HashMap<>();
        for (int i = 0; i < descriptions.size(); i++) {
            JSONObject desc = descriptions.getJSONObject(i);
            String classId = desc.getString("classid");
            String instanceId = desc.getString("instanceid");
            String key = classId + "_" + instanceId;
            descriptionMap.put(key, desc);
        }

        Map<String, JSONArray> assetPropertiesMap = buildAssetPropertiesMap(assetProperties);

        // 获取所有饰品用于匹配
        List<Item> allItems = itemMapper.selectAllActive();
        Map<String, Item> itemMap = buildItemLookup(allItems);

        // 处理每个库存物品
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            JSONArray properties = assetPropertiesMap.get(asset.getString("assetid"));
            if (properties != null && !properties.isEmpty()) {
                asset.put("asset_properties", properties);
            }

            String classId = asset.getString("classid");
            String instanceId = asset.getString("instanceid");
            String assetId = asset.getString("assetid");

            String key = classId + "_" + instanceId;
            JSONObject description = descriptionMap.get(key);

            if (description == null) {
                continue;
            }

            UserInventory inventory = parseInventoryItem(asset, description, userId, itemMap);
            if (inventory != null) {
                inventoryList.add(inventory);
            }
        }

        log.info("成功解析Steam库存: count={}", inventoryList.size());
        return inventoryList;
    }

    /**
     * 解析单个库存物品
     */
    private Map<String, JSONArray> buildAssetPropertiesMap(JSONArray assetProperties) {
        Map<String, JSONArray> assetPropertiesMap = new HashMap<>();
        if (assetProperties == null) {
            return assetPropertiesMap;
        }

        for (int i = 0; i < assetProperties.size(); i++) {
            JSONObject wrapper = assetProperties.getJSONObject(i);
            if (wrapper == null) {
                continue;
            }

            String assetId = wrapper.getString("assetid");
            JSONArray properties = wrapper.getJSONArray("asset_properties");
            if (assetId != null && properties != null && !properties.isEmpty()) {
                assetPropertiesMap.put(assetId, properties);
            }
        }

        return assetPropertiesMap;
    }

    private UserInventory parseInventoryItem(JSONObject asset, JSONObject description,
                                             Long userId, Map<String, Item> itemMap) {
        try {
            UserInventory inventory = new UserInventory();
            inventory.setUserId(userId);
            inventory.setAssetId(asset.getString("assetid"));
            inventory.setClassId(asset.getString("classid"));
            inventory.setInstanceId(asset.getString("instanceid"));

            // 获取物品名称
            String marketHashName = description.getString("market_hash_name");
            String name = description.getString("name");
            inventory.setMarketHashName(marketHashName);
            inventory.setName(name != null ? name : marketHashName);

            // 获取图标URL
            String iconUrl = description.getString("icon_url");
            inventory.setIconUrl(SteamItemIdentityUtils.normalizeSteamIconUrl(iconUrl));

            // 获取大图URL
            String iconUrlLarge = description.getString("icon_url_large");
            inventory.setIconUrlLarge(SteamItemIdentityUtils.normalizeSteamIconUrl(iconUrlLarge));

            // 检查是否可交易
            Integer tradable = description.getInteger("tradable");
            Integer marketable = description.getInteger("marketable");
            inventory.setIsMarketable((tradable != null && tradable == 1 && marketable != null && marketable == 1)
                    ? UserInventory.IS_MARKETABLE : UserInventory.NOT_MARKETABLE);
            inventory.setMarketableReason(resolveMarketableReason(tradable, marketable, description));

            // 解析库存显示所需的元数据
            parseInventoryMetadata(asset, description, inventory);

            // 尝试匹配平台饰品
            Item matchedItem = matchItem(itemMap, marketHashName, inventory.getName());
            if (matchedItem == null && marketHashName != null && !marketHashName.isBlank()) {
                try {
                    matchedItem = createItemFromInventory(marketHashName, inventory);
                    registerItemAliases(itemMap, matchedItem);
                } catch (Exception e) {
                    log.warn("Create inventory-backed item failed: assetId={}, marketHashName={}, message={}",
                            inventory.getAssetId(), marketHashName, e.getMessage());
                }
            }
            if (matchedItem != null) {
                backfillSteamCatalogMetadataFromInventory(matchedItem, inventory);
                registerItemAliases(itemMap, matchedItem);
                inventory.setItemId(matchedItem.getId());
                inventory.setItem(matchedItem);
                inventory.setMarketPrice(resolveReferencePrice(matchedItem));
                inventory.setRarity(normalizeRarity(firstNonBlank(inventory.getRarity(), matchedItem.getRarity(), matchedItem.getQuality())));
                inventory.setType(detectItemType(inventory.getName()));
                inventory.setExterior(resolveExterior(
                        inventory.getPaintWear(),
                        inventory.getExterior(),
                        inventory.getName(),
                        matchedItem.getNameCn(),
                        matchedItem.getName()
                ));

                if ((inventory.getInspectUrl() == null || inventory.getInspectUrl().isBlank())
                        && matchedItem.getInspectLinkTemplate() != null
                        && !matchedItem.getInspectLinkTemplate().isBlank()) {
                    inventory.setInspectUrl(buildInspectUrl(matchedItem, inventory));
                }
            } else {
                // 未匹配到平台饰品，创建或获取未知饰品
                Item unknownItem = getOrCreateUnknownItem();
                inventory.setItemId(unknownItem.getId());
                inventory.setItem(unknownItem);
                inventory.setMarketPrice(BigDecimal.ZERO);
                inventory.setType(detectItemType(inventory.getName()));
                log.debug("未匹配到平台饰品: name={}, 使用未知饰品id={}", inventory.getName(), unknownItem.getId());
            }

            // 设置获取时间
            inspectMetadataService.applyDecodedInspectMetadata(inventory, true);
            inventory.setExterior(resolveExterior(
                    inventory.getPaintWear(),
                    inventory.getExterior(),
                    inventory.getName(),
                    matchedItem != null ? matchedItem.getNameCn() : null,
                    matchedItem != null ? matchedItem.getName() : null
            ));
            inventory.setAcquiredAt(LocalDateTime.now());
            inventory.setCreatedAt(LocalDateTime.now());
            inventory.setUpdatedAt(LocalDateTime.now());
            
            // 设置默认状态为正常
            inventory.setStatus(UserInventory.STATUS_NORMAL);

            return inventory;

        } catch (Exception e) {
            log.error("解析库存物品失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析外观和磨损信息
     */
    private void parseExteriorAndWear(JSONObject description, UserInventory inventory) {
        // 获取tags
        JSONArray tags = description.getJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                String category = tag.getString("category");

                // 外观 (Wear)
                if ("Exterior".equals(category) || "exterior".equals(category)) {
                    inventory.setExterior(tag.getString("localized_tag_name"));
                }
            }
        }

        // 获取磨损值 (从 fraudwarnings 或 descriptions)
        JSONArray fraudWarnings = description.getJSONArray("fraudwarnings");
        if (fraudWarnings != null && !fraudWarnings.isEmpty()) {
            for (int i = 0; i < fraudWarnings.size(); i++) {
                String warning = fraudWarnings.getString(i);
                if (warning.contains("磨损") || warning.toLowerCase().contains("wear")) {
                    parseWearValue(warning, inventory);
                }
            }
        }

        // 从 descriptions 中查找磨损
        JSONArray descriptions = description.getJSONArray("descriptions");
        if (descriptions != null) {
            for (int i = 0; i < descriptions.size(); i++) {
                JSONObject desc = descriptions.getJSONObject(i);
                String value = desc.getString("value");
                if (value != null && (value.contains("磨损") || value.toLowerCase().contains("wear"))) {
                    parseWearValue(value, inventory);
                }
            }
        }
    }

    /**
     * 解析磨损值
     */
    private void parseWearValue(String text, UserInventory inventory) {
        // 磨损值格式通常是 "磨损: 0.12345678"
        try {
            if (text.contains("磨损")) {
                int start = text.indexOf("磨损");
                String substr = text.substring(start);

                // 提取数字
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+\\.\\d+");
                java.util.regex.Matcher matcher = pattern.matcher(substr);

                if (matcher.find()) {
                    String wearStr = matcher.group();
                    BigDecimal wear = new BigDecimal(wearStr);
                    inventory.setPaintWear(wear);
                }
            }
        } catch (Exception e) {
            log.debug("解析磨损值失败: {}", text);
        }
    }

    /**
     * 获取或创建未知饰品
     * 当item表为空时自动创建未知饰品记录
     */
    private void parseInventoryMetadata(JSONObject asset, JSONObject description, UserInventory inventory) {
        JSONArray tags = description.getJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                String category = tag.getString("category");
                String localizedCategory = tag.getString("localized_category_name");
                String internalName = tag.getString("internal_name");
                String localizedTagName = tag.getString("localized_tag_name");
                String tagName = tag.getString("name");

                if ("Exterior".equalsIgnoreCase(category)
                        || "外观".equals(localizedCategory)
                        || "exterior".equalsIgnoreCase(internalName)) {
                    inventory.setExterior(resolveExterior(
                            inventory.getPaintWear(),
                            firstNonBlank(localizedTagName, tagName, internalName),
                            inventory.getName()
                    ));
                }

                if ("Rarity".equalsIgnoreCase(category) || "品质".equals(localizedCategory)) {
                    inventory.setRarity(normalizeRarity(firstNonBlank(localizedTagName, tagName, internalName)));
                }
            }
        }

        List<String> metadataTexts = new ArrayList<>();
        collectTextValues(description.getJSONArray("fraudwarnings"), metadataTexts);
        collectDescriptionValues(description.getJSONArray("descriptions"), metadataTexts);
        collectDescriptionValues(description.getJSONArray("owner_descriptions"), metadataTexts);
        applyInspectLink(asset, description, inventory);
        collectActionLinks(description.getJSONArray("actions"), metadataTexts, inventory, asset);
        collectActionLinks(description.getJSONArray("market_actions"), metadataTexts, inventory, asset);

        inventory.setDescription(buildDescription(metadataTexts));
        inventory.setType(detectItemType(inventory.getName()));
        inventory.setStickers(parseStickers(metadataTexts));

        for (String text : metadataTexts) {
            parseWearValueFlexible(text, inventory);
            parsePaintSeed(text, inventory);
            parsePaintIndex(text, inventory);
        }

        if (containsSteamTradeRestriction(metadataTexts)
                && (inventory.getMarketableReason() == null || "可出售".equals(inventory.getMarketableReason()))) {
            inventory.setMarketableReason("该物品存在 Steam 交易限制");
        }

        inventory.setExterior(resolveExterior(inventory.getPaintWear(), inventory.getExterior(), inventory.getName()));

        if ((inventory.getInspectUrl() == null || inventory.getInspectUrl().isBlank()) && asset != null) {
            inventory.setInspectUrl(buildInspectUrl(null, inventory));
        }
    }

    private void parseWearValueFlexible(String text, UserInventory inventory) {
        if (inventory.getPaintWear() != null) {
            return;
        }

        try {
            String normalized = stripHtml(text).toLowerCase();
            if (!containsAny(normalized, "磨损", "wear", "float")) {
                return;
            }

            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+\\.\\d+)").matcher(normalized);
            if (matcher.find()) {
                inventory.setPaintWear(new BigDecimal(matcher.group(1)));
            }
        } catch (Exception e) {
            log.debug("解析磨损值失败: {}", text);
        }
    }

    private void parsePaintSeed(String text, UserInventory inventory) {
        if (inventory.getPaintSeed() != null) {
            return;
        }

        Integer value = extractIntByLabels(text, "图案模板", "模板", "pattern template", "paint seed");
        if (value != null) {
            inventory.setPaintSeed(value);
        }
    }

    private void parsePaintIndex(String text, UserInventory inventory) {
        if (inventory.getPaintIndex() != null) {
            return;
        }

        Integer value = extractIntByLabels(text, "皮肤编号", "饰面编号", "finish catalog", "paint index", "finish style");
        if (value != null) {
            inventory.setPaintIndex(value);
        }
    }

    private Integer extractIntByLabels(String text, String... labels) {
        String normalized = stripHtml(text);
        for (String label : labels) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    java.util.regex.Pattern.quote(label) + "\\s*[:：#]?\\s*(\\d+)",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return null;
    }

    private void collectTextValues(JSONArray source, List<String> out) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            String value = source.getString(i);
            if (value != null && !value.isBlank()) {
                out.add(value);
            }
        }
    }

    private void collectDescriptionValues(JSONArray source, List<String> out) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            JSONObject entry = source.getJSONObject(i);
            if (entry == null) {
                continue;
            }
            String value = entry.getString("value");
            if (value != null && !value.isBlank()) {
                out.add(value);
            }
        }
    }

    private void applyInspectLink(JSONObject asset, JSONObject description, UserInventory inventory) {
        if (inventory.getInspectUrl() != null && !inventory.getInspectUrl().isBlank()) {
            return;
        }

        String inspectLink = firstNonBlank(
                resolveInspectLink(description != null ? description.getString("inspect_link") : null, asset),
                findInspectLink(description != null ? description.getJSONArray("actions") : null, asset),
                findInspectLink(description != null ? description.getJSONArray("market_actions") : null, asset),
                findInspectLink(asset != null ? asset.getJSONArray("actions") : null, asset),
                findInspectLink(asset != null ? asset.getJSONArray("market_actions") : null, asset)
        );

        if (inspectLink != null) {
            inventory.setInspectUrl(inspectLink);
        }
    }

    private String findInspectLink(JSONArray source, JSONObject asset) {
        if (source == null) {
            return null;
        }

        for (int i = 0; i < source.size(); i++) {
            JSONObject entry = source.getJSONObject(i);
            if (entry == null) {
                continue;
            }

            String resolvedLink = resolveInspectLink(entry.getString("link"), asset);
            if (resolvedLink != null) {
                return resolvedLink;
            }
        }

        return null;
    }

    private String resolveInspectLink(String link, JSONObject asset) {
        String resolved = SteamInspectLinkResolver.resolve(
                link,
                asset != null ? asset.getJSONArray("asset_properties") : null
        );
        if (resolved == null || resolved.isBlank()) {
            return null;
        }

        return resolved.contains("csgo_econ_action_preview") ? resolved : null;
    }

    private void collectActionLinks(JSONArray source, List<String> out, UserInventory inventory, JSONObject asset) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            JSONObject entry = source.getJSONObject(i);
            if (entry == null) {
                continue;
            }

            String link = entry.getString("link");
            if (link != null && !link.isBlank()) {
                String resolvedLink = resolveInspectLink(link, asset);
                out.add(resolvedLink != null ? resolvedLink : link);
                if ((inventory.getInspectUrl() == null || inventory.getInspectUrl().isBlank()) && resolvedLink != null) {
                    inventory.setInspectUrl(resolvedLink);
                }
            }
        }
    }

    private List<UserInventory.StickerInfo> parseStickers(List<String> metadataTexts) {
        List<UserInventory.StickerInfo> stickers = new ArrayList<>();
        for (String text : metadataTexts) {
            String normalized = stripHtml(text);
            if (!containsAny(normalized.toLowerCase(), "贴纸", "sticker", "挂件", "charm")) {
                continue;
            }

            UserInventory.StickerInfo sticker = new UserInventory.StickerInfo();
            sticker.setName(normalized);
            stickers.add(sticker);
        }
        return stickers.isEmpty() ? null : stickers;
    }

    private String buildDescription(List<String> metadataTexts) {
        return metadataTexts.stream()
                .map(this::stripHtml)
                .filter(text -> text != null && !text.isBlank())
                .filter(text -> !text.startsWith("steam://"))
                .distinct()
                .limit(8)
                .collect(Collectors.joining(" | "));
    }

    private String resolveMarketableReason(Integer tradable, Integer marketable, JSONObject description) {
        if (tradable != null && tradable == 0) {
            return "该物品当前不可交易";
        }
        if (marketable != null && marketable == 0) {
            return "该物品当前不可上架出售";
        }

        if (containsSteamTradeRestriction(description)) {
            return "该物品存在 Steam 交易限制";
        }

        return "可出售";
    }

    private boolean containsSteamTradeRestriction(JSONObject description) {
        if (description == null) {
            return false;
        }

        List<String> metadataTexts = new ArrayList<>();
        collectTextValues(description.getJSONArray("fraudwarnings"), metadataTexts);
        collectDescriptionValues(description.getJSONArray("owner_descriptions"), metadataTexts);
        return containsSteamTradeRestriction(metadataTexts);
    }

    private boolean containsSteamTradeRestriction(List<String> metadataTexts) {
        if (metadataTexts == null || metadataTexts.isEmpty()) {
            return false;
        }

        for (String text : metadataTexts) {
            String normalized = stripHtml(text);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }

            String lower = normalized.toLowerCase();
            if (containsAny(
                    lower,
                    "受交易保护",
                    "交易保护",
                    "交易限制",
                    "不可交易",
                    "不可出售",
                    "不可转让",
                    "不能被交易",
                    "不能被出售",
                    "不能被转让",
                    "不能被消耗",
                    "不能被改造",
                    "trade hold",
                    "trade restricted",
                    "trade restriction",
                    "not tradable",
                    "not marketable",
                    "tradable after",
                    "tradeable after",
                    "cannot be traded",
                    "cannot be sold",
                    "cannot be marketed",
                    "cannot be transferred",
                    "cannot be consumed",
                    "cannot be modified"
            )) {
                return true;
            }

            if (lower.matches(".*(before|until|之前).*?(cannot|not|不可|不能).*?(trade|market|sell|transfer|交易|出售|转让).*")) {
                return true;
            }
        }

        return false;
    }

    private void mergeDisplayFields(UserInventory target, UserInventory live) {
        if (target == null) {
            return;
        }

        if (target.getItem() != null) {
            target.setRarity(normalizeRarity(firstNonBlank(target.getRarity(), target.getItem().getRarity(), target.getItem().getQuality())));
            refreshReferencePrice(target);
        }

        target.setExterior(resolveExterior(
                target.getPaintWear(),
                target.getExterior(),
                target.getName(),
                target.getItem() != null ? target.getItem().getNameCn() : null,
                target.getItem() != null ? target.getItem().getName() : null
        ));

        if (target.getType() == null) {
            target.setType(detectItemType(target.getName()));
        }

        if (live == null) {
            if (target.getMarketableReason() == null) {
                target.setMarketableReason(resolveLocalMarketableReason(target));
            }
            return;
        }

        target.setIconUrlLarge(firstNonBlank(live.getIconUrlLarge(), target.getIconUrlLarge()));
        target.setMarketHashName(firstNonBlank(live.getMarketHashName(), target.getMarketHashName()));
        target.setPaintWear(firstNonNull(live.getPaintWear(), target.getPaintWear()));
        target.setExterior(resolveExterior(
                target.getPaintWear(),
                firstNonBlank(live.getExterior(), target.getExterior()),
                target.getName(),
                target.getItem() != null ? target.getItem().getNameCn() : null,
                target.getItem() != null ? target.getItem().getName() : null
        ));
        target.setPaintSeed(firstNonNull(live.getPaintSeed(), target.getPaintSeed()));
        target.setPaintIndex(firstNonNull(live.getPaintIndex(), target.getPaintIndex()));
        target.setInspectUrl(firstNonBlank(live.getInspectUrl(), target.getInspectUrl()));
        target.setDescription(firstNonBlank(live.getDescription(), target.getDescription()));
        target.setStickers(firstNonNull(live.getStickers(), target.getStickers()));
        target.setRarity(normalizeRarity(firstNonBlank(live.getRarity(), target.getRarity())));
        target.setType(firstNonBlank(live.getType(), target.getType()));
        target.setMarketableReason(firstNonBlank(live.getMarketableReason(), resolveLocalMarketableReason(target)));
    }

    private void refreshReferencePrice(UserInventory target) {
        BigDecimal referencePrice = resolveReferencePrice(target.getItem());
        if (referencePrice != null && referencePrice.compareTo(BigDecimal.ZERO) > 0) {
            target.setMarketPrice(referencePrice);
        }
    }

    private String resolveLocalMarketableReason(UserInventory inventory) {
        if (inventory.getStatus() != null && inventory.getStatus() != UserInventory.STATUS_NORMAL) {
            return switch (inventory.getStatus()) {
                case UserInventory.STATUS_SOLD -> "该物品已售出";
                case UserInventory.STATUS_TRADING -> "该物品正在交易中";
                case UserInventory.STATUS_ON_SALE -> "该物品已在售";
                default -> "该物品当前不可出售";
            };
        }

        if (inventory.getIsMarketable() != null && inventory.getIsMarketable() == UserInventory.NOT_MARKETABLE) {
            return "该物品当前不可上架出售";
        }

        return "可出售";
    }

    private String detectItemType(String name) {
        if (name == null) {
            return null;
        }
        String lower = name.toLowerCase();
        if (lower.contains("stattrak")) {
            return name.contains("★") ? "star_stattrak" : "stattrak";
        }
        if (lower.contains("souvenir")) {
            return "souvenir";
        }
        if (name.contains("★")) {
            return "star";
        }
        return "normal";
    }

    private String buildInspectUrl(Item item, UserInventory inventory) {
        if (item == null || item.getInspectLinkTemplate() == null || item.getInspectLinkTemplate().isBlank()) {
            return inventory.getInspectUrl();
        }

        return item.getInspectLinkTemplate()
                .replace("%assetid%", String.valueOf(inventory.getAssetId()))
                .replace("%classid%", String.valueOf(inventory.getClassId()))
                .replace("%instanceid%", String.valueOf(inventory.getInstanceId()));
    }

    private String normalizeExterior(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim();
        String lower = normalized.toLowerCase();
        return switch (lower) {
            case "factory_new", "factory new", "崭新出厂" -> "FN";
            case "minimal_wear", "minimal wear", "略有磨损" -> "MW";
            case "field-tested", "field tested", "久经沙场" -> "FT";
            case "well-worn", "well worn", "破损不堪" -> "WW";
            case "battle-scarred", "battle scarred", "战痕累累" -> "BS";
            default -> normalized;
        };
    }

    private String resolveExterior(String raw, String... nameCandidates) {
        return resolveExterior(null, raw, nameCandidates);
    }

    private String resolveExterior(BigDecimal paintWear, String raw, String... nameCandidates) {
        return InventoryExteriorResolver.resolve(paintWear, raw, nameCandidates);
    }

    private String inferExteriorFromNames(String... names) {
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }

            String lower = name.toLowerCase();
            if (name.contains("崭新出厂") || lower.contains("factory new")) {
                return "FN";
            }
            if (name.contains("略有磨损") || lower.contains("minimal wear")) {
                return "MW";
            }
            if (name.contains("久经沙场") || lower.contains("field-tested") || lower.contains("field tested")) {
                return "FT";
            }
            if (name.contains("破损不堪") || lower.contains("well-worn") || lower.contains("well worn")) {
                return "WW";
            }
            if (name.contains("战痕累累") || lower.contains("battle-scarred") || lower.contains("battle scarred")) {
                return "BS";
            }
        }
        return null;
    }

    private String normalizeRarity(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }

        String compact = normalized
                .replace("rarity_", "")
                .replace("weapon_", "")
                .replace("csgo_", "")
                .replaceAll("[\\s_\\-]+", "");

        return switch (compact) {
            case "ancient", "contraband", "违禁" -> "contraband";
            case "immortal", "extraordinary", "非凡" -> "extraordinary";
            case "legendary", "covert", "隐秘" -> "covert";
            case "mythical", "classified", "保密" -> "classified";
            case "rare", "restricted", "受限" -> "restricted";
            case "uncommon", "milspec", "milspecgrade", "军规级" -> "mil-spec";
            case "industrial", "industrialgrade", "工业级" -> "industrial";
            case "common", "consumer", "consumergrade", "消费级" -> "consumer";
            case "exotic", "卓越" -> "exotic";
            case "remarkable", "奇异" -> "remarkable";
            case "superior", "highgrade", "高级" -> "high-grade";
            case "basegrade", "normalgrade", "normal", "普通级" -> "normal-grade";
            case "distinguished", "exceptional", "master", "agentgrade", "探员品质" -> "agent-grade";
            default -> normalized.replace('_', '-').replaceAll("\\s+", "-");
        };
    }

    private boolean containsAny(String text, String... tokens) {
        return Arrays.stream(tokens).anyMatch(text::contains);
    }

    private String stripHtml(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal resolveReferencePrice(Item item) {
        if (item == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal steamReferencePrice = item.getSteamReferencePrice();
        if (steamReferencePrice != null && steamReferencePrice.compareTo(BigDecimal.ZERO) > 0) {
            return steamReferencePrice;
        }

        BigDecimal buffPrice = item.getBuffPrice();
        return buffPrice != null && buffPrice.compareTo(BigDecimal.ZERO) > 0 ? buffPrice : BigDecimal.ZERO;
    }

    private void backfillSteamCatalogMetadataFromInventory(Item item, UserInventory inventory) {
        if (item == null || item.getId() == null || inventory == null) {
            return;
        }

        String marketHashName = inventory.getMarketHashName();
        String preferredInventoryIcon = SteamItemIdentityUtils.firstUsableIconUrl(
                inventory.getIconUrlLarge(),
                inventory.getIconUrl()
        );
        String steamMarketUrl = SteamItemIdentityUtils.buildSteamMarketUrl(marketHashName);

        Item update = new Item();
        update.setId(item.getId());
        boolean changed = false;

        if (marketHashName != null && !marketHashName.isBlank()
                && !marketHashName.equals(item.getSteamMarketHashName())) {
            update.setSteamMarketHashName(marketHashName);
            item.setSteamMarketHashName(marketHashName);
            changed = true;
        }

        if (steamMarketUrl != null && !steamMarketUrl.equals(item.getSteamMarketUrl())) {
            update.setSteamMarketUrl(steamMarketUrl);
            item.setSteamMarketUrl(steamMarketUrl);
            changed = true;
        }

        if (preferredInventoryIcon != null
                && !SteamItemIdentityUtils.isUsableItemIconUrl(item.getIconUrl())
                && !preferredInventoryIcon.equals(item.getIconUrl())) {
            update.setIconUrl(preferredInventoryIcon);
            item.setIconUrl(preferredInventoryIcon);
            changed = true;
        }

        if (changed) {
            itemMapper.updateById(update);
        }
    }

    private Item createItemFromInventory(String marketHashName, UserInventory inventory) {
        if (marketHashName == null || marketHashName.isBlank()) {
            return null;
        }

        Item existing = itemMapper.selectByItemId(marketHashName);
        if (existing != null) {
            return existing;
        }

        Item item = new Item();
        item.setItemId(marketHashName);
        item.setName(firstNonBlank(inventory.getName(), marketHashName));
        item.setNameCn(firstNonBlank(inventory.getName(), marketHashName));
        item.setCategory("other");
        item.setSubCategory(inventory.getType());
        item.setQuality(firstNonBlank(inventory.getRarity(), "common"));
        item.setRarity(firstNonBlank(inventory.getRarity(), "common"));
        item.setExterior(inventory.getExterior());
        item.setIconUrl(firstNonBlank(
                SteamItemIdentityUtils.firstUsableIconUrl(inventory.getIconUrlLarge(), inventory.getIconUrl()),
                inventory.getIconUrlLarge(),
                inventory.getIconUrl()
        ));
        item.setSteamMarketHashName(marketHashName);
        item.setSteamMarketUrl(SteamItemIdentityUtils.buildSteamMarketUrl(marketHashName));
        item.setBuffPrice(BigDecimal.ZERO);
        item.setSteamReferenceCurrency(STEAM_REFERENCE_CURRENCY);

        item.setIsActive(1);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.insert(item);

        log.info("Created inventory-backed item: itemId={}, name={}, steamReferencePrice={}",
                item.getItemId(), item.getName(), item.getSteamReferencePrice());
        return item;
    }

    private Item getOrCreateUnknownItem() {
        // 尝试查找已存在的未知饰品
        List<Item> items = itemMapper.selectAllActive();
        for (Item item : items) {
            if ("unknown".equals(item.getItemId()) || "未知饰品".equals(item.getNameCn())) {
                return item;
            }
        }
        
        // 创建新的未知饰品
        Item unknownItem = new Item();
        unknownItem.setItemId("unknown");
        unknownItem.setName("Unknown Item");
        unknownItem.setNameCn("未知饰品");
        unknownItem.setCategory("other");
        unknownItem.setRarity("consumer");
        unknownItem.setBuffPrice(BigDecimal.ZERO);
        unknownItem.setIsActive(1);
        unknownItem.setIconUrl("/default-item.png");
        unknownItem.setCreatedAt(LocalDateTime.now());
        unknownItem.setUpdatedAt(LocalDateTime.now());
        
        itemMapper.insert(unknownItem);
        log.info("创建未知饰品记录: id={}", unknownItem.getId());
        
        return unknownItem;
    }

    /**
     * 匹配平台饰品
     */
    private Map<String, Item> buildItemLookup(List<Item> items) {
        Map<String, Item> itemMap = new HashMap<>();
        if (items == null) {
            return itemMap;
        }

        for (Item item : items) {
            registerItemAliases(itemMap, item);
        }

        return itemMap;
    }

    private void registerItemAliases(Map<String, Item> itemMap, Item item) {
        if (itemMap == null || item == null) {
            return;
        }

        addItemAlias(itemMap, item.getItemId(), item);
        addItemAlias(itemMap, item.getSteamMarketHashName(), item);
        addItemAlias(itemMap, item.getName(), item);
        addItemAlias(itemMap, item.getNameCn(), item);
    }

    private void addItemAlias(Map<String, Item> itemMap, String alias, Item item) {
        String key = normalizeLookupKey(alias);
        if (key != null) {
            itemMap.putIfAbsent(key, item);
        }
    }

    private String normalizeLookupKey(String value) {
        return SteamItemIdentityUtils.normalizeLookupKey(value);
    }

    private Item matchItem(Map<String, Item> itemMap, String... itemNames) {
        if (itemMap == null || itemMap.isEmpty() || itemNames == null) {
            return null;
        }

        // 直接匹配
        for (String itemName : itemNames) {
            String key = normalizeLookupKey(itemName);
            if (key == null) {
                continue;
            }

            Item item = itemMap.get(key);
            if (item != null) {
                return item;
            }
        }

        // 尝试模糊匹配
        for (String itemName : itemNames) {
            String searchName = normalizeLookupKey(itemName);
            if (searchName == null) {
                continue;
            }

            for (Map.Entry<String, Item> entry : itemMap.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) {
                    continue;
                }
                if (key.contains(searchName) || searchName.contains(key)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    @Override
    public List<UserInventory> getUserInventory(Long userId) {
        return buildSnapshotInventoryView(userId, false);
    }

    @Override
    public List<UserInventory> getMarketableInventory(Long userId) {
        return buildSnapshotInventoryView(userId, true);
    }

    @Override
    public UserInventory getInventoryById(Long id) {
        UserInventory inventory = userInventoryMapper.selectById(id);
        inspectMetadataService.repairAndPersist(inventory);
        return inventory;
    }

    @Override
    public void deleteInventory(Long id) {
        userInventoryMapper.deleteById(id);
    }

    @Override
    public void clearUserInventory(Long userId) {
        clearUserInventorySnapshot(userId);
    }

    private int clearUserInventorySnapshot(Long userId) {
        int deletedCount = userInventoryMapper.deleteUnreferencedByUserId(userId);
        log.info("已清空本地库存快照：userId={}, deleted={}", userId, deletedCount);
        return deletedCount;
    }

    private List<UserInventory> buildSnapshotInventoryView(Long userId, boolean marketableOnly) {
        List<UserInventory> localInventory = userInventoryMapper.selectByUserId(userId);
        localInventory.forEach(inspectMetadataService::repairAndPersist);
        localInventory.forEach(inventory -> mergeDisplayFields(inventory, null));
        return filterInventoryForView(localInventory, marketableOnly);
    }

    private List<UserInventory> buildSyncedInventoryView(List<UserInventory> syncedInventory,
                                                         Map<String, UserInventory> localMap,
                                                         boolean marketableOnly) {
        if (syncedInventory == null || syncedInventory.isEmpty()) {
            return new ArrayList<>();
        }

        for (UserInventory inventory : syncedInventory) {
            mergeLocalStateIntoLive(inventory, localMap != null ? localMap.get(inventory.getAssetId()) : null);
            mergeDisplayFields(inventory, null);
        }

        return filterInventoryForView(syncedInventory, marketableOnly);
    }

    private List<UserInventory> filterInventoryForView(List<UserInventory> inventoryList, boolean marketableOnly) {
        if (!marketableOnly) {
            return inventoryList;
        }

        return inventoryList.stream()
                .filter(inventory -> inventory.getIsMarketable() != null
                        && inventory.getIsMarketable() == UserInventory.IS_MARKETABLE)
                .filter(inventory -> inventory.getStatus() == null
                        || inventory.getStatus() == UserInventory.STATUS_NORMAL)
                .collect(Collectors.toList());
    }

    private void mergeLocalStateIntoLive(UserInventory live, UserInventory local) {
        if (live == null || local == null) {
            return;
        }

        live.setId(local.getId());
        live.setItemId(firstNonNull(local.getItemId(), live.getItemId()));
        live.setMarketPrice(firstNonNull(local.getMarketPrice(), live.getMarketPrice()));
        live.setStatus(firstNonNull(local.getStatus(), live.getStatus(), UserInventory.STATUS_NORMAL));
        live.setAcquiredAt(firstNonNull(local.getAcquiredAt(), live.getAcquiredAt()));
        live.setCreatedAt(firstNonNull(local.getCreatedAt(), live.getCreatedAt()));
        live.setUpdatedAt(firstNonNull(local.getUpdatedAt(), live.getUpdatedAt()));
        live.setItem(firstNonNull(local.getItem(), live.getItem()));

        live.setIconUrlLarge(firstNonBlank(live.getIconUrlLarge(), local.getIconUrlLarge()));
        live.setMarketHashName(firstNonBlank(live.getMarketHashName(), local.getMarketHashName()));
        live.setPaintWear(firstNonNull(live.getPaintWear(), local.getPaintWear()));
        live.setExterior(resolveExterior(
                live.getPaintWear(),
                firstNonBlank(live.getExterior(), local.getExterior()),
                live.getName(),
                live.getItem() != null ? live.getItem().getNameCn() : null,
                live.getItem() != null ? live.getItem().getName() : null
        ));
        live.setPaintSeed(firstNonNull(live.getPaintSeed(), local.getPaintSeed()));
        live.setPaintIndex(firstNonNull(live.getPaintIndex(), local.getPaintIndex()));
        live.setInspectUrl(firstNonBlank(live.getInspectUrl(), local.getInspectUrl()));
        live.setDescription(firstNonBlank(live.getDescription(), local.getDescription()));
        live.setStickers(firstNonNull(live.getStickers(), local.getStickers()));
        live.setRarity(normalizeRarity(firstNonBlank(live.getRarity(), local.getRarity())));
        live.setType(firstNonBlank(live.getType(), local.getType()));
        live.setMarketableReason(firstNonBlank(live.getMarketableReason(), resolveLocalMarketableReason(local)));
    }

    private boolean applySnapshotToExisting(UserInventory existing, UserInventory latest) {
        if (existing == null || latest == null) {
            return false;
        }

        boolean changed = false;

        changed |= updateIfChanged(existing.getItemId(), latest.getItemId(), existing::setItemId);
        changed |= updateIfChanged(existing.getClassId(), latest.getClassId(), existing::setClassId);
        changed |= updateIfChanged(existing.getInstanceId(), latest.getInstanceId(), existing::setInstanceId);
        changed |= updateIfChanged(existing.getMarketHashName(), latest.getMarketHashName(), existing::setMarketHashName);
        changed |= updateIfChanged(existing.getName(), latest.getName(), existing::setName);
        changed |= updateIfChanged(existing.getIconUrl(), latest.getIconUrl(), existing::setIconUrl);
        changed |= updateIfChanged(existing.getIconUrlLarge(), latest.getIconUrlLarge(), existing::setIconUrlLarge);
        changed |= updateIfChanged(existing.getExterior(), latest.getExterior(), existing::setExterior);
        changed |= updateIfChanged(existing.getPaintSeed(), latest.getPaintSeed(), existing::setPaintSeed);
        changed |= updateIfChanged(existing.getPaintIndex(), latest.getPaintIndex(), existing::setPaintIndex);
        changed |= updateIfChanged(existing.getPaintWear(), latest.getPaintWear(), existing::setPaintWear);
        changed |= updateIfChanged(existing.getInspectUrl(), latest.getInspectUrl(), existing::setInspectUrl);
        changed |= updateIfChanged(existing.getDescription(), latest.getDescription(), existing::setDescription);
        changed |= updateIfChanged(existing.getMarketableReason(), latest.getMarketableReason(), existing::setMarketableReason);
        changed |= updateIfChanged(existing.getRarity(), latest.getRarity(), existing::setRarity);
        changed |= updateIfChanged(existing.getType(), latest.getType(), existing::setType);
        changed |= updateIfChanged(existing.getIsMarketable(), latest.getIsMarketable(), existing::setIsMarketable);
        changed |= updateIfChanged(existing.getMarketPrice(), latest.getMarketPrice(), existing::setMarketPrice);

        if (latest.getItem() != null) {
            existing.setItem(latest.getItem());
        }

        if (existing.getAcquiredAt() == null && latest.getAcquiredAt() != null) {
            existing.setAcquiredAt(latest.getAcquiredAt());
            changed = true;
        }

        if (changed) {
            existing.setUpdatedAt(LocalDateTime.now());
        }

        return changed;
    }

    private <T> boolean updateIfChanged(T currentValue, T newValue, java.util.function.Consumer<T> setter) {
        if (java.util.Objects.equals(currentValue, newValue)) {
            return false;
        }
        setter.accept(newValue);
        return true;
    }

    /**
     * 修复 item_id 为 NULL 的库存记录
     * 重新匹配物品并更新 item_id 字段
     */
    @Override
    public void fixNullItemIds() {
        log.info("开始修复 item_id 为 NULL 的库存记录");
        
        // 获取所有 item_id 为 NULL 的库存
        List<UserInventory> nullItemInventories = userInventoryMapper.selectWithNullItemId();
        log.info("找到 {} 条 item_id 为 NULL 的记录", nullItemInventories.size());
        
        if (nullItemInventories.isEmpty()) {
            log.info("没有需要修复的记录");
            return;
        }
        
        // 获取所有物品用于匹配
        List<Item> allItems = itemMapper.selectAllActive();
        Map<String, Item> itemMap = buildItemLookup(allItems);
        
        int fixedCount = 0;
        int notFoundCount = 0;
        
        for (UserInventory inventory : nullItemInventories) {
            try {
                // 尝试匹配物品
                Item matchedItem = matchItem(itemMap, inventory.getMarketHashName(), inventory.getName());
                
                if (matchedItem != null) {
                    // 匹配成功，更新 item_id
                    inventory.setItemId(matchedItem.getId());
                    inventory.setMarketPrice(resolveReferencePrice(matchedItem));
                    userInventoryMapper.updateById(inventory);
                    fixedCount++;
                    log.info("修复成功：inventoryId={}, name={}, itemId={}", inventory.getId(), inventory.getName(), matchedItem.getId());
                } else {
                    // 匹配失败，设置为未知饰品
                    Item unknownItem = getOrCreateUnknownItem();
                    inventory.setItemId(unknownItem.getId());
                    inventory.setMarketPrice(BigDecimal.ZERO);
                    userInventoryMapper.updateById(inventory);
                    notFoundCount++;
                    log.warn("未匹配到物品：inventoryId={}, name={}, 设置为未知饰品 id={}", inventory.getId(), inventory.getName(), unknownItem.getId());
                }
            } catch (Exception e) {
                log.error("修复失败：inventoryId={}, error={}", inventory.getId(), e.getMessage());
            }
        }
        
        log.info("修复完成：总计={}, 成功={}, 未匹配={}", nullItemInventories.size(), fixedCount, notFoundCount);
    }
}

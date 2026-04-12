package com.cs2trade.service;

import com.cs2trade.entity.Favorite;
import com.cs2trade.entity.Item;
import com.cs2trade.entity.News;
import com.cs2trade.mapper.FavoriteMapper;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final NewsMapper newsMapper;

    public List<Favorite> getFavorites(Long userId, Integer type) {
        return favoriteMapper.getFavorites(userId, type);
    }

    public void addFavorite(Long userId, Long targetId, Integer type) {
        if (targetId == null) {
            throw new IllegalArgumentException("收藏目标不能为空");
        }

        if (type == null || (type != 1 && type != 2)) {
            throw new IllegalArgumentException("收藏类型不正确");
        }

        if (type == 1 && !isDisplayableItem(itemMapper.selectById(targetId))) {
            throw new IllegalArgumentException("饰品不存在，无法收藏");
        }

        if (type == 2 && !isDisplayableNews(newsMapper.selectById(targetId))) {
            throw new IllegalArgumentException("资讯不存在，无法收藏");
        }

        if (favoriteMapper.isFavorite(userId, targetId, type)) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setType(type);
        if (type == 1) {
            favorite.setItemId(targetId);
        } else if (type == 2) {
            favorite.setNewsId(targetId);
        }
        favoriteMapper.addFavorite(favorite);
    }

    public void removeFavorite(Long userId, Long targetId, Integer type) {
        favoriteMapper.removeFavorite(userId, targetId, type);
    }

    public boolean isFavorite(Long userId, Long targetId, Integer type) {
        return favoriteMapper.isFavorite(userId, targetId, type);
    }

    private boolean isDisplayableItem(Item item) {
        if (item == null || item.getId() == null) {
            return false;
        }

        if ("unknown".equalsIgnoreCase(item.getItemId())) {
            return false;
        }

        return hasText(item.getNameCn()) || hasText(item.getName()) || hasText(item.getIconUrl());
    }

    private boolean isDisplayableNews(News news) {
        return news != null && news.getId() != null && (hasText(news.getTitle()) || hasText(news.getSummary()) || hasText(news.getCoverImage()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

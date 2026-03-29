package com.cs2trade.service;

import com.cs2trade.entity.Favorite;
import com.cs2trade.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    public List<Favorite> getFavorites(Long userId, Integer type) {
        return favoriteMapper.getFavorites(userId, type);
    }

    public void addFavorite(Long userId, Long targetId, Integer type) {
        // Check if already exists
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
}

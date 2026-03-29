package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.Favorite;
import com.cs2trade.service.FavoriteService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final JwtUtils jwtUtils;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtils.getUserIdFromToken(token.substring(7));
        }
        throw new RuntimeException("未登录");
    }

    @GetMapping
    public Result<List<Favorite>> getFavorites(HttpServletRequest request, @RequestParam(required = false) Integer type) {
        Long userId = getCurrentUserId(request);
        return Result.success(favoriteService.getFavorites(userId, type));
    }

    @PostMapping
    public Result<Void> addFavorite(HttpServletRequest request, @RequestBody AddFavoriteRequest req) {
        Long userId = getCurrentUserId(request);
        favoriteService.addFavorite(userId, req.getTargetId(), req.getType());
        return Result.success(null);
    }

    @DeleteMapping("/{type}/{targetId}")
    public Result<Void> removeFavorite(HttpServletRequest request, @PathVariable Integer type, @PathVariable Long targetId) {
        Long userId = getCurrentUserId(request);
        favoriteService.removeFavorite(userId, targetId, type);
        return Result.success(null);
    }

    @GetMapping("/{type}/{targetId}/check")
    public Result<Boolean> checkFavorite(HttpServletRequest request, @PathVariable Integer type, @PathVariable Long targetId) {
        Long userId = getCurrentUserId(request);
        return Result.success(favoriteService.isFavorite(userId, targetId, type));
    }

    @lombok.Data
    public static class AddFavoriteRequest {
        private Long targetId;
        private Integer type; // 1: Item, 2: News
    }
}

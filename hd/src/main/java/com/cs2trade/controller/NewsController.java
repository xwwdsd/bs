package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.News;
import com.cs2trade.service.NewsService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;
    private final JwtUtils jwtUtils;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtils.getUserIdFromToken(token.substring(7));
        }
        return null;
    }

    @GetMapping
    public Result<List<News>> getNews(@RequestParam(required = false) String category) {
        return Result.success(newsService.getNews(category));
    }

    @GetMapping("/my")
    public Result<List<News>> getMyNews(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(newsService.getNewsByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<News> getNewsById(@PathVariable Long id) {
        return Result.success(newsService.getNewsById(id));
    }

    @PostMapping
    public Result<News> createNews(HttpServletRequest request, @RequestBody News news) {
        Long userId = getCurrentUserId(request);
        if (userId != null) {
            news.setUserId(userId);
        }
        return Result.success(newsService.createNews(news));
    }

    @PutMapping("/{id}")
    public Result<News> updateNews(HttpServletRequest request, @PathVariable Long id, @RequestBody News news) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        news.setId(id);
        News updated = newsService.updateNews(news, userId);
        if (updated == null) {
            return Result.error(403, "无权修改或文章不存在");
        }
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNews(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        boolean success = newsService.deleteNews(id, userId);
        if (success) {
            return Result.success(null);
        }
        return Result.error(403, "无权删除或文章不存在");
    }
}

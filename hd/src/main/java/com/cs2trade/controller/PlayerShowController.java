package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.PlayerShow;
import com.cs2trade.entity.PlayerShowComment;
import com.cs2trade.service.PlayerShowService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/player-shows")
@RequiredArgsConstructor
public class PlayerShowController {
    private final PlayerShowService playerShowService;
    private final JwtUtils jwtUtils;

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return jwtUtils.getUserIdFromToken(token.substring(7));
        }
        return null;
    }

    @GetMapping
    public Result<List<PlayerShow>> getAllShows() {
        return Result.success(playerShowService.getAllShows());
    }

    @GetMapping("/my")
    public Result<List<PlayerShow>> getMyShows(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(playerShowService.getMyShows(userId));
    }

    @PostMapping
    public Result<PlayerShow> createShow(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(playerShowService.createShow(userId, body.get("imageUrl"), body.get("description")));
    }

    @PostMapping("/{id}/like")
    public Result<Void> likeShow(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        boolean success = playerShowService.likeShow(id, userId);
        if (success) {
            return Result.success(null);
        }
        return Result.error(400, "已经点赞过了");
    }

    @GetMapping("/{id}/liked")
    public Result<Boolean> hasLiked(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.success(false);
        }
        return Result.success(playerShowService.hasLiked(id, userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteShow(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        boolean success = playerShowService.deleteShow(id, userId);
        if (success) {
            return Result.success(null);
        }
        return Result.error(403, "无权删除或不存在");
    }

    @GetMapping("/{id}/comments")
    public Result<List<PlayerShowComment>> getComments(@PathVariable Long id) {
        return Result.success(playerShowService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public Result<PlayerShowComment> addComment(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(playerShowService.addComment(id, userId, body.get("content")));
    }
}

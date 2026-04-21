package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.Banner;
import com.cs2trade.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    // 公开接口：获取启用的Banner
    @GetMapping("/banners")
    public Result<List<Banner>> getActiveBanners() {
        return Result.success(bannerService.getActiveBanners());
    }

    // 管理员接口：获取所有Banner
    @GetMapping("/admin/banners")
    public Result<List<Banner>> getAllBanners() {
        return Result.success(bannerService.getAllBanners());
    }

    // 管理员接口：创建Banner
    @PostMapping("/admin/banners")
    public Result<Banner> createBanner(@RequestBody Banner banner) {
        return Result.success(bannerService.createBanner(banner));
    }

    // 管理员接口：更新Banner
    @PutMapping("/admin/banners/{id}")
    public Result<Banner> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        banner.setId(id);
        return Result.success(bannerService.updateBanner(banner));
    }

    // 管理员接口：删除Banner
    @DeleteMapping("/admin/banners/{id}")
    public Result<Boolean> deleteBanner(@PathVariable Long id) {
        return Result.success(bannerService.deleteBanner(id));
    }

    // 管理员接口：更新Banner状态
    @PutMapping("/admin/banners/{id}/status")
    public Result<Boolean> updateBannerStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success(bannerService.updateBannerStatus(id, status));
    }

    @PutMapping("/admin/banners/{id}/sort")
    public Result<Boolean> updateBannerSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        return Result.success(bannerService.updateBannerSort(id, sortOrder));
    }
}

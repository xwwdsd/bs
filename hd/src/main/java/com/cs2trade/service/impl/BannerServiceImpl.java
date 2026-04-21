package com.cs2trade.service.impl;

import com.cs2trade.entity.Banner;
import com.cs2trade.mapper.BannerMapper;
import com.cs2trade.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;

    @Override
    public List<Banner> getAllBanners() {
        return bannerMapper.selectAll();
    }

    @Override
    public List<Banner> getActiveBanners() {
        return bannerMapper.selectActive();
    }

    @Override
    public Banner getBannerById(Long id) {
        return bannerMapper.selectById(id);
    }

    @Override
    public Banner createBanner(Banner banner) {
        if (banner.getStatus() == null) {
            banner.setStatus(1); // 默认启用
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    public Banner updateBanner(Banner banner) {
        bannerMapper.update(banner);
        return bannerMapper.selectById(banner.getId());
    }

    @Override
    public boolean deleteBanner(Long id) {
        return bannerMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateBannerStatus(Long id, Integer status) {
        Banner banner = bannerMapper.selectById(id);
        if (banner != null) {
            banner.setStatus(status);
            return bannerMapper.update(banner) > 0;
        }
        return false;
    }

    @Override
    public boolean updateBannerSort(Long id, Integer sortOrder) {
        Banner banner = bannerMapper.selectById(id);
        if (banner != null) {
            banner.setSortOrder(sortOrder);
            return bannerMapper.update(banner) > 0;
        }
        return false;
    }
}

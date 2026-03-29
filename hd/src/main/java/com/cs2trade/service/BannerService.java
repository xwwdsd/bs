package com.cs2trade.service;

import com.cs2trade.entity.Banner;
import java.util.List;

public interface BannerService {
    List<Banner> getAllBanners();
    List<Banner> getActiveBanners();
    Banner getBannerById(Long id);
    Banner createBanner(Banner banner);
    Banner updateBanner(Banner banner);
    boolean deleteBanner(Long id);
    boolean updateBannerStatus(Long id, Integer status);
}

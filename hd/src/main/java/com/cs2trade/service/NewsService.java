package com.cs2trade.service;

import com.cs2trade.entity.News;
import com.cs2trade.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsMapper newsMapper;

    public List<News> getNews(String category) {
        if (category == null || category.isEmpty() || "all".equals(category)) {
            return newsMapper.selectAll();
        }
        return newsMapper.selectByCategory(category);
    }

    public List<News> getNewsByUserId(Long userId) {
        return newsMapper.selectByUserId(userId);
    }

    public News getNewsById(Long id) {
        newsMapper.updateViews(id);
        return newsMapper.selectById(id);
    }

    public News createNews(News news) {
        if (news.getAuthor() == null || news.getAuthor().isEmpty()) {
            news.setAuthor("匿名用户");
        }
        if (news.getSource() == null || news.getSource().isEmpty()) {
            news.setSource("用户投稿");
        }
        if (news.getStatus() == null) {
            news.setStatus(0);
        }
        newsMapper.insert(news);
        return news;
    }

    public News updateNews(News news, Long userId) {
        News existing = newsMapper.selectById(news.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            return null;
        }
        news.setUserId(userId);
        newsMapper.update(news);
        return newsMapper.selectById(news.getId());
    }

    public boolean deleteNews(Long id, Long userId) {
        News news = newsMapper.selectById(id);
        if (news != null && news.getUserId().equals(userId)) {
            return newsMapper.deleteById(id) > 0;
        }
        return false;
    }
}

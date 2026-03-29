package com.cs2trade.service;

import com.cs2trade.entity.PlayerShow;
import com.cs2trade.entity.PlayerShowComment;
import com.cs2trade.entity.PlayerShowLike;
import com.cs2trade.mapper.PlayerShowMapper;
import com.cs2trade.mapper.PlayerShowCommentMapper;
import com.cs2trade.mapper.PlayerShowLikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerShowService {
    private final PlayerShowMapper playerShowMapper;
    private final PlayerShowCommentMapper commentMapper;
    private final PlayerShowLikeMapper likeMapper;

    public List<PlayerShow> getAllShows() {
        return playerShowMapper.selectAll();
    }

    public List<PlayerShow> getMyShows(Long userId) {
        return playerShowMapper.selectByUserId(userId);
    }

    @Transactional
    public PlayerShow createShow(Long userId, String imageUrl, String description) {
        PlayerShow show = new PlayerShow();
        show.setUserId(userId);
        show.setImageUrl(imageUrl);
        show.setDescription(description);
        playerShowMapper.insert(show);
        return playerShowMapper.selectById(show.getId());
    }

    @Transactional
    public boolean likeShow(Long showId, Long userId) {
        if (likeMapper.hasLiked(showId, userId)) {
            return false;
        }
        PlayerShowLike like = new PlayerShowLike();
        like.setShowId(showId);
        like.setUserId(userId);
        likeMapper.insert(like);
        playerShowMapper.incrementLikes(showId);
        return true;
    }

    public boolean hasLiked(Long showId, Long userId) {
        return likeMapper.hasLiked(showId, userId);
    }

    public List<PlayerShowComment> getComments(Long showId) {
        return commentMapper.selectByShowId(showId);
    }

    @Transactional
    public PlayerShowComment addComment(Long showId, Long userId, String content) {
        PlayerShowComment comment = new PlayerShowComment();
        comment.setShowId(showId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);
        return comment;
    }

    public boolean deleteShow(Long id, Long userId) {
        PlayerShow show = playerShowMapper.selectById(id);
        if (show != null && show.getUserId().equals(userId)) {
            return playerShowMapper.deleteById(id) > 0;
        }
        return false;
    }
}

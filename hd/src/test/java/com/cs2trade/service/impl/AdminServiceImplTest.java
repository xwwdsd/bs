package com.cs2trade.service.impl;

import com.cs2trade.entity.News;
import com.cs2trade.entity.Withdrawal;
import com.cs2trade.mapper.BannerMapper;
import com.cs2trade.mapper.BuyOrderMapper;
import com.cs2trade.mapper.FavoriteMapper;
import com.cs2trade.mapper.ItemMapper;
import com.cs2trade.mapper.MessageMapper;
import com.cs2trade.mapper.NewsMapper;
import com.cs2trade.mapper.PlayerShowCommentMapper;
import com.cs2trade.mapper.PlayerShowLikeMapper;
import com.cs2trade.mapper.PlayerShowMapper;
import com.cs2trade.mapper.SellOrderMapper;
import com.cs2trade.mapper.SteamSyncTaskMapper;
import com.cs2trade.mapper.TradeOrderMapper;
import com.cs2trade.mapper.UserInventoryMapper;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.mapper.WalletMapper;
import com.cs2trade.mapper.WalletTransactionMapper;
import com.cs2trade.mapper.WithdrawalMapper;
import com.cs2trade.service.MessagePublishService;
import com.cs2trade.service.TradeOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private TradeOrderService tradeOrderService;

    @Mock
    private BannerMapper bannerMapper;

    @Mock
    private NewsMapper newsMapper;

    @Mock
    private PlayerShowMapper playerShowMapper;

    @Mock
    private WithdrawalMapper withdrawalMapper;

    @Mock
    private SellOrderMapper sellOrderMapper;

    @Mock
    private UserInventoryMapper userInventoryMapper;

    @Mock
    private BuyOrderMapper buyOrderMapper;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessagePublishService messagePublishService;

    @Mock
    private PlayerShowCommentMapper playerShowCommentMapper;

    @Mock
    private PlayerShowLikeMapper playerShowLikeMapper;

    @Mock
    private SteamSyncTaskMapper steamSyncTaskMapper;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(
                userMapper,
                tradeOrderMapper,
                itemMapper,
                tradeOrderService,
                bannerMapper,
                newsMapper,
                playerShowMapper,
                withdrawalMapper,
                sellOrderMapper,
                userInventoryMapper,
                buyOrderMapper,
                walletMapper,
                walletTransactionMapper,
                favoriteMapper,
                messageMapper,
                messagePublishService,
                playerShowCommentMapper,
                playerShowLikeMapper,
                steamSyncTaskMapper
        );
    }

    @Test
    void auditNewsPublishesSystemMessageToAuthor() {
        News news = new News();
        news.setId(10L);
        news.setUserId(99L);
        news.setTitle("测试资讯");

        when(newsMapper.selectById(10L)).thenReturn(news);
        when(newsMapper.updateStatus(10L, 1, null)).thenReturn(1);

        boolean success = adminService.auditNews(10L, 1, null);

        assertTrue(success);
        verify(messagePublishService).sendSystemMessage(
                eq(99L),
                eq(3),
                eq("资讯审核已通过"),
                contains("测试资讯")
        );
    }

    @Test
    void auditWithdrawalPublishesSystemMessageToApplicant() {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setId(21L);
        withdrawal.setUserId(8L);
        withdrawal.setAmount(new BigDecimal("100.50"));

        when(withdrawalMapper.selectById(21L)).thenReturn(withdrawal);
        when(withdrawalMapper.updateStatus(21L, 1, null)).thenReturn(1);
        when(messagePublishService.formatMoneyWithSymbol(new BigDecimal("100.50"))).thenReturn("¥100.50");

        boolean success = adminService.auditWithdrawal(21L, 1, null);

        assertTrue(success);
        verify(messagePublishService).sendSystemMessage(
                eq(8L),
                eq(1),
                eq("提现申请已通过"),
                contains("¥100.50")
        );
    }
}

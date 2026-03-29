package com.cs2trade.service.impl;

import com.cs2trade.entity.Wallet;
import com.cs2trade.entity.WalletTransaction;
import com.cs2trade.mapper.WalletMapper;
import com.cs2trade.mapper.WalletTransactionMapper;
import com.cs2trade.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包服务实现类
 * 实现钱包相关的业务逻辑
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final WalletTransactionMapper transactionMapper;

    @Override
    public Wallet getWalletByUserId(Long userId) {
        Wallet wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            // 如果钱包不存在，自动创建
            wallet = createWallet(userId);
        }
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Wallet createWallet(Long userId) {
        log.info("创建用户钱包: userId={}", userId);

        // 检查是否已存在
        Wallet existingWallet = walletMapper.selectByUserId(userId);
        if (existingWallet != null) {
            return existingWallet;
        }

        // 创建新钱包
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozenAmount(BigDecimal.ZERO);
        wallet.setBuyOrderAmount(BigDecimal.ZERO);
        wallet.setFrozenBuyOrderAmount(BigDecimal.ZERO);
        wallet.setTotalRecharge(BigDecimal.ZERO);
        wallet.setTotalWithdraw(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());

        walletMapper.insert(wallet);

        log.info("用户钱包创建成功: userId={}", userId);
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("增加余额: userId={}, amount={}", userId, amount);

        // 确保钱包存在
        getWalletByUserId(userId);

        int result = walletMapper.addBalance(userId, amount);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("扣除余额: userId={}, amount={}", userId, amount);

        // 检查余额
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("余额不足: userId={}, balance={}, amount={}", userId, wallet.getBalance(), amount);
            return false;
        }

        int result = walletMapper.deductBalance(userId, amount);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeAmount(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("冻结金额: userId={}, amount={}", userId, amount);

        // 检查余额
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("余额不足，无法冻结: userId={}, balance={}, amount={}", userId, wallet.getBalance(), amount);
            return false;
        }

        int result = walletMapper.freezeAmount(userId, amount);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeAmount(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("解冻金额: userId={}, amount={}", userId, amount);

        int result = walletMapper.unfreezeAmount(userId, amount);
        return result > 0;
    }

    // ==================== 求购金相关方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addBuyOrderAmount(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("增加求购金: userId={}, amount={}", userId, amount);

        // 确保钱包存在
        getWalletByUserId(userId);

        int result = walletMapper.addBuyOrderAmount(userId, amount);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBuyOrderAmount(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("扣除求购金: userId={}, amount={}", userId, amount);

        // 检查求购金余额
        Wallet wallet = getWalletByUserId(userId);
        BigDecimal availableBuyOrderAmount = wallet.getBuyOrderAmount().subtract(wallet.getFrozenBuyOrderAmount());
        if (availableBuyOrderAmount.compareTo(amount) < 0) {
            log.warn("求购金不足: userId={}, available={}, amount={}", userId, availableBuyOrderAmount, amount);
            return false;
        }

        int result = walletMapper.deductBuyOrderAmount(userId, amount);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeBuyOrderAmount(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("冻结求购金: userId={}, amount={}", userId, amount);

        // 检查可用求购金
        Wallet wallet = getWalletByUserId(userId);
        BigDecimal availableBuyOrderAmount = wallet.getBuyOrderAmount().subtract(wallet.getFrozenBuyOrderAmount());
        if (availableBuyOrderAmount.compareTo(amount) < 0) {
            log.warn("求购金不足，无法冻结: userId={}, available={}, amount={}", userId, availableBuyOrderAmount, amount);
            return false;
        }

        int result = walletMapper.freezeBuyOrderAmount(userId, amount);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfreezeBuyOrderAmount(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        log.info("解冻求购金: userId={}, amount={}", userId, amount);

        int result = walletMapper.unfreezeBuyOrderAmount(userId, amount);
        return result > 0;
    }

    // ==================== 充值提现相关方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recharge(Long userId, BigDecimal amount, String channel) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        log.info("充值: userId={}, amount={}, channel={}", userId, amount, channel);

        // 确保钱包存在
        Wallet wallet = getWalletByUserId(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        // 增加余额
        boolean success = addBalance(userId, amount);
        if (!success) {
            throw new RuntimeException("充值失败");
        }

        // 增加累计充值
        walletMapper.addTotalRecharge(userId, amount);

        // 记录交易流水
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(WalletTransaction.TYPE_RECHARGE);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceBefore.add(amount));
        transaction.setDescription("通过" + getChannelName(channel) + "充值 " + amount + " 元");
        transaction.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(transaction);

        log.info("充值成功: userId={}, amount={}", userId, amount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdraw(Long userId, BigDecimal amount, String account, String channel) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("提现金额必须大于0");
        }

        log.info("提现: userId={}, amount={}, channel={}", userId, amount, channel);

        // 检查余额
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }
        BigDecimal balanceBefore = wallet.getBalance();

        // 扣除余额
        boolean success = deductBalance(userId, amount);
        if (!success) {
            throw new RuntimeException("提现失败");
        }

        // 增加累计提现
        walletMapper.addTotalWithdraw(userId, amount);

        // 记录交易流水
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(WalletTransaction.TYPE_WITHDRAW);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceBefore.subtract(amount));
        String accountInfo = account != null && !account.isEmpty() ? " (" + account + ")" : "";
        transaction.setDescription("提现 " + amount + " 元到" + getChannelName(channel) + accountInfo);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(transaction);

        log.info("提现成功: userId={}, amount={}", userId, amount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pay(Long userId, BigDecimal amount, String description, Long relatedOrderId, String relatedOrderType) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("支付金额必须大于0");
        }

        log.info("支付: userId={}, amount={}, description={}", userId, amount, description);

        // 获取当前余额（用于记录流水）
        Wallet wallet = getWalletByUserId(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        // 扣除余额
        boolean success = deductBalance(userId, amount);
        if (!success) {
            throw new RuntimeException("支付失败，余额不足");
        }

        // 记录交易流水
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(WalletTransaction.TYPE_EXPENSE);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceBefore.subtract(amount));
        transaction.setDescription(description);
        transaction.setRelatedOrderId(relatedOrderId);
        transaction.setRelatedOrderType(relatedOrderType);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(transaction);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean receive(Long userId, BigDecimal amount, String description, Long relatedOrderId, String relatedOrderType) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("收款金额必须大于0");
        }

        log.info("收款: userId={}, amount={}, description={}", userId, amount, description);

        // 获取当前余额
        Wallet wallet = getWalletByUserId(userId);
        BigDecimal balanceBefore = wallet.getBalance();

        // 增加余额
        boolean success = addBalance(userId, amount);
        if (!success) {
            throw new RuntimeException("收款失败");
        }

        // 记录交易流水
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(WalletTransaction.TYPE_INCOME);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceBefore.add(amount));
        transaction.setDescription(description);
        transaction.setRelatedOrderId(relatedOrderId);
        transaction.setRelatedOrderType(relatedOrderType);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionMapper.insert(transaction);

        log.info("收款成功: userId={}, amount={}", userId, amount);
        return true;
    }

    /**
     * 获取支付渠道名称
     */
    private String getChannelName(String channel) {
        if (channel == null) return "未知";
        switch (channel) {
            case "alipay": return "支付宝";
            case "wechat": return "微信支付";
            case "bank": return "银行卡";
            default: return channel;
        }
    }
}

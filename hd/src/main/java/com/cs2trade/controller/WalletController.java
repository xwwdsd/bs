package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import com.cs2trade.entity.Wallet;
import com.cs2trade.entity.WalletTransaction;
import com.cs2trade.mapper.WalletTransactionMapper;
import com.cs2trade.service.WalletService;
import com.cs2trade.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包控制器
 * 处理钱包相关的HTTP请求
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@Slf4j
@RestController
@RequestMapping("/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionMapper transactionMapper;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 获取我的钱包信息
     *
     * @return Result<Wallet> 钱包信息
     */
    @GetMapping("/my")
    public Result<Wallet> getMyWallet() {
        Long userId = getCurrentUserId();
        log.info("获取钱包信息: userId={}", userId);

        Wallet wallet = walletService.getWalletByUserId(userId);
        return Result.success(wallet);
    }

    /**
     * 充值
     *
     * @param request 充值请求
     * @return Result<Boolean> 充值结果
     */
    @PostMapping("/recharge")
    public Result<Boolean> recharge(@RequestBody RechargeRequest request) {
        Long userId = getCurrentUserId();
        log.info("充值: userId={}, amount={}, channel={}", userId, request.getAmount(), request.getChannel());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }

        if (request.getChannel() == null || request.getChannel().trim().isEmpty()) {
            return Result.error("请选择支付方式");
        }

        boolean success = walletService.recharge(userId, request.getAmount(), request.getChannel());
        return Result.success("充值成功", success);
    }

    /**
     * 提现
     *
     * @param request 提现请求
     * @return Result<Boolean> 提现结果
     */
    @PostMapping("/withdraw")
    public Result<Boolean> withdraw(@RequestBody WithdrawRequest request) {
        Long userId = getCurrentUserId();
        log.info("提现: userId={}, amount={}, channel={}", userId, request.getAmount(), request.getChannel());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("提现金额必须大于0");
        }

        if (request.getChannel() == null || request.getChannel().trim().isEmpty()) {
            request.setChannel("alipay");
        }

        boolean success = walletService.withdraw(userId, request.getAmount(), request.getAccount(), request.getChannel());
        return Result.success("提现申请已提交", success);
    }

    /**
     * 获取交易流水
     *
     * @param type 交易类型（可选）
     * @return Result<List<WalletTransaction>> 流水列表
     */
    @GetMapping("/transactions")
    public Result<List<WalletTransaction>> getTransactions(@RequestParam(required = false) Integer type) {
        Long userId = getCurrentUserId();
        log.info("获取交易流水: userId={}, type={}", userId, type);

        List<WalletTransaction> transactions;
        if (type != null) {
            transactions = transactionMapper.selectByUserIdAndType(userId, type);
        } else {
            transactions = transactionMapper.selectByUserId(userId);
        }
        return Result.success(transactions);
    }

    // ==================== 求购金相关接口 ====================

    /**
     * 充值求购金（从余额转入）
     *
     * @param request 充值请求
     * @return Result<Boolean> 结果
     */
    @PostMapping("/buyorder/recharge")
    public Result<Boolean> rechargeBuyOrder(@RequestBody RechargeRequest request) {
        Long userId = getCurrentUserId();
        log.info("充值求购金: userId={}, amount={}", userId, request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }

        // 从余额扣除
        boolean deductSuccess = walletService.deductBalance(userId, request.getAmount());
        if (!deductSuccess) {
            return Result.error("余额不足");
        }

        // 增加到求购金
        boolean addSuccess = walletService.addBuyOrderAmount(userId, request.getAmount());
        if (!addSuccess) {
            // 回滚余额
            walletService.addBalance(userId, request.getAmount());
            return Result.error("充值求购金失败");
        }

        return Result.success("求购金充值成功", true);
    }

    /**
     * 提取求购金（转回余额）
     *
     * @param request 提取请求
     * @return Result<Boolean> 结果
     */
    @PostMapping("/buyorder/withdraw")
    public Result<Boolean> withdrawBuyOrder(@RequestBody RechargeRequest request) {
        Long userId = getCurrentUserId();
        log.info("提取求购金: userId={}, amount={}", userId, request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("提取金额必须大于0");
        }

        // 从求购金扣除
        boolean deductSuccess = walletService.deductBuyOrderAmount(userId, request.getAmount());
        if (!deductSuccess) {
            return Result.error("求购金不足");
        }

        // 增加到余额
        boolean addSuccess = walletService.addBalance(userId, request.getAmount());
        if (!addSuccess) {
            // 回滚求购金
            walletService.addBuyOrderAmount(userId, request.getAmount());
            return Result.error("提取失败");
        }

        return Result.success("求购金提取成功", true);
    }

    // ==================== 请求类 ====================

    /**
     * 充值请求
     */
    @lombok.Data
    public static class RechargeRequest {
        private BigDecimal amount;
        private String channel;
    }

    /**
     * 提现请求
     */
    @lombok.Data
    public static class WithdrawRequest {
        private BigDecimal amount;
        private String account;
        private String channel;
    }
}

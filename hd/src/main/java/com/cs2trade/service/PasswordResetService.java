package com.cs2trade.service;

import com.cs2trade.dto.Result;

public interface PasswordResetService {

    Result<Void> sendResetEmail(String email);

    Result<Void> resetPassword(String token, String newPassword);

    Result<Boolean> validateToken(String token);
}

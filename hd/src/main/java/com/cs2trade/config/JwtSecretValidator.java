package com.cs2trade.config;

import com.cs2trade.utils.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtSecretValidator {

    private final JwtUtils jwtUtils;

    @PostConstruct
    public void validate() {
        jwtUtils.validateConfiguredSecret();
    }
}

package com.cs2trade.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "CS2饰品交易平台 API");
        result.put("version", "1.0.0");
        result.put("status", "running");
        result.put("message", "欢迎使用CS2饰品交易平台API服务");
        return result;
    }
}

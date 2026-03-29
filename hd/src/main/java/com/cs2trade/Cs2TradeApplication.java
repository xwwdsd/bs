package com.cs2trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CS2饰品交易平台 - Spring Boot启动类
 * 
 * @SpringBootApplication 组合注解，包含：
 * - @Configuration: 标记为配置类
 * - @EnableAutoConfiguration: 启用自动配置
 * - @ComponentScan: 组件扫描，自动发现并注册Spring Bean
 * 
 * @MapperScan 指定MyBatis Mapper接口的扫描路径
 * 
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-02
 */
@SpringBootApplication
@MapperScan("com.cs2trade.mapper")
public class Cs2TradeApplication {

    /**
     * 应用程序入口方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Cs2TradeApplication.class, args);
        System.out.println("==================================================");
        System.out.println("  CS2饰品交易平台后端服务启动成功！");
        System.out.println("  API地址: http://localhost:8080/api");
        System.out.println("==================================================");
    }
}

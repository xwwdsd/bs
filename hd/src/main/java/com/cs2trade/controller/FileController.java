package com.cs2trade.controller;

import com.cs2trade.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 * @author CS2Trade Team
 * @version 1.0
 * @since 2024-03-04
 */
@Slf4j
@RestController
@RequestMapping("/v1/file")
public class FileController {

    @Value("${app.upload.path:./uploads/}")
    private String uploadPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            // 确保上传目录存在
            File directory = new File(uploadPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 获取原始文件名和后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成新文件名
            String fileName = UUID.randomUUID().toString() + suffix;
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, fileName);
            Files.write(filePath, file.getBytes());

            // 构建访问URL
            // 返回带ContextPath的路径，方便前端直接访问
            // 例如: /api/uploads/filename.ext
            String fileUrl = contextPath + "/uploads/" + fileName;
            // 移除可能重复的斜杠
            fileUrl = fileUrl.replace("//", "/");
            
            log.info("文件上传成功: {}", filePath);
            return Result.success("上传成功", fileUrl);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}

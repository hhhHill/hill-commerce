package com.hillcommerce.modules.oss.web;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hillcommerce.modules.oss.dto.OssUploadResult;
import com.hillcommerce.modules.oss.service.OssService;

@RestController
public class OssController {

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    @PostMapping("/api/admin/oss/upload")
    public OssUploadResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category must not be blank");
        }
        return ossService.upload(file.getInputStream(), file.getOriginalFilename(), category);
    }
}

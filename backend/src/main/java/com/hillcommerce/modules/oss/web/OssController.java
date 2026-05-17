package com.hillcommerce.modules.oss.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.oss.dto.OssStsToken;
import com.hillcommerce.modules.oss.service.OssService;

@RestController
public class OssController {

    private final OssService ossService;

    public OssController(OssService ossService) {
        this.ossService = ossService;
    }

    @GetMapping("/api/admin/oss/sts")
    public OssStsToken getStsToken() {
        return ossService.generateStsToken();
    }
}

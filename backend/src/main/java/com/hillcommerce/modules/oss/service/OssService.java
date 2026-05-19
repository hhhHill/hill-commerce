package com.hillcommerce.modules.oss.service;

import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.ObjectMetadata;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssUploadResult;

@Service
public class OssService {

    private static final Logger log = LoggerFactory.getLogger(OssService.class);

    private final OssProperties properties;
    private final OSS ossClient;
    private volatile OSS lazyOssClient;

    @Autowired
    public OssService(OssProperties properties) {
        this.properties = properties;
        this.ossClient = null;
    }

    public OssService(OssProperties properties, OSS ossClient) {
        this.properties = properties;
        this.ossClient = ossClient;
    }

    public OssUploadResult upload(InputStream inputStream, String fileName, String category) {
        requireConfigured();

        String objectKey = buildObjectKey(category, fileName);
        ObjectMetadata metadata = new ObjectMetadata();
        String contentType = detectContentType(fileName);
        if (contentType != null) {
            metadata.setContentType(contentType);
        }

        try {
            ossClient().putObject(properties.getBucket(), objectKey, inputStream, metadata);
            String url = buildUrl(objectKey);
            log.info("Uploaded to OSS: {}", objectKey);
            return new OssUploadResult(url, objectKey);
        } catch (Exception e) {
            log.error("Failed to upload to OSS: {}", objectKey, e);
            throw new BusinessException(ErrorCode.UPLOAD_FAILED, "Failed to upload file: " + e.getMessage());
        }
    }

    private String buildObjectKey(String category, String fileName) {
        String baseDir = normalizedBaseDir();
        String safeCategory = sanitize(category);
        String safeFileName = sanitize(fileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomSuffix = Integer.toHexString(ThreadLocalRandom.current().nextInt(0x100000, 0xffffff));
        return baseDir + safeCategory + "/" + timestamp + "_" + randomSuffix + "_" + safeFileName;
    }

    private String buildUrl(String objectKey) {
        String host = properties.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + properties.getBucket() + "." + host + "/" + objectKey;
    }

    private String normalizedBaseDir() {
        String dir = properties.getBaseDir();
        if (dir == null || dir.isBlank()) {
            return "uploads/";
        }
        return dir.endsWith("/") ? dir : dir + "/";
    }

    private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String detectContentType(String fileName) {
        if (fileName == null) {
            return null;
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return null;
    }

    private void requireConfigured() {
        if (isBlank(properties.getEndpoint())
                || isBlank(properties.getBucket())
                || isBlank(properties.getRegion())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getAccessKeySecret())) {
            throw new BusinessException(ErrorCode.OSS_NOT_CONFIGURED,
                "OSS not configured: missing endpoint, bucket, region, access key id, or access key secret");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private OSS ossClient() {
        if (ossClient != null) {
            return ossClient;
        }
        OSS local = lazyOssClient;
        if (local == null) {
            synchronized (this) {
                local = lazyOssClient;
                if (local == null) {
                    local = OSSClientBuilder.create()
                        .endpoint(properties.getEndpoint())
                        .credentialsProvider(new DefaultCredentialProvider(
                            properties.getAccessKeyId(),
                            properties.getAccessKeySecret()))
                        .region(properties.getRegion())
                        .build();
                    lazyOssClient = local;
                }
            }
        }
        return local;
    }
}

package com.hillcommerce.modules.oss.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssStsToken;

@Service
public class OssService {

    private static final Logger log = LoggerFactory.getLogger(OssService.class);
    private static final long TOKEN_DURATION_SECONDS = 3600L;
    private static final String ROLE_SESSION_NAME = "hill-commerce-upload";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OssProperties properties;
    private final StsClient stsClient;

    @Autowired
    public OssService(OssProperties properties) {
        this(properties, new AliyunStsClient(properties));
    }

    public OssService(OssProperties properties, StsClient stsClient) {
        this.properties = properties;
        this.stsClient = stsClient;
    }

    public OssStsToken generateStsToken() {
        requireConfigured();

        AssumeRoleRequest request = new AssumeRoleRequest();
        request.setSysMethod(MethodType.POST);
        request.setRoleArn(properties.getRoleArn());
        request.setRoleSessionName(ROLE_SESSION_NAME);
        request.setDurationSeconds(TOKEN_DURATION_SECONDS);
        request.setPolicy(buildUploadPolicy());

        try {
            AssumeRoleResponse response = stsClient.assumeRole(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();
            if (credentials == null) {
                throw new IllegalStateException("STS response missing credentials");
            }
            return new OssStsToken(
                credentials.getAccessKeyId(),
                credentials.getAccessKeySecret(),
                credentials.getSecurityToken(),
                properties.getOssRegion(),
                properties.getBucket(),
                properties.getEndpoint(),
                normalizedUploadDir());
        } catch (Exception e) {
            log.error("Failed to generate STS token", e);
            throw new IllegalStateException("Failed to generate STS token: " + e.getMessage(), e);
        }
    }

    private void requireConfigured() {
        if (isBlank(properties.getEndpoint())
                || isBlank(properties.getBucket())
                || isBlank(properties.getOssRegion())
                || isBlank(properties.getStsRegion())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getAccessKeySecret())
                || isBlank(properties.getRoleArn())) {
            throw new IllegalStateException("OSS not configured: missing endpoint, bucket, region, access key, or roleArn");
        }
    }

    private String buildUploadPolicy() {
        String resource = "acs:oss:*:*:" + properties.getBucket() + "/" + normalizedUploadDir() + "*";
        Map<String, Object> policy = Map.of(
            "Version", "1",
            "Statement", List.of(Map.of(
                "Effect", "Allow",
                "Action", List.of("oss:PutObject"),
                "Resource", List.of(resource)
            ))
        );
        try {
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build STS policy JSON", e);
        }
    }

    private String normalizedUploadDir() {
        String uploadDir = properties.getUploadDir();
        if (isBlank(uploadDir)) {
            return "products/";
        }
        return uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public interface StsClient {
        AssumeRoleResponse assumeRole(AssumeRoleRequest request) throws Exception;
    }

    private static final class AliyunStsClient implements StsClient {
        private final OssProperties properties;

        private AliyunStsClient(OssProperties properties) {
            this.properties = properties;
        }

        @Override
        public AssumeRoleResponse assumeRole(AssumeRoleRequest request) throws Exception {
            DefaultProfile profile = DefaultProfile.getProfile(
                properties.getStsRegion(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret());
            return new DefaultAcsClient(profile).getAcsResponse(request);
        }
    }
}

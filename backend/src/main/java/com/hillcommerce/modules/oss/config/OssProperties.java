package com.hillcommerce.modules.oss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oss")
public class OssProperties {

    private String endpoint;
    private String bucket;
    private String ossRegion;
    private String stsRegion;
    private String accessKeyId;
    private String accessKeySecret;
    private String roleArn;
    private String uploadDir = "products/";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getOssRegion() {
        return ossRegion;
    }

    public void setOssRegion(String ossRegion) {
        this.ossRegion = ossRegion;
    }

    public String getStsRegion() {
        return stsRegion;
    }

    public void setStsRegion(String stsRegion) {
        this.stsRegion = stsRegion;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}

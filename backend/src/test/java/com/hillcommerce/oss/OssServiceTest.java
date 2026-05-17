package com.hillcommerce.oss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssStsToken;
import com.hillcommerce.modules.oss.service.OssService;

class OssServiceTest {

    @Test
    void generateStsTokenRequiresConfiguration() {
        OssService service = new OssService(new OssProperties());

        assertThatThrownBy(service::generateStsToken)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OSS not configured");
    }

    @Test
    void generateStsTokenConstrainsUploadPolicyAndMapsCredentials() {
        OssProperties properties = configuredProperties();
        CapturingStsClient stsClient = new CapturingStsClient();
        OssService service = new OssService(properties, stsClient);

        OssStsToken token = service.generateStsToken();

        assertThat(stsClient.request.getRoleArn()).isEqualTo("acs:ram::123:role/test-role");
        assertThat(stsClient.request.getRoleSessionName()).isEqualTo("hill-commerce-upload");
        assertThat(stsClient.request.getDurationSeconds()).isEqualTo(3600L);
        assertThat(stsClient.request.getPolicy()).contains("\"Action\":[\"oss:PutObject\"]");
        assertThat(stsClient.request.getPolicy()).contains("\"Resource\":[\"acs:oss:*:*:test-bucket/products/*\"]");
        assertThat(token.accessKey()).isEqualTo("STS.ak");
        assertThat(token.secretKey()).isEqualTo("STS.sk");
        assertThat(token.securityToken()).isEqualTo("STS.token");
        assertThat(token.ossRegion()).isEqualTo("oss-cn-hangzhou");
        assertThat(token.bucket()).isEqualTo("test-bucket");
        assertThat(token.endpoint()).isEqualTo("oss-cn-hangzhou.aliyuncs.com");
        assertThat(token.uploadDir()).isEqualTo("products/");
    }

    private OssProperties configuredProperties() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        properties.setBucket("test-bucket");
        properties.setOssRegion("oss-cn-hangzhou");
        properties.setStsRegion("cn-hangzhou");
        properties.setAccessKeyId("test-ak");
        properties.setAccessKeySecret("test-sk");
        properties.setRoleArn("acs:ram::123:role/test-role");
        properties.setUploadDir("products/");
        return properties;
    }

    private static final class CapturingStsClient implements OssService.StsClient {
        private AssumeRoleRequest request;

        @Override
        public AssumeRoleResponse assumeRole(AssumeRoleRequest request) {
            this.request = request;
            AssumeRoleResponse response = new AssumeRoleResponse();
            AssumeRoleResponse.Credentials credentials = new AssumeRoleResponse.Credentials();
            credentials.setAccessKeyId("STS.ak");
            credentials.setAccessKeySecret("STS.sk");
            credentials.setSecurityToken("STS.token");
            response.setCredentials(credentials);
            return response;
        }
    }
}

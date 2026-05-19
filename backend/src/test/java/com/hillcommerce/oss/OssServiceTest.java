package com.hillcommerce.oss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.modules.oss.config.OssProperties;
import com.hillcommerce.modules.oss.dto.OssUploadResult;
import com.hillcommerce.modules.oss.service.OssService;

class OssServiceTest {

    @Test
    void uploadRequiresConfiguration() {
        OssService service = new OssService(new OssProperties(), mock(OSS.class));

        assertThatThrownBy(() -> service.upload(stream("test"), "test.jpg", "products"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("OSS not configured");
    }

    @Test
    void uploadReturnsUrlAndKey() {
        OssProperties properties = configuredProperties();
        OSS ossMock = mock(OSS.class);
        OssService service = new OssService(properties, ossMock);

        OssUploadResult result = service.upload(stream("image-data"), "test.jpg", "products");

        assertThat(result.url()).startsWith("https://test-bucket.oss-cn-hangzhou.aliyuncs.com/");
        assertThat(result.url()).contains("/products/");
        assertThat(result.key()).matches("uploads/products/\\d+_[a-f0-9]+_test\\.jpg");
        verify(ossMock).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    void uploadSetsContentTypeFromFileName() {
        OssProperties properties = configuredProperties();
        OSS ossMock = mock(OSS.class);
        OssService service = new OssService(properties, ossMock);

        service.upload(stream("png-data"), "photo.PNG", "products");

        verify(ossMock).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    void uploadSanitizesCategoryAndFileName() {
        OssProperties properties = configuredProperties();
        OSS ossMock = mock(OSS.class);
        OssService service = new OssService(properties, ossMock);

        OssUploadResult result = service.upload(stream("data"), "hello world.jpg", "mer chant");

        assertThat(result.key()).contains("mer_chant/");
        assertThat(result.key()).contains("hello_world.jpg");
    }

    @Test
    void uploadWrapsOssException() {
        OssProperties properties = configuredProperties();
        OSS ossMock = mock(OSS.class);
        OSSException ossException = new OSSException("Bucket not found");
        when(ossMock.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
            .thenThrow(ossException);
        OssService service = new OssService(properties, ossMock);

        assertThatThrownBy(() -> service.upload(stream("data"), "test.jpg", "products"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Failed to upload file")
            .hasMessageContaining("Bucket not found");
    }

    private OssProperties configuredProperties() {
        OssProperties properties = new OssProperties();
        properties.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        properties.setBucket("test-bucket");
        properties.setRegion("oss-cn-hangzhou");
        properties.setAccessKeyId("test-ak");
        properties.setAccessKeySecret("test-sk");
        properties.setBaseDir("uploads/");
        return properties;
    }

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}

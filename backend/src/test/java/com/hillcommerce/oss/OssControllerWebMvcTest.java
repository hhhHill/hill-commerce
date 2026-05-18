package com.hillcommerce.oss;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hillcommerce.framework.web.ApiExceptionHandler;
import com.hillcommerce.modules.oss.dto.OssUploadResult;
import com.hillcommerce.modules.oss.service.OssService;
import com.hillcommerce.modules.oss.web.OssController;

class OssControllerWebMvcTest {

    private MockMvc mockMvc;
    private OssService ossService;

    @BeforeEach
    void setUp() {
        ossService = Mockito.mock(OssService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OssController(ossService))
            .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUploadResultJson() throws Exception {
        when(ossService.upload(any(InputStream.class), eq("test.jpg"), eq("products")))
            .thenReturn(new OssUploadResult("https://example.com/img.jpg", "uploads/products/123_test.jpg"));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/admin/oss/upload")
                .file(file)
                .param("category", "products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("https://example.com/img.jpg"))
            .andExpect(jsonPath("$.key").value("uploads/products/123_test.jpg"));
    }

    @Test
    void returns400WhenCategoryBlank() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/admin/oss/upload")
                .file(file)
                .param("category", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenFileEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/admin/oss/upload")
                .file(file)
                .param("category", "products"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void securityConfigAllowsAdminAndSalesForAdminApis() throws Exception {
        String securityConfig = Files.readString(Path.of("src/main/java/com/hillcommerce/framework/security/SecurityConfig.java"));

        org.assertj.core.api.Assertions.assertThat(securityConfig)
            .contains(".requestMatchers(\"/api/admin/**\").hasAnyRole(\"ADMIN\", \"SALES\")");
    }

    @Test
    void securityConfigRequiresAuthenticationByDefault() throws Exception {
        String securityConfig = Files.readString(Path.of("src/main/java/com/hillcommerce/framework/security/SecurityConfig.java"));

        org.assertj.core.api.Assertions.assertThat(securityConfig)
            .contains(".anyRequest().authenticated()");
    }

    @Test
    void securityConfigReturns401ForUnauthenticatedAdminApi() throws Exception {
        String securityConfig = Files.readString(Path.of("src/main/java/com/hillcommerce/framework/security/SecurityConfig.java"));

        org.assertj.core.api.Assertions.assertThat(securityConfig)
            .contains("writeJson(response, 401, ");
        org.assertj.core.api.Assertions.assertThat(securityConfig)
            .contains(".requestMatchers(\"/api/admin/**\").hasAnyRole(\"ADMIN\", \"SALES\")")
            .doesNotContain(".requestMatchers(\"/api/admin/**\").permitAll()");
    }
}

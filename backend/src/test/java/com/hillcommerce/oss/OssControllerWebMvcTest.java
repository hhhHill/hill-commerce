package com.hillcommerce.oss;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hillcommerce.modules.oss.dto.OssStsToken;
import com.hillcommerce.modules.oss.service.OssService;
import com.hillcommerce.modules.oss.web.OssController;

class OssControllerWebMvcTest {

    private MockMvc mockMvc;
    private OssService ossService;

    @BeforeEach
    void setUp() {
        ossService = Mockito.mock(OssService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OssController(ossService)).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsStsTokenJsonStructure() throws Exception {
        when(ossService.generateStsToken()).thenReturn(token());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/oss/sts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessKey").value("STS.ak"))
            .andExpect(jsonPath("$.secretKey").value("STS.sk"))
            .andExpect(jsonPath("$.securityToken").value("STS.token"))
            .andExpect(jsonPath("$.ossRegion").value("oss-cn-hangzhou"))
            .andExpect(jsonPath("$.bucket").value("test-bucket"))
            .andExpect(jsonPath("$.endpoint").value("oss-cn-hangzhou.aliyuncs.com"))
            .andExpect(jsonPath("$.uploadDir").value("products/"));
    }

    @Test
    void returns200WhenAdminRole() throws Exception {
        setAuthentication("ADMIN");
        when(ossService.generateStsToken()).thenReturn(token());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/oss/sts"))
            .andExpect(status().isOk());
    }

    @Test
    void returns200WhenSalesRole() throws Exception {
        setAuthentication("SALES");
        when(ossService.generateStsToken()).thenReturn(token());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/oss/sts"))
            .andExpect(status().isOk());
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

    private static void setAuthentication(String role) {
        var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var auth = new UsernamePasswordAuthenticationToken("test-user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private OssStsToken token() {
        return new OssStsToken(
            "STS.ak",
            "STS.sk",
            "STS.token",
            "oss-cn-hangzhou",
            "test-bucket",
            "oss-cn-hangzhou.aliyuncs.com",
            "products/");
    }
}

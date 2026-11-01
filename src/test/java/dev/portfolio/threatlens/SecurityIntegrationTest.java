package dev.portfolio.threatlens;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(IndicatorController.class)
@Import(SecurityConfiguration.class)
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndicatorService service;

    @Test
    void viewerCanReadButCannotCreateIndicators() throws Exception {
        when(service.indicators()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/indicators").with(httpBasic("viewer", "local-viewer-change-me")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/indicators").with(httpBasic("viewer", "local-viewer-change-me"))
                        .contentType("application/json")
                        .content("{\"type\":\"DOMAIN\",\"value\":\"new.example\",\"source\":\"test\",\"confidence\":50}"))
                .andExpect(status().isForbidden());
    }
}
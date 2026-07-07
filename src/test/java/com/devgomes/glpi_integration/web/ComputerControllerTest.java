package com.devgomes.glpi_integration.web;

import com.devgomes.glpi_integration.service.ComputerWarrantyReportService;
import com.devgomes.glpi_integration.service.GlpiIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComputerControllerTest {

    @Mock
    private GlpiIntegrationService glpiIntegrationService;

    @Mock
    private ComputerWarrantyReportService computerWarrantyReportService;

    @InjectMocks
    private ComputerController computerController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(computerController)
                .setControllerAdvice(new GlpiApiExceptionHandler())
                .build();
    }

    @Test
    void putComputer_returnsOk() throws Exception {
        mockMvc.perform(put("/api/computers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"users_id":42,"serial":"SN-TEST-PUT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.computerId").value(10))
                .andExpect(jsonPath("$.status").value("updated"));

        verify(glpiIntegrationService).updateComputer(eq(10), any());
    }
}

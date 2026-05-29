package com.devgomes.glpi_integration.client;

import com.devgomes.glpi_integration.config.GlpiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

class GlpiApiClientTest {

    private GlpiProperties properties;
    private MockRestServiceServer mockServer;
    private GlpiApiClient client;

    @BeforeEach
    void setUp() {
        properties = new GlpiProperties();
        properties.setBaseUrl("http://glpi.test/apirest.php");
        properties.setAppToken("app-token-test");
        properties.setUserToken("user-token-test");

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("http://glpi.test/apirest.php/");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new GlpiApiClient(restClientBuilder.build(), properties);
    }

    @Test
    void initSession_returnsSessionToken() {
        mockServer.expect(requestTo("http://glpi.test/apirest.php/initSession/"))
                .andExpect(method(GET))
                .andExpect(header("App-Token", "app-token-test"))
                .andExpect(header("Authorization", "user_token user-token-test"))
                .andRespond(withSuccess("{\"session_token\":\"abc123token\"}", MediaType.APPLICATION_JSON));

        String token = client.initSession();

        assertThat(token).isEqualTo("abc123token");
        mockServer.verify();
    }

    @Test
    void listComputers_returnsJsonArray() {
        mockServer.expect(requestTo("http://glpi.test/apirest.php/Computer/?range=0-2"))
                .andExpect(method(GET))
                .andExpect(header("Session-Token", "sess-xyz"))
                .andRespond(withSuccess("[{\"id\":1,\"name\":\"pc-1\"}]", MediaType.APPLICATION_JSON)
                        .headers(org.springframework.http.HttpHeaders.EMPTY));

        ResponseEntity<String> response = client.listComputers("sess-xyz", "0-2", false);

        assertThat(response.getBody()).contains("\"id\":1");
        mockServer.verify();
    }

    @Test
    void updateComputer_sendsInputPayload() {
        mockServer.expect(requestTo("http://glpi.test/apirest.php/Computer/10?session_write=true"))
                .andExpect(method(PUT))
                .andExpect(header("Session-Token", "sess-xyz"))
                .andExpect(header("App-Token", "app-token-test"))
                .andRespond(withSuccess("[{\"10\":true}]", MediaType.APPLICATION_JSON));

        client.updateComputer("sess-xyz", 10, Map.of("users_id", 5));

        mockServer.verify();
    }
}

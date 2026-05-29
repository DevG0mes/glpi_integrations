package com.devgomes.glpi_integration.client;

import com.devgomes.glpi_integration.config.GlpiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GlpiApiClient {

    private static final Pattern SESSION_TOKEN_JSON =
            Pattern.compile("\"session_token\"\\s*:\\s*\"([^\"]+)\"");

    private final RestClient restClient;
    private final GlpiProperties properties;

    public GlpiApiClient(RestClient glpiRestClient, GlpiProperties properties) {
        this.restClient = glpiRestClient;
        this.properties = properties;
    }

    public String initSession() {
        String body = restClient.get()
                .uri("initSession/")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    applyAppToken(headers);
                    headers.set("Authorization", "user_token " + properties.getUserToken());
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw errorFromResponse(response.getStatusCode(), readBody(response));
                })
                .body(String.class);

        return parseSessionToken(body);
    }

    public void killSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        restClient.get()
                .uri("killSession/")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    applyAppToken(headers);
                    headers.set("Session-Token", sessionToken);
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw errorFromResponse(response.getStatusCode(), readBody(response));
                })
                .toBodilessEntity();
    }

    public ResponseEntity<String> listComputers(String sessionToken, String range, boolean expandDropdowns) {
        return listItems(sessionToken, "Computer", range, expandDropdowns);
    }

    public ResponseEntity<String> listComputerModels(String sessionToken, String range) {
        return listItems(sessionToken, "ComputerModel", range, false);
    }

    public ResponseEntity<String> listItems(
            String sessionToken,
            String itemType,
            String range,
            boolean expandDropdowns) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(itemType + "/");
                    if (range != null && !range.isBlank()) {
                        builder.queryParam("range", range);
                    }
                    if (expandDropdowns) {
                        builder.queryParam("expand_dropdowns", "true");
                    }
                    return builder.build();
                })
                .headers(headers -> applySessionHeaders(headers, sessionToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw errorFromResponse(response.getStatusCode(), readBody(response));
                })
                .toEntity(String.class);
    }

    public String getComputer(String sessionToken, int computerId, boolean expandDropdowns) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("Computer/{id}");
                    if (expandDropdowns) {
                        builder.queryParam("expand_dropdowns", "true");
                    }
                    return builder.build(computerId);
                })
                .headers(headers -> applySessionHeaders(headers, sessionToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw errorFromResponse(response.getStatusCode(), readBody(response));
                })
                .body(String.class);
    }

    public void updateComputer(String sessionToken, int computerId, Map<String, Object> inputFields) {
        Map<String, Object> payload = Map.of("input", inputFields);

        restClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("Computer/{id}")
                        .queryParam("session_write", "true")
                        .build(computerId))
                .headers(headers -> applySessionHeaders(headers, sessionToken))
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw errorFromResponse(response.getStatusCode(), readBody(response));
                })
                .toBodilessEntity();
    }

    private void applySessionHeaders(HttpHeaders headers, String sessionToken) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyAppToken(headers);
        headers.set("Session-Token", sessionToken);
    }

    private void applyAppToken(HttpHeaders headers) {
        if (properties.getAppToken() != null && !properties.getAppToken().isBlank()) {
            headers.set("App-Token", properties.getAppToken());
        }
    }

    private String parseSessionToken(String body) {
        if (body == null || body.isBlank()) {
            throw new GlpiApiException(HttpStatus.BAD_GATEWAY, "Resposta vazia do initSession");
        }
        Matcher matcher = SESSION_TOKEN_JSON.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        throw new GlpiApiException(HttpStatus.BAD_GATEWAY, "Formato de session_token não reconhecido: " + body);
    }

    private static String readBody(ClientHttpResponse response) {
        try {
            if (response.getBody() != null) {
                return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // corpo indisponível
        }
        return "";
    }

    private GlpiApiException errorFromResponse(HttpStatusCode status, String body) {
        String message = body != null && !body.isBlank() ? body : status.toString();
        return new GlpiApiException(status, message);
    }
}

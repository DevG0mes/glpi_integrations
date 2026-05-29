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
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
                .uri(uriBuilder -> buildItemTypeUri(uriBuilder, itemType, range, expandDropdowns))
                .headers(headers -> applySessionHeaders(headers, sessionToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw errorFromResponse(response.getStatusCode(), readBody(response));
                })
                .toEntity(String.class);
    }

    public String getComputer(String sessionToken, int computerId, boolean expandDropdowns) {
        return getItem(sessionToken, "Computer", computerId, expandDropdowns);
    }

    public void updateComputer(String sessionToken, int computerId, Map<String, Object> inputFields) {
        updateItem(sessionToken, "Computer", computerId, inputFields);
    }

    public String getItem(String sessionToken, String itemType, int itemId, boolean expandDropdowns) {
        return restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = appendItemTypeSegments(uriBuilder, itemType)
                            .pathSegment(String.valueOf(itemId));
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
                .body(String.class);
    }

    public void updateItem(String sessionToken, String itemType, int itemId, Map<String, Object> inputFields) {
        Map<String, Object> payload = Map.of("input", enrichInput(itemId, inputFields));

        restClient.put()
                .uri(uriBuilder -> appendItemTypeSegments(uriBuilder, itemType)
                        .pathSegment(String.valueOf(itemId))
                        .queryParam("session_write", "true")
                        .build())
                .headers(headers -> applySessionHeaders(headers, sessionToken))
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String body = readBody(response);
                    throw errorFromResponse(
                            response.getStatusCode(),
                            "PUT " + itemType + "/" + itemId + " campos=" + inputFields.keySet() + " → " + body);
                })
                .toBodilessEntity();
    }

    public int createItem(String sessionToken, String itemType, Map<String, Object> inputFields) {
        Map<String, Object> payload = Map.of("input", inputFields);

        String body = restClient.post()
                .uri(uriBuilder -> appendItemTypeSegments(uriBuilder, itemType)
                        .queryParam("session_write", "true")
                        .build())
                .headers(headers -> applySessionHeaders(headers, sessionToken))
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw errorFromResponse(response.getStatusCode(), readBody(response));
                })
                .body(String.class);

        return parseCreatedId(body);
    }

    private static java.net.URI buildItemTypeUri(
            UriBuilder uriBuilder,
            String itemType,
            String range,
            boolean expandDropdowns) {
        UriBuilder builder = appendItemTypeSegments(uriBuilder, itemType);
        if (range != null && !range.isBlank()) {
            builder.queryParam("range", range);
        }
        if (expandDropdowns) {
            builder.queryParam("expand_dropdowns", "true");
        }
        return builder.build();
    }

    static Map<String, Object> enrichInput(int itemId, Map<String, Object> inputFields) {
        Map<String, Object> input = new LinkedHashMap<>(inputFields);
        input.put("id", itemId);
        return input;
    }

    private static UriBuilder appendItemTypeSegments(UriBuilder uriBuilder, String itemType) {
        UriBuilder builder = uriBuilder;
        for (String segment : GlpiItemTypePath.toPathSegments(itemType)) {
            builder = builder.pathSegment(segment);
        }
        return builder;
    }

    private int parseCreatedId(String body) {
        if (body == null || body.isBlank()) {
            throw new GlpiApiException(HttpStatus.BAD_GATEWAY, "Resposta vazia ao criar item");
        }
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new GlpiApiException(HttpStatus.BAD_GATEWAY, "ID não encontrado na resposta de criação: " + body);
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
        String raw = body != null && !body.isBlank() ? body : status.toString();
        String parsed = GlpiApiErrorParser.humanMessage(raw);
        String message = parsed.isBlank() ? raw : parsed;
        return new GlpiApiException(status, message);
    }
}

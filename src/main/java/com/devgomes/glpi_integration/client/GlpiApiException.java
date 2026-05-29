package com.devgomes.glpi_integration.client;

import org.springframework.http.HttpStatusCode;

public class GlpiApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public GlpiApiException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}

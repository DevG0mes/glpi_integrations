package com.devgomes.glpi_integration.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Encaminha a raiz da aplicação para a interface web estática em {@code /app/index.html}.
 */
@Controller
public class WebUiController {

    @GetMapping({"/", "/app", "/app/"})
    public String forwardToApp() {
        return "forward:/app/index.html";
    }
}

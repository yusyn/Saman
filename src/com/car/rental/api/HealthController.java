package com.car.rental.api;

import com.car.rental.api.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final boolean uiEnabled;

    public HealthController(@Value("${saman.ui.enabled:true}") boolean uiEnabled) {
        this.uiEnabled = uiEnabled;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "saman", uiEnabled);
    }
}

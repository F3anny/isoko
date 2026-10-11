package rw.ac.rca.isoko.common.web;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health")
public class HealthController {

    @GetMapping("/api/v1/health")
    @Operation(summary = "Liveness/readiness for smoke tests (no version leakage)")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @Hidden
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "service", "cloudsphere-api",
                "docs", "/swagger-ui",
                "health", "/api/v1/health"
        );
    }
}

package com.yowpainter.modules.system.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@Tag(name = "System & Diagnostics", description = "Monitoring et sante du backend")
public class SystemController {

    @GetMapping("/health")
    @Operation(summary = "Verifie la sante du systeme")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "timestamp", LocalDateTime.now().toString()));
    }

    @GetMapping("/version")
    @Operation(summary = "Recuperer la version du build")
    public ResponseEntity<Map<String, String>> version() {
        return ResponseEntity.ok(Map.of("version", "1.0.0-RELEASE", "env", "PROD-READY"));
    }

    @GetMapping("/ping")
    @Operation(summary = "Ping simple pour test de latence")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/metrics")
    @Operation(summary = "Recuperer des metriques de base (JVM)")
    public ResponseEntity<Map<String, Object>> metrics() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        
        return ResponseEntity.ok(Map.of(
            "uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + "s",
            "heap_usage", (allocatedMemory - freeMemory) / (1024 * 1024) + "MB",
            "heap_allocated", allocatedMemory / (1024 * 1024) + "MB",
            "heap_max", maxMemory / (1024 * 1024) + "MB",
            "available_processors", runtime.availableProcessors()
        ));
    }
}

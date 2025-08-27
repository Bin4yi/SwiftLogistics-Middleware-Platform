package com.swiftlogistics.tracking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "Tracking Service is running!";
    }

    @GetMapping("/status")
    public String status() {
        return "Service Status: ACTIVE";
    }

    @PostMapping("/create-sample-data")
    public ResponseEntity<Map<String, Object>> createSampleData() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sample data created successfully");
        response.put("success", true);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    // Explicitly handle OPTIONS requests
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }
}
package com.hng.profile.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of(
            "status", "success",
            "message", "Insighta Labs+ API is running",
            "version", "1"
        );
    }
}

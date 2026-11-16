package com.corocorabenz.engine.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo-users")
public class DemoUserController {

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        return Map.of("data", Map.of("email", "usuario" + id.replaceAll("[^a-zA-Z0-9]", "") + "@example.com"));
    }
}

package com.example.smartshelfx.controller;

import com.example.smartshelfx.ai.SpringAiTestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class SpringAiTestController {

    private final SpringAiTestService service;

    public SpringAiTestController(SpringAiTestService service) {
        this.service = service;
    }

    @GetMapping("/test")
    public String test() {
        return service.testModel();
    }
}

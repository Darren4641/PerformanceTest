package com.example.performance.controller;

import com.example.performance.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final TestService testService;


    @GetMapping("/default")
    public String sendEmailDefault() {
        for(int i = 0; i <= 100; i++) {
            testService.sendEmailDefault();
        }
       return "Finished";
    }

    @GetMapping("/thread")
    public String sendEmailThread() throws InterruptedException {
        for(int i = 0; i <= 100; i++) {
            testService.sendEmailThread();
        }
        return "Finished";
    }
}

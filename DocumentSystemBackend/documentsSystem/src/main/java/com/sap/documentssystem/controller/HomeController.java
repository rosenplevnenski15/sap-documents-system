package com.sap.documentssystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Documents System API is running";
    }

    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }

}
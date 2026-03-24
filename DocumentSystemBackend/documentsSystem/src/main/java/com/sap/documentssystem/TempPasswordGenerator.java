package com.sap.documentssystem;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TempPasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println(encodedPassword);
    }
}
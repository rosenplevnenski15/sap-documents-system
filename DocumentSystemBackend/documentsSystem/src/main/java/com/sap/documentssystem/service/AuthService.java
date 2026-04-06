package com.sap.documentssystem.service;

import com.sap.documentssystem.dto.LoginRequest;
import com.sap.documentssystem.dto.LoginResponse;
import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.mapper.MapUser;
import com.sap.documentssystem.entity.User;
import com.sap.documentssystem.repository.UserRepository;
import com.sap.documentssystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpiration()/1000)
                .user(UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
    public LoginResponse refreshToken(String refreshToken) {

        jwtService.validateRefreshToken(refreshToken);

        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String newAccessToken = jwtService.generateToken(user);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getExpiration()/1000)
                .user(MapUser.mapUser(user))
                .build();
    }
}
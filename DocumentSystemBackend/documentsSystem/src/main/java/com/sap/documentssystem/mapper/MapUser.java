package com.sap.documentssystem.mapper;

import com.sap.documentssystem.dto.UserDto;
import com.sap.documentssystem.entity.User;

public class MapUser {
    public static UserDto mapUser(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .build();
    }
}
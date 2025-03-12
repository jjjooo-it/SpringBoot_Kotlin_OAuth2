package com.example.springboot_springsecurity_jwt.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequestDTO {
    private String email;
    private String password;
    private String name;
}

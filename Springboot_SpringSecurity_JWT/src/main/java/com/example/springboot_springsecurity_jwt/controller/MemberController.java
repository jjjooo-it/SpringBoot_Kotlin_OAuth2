package com.example.springboot_springsecurity_jwt.controller;

import com.example.springboot_springsecurity_jwt.dto.LoginRequest;
import com.example.springboot_springsecurity_jwt.dto.LoginResponse;
import com.example.springboot_springsecurity_jwt.dto.SignupRequest;
import com.example.springboot_springsecurity_jwt.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest signupRequest) {
        return memberService.signup(signupRequest);
    }

    // 로그인
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return memberService.login(loginRequest);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        return memberService.logout(request);
    }
}

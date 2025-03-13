package com.example.springboot_springsecurity_jwt.controller;

import com.example.springboot_springsecurity_jwt.dto.LoginRequestDTO;
import com.example.springboot_springsecurity_jwt.dto.LogoutRequestDTO;
import com.example.springboot_springsecurity_jwt.dto.SignupRequestDTO;
import com.example.springboot_springsecurity_jwt.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    // 회원가입
    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequestDTO signupRequestDTO) {
        return memberService.signup(signupRequestDTO);
    }

    // 로그인
    @PostMapping("/login")
    public String login(@RequestBody LoginRequestDTO loginRequestDTO) {
        return memberService.login(loginRequestDTO);
    }

    // 로그아웃
    @PostMapping("/logout")
    public String logout(@RequestBody LogoutRequestDTO logoutRequestDTO) {
        return memberService.logout(logoutRequestDTO);
    }

    // AT 갱신


}

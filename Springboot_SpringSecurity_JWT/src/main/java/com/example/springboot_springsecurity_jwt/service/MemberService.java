package com.example.springboot_springsecurity_jwt.service;

import com.example.springboot_springsecurity_jwt.dto.LoginRequestDTO;
import com.example.springboot_springsecurity_jwt.dto.LogoutRequestDTO;
import com.example.springboot_springsecurity_jwt.dto.SignupRequestDTO;
import com.example.springboot_springsecurity_jwt.entity.Member;
import com.example.springboot_springsecurity_jwt.util.TokenProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.example.springboot_springsecurity_jwt.entity.QMember;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;  // 추가
    private final TokenProvider tokenProvider;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    // 이메일 중복 체크
    private BooleanExpression emailEq(String email) {
        QMember member = QMember.member;
        return member.email.eq(email);
    }

    // 회원가입 로직
    @Transactional
    public String signup(SignupRequestDTO signupRequestDTO) {
        // 이메일 중복 체크
        QMember member = QMember.member;
        long count = queryFactory.select(member.count())
                .from(member)
                .where(emailEq(signupRequestDTO.getEmail()))
                .fetchOne();

        if (count != 0 && count > 0) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequestDTO.getPassword());

        // 회원 정보 저장
        Member newMember = Member.builder()
                .email(signupRequestDTO.getEmail())
                .password(encodedPassword)
                .name(signupRequestDTO.getName())
                .build();

        entityManager.persist(newMember);  // `queryFactory`가 아닌 `entityManager` 사용

        return "회원가입이 완료되었습니다.";
    }

    // 로그인 로직
    public String login(LoginRequestDTO loginRequestDTO) {
        QMember member = QMember.member;
        Member foundMember = queryFactory.selectFrom(member)
                .where(emailEq(loginRequestDTO.getEmail()))
                .fetchOne();

        if (foundMember == null) {
            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), foundMember.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        String accessToken = tokenProvider.makeAccessToken(foundMember.getId());
        String refreshToken = tokenProvider.makeRefreshToken(foundMember.getId());

        return "로그인 성공, AccessToken: " + accessToken + ", RefreshToken: " + refreshToken;
    }

    // 로그아웃 로직
    public String logout(LogoutRequestDTO logoutRequestDTO) {
        QMember member = QMember.member;
        Member foundMember = queryFactory.selectFrom(member)
                .where(emailEq(logoutRequestDTO.getEmail()))
                .fetchOne();

        if (foundMember == null) {
            throw new RuntimeException("회원 정보가 없습니다.");
        }

        String refreshToken = tokenProvider.getRefreshTokenFromRedis(Long.valueOf(logoutRequestDTO.getEmail()));
        if (refreshToken != null) {
            tokenService.deleteRefreshToken(Long.valueOf(logoutRequestDTO.getEmail()));
        }

        return "로그아웃 되었습니다.";
    }
}

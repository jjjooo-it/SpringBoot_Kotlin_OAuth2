package com.example.springboot_springsecurity_jwt.service;

import com.example.springboot_springsecurity_jwt.dto.LoginRequest;
import com.example.springboot_springsecurity_jwt.dto.LoginResponse;
import com.example.springboot_springsecurity_jwt.dto.SignupRequest;
import com.example.springboot_springsecurity_jwt.entity.Member;
import com.example.springboot_springsecurity_jwt.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.example.springboot_springsecurity_jwt.entity.QMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JPAQueryFactory queryFactory;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;

    private static final QMember member = QMember.member;


    // 이메일 중복 체크
    private boolean emailDuplicateCheck(String email) {
        Long count = queryFactory
                .select(member.count())
                .from(member)
                .where(member.email.eq(email))
                .fetchOne();

        return count != null && count > 0;
    }


    // 회원가입 로직
    @Transactional
    public ResponseEntity<String> signup(SignupRequest signupRequest) {
        if (emailDuplicateCheck(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());

        // 회원 정보 저장
        Member newMember = Member.builder()
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .name(signupRequest.getName())
                .build();

        memberRepository.save(newMember);
        return ResponseEntity.ok("회원가입 성공");
    }

    // 로그인 로직
    public LoginResponse login(LoginRequest loginRequest) {
        Member foundMember = queryFactory.selectFrom(member)
                .where(member.email.eq(loginRequest.getEmail()))
                .fetchOne();

        if (foundMember == null || !passwordEncoder.matches(loginRequest.getPassword(), foundMember.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        // accessToken, refreshToken 생성 :: refresh 는 redis 에 저장
        String accessToken = tokenService.makeAccessToken(foundMember.getMemberId());
        String refreshToken = tokenService.makeRefreshToken(foundMember.getMemberId());

        return new LoginResponse(accessToken, refreshToken);
    }

    // 로그아웃 로직 :: redis 에 있는 RT 삭제
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        // Bearer 제거 후 토큰 추출
        String accessToken = authorizationHeader.substring(7);
        if (!tokenService.validateAccessToken(accessToken)) {
            return ResponseEntity.badRequest().body("유효하지 않은 AccessToken입니다.");
        }

        // AccessToken에서 memberId 추출
        Object idValue = tokenService.getClaims(accessToken,true).get("id");
        String memberId = String.valueOf(idValue);

        Member foundMember = queryFactory.selectFrom(member)
                .where(member.memberId.eq(Long.parseLong(memberId)))
                .fetchOne();

        if (foundMember == null) {
            throw new RuntimeException("회원 정보가 없습니다.");
        }

        String refreshToken = tokenService.getRefreshTokenFromRedis(foundMember.getMemberId());
        if (refreshToken != null) {
            redisService.deleteValue("RT:" + foundMember.getMemberId());
            return ResponseEntity.ok("로그아웃 성공");
        } else {
            return ResponseEntity.badRequest().body("리프레시 토큰이 존재하지 않습니다.");
        }
    }

}

package com.example.springboot_springsecurity_jwt.service;

import com.example.springboot_springsecurity_jwt.dto.LoginRequest;
import com.example.springboot_springsecurity_jwt.dto.LoginResponse;
import com.example.springboot_springsecurity_jwt.dto.SignupRequest;
import com.example.springboot_springsecurity_jwt.entity.Member;
import com.example.springboot_springsecurity_jwt.repository.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.example.springboot_springsecurity_jwt.entity.QMember;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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

    private static final Logger logger = LoggerFactory.getLogger(MemberService.class);
    // 로그아웃 로직 :: redis에 있는 RT 삭제
    public ResponseEntity<String> logout() {
        String memberIdString = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("로그아웃 시도: memberId = {}", memberIdString);  // 로그 찍기

        Long memberId = Long.parseLong(memberIdString); // String을 Long으로 변환
        logger.info("변환된 memberId: {}", memberId);  // 변환된 memberId 로그

        // 회원 조회
        Member foundMember = queryFactory.selectFrom(member)
                .where(member.memberId.eq(memberId))
                .fetchOne();

        if (foundMember == null) {
            logger.error("회원 정보가 없습니다. memberId: {}", memberId);  // 에러 로그
            throw new RuntimeException("회원 정보가 없습니다.");
        }

        // Redis에서 refresh token 가져오기
        String refreshToken = tokenService.getRefreshTokenFromRedis(foundMember.getMemberId());
        if (refreshToken != null) {
            logger.info("리프레시 토큰이 존재합니다. 삭제 시도: memberId = {}", memberId);  // 리프레시 토큰 존재시 로그
            // Redis에서 refresh token 삭제
            redisService.deleteValue("RT:" + foundMember.getMemberId());
            logger.info("리프레시 토큰 삭제 성공: memberId = {}", memberId);  // 삭제 성공 로그
            return ResponseEntity.ok("로그아웃 성공");
        } else {
            logger.warn("리프레시 토큰이 존재하지 않습니다. memberId = {}", memberId);  // 경고 로그
            return ResponseEntity.badRequest().body("리프레시 토큰이 존재하지 않습니다.");
        }
    }
}

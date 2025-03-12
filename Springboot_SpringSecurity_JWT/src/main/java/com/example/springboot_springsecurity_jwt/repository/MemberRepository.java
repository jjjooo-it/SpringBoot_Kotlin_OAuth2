package com.example.springboot_springsecurity_jwt.repository;

import com.example.springboot_springsecurity_jwt.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}

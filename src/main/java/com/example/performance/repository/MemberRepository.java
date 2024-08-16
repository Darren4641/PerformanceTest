package com.example.performance.repository;

import com.example.performance.entity.Member;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MemberRepository extends MongoRepository<Member, String> {
    List<Member> findTop1000ByIsSend(Boolean isSend);
}

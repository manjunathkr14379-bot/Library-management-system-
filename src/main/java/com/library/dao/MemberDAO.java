package com.library.dao;

import com.library.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberDAO {
    Member save(Member member);
    Optional<Member> findById(int memberId);
    List<Member> findAll();
    int countActiveBorrowsByMember(int memberId);
    boolean deactivate(int memberId);
}

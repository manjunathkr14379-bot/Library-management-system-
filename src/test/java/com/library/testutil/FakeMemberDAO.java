package com.library.testutil;

import com.library.dao.MemberDAO;
import com.library.model.Member;

import java.util.*;

public class FakeMemberDAO implements MemberDAO {
    private final Map<Integer, Member> store = new HashMap<>();
    private int nextId = 1;
    public final Map<Integer, Integer> activeBorrowCounts = new HashMap<>();

    @Override
    public Member save(Member member) {
        member.setMemberId(nextId++);
        store.put(member.getMemberId(), member);
        return member;
    }

    @Override
    public Optional<Member> findById(int memberId) {
        return Optional.ofNullable(store.get(memberId));
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public int countActiveBorrowsByMember(int memberId) {
        return activeBorrowCounts.getOrDefault(memberId, 0);
    }

    @Override
    public boolean deactivate(int memberId) {
        Member m = store.get(memberId);
        if (m == null) return false;
        m.setActive(false);
        return true;
    }
}

package com.library.model;

import java.time.LocalDate;

public class Member {
    public enum MembershipType { STUDENT, FACULTY, GENERAL }

    private int memberId;
    private String name;
    private String email;
    private String phone;
    private MembershipType membershipType;
    private LocalDate joinedOn;
    private boolean active;

    public Member() { }

    public Member(int memberId, String name, String email, String phone,
                  MembershipType membershipType, LocalDate joinedOn, boolean active) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.membershipType = membershipType;
        this.joinedOn = joinedOn;
        this.active = active;
    }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public MembershipType getMembershipType() { return membershipType; }
    public void setMembershipType(MembershipType membershipType) { this.membershipType = membershipType; }
    public LocalDate getJoinedOn() { return joinedOn; }
    public void setJoinedOn(LocalDate joinedOn) { this.joinedOn = joinedOn; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Borrowing limit differs by membership tier - a small piece of business
     * logic that belongs on the domain model itself.
     */
    public int getMaxBooksAllowed() {
        return switch (membershipType) {
            case FACULTY -> 10;
            case STUDENT -> 5;
            case GENERAL -> 3;
        };
    }

    @Override
    public String toString() {
        return String.format("[%d] %-25s | %-25s | %-8s | active=%s", memberId, name, email, membershipType, active);
    }
}

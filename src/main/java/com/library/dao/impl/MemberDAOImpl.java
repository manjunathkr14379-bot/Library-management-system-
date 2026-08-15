package com.library.dao.impl;

import com.library.config.ConnectionPool;
import com.library.dao.MemberDAO;
import com.library.exception.DataAccessException;
import com.library.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDAOImpl implements MemberDAO {

    private static final String SELECT_BASE =
            "SELECT member_id, name, email, phone, membership_type, joined_on, active FROM members ";

    @Override
    public Member save(Member member) {
        String sql = "INSERT INTO members (name, email, phone, membership_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getMembershipType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) member.setMemberId(keys.getInt(1));
            }
            return member;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save member: " + member.getEmail(), e);
        }
    }

    @Override
    public Optional<Member> findById(int memberId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch member id=" + memberId, e);
        }
    }

    @Override
    public List<Member> findAll() {
        List<Member> members = new ArrayList<>();
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) members.add(map(rs));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch members", e);
        }
        return members;
    }

    @Override
    public int countActiveBorrowsByMember(int memberId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE member_id = ? AND status = 'ISSUED'";
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count active borrows for member id=" + memberId, e);
        }
    }

    @Override
    public boolean deactivate(int memberId) {
        try (Connection conn = ConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE members SET active = FALSE WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to deactivate member id=" + memberId, e);
        }
    }

    private Member map(ResultSet rs) throws SQLException {
        return new Member(
                rs.getInt("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                Member.MembershipType.valueOf(rs.getString("membership_type")),
                rs.getDate("joined_on").toLocalDate(),
                rs.getBoolean("active")
        );
    }
}

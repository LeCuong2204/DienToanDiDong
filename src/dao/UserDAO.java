package dao;

import db.DatabaseConnection;
import model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static String hashPassword(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return plain;
        }
    }

    public User login(String usernameOrEmail, String plainPassword) {
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail.trim());
            ps.setString(2, usernameOrEmail.trim());
            ps.setString(3, hashPassword(plainPassword));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = mapUser(rs);
                updateStatus(u.getId(), "ONLINE");
                u.setStatus("ONLINE");
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // THÊM người dùng mới
    public boolean insertUser(User user, String plainPassword) {
        String sql = "INSERT INTO users (username, email, password_hash, full_name, avatar_url, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername().trim());
            ps.setString(2, user.getEmail().trim());
            ps.setString(3, hashPassword(plainPassword));
            ps.setString(4, user.getFullName().trim());
            ps.setString(5, user.getAvatarUrl());
            ps.setString(6, user.getStatus() != null ? user.getStatus() : "OFFLINE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // SỬA thông tin người dùng
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, full_name = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername().trim());
            ps.setString(2, user.getEmail().trim());
            ps.setString(3, user.getFullName().trim());
            ps.setString(4, user.getStatus());
            ps.setInt(5, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // XÓA người dùng
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // TÌM KIẾM & LẤY DANH SÁCH người dùng
    public List<User> searchUsers(String keyword) {
        List<User> list = new ArrayList<>();
        String kw = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? OR full_name LIKE ? ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countUsers() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setAvatarUrl(rs.getString("avatar_url"));
        u.setStatus(rs.getString("status"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
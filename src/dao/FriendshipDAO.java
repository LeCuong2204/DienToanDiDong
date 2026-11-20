package dao;

import db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendshipDAO {

    public boolean addOrUpdateFriend(int userId, int friendId, String status) {
        if (userId == friendId) return false;
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE status = VALUES(status)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.setString(3, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFriendship(int userId, int friendId) {
        String sql = "DELETE FROM friendships WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.setInt(3, friendId);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String[]> getFriendsWithStatus(int userId, String keyword) {
        List<String[]> rows = new ArrayList<>();
        String kw = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = "SELECT u.id, u.full_name, u.username, u.status, f.status AS rel_status " +
                     "FROM friendships f JOIN users u ON f.friend_id = u.id " +
                     "WHERE f.user_id = ? AND (u.full_name LIKE ? OR u.username LIKE ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("full_name"),
                    rs.getString("username"),
                    rs.getString("status"),
                    rs.getString("rel_status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }
}
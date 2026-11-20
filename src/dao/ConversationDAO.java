package dao;

import db.DatabaseConnection;
import model.Conversation;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {

    public int createConversation(boolean isGroup, String groupName, List<Integer> memberIds) {
        String insertConv = "INSERT INTO conversations (is_group, group_name) VALUES (?, ?)";
        String insertMem = "INSERT IGNORE INTO conversation_members (conversation_id, user_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psConv = conn.prepareStatement(insertConv, Statement.RETURN_GENERATED_KEYS)) {
                psConv.setBoolean(1, isGroup);
                psConv.setString(2, groupName);
                psConv.executeUpdate();
                ResultSet keys = psConv.getGeneratedKeys();
                if (!keys.next()) {
                    conn.rollback();
                    return -1;
                }
                int convId = keys.getInt(1);
                try (PreparedStatement psMem = conn.prepareStatement(insertMem)) {
                    for (Integer uid : memberIds) {
                        psMem.setInt(1, convId);
                        psMem.setInt(2, uid);
                        psMem.addBatch();
                    }
                    psMem.executeBatch();
                }
                conn.commit();
                return convId;
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean renameGroup(int conversationId, String newName) {
        String sql = "UPDATE conversations SET group_name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName.trim());
            ps.setInt(2, conversationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteConversation(int conversationId) {
        String sql = "DELETE FROM conversations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Conversation> getConversationsByUser(int userId, String keyword) {
        List<Conversation> list = new ArrayList<>();
        String kw = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = "SELECT c.* FROM conversations c " +
                     "JOIN conversation_members cm ON c.id = cm.conversation_id " +
                     "WHERE cm.user_id = ? AND (c.group_name LIKE ? OR ? = '%%') " +
                     "ORDER BY c.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Conversation c = new Conversation();
                c.setId(rs.getInt("id"));
                c.setGroup(rs.getBoolean("is_group"));
                c.setGroupName(rs.getString("group_name"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                if (!c.isGroup()) {
                    c.setDisplayTitle(resolveDirectTitle(conn, c.getId(), userId));
                } else {
                    c.setDisplayTitle(c.getGroupName() != null ? c.getGroupName() : "Nhóm #" + c.getId());
                }
                c.setLastMessage(resolveLastMessage(conn, c.getId()));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String resolveDirectTitle(Connection conn, int convId, int currentUserId) throws SQLException {
        String sql = "SELECT u.full_name, u.username FROM conversation_members cm " +
                     "JOIN users u ON cm.user_id = u.id " +
                     "WHERE cm.conversation_id = ? AND u.id <> ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, convId);
            ps.setInt(2, currentUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("full_name") + " (@" + rs.getString("username") + ")";
            }
        }
        return "Chat cá nhân #" + convId;
    }

    private String resolveLastMessage(Connection conn, int convId) throws SQLException {
        String sql = "SELECT content FROM messages WHERE conversation_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, convId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("content");
        }
        return "Chưa có tin nhắn";
    }
}
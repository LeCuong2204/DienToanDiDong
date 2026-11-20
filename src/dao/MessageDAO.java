package dao;

import db.DatabaseConnection;
import model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public int insertMessage(Message msg) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, message_type, content) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, msg.getConversationId());
            ps.setInt(2, msg.getSenderId());
            ps.setString(3, msg.getMessageType() != null ? msg.getMessageType() : "TEXT");
            ps.setString(4, msg.getContent());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateMessageContent(int messageId, int senderId, String newContent) {
        String sql = "UPDATE messages SET content = ? WHERE id = ? AND sender_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newContent);
            ps.setInt(2, messageId);
            ps.setInt(3, senderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteMessage(int messageId, int senderId) {
        String sql = "DELETE FROM messages WHERE id = ? AND sender_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, senderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Message> getMessages(int conversationId, String keyword) {
        List<Message> list = new ArrayList<>();
        String kw = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = "SELECT m.*, u.full_name AS sender_name FROM messages m " +
                     "JOIN users u ON m.sender_id = u.id " +
                     "WHERE m.conversation_id = ? AND m.content LIKE ? " +
                     "ORDER BY m.id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setString(2, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = new Message();
                m.setId(rs.getInt("id"));
                m.setConversationId(rs.getInt("conversation_id"));
                m.setSenderId(rs.getInt("sender_id"));
                m.setSenderName(rs.getString("sender_name"));
                m.setMessageType(rs.getString("message_type"));
                m.setContent(rs.getString("content"));
                m.setSentAt(rs.getTimestamp("sent_at"));
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countMessages() {
        String sql = "SELECT COUNT(*) FROM messages";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
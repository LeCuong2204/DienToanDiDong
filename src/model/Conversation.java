package model;

import java.sql.Timestamp;

public class Conversation {
    private int id;
    private boolean isGroup;
    private String groupName;
    private Timestamp createdAt;
    private String displayTitle;
    private String lastMessage;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { isGroup = group; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getDisplayTitle() { return displayTitle != null ? displayTitle : (groupName != null ? groupName : "Cuộc trò chuyện #" + id); }
    public void setDisplayTitle(String displayTitle) { this.displayTitle = displayTitle; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    @Override
    public String toString() {
        String prefix = isGroup ? "[Nhóm] " : "[1-1] ";
        return prefix + getDisplayTitle();
    }
}
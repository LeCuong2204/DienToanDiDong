package ui;

import dao.ConversationDAO;
import dao.FriendshipDAO;
import dao.MessageDAO;
import dao.UserDAO;
import model.Conversation;
import model.Message;
import model.User;
import network.ChatClientSocket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ClientChatFrame extends JFrame {
    private final User currentUser;
    private final UserDAO userDAO = new UserDAO();
    private final FriendshipDAO friendshipDAO = new FriendshipDAO();
    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final ChatClientSocket clientSocket;

    // Conversations list
    private DefaultListModel<Conversation> convListModel;
    private JList<Conversation> lstConversations;
    private JTextField txtSearchConv;
    private Conversation activeConversation;

    // Chat area
    private JLabel lblActiveTitle;
    private JTextField txtSearchMsg;
    private JPanel chatBubbleContainer;
    private JScrollPane chatScroll;
    private JTextField txtMessageInput;
    private Message selectedMyMessage;

    // Friends CRUD table
    private DefaultTableModel friendTableModel;
    private JTable tblFriends;
    private JTextField txtSearchFriend;
    private JComboBox<User> cbAllUsers;

    public ClientChatFrame(User user, String host, int port) {
        this.currentUser = user;
        UITheme.applyNimbus();
        setTitle("CHAT ONLINE - " + user.getFullName() + " (@" + user.getUsername() + ")");
        setSize(1160, 740);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        initUI();
        loadConversations("");
        loadFriendCombo();
        loadFriends("");

        this.clientSocket = new ChatClientSocket(host, port, this::onSocketEvent);
        boolean ok = this.clientSocket.connect();
        if (ok) {
            this.clientSocket.sendEvent("PRESENCE|" + currentUser.getId() + "|ONLINE");
        } else {
            JOptionPane.showMessageDialog(this, "Chưa kết nối được Host Socket (vẫn hoạt động chế độ CSDL trực tiếp).");
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                userDAO.updateStatus(currentUser.getId(), "OFFLINE");
                if (clientSocket != null) clientSocket.disconnect();
                dispose();
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UITheme.BG_LIGHT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.PRIMARY);
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel lblMe = new JLabel("Xin chào, " + currentUser.getFullName() + " (@" + currentUser.getUsername() + ")");
        lblMe.setFont(UITheme.FONT_TITLE);
        lblMe.setForeground(Color.WHITE);

        JPanel rightHdr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHdr.setOpaque(false);

        JComboBox<String> cbMyStatus = new JComboBox<>(new String[]{"ONLINE", "BUSY", "OFFLINE"});
        cbMyStatus.setSelectedItem(currentUser.getStatus());
        cbMyStatus.addActionListener(e -> {
            String st = cbMyStatus.getSelectedItem().toString();
            userDAO.updateStatus(currentUser.getId(), st);
            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.sendEvent("PRESENCE|" + currentUser.getId() + "|" + st);
            }
        });

        JButton btnNewClient = UITheme.createButton("+ Cửa sổ User khác", Color.WHITE, UITheme.PRIMARY);
        btnNewClient.addActionListener(e -> new LoginRegisterDialog(this).setVisible(true));

        rightHdr.add(new JLabel("Trạng thái:"));
        rightHdr.add(cbMyStatus);
        rightHdr.add(btnNewClient);

        header.add(lblMe, BorderLayout.WEST);
        header.add(rightHdr, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTabbedPane mainTabs = new JTabbedPane();
        mainTabs.setFont(UITheme.FONT_BOLD);
        mainTabs.addTab("Hộp thoại & Nhắn tin Real-time", buildChatTab());
        mainTabs.addTab("Quản lý Bạn bè (Thêm / Chấp nhận / Xóa / Tìm kiếm)", buildFriendsTab());

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UITheme.BG_LIGHT);
        body.setBorder(new EmptyBorder(0, 10, 10, 10));
        body.add(mainTabs, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private JPanel buildChatTab() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(320);

        // Left: Conversations + Create/Rename/Delete
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBackground(UITheme.SURFACE);
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel searchConvRow = new JPanel(new BorderLayout(6, 0));
        searchConvRow.setBackground(UITheme.SURFACE);
        txtSearchConv = UITheme.createTextField("Tìm phòng chat...");
        JButton btnSearchConv = UITheme.createButton("Tìm", UITheme.PRIMARY, Color.WHITE);
        searchConvRow.add(txtSearchConv, BorderLayout.CENTER);
        searchConvRow.add(btnSearchConv, BorderLayout.EAST);

        convListModel = new DefaultListModel<>();
        lstConversations = new JList<>(convListModel);
        lstConversations.setFont(UITheme.FONT_BODY);
        lstConversations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstConversations.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                activeConversation = lstConversations.getSelectedValue();
                openConversation(activeConversation);
            }
        });

        JPanel convActions = new JPanel(new GridLayout(1, 3, 6, 0));
        convActions.setBackground(UITheme.SURFACE);
        JButton btnCreateConv = UITheme.createButton("+ Tạo", UITheme.SUCCESS, Color.WHITE);
        JButton btnRenameConv = UITheme.createButton("Sửa tên", UITheme.PRIMARY, Color.WHITE);
        JButton btnDelConv = UITheme.createButton("Xóa", UITheme.DANGER, Color.WHITE);
        convActions.add(btnCreateConv);
        convActions.add(btnRenameConv);
        convActions.add(btnDelConv);

        leftPanel.add(searchConvRow, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(lstConversations), BorderLayout.CENTER);
        leftPanel.add(convActions, BorderLayout.SOUTH);

        // Right: Messages
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBackground(UITheme.SURFACE);
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topChat = new JPanel(new BorderLayout(8, 0));
        topChat.setBackground(UITheme.SURFACE);
        lblActiveTitle = new JLabel("Chọn một cuộc trò chuyện bên trái");
        lblActiveTitle.setFont(UITheme.FONT_SUBTITLE);

        JPanel findMsgBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        findMsgBox.setBackground(UITheme.SURFACE);
        txtSearchMsg = UITheme.createTextField("Tìm nội dung tin nhắn...");
        txtSearchMsg.setPreferredSize(new Dimension(190, 32));
        JButton btnFindMsg = UITheme.createButton("Tìm tin", UITheme.PRIMARY, Color.WHITE);
        JButton btnEditMsg = UITheme.createButton("Sửa tin", UITheme.BG_LIGHT, UITheme.TEXT_DARK);
        JButton btnDelMsg = UITheme.createButton("Xóa tin", UITheme.DANGER, Color.WHITE);
        findMsgBox.add(txtSearchMsg);
        findMsgBox.add(btnFindMsg);
        findMsgBox.add(btnEditMsg);
        findMsgBox.add(btnDelMsg);

        topChat.add(lblActiveTitle, BorderLayout.WEST);
        topChat.add(findMsgBox, BorderLayout.EAST);

        chatBubbleContainer = new JPanel();
        chatBubbleContainer.setLayout(new BoxLayout(chatBubbleContainer, BoxLayout.Y_AXIS));
        chatBubbleContainer.setBackground(UITheme.BG_LIGHT);
        chatBubbleContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        chatScroll = new JScrollPane(chatBubbleContainer);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel sendBar = new JPanel(new BorderLayout(8, 0));
        sendBar.setBackground(UITheme.SURFACE);
        txtMessageInput = UITheme.createTextField("Nhập tin nhắn và nhấn Enter hoặc Gửi...");
        JButton btnSend = UITheme.createButton("Gửi", UITheme.PRIMARY, Color.WHITE);
        sendBar.add(txtMessageInput, BorderLayout.CENTER);
        sendBar.add(btnSend, BorderLayout.EAST);

        rightPanel.add(topChat, BorderLayout.NORTH);
        rightPanel.add(chatScroll, BorderLayout.CENTER);
        rightPanel.add(sendBar, BorderLayout.SOUTH);

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);

        btnSearchConv.addActionListener(e -> loadConversations(txtSearchConv.getText()));
        btnCreateConv.addActionListener(e -> createConversationDialog());
        btnRenameConv.addActionListener(e -> renameConversationAction());
        btnDelConv.addActionListener(e -> deleteConversationAction());
        btnFindMsg.addActionListener(e -> reloadMessages());
        btnEditMsg.addActionListener(e -> editSelectedMessage());
        btnDelMsg.addActionListener(e -> deleteSelectedMessage());
        btnSend.addActionListener(e -> sendMessageAction());
        txtMessageInput.addActionListener(e -> sendMessageAction());

        return wrapper;
    }

    private JPanel buildFriendsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topRow.setBackground(UITheme.SURFACE);

        topRow.add(new JLabel("Tìm bạn bè:"));
        txtSearchFriend = UITheme.createTextField("Tên hoặc username...");
        txtSearchFriend.setPreferredSize(new Dimension(200, 34));
        JButton btnSearchF = UITheme.createButton("Tìm kiếm", UITheme.PRIMARY, Color.WHITE);

        topRow.add(txtSearchFriend);
        topRow.add(btnSearchF);

        topRow.add(new JLabel("   Thêm/Kết bạn với:"));
        cbAllUsers = new JComboBox<>();
        cbAllUsers.setPreferredSize(new Dimension(240, 34));
        JButton btnAddF = UITheme.createButton("Gửi lời mời", UITheme.SUCCESS, Color.WHITE);
        JButton btnAcceptF = UITheme.createButton("Chấp nhận", UITheme.PRIMARY, Color.WHITE);
        JButton btnDelF = UITheme.createButton("Xóa bạn", UITheme.DANGER, Color.WHITE);

        topRow.add(cbAllUsers);
        topRow.add(btnAddF);
        topRow.add(btnAcceptF);
        topRow.add(btnDelF);

        friendTableModel = new DefaultTableModel(
                new String[]{"ID", "Họ tên", "Username", "Trạng thái mạng", "Quan hệ"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblFriends = new JTable(friendTableModel);
        tblFriends.setRowHeight(28);
        tblFriends.setFont(UITheme.FONT_BODY);
        tblFriends.getTableHeader().setFont(UITheme.FONT_BOLD);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(tblFriends), BorderLayout.CENTER);

        btnSearchF.addActionListener(e -> loadFriends(txtSearchFriend.getText()));
        btnAddF.addActionListener(e -> {
            User target = (User) cbAllUsers.getSelectedItem();
            if (target != null && friendshipDAO.addOrUpdateFriend(currentUser.getId(), target.getId(), "PENDING")) {
                loadFriends(txtSearchFriend.getText());
            }
        });
        btnAcceptF.addActionListener(e -> {
            int row = tblFriends.getSelectedRow();
            if (row >= 0) {
                int fid = Integer.parseInt(friendTableModel.getValueAt(row, 0).toString());
                if (friendshipDAO.addOrUpdateFriend(currentUser.getId(), fid, "ACCEPTED")) {
                    loadFriends(txtSearchFriend.getText());
                }
            }
        });
        btnDelF.addActionListener(e -> {
            int row = tblFriends.getSelectedRow();
            if (row >= 0) {
                int fid = Integer.parseInt(friendTableModel.getValueAt(row, 0).toString());
                if (friendshipDAO.removeFriendship(currentUser.getId(), fid)) {
                    loadFriends(txtSearchFriend.getText());
                }
            }
        });

        return panel;
    }

    private void loadFriendCombo() {
        cbAllUsers.removeAllItems();
        for (User u : userDAO.searchUsers("")) {
            if (u.getId() != currentUser.getId()) {
                cbAllUsers.addItem(u);
            }
        }
    }

    private void loadFriends(String kw) {
        friendTableModel.setRowCount(0);
        for (String[] row : friendshipDAO.getFriendsWithStatus(currentUser.getId(), kw)) {
            friendTableModel.addRow(row);
        }
    }

    private void loadConversations(String kw) {
        convListModel.clear();
        for (Conversation c : conversationDAO.getConversationsByUser(currentUser.getId(), kw)) {
            convListModel.addElement(c);
        }
    }

    private void createConversationDialog() {
        List<User> all = userDAO.searchUsers("");
        List<JCheckBox> boxes = new ArrayList<>();
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        JTextField txtGroupName = new JTextField();
        p.add(new JLabel("Tên nhóm (để trống nếu là chat 1-1):"));
        p.add(txtGroupName);
        p.add(new JLabel("Chọn thành viên tham gia:"));
        for (User u : all) {
            if (u.getId() != currentUser.getId()) {
                JCheckBox cb = new JCheckBox(u.getFullName() + " (@" + u.getUsername() + ")");
                cb.putClientProperty("uid", u.getId());
                boxes.add(cb);
                p.add(cb);
            }
        }
        int res = JOptionPane.showConfirmDialog(this, p, "Tạo cuộc trò chuyện mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            List<Integer> members = new ArrayList<>();
            members.add(currentUser.getId());
            for (JCheckBox cb : boxes) {
                if (cb.isSelected()) members.add((Integer) cb.getClientProperty("uid"));
            }
            if (members.size() < 2) {
                JOptionPane.showMessageDialog(this, "Hãy chọn ít nhất 1 người khác!");
                return;
            }
            String gName = txtGroupName.getText().trim();
            boolean isGroup = members.size() > 2 || !gName.isEmpty();
            int newId = conversationDAO.createConversation(isGroup, isGroup ? (gName.isEmpty() ? "Nhóm chat" : gName) : null, members);
            if (newId > 0) {
                loadConversations(txtSearchConv.getText());
                if (clientSocket != null && clientSocket.isConnected()) {
                    clientSocket.sendEvent("CONV_CREATED|" + newId);
                }
            }
        }
    }

    private void renameConversationAction() {
        if (activeConversation == null) return;
        String newName = JOptionPane.showInputDialog(this, "Tên cuộc trò chuyện mới:", activeConversation.getDisplayTitle());
        if (newName != null && !newName.trim().isEmpty()) {
            if (conversationDAO.renameGroup(activeConversation.getId(), newName)) {
                loadConversations(txtSearchConv.getText());
            }
        }
    }

    private void deleteConversationAction() {
        if (activeConversation == null) return;
        int c = JOptionPane.showConfirmDialog(this, "Xóa cuộc trò chuyện này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION && conversationDAO.deleteConversation(activeConversation.getId())) {
            activeConversation = null;
            lblActiveTitle.setText("Chọn một cuộc trò chuyện bên trái");
            chatBubbleContainer.removeAll();
            chatBubbleContainer.revalidate();
            chatBubbleContainer.repaint();
            loadConversations(txtSearchConv.getText());
        }
    }

    private void openConversation(Conversation conv) {
        if (conv == null) return;
        lblActiveTitle.setText(conv.toString());
        selectedMyMessage = null;
        reloadMessages();
    }

    private void reloadMessages() {
        if (activeConversation == null) return;
        chatBubbleContainer.removeAll();
        List<Message> msgs = messageDAO.getMessages(activeConversation.getId(), txtSearchMsg.getText());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        for (Message m : msgs) {
            boolean isMe = (m.getSenderId() == currentUser.getId());
            JPanel row = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 4));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

            JPanel bubble = new JPanel(new BorderLayout(4, 2));
            bubble.setBackground(isMe ? UITheme.BUBBLE_ME : UITheme.BUBBLE_OTHER);
            bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

            JLabel lblMeta = new JLabel((isMe ? "Bạn" : m.getSenderName()) + " • " + (m.getSentAt() != null ? sdf.format(m.getSentAt()) : ""));
            lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblMeta.setForeground(isMe ? new Color(219, 234, 254) : UITheme.TEXT_MUTED);

            JLabel lblContent = new JLabel("<html><body style='max-width:340px;'>" + escapeHtml(m.getContent()) + "</body></html>");
            lblContent.setFont(UITheme.FONT_BODY);
            lblContent.setForeground(isMe ? Color.WHITE : UITheme.TEXT_DARK);

            bubble.add(lblMeta, BorderLayout.NORTH);
            bubble.add(lblContent, BorderLayout.CENTER);

            if (isMe) {
                bubble.setCursor(new Cursor(Cursor.HAND_CURSOR));
                bubble.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectedMyMessage = m;
                        JOptionPane.showMessageDialog(ClientChatFrame.this, "Đã chọn tin nhắn của bạn (#" + m.getId() + "). Bạn có thể nhấn 'Sửa tin' hoặc 'Xóa tin'.");
                    }
                });
            }

            row.add(bubble);
            chatBubbleContainer.add(row);
        }

        chatBubbleContainer.revalidate();
        chatBubbleContainer.repaint();
        SwingUtilities.invokeLater(() -> chatScroll.getVerticalScrollBar().setValue(chatScroll.getVerticalScrollBar().getMaximum()));
    }

    private void sendMessageAction() {
        if (activeConversation == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn hoặc tạo một cuộc trò chuyện trước!");
            return;
        }
        String text = txtMessageInput.getText().trim();
        if (text.isEmpty()) return;

        Message m = new Message();
        m.setConversationId(activeConversation.getId());
        m.setSenderId(currentUser.getId());
        m.setMessageType("TEXT");
        m.setContent(text);

        int id = messageDAO.insertMessage(m);
        if (id > 0) {
            txtMessageInput.setText("");
            reloadMessages();
            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.sendEvent("NEW_MSG|" + activeConversation.getId() + "|" + currentUser.getId());
            }
        }
    }

    private void editSelectedMessage() {
        if (selectedMyMessage == null) {
            JOptionPane.showMessageDialog(this, "Hãy click vào một tin nhắn của chính bạn để chọn sửa!");
            return;
        }
        String updated = JOptionPane.showInputDialog(this, "Sửa nội dung tin nhắn:", selectedMyMessage.getContent());
        if (updated != null && !updated.trim().isEmpty()) {
            if (messageDAO.updateMessageContent(selectedMyMessage.getId(), currentUser.getId(), updated.trim())) {
                selectedMyMessage = null;
                reloadMessages();
                if (clientSocket != null && clientSocket.isConnected()) {
                    clientSocket.sendEvent("EDIT_MSG|" + activeConversation.getId());
                }
            }
        }
    }

    private void deleteSelectedMessage() {
        if (selectedMyMessage == null) {
            JOptionPane.showMessageDialog(this, "Hãy click vào một tin nhắn của chính bạn để chọn xóa!");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Xóa tin nhắn này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION && messageDAO.deleteMessage(selectedMyMessage.getId(), currentUser.getId())) {
            selectedMyMessage = null;
            reloadMessages();
            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.sendEvent("DEL_MSG|" + activeConversation.getId());
            }
        }
    }

    private void onSocketEvent(String payload) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = payload.split("\\|");
            if (parts.length == 0) return;
            String type = parts[0];
            if ("CONV_CREATED".equals(type)) {
                loadConversations(txtSearchConv.getText());
            } else if ("NEW_MSG".equals(type) || "EDIT_MSG".equals(type) || "DEL_MSG".equals(type)) {
                loadConversations(txtSearchConv.getText());
                if (parts.length >= 2 && activeConversation != null) {
                    try {
                        int convId = Integer.parseInt(parts[1]);
                        if (convId == activeConversation.getId()) reloadMessages();
                    } catch (NumberFormatException ignored) {}
                }
            } else if ("PRESENCE".equals(type)) {
                loadFriends(txtSearchFriend.getText());
            }
        });
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&").replace("<", "<").replace(">", ">");
    }
}
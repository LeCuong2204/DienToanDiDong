package ui;

import dao.MessageDAO;
import dao.UserDAO;
import model.User;
import network.ChatServerSocket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HostServerFrame extends JFrame {
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private ChatServerSocket serverSocket;

    private JTextField txtPort;
    private JButton btnStart, btnStop, btnOpenClient;
    private JLabel lblStatus, lblStats;
    private JTextArea txtLog;

    // CRUD Users
    private JTextField txtSearchUser, txtUsername, txtEmail, txtFullName;
    private JPasswordField txtPassword;
    private JComboBox<String> cbStatus;
    private DefaultTableModel userTableModel;
    private JTable tblUsers;
    private int selectedUserId = -1;

    public HostServerFrame() {
        UITheme.applyNimbus();
        setTitle("HOST SERVER CONSOLE - HỆ THỐNG CHAT ONLINE (MySQL + Socket)");
        setSize(1120, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
        refreshUsers("");
        refreshStats();
    }

    private void initUI() {
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(UITheme.BG_LIGHT);

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.PRIMARY);
        topBar.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel lblTitle = new JLabel("MÁY CHỦ HOST CHAT ONLINE & QUẢN TRỊ CSDL");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        lblStats = new JLabel("Users: 0 | Messages: 0 | Sockets: 0");
        lblStats.setFont(UITheme.FONT_BOLD);
        lblStats.setForeground(new Color(224, 242, 254));

        topBar.add(lblTitle, BorderLayout.WEST);
        topBar.add(lblStats, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BOLD);
        tabs.addTab("Máy Host & Log Real-time", buildHostTab());
        tabs.addTab("Quản lý Người dùng (Thêm / Sửa / Xóa / Tìm kiếm)", buildUsersCrudTab());

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBorder(new EmptyBorder(0, 12, 12, 12));
        centerWrap.setBackground(UITheme.BG_LIGHT);
        centerWrap.add(tabs, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);
    }

    private JPanel buildHostTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        controlPanel.setBackground(UITheme.SURFACE);

        controlPanel.add(new JLabel("Cổng Socket:"));
        txtPort = UITheme.createTextField("Port");
        txtPort.setText("9999");
        txtPort.setPreferredSize(new Dimension(90, 34));
        controlPanel.add(txtPort);

        btnStart = UITheme.createButton("Khởi động Host", UITheme.SUCCESS, Color.WHITE);
        btnStop = UITheme.createButton("Dừng Host", UITheme.DANGER, Color.WHITE);
        btnStop.setEnabled(false);
        btnOpenClient = UITheme.createButton("Mở giao diện Máy Người Dùng", UITheme.PRIMARY, Color.WHITE);

        lblStatus = new JLabel("Trạng thái: ĐANG TẮT");
        lblStatus.setFont(UITheme.FONT_BOLD);
        lblStatus.setForeground(UITheme.DANGER);

        controlPanel.add(btnStart);
        controlPanel.add(btnStop);
        controlPanel.add(btnOpenClient);
        controlPanel.add(lblStatus);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setBackground(new Color(17, 24, 39));
        txtLog.setForeground(new Color(134, 239, 172));

        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Nhật ký hoạt động Socket Server"));

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollLog, BorderLayout.CENTER);

        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
        btnOpenClient.addActionListener(e -> new LoginRegisterDialog(this).setVisible(true));

        return panel;
    }

    private JPanel buildUsersCrudTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.SURFACE);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Search bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBar.setBackground(UITheme.SURFACE);
        searchBar.add(new JLabel("Tìm kiếm:"));
        txtSearchUser = UITheme.createTextField("Nhập tên, username hoặc email...");
        txtSearchUser.setPreferredSize(new Dimension(260, 34));
        JButton btnSearch = UITheme.createButton("Tìm", UITheme.PRIMARY, Color.WHITE);
        JButton btnReset = UITheme.createButton("Tất cả", UITheme.BG_LIGHT, UITheme.TEXT_DARK);
        searchBar.add(txtSearchUser);
        searchBar.add(btnSearch);
        searchBar.add(btnReset);

        // Table
        userTableModel = new DefaultTableModel(
                new String[]{"ID", "Username", "Họ tên", "Email", "Trạng thái", "Ngày tạo"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tblUsers = new JTable(userTableModel);
        tblUsers.setRowHeight(28);
        tblUsers.setFont(UITheme.FONT_BODY);
        tblUsers.getTableHeader().setFont(UITheme.FONT_BOLD);

        // Form CRUD
        JPanel formPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        formPanel.setBackground(UITheme.BG_LIGHT);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_GRAY),
                new EmptyBorder(12, 12, 12, 12)
        ));

        txtUsername = UITheme.createTextField("Username");
        txtFullName = UITheme.createTextField("Họ và tên");
        txtEmail = UITheme.createTextField("Email");
        txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(180, 34));
        cbStatus = new JComboBox<>(new String[]{"OFFLINE", "ONLINE", "BUSY"});
        cbStatus.setFont(UITheme.FONT_BODY);

        formPanel.add(new JLabel("Username:"));
        formPanel.add(new JLabel("Họ và tên:"));
        formPanel.add(new JLabel("Email:"));
        formPanel.add(new JLabel("Mật khẩu (khi thêm):"));
        formPanel.add(new JLabel("Trạng thái:"));

        formPanel.add(txtUsername);
        formPanel.add(txtFullName);
        formPanel.add(txtEmail);
        formPanel.add(txtPassword);
        formPanel.add(cbStatus);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        actionPanel.setBackground(UITheme.SURFACE);
        JButton btnAdd = UITheme.createButton("Thêm mới", UITheme.SUCCESS, Color.WHITE);
        JButton btnUpdate = UITheme.createButton("Cập nhật", UITheme.PRIMARY, Color.WHITE);
        JButton btnDelete = UITheme.createButton("Xóa", UITheme.DANGER, Color.WHITE);
        JButton btnClear = UITheme.createButton("Làm mới form", UITheme.BG_LIGHT, UITheme.TEXT_DARK);
        actionPanel.add(btnAdd);
        actionPanel.add(btnUpdate);
        actionPanel.add(btnDelete);
        actionPanel.add(btnClear);

        JPanel bottomBox = new JPanel(new BorderLayout(6, 6));
        bottomBox.setBackground(UITheme.SURFACE);
        bottomBox.add(formPanel, BorderLayout.CENTER);
        bottomBox.add(actionPanel, BorderLayout.SOUTH);

        panel.add(searchBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(tblUsers), BorderLayout.CENTER);
        panel.add(bottomBox, BorderLayout.SOUTH);

        tblUsers.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tblUsers.getSelectedRow() >= 0) {
                int row = tblUsers.getSelectedRow();
                selectedUserId = Integer.parseInt(userTableModel.getValueAt(row, 0).toString());
                txtUsername.setText(userTableModel.getValueAt(row, 1).toString());
                txtFullName.setText(userTableModel.getValueAt(row, 2).toString());
                txtEmail.setText(userTableModel.getValueAt(row, 3).toString());
                cbStatus.setSelectedItem(userTableModel.getValueAt(row, 4).toString());
            }
        });

        btnSearch.addActionListener(e -> refreshUsers(txtSearchUser.getText()));
        btnReset.addActionListener(e -> {
            txtSearchUser.setText("");
            refreshUsers("");
        });
        btnAdd.addActionListener(e -> addUserAction());
        btnUpdate.addActionListener(e -> updateUserAction());
        btnDelete.addActionListener(e -> deleteUserAction());
        btnClear.addActionListener(e -> clearForm());

        return panel;
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cổng Socket không hợp lệ!");
            return;
        }
        serverSocket = new ChatServerSocket(port, this::appendLog);
        serverSocket.start();
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        txtPort.setEnabled(false);
        lblStatus.setText("Trạng thái: ĐANG CHẠY (" + port + ")");
        lblStatus.setForeground(UITheme.SUCCESS);
        refreshStats();
    }

    private void stopServer() {
        if (serverSocket != null) {
            serverSocket.stop();
        }
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        txtPort.setEnabled(true);
        lblStatus.setText("Trạng thái: ĐANG TẮT");
        lblStatus.setForeground(UITheme.DANGER);
        refreshStats();
    }

    private void appendLog(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            txtLog.append("[" + time + "] " + msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
            refreshStats();
        });
    }

    private void refreshUsers(String keyword) {
        userTableModel.setRowCount(0);
        List<User> list = userDAO.searchUsers(keyword);
        for (User u : list) {
            userTableModel.addRow(new Object[]{
                u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getStatus(), u.getCreatedAt()
            });
        }
        refreshStats();
    }

    private void addUserAction() {
        String uname = txtUsername.getText().trim();
        String fname = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        if (uname.isEmpty() || fname.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Username, Họ tên, Email và Mật khẩu!");
            return;
        }
        User u = new User();
        u.setUsername(uname);
        u.setFullName(fname);
        u.setEmail(email);
        u.setStatus(cbStatus.getSelectedItem().toString());
        if (userDAO.insertUser(u, pass)) {
            JOptionPane.showMessageDialog(this, "Thêm người dùng thành công!");
            clearForm();
            refreshUsers(txtSearchUser.getText());
        } else {
            JOptionPane.showMessageDialog(this, "Thêm thất bại (có thể trùng username hoặc email)!");
        }
    }

    private void updateUserAction() {
        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một dòng trong bảng để cập nhật!");
            return;
        }
        User u = new User();
        u.setId(selectedUserId);
        u.setUsername(txtUsername.getText().trim());
        u.setFullName(txtFullName.getText().trim());
        u.setEmail(txtEmail.getText().trim());
        u.setStatus(cbStatus.getSelectedItem().toString());
        if (userDAO.updateUser(u)) {
            JOptionPane.showMessageDialog(this, "Cập nhật người dùng thành công!");
            refreshUsers(txtSearchUser.getText());
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật thất bại!");
        }
    }

    private void deleteUserAction() {
        if (selectedUserId <= 0) {
            JOptionPane.showMessageDialog(this, "Hãy chọn người dùng cần xóa!");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Xóa người dùng #" + selectedUserId + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION && userDAO.deleteUser(selectedUserId)) {
            JOptionPane.showMessageDialog(this, "Đã xóa người dùng!");
            clearForm();
            refreshUsers(txtSearchUser.getText());
        }
    }

    private void clearForm() {
        selectedUserId = -1;
        txtUsername.setText("");
        txtFullName.setText("");
        txtEmail.setText("");
        txtPassword.setText("");
        cbStatus.setSelectedIndex(0);
        tblUsers.clearSelection();
    }

    private void refreshStats() {
        int sockCount = (serverSocket != null) ? serverSocket.getConnectedCount() : 0;
        lblStats.setText("Users: " + userDAO.countUsers() + " | Messages: " + messageDAO.countMessages() + " | Sockets: " + sockCount);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HostServerFrame().setVisible(true));
    }
}
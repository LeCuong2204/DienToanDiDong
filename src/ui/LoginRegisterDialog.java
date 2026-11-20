package ui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginRegisterDialog extends JDialog {
    private final UserDAO userDAO = new UserDAO();

    public LoginRegisterDialog(Frame owner) {
        super(owner, "Đăng nhập / Đăng ký - Máy Người Dùng", true);
        setSize(420, 360);
        setLocationRelativeTo(owner);
        initUI();
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BOLD);
        tabs.addTab("Đăng nhập", buildLoginPanel());
        tabs.addTab("Đăng ký", buildRegisterPanel());
        add(tabs);
    }

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel(new GridLayout(4, 2, 10, 12));
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        p.setBackground(UITheme.SURFACE);

        JTextField txtUser = UITheme.createTextField("Username hoặc Email");
        JPasswordField txtPass = new JPasswordField();
        txtPass.setPreferredSize(new Dimension(180, 34));
        JTextField txtHost = UITheme.createTextField("Host");
        txtHost.setText("localhost");
        JTextField txtPort = UITheme.createTextField("Port");
        txtPort.setText("9999");

        p.add(new JLabel("Username / Email:"));
        p.add(txtUser);
        p.add(new JLabel("Mật khẩu:"));
        p.add(txtPass);
        p.add(new JLabel("Host / Port:"));

        JPanel hp = new JPanel(new GridLayout(1, 2, 6, 0));
        hp.setBackground(UITheme.SURFACE);
        hp.add(txtHost);
        hp.add(txtPort);
        p.add(hp);

        JButton btnLogin = UITheme.createButton("Đăng nhập & Mở Chat", UITheme.PRIMARY, Color.WHITE);
        p.add(new JLabel(""));
        p.add(btnLogin);

        btnLogin.addActionListener(e -> {
            User u = userDAO.login(txtUser.getText(), new String(txtPass.getPassword()));
            if (u == null) {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
                return;
            }
            int port = 9999;
            try { port = Integer.parseInt(txtPort.getText().trim()); } catch (Exception ignored) {}
            dispose();
            new ClientChatFrame(u, txtHost.getText().trim(), port).setVisible(true);
        });

        return p;
    }

    private JPanel buildRegisterPanel() {
        JPanel p = new JPanel(new GridLayout(5, 2, 10, 10));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.setBackground(UITheme.SURFACE);

        JTextField txtU = UITheme.createTextField("Username");
        JTextField txtF = UITheme.createTextField("Họ tên");
        JTextField txtE = UITheme.createTextField("Email");
        JPasswordField txtP = new JPasswordField();
        txtP.setPreferredSize(new Dimension(180, 34));

        p.add(new JLabel("Username:"));
        p.add(txtU);
        p.add(new JLabel("Họ và tên:"));
        p.add(txtF);
        p.add(new JLabel("Email:"));
        p.add(txtE);
        p.add(new JLabel("Mật khẩu:"));
        p.add(txtP);

        JButton btnReg = UITheme.createButton("Tạo tài khoản", UITheme.SUCCESS, Color.WHITE);
        p.add(new JLabel(""));
        p.add(btnReg);

        btnReg.addActionListener(e -> {
            User u = new User();
            u.setUsername(txtU.getText());
            u.setFullName(txtF.getText());
            u.setEmail(txtE.getText());
            u.setStatus("OFFLINE");
            if (userDAO.insertUser(u, new String(txtP.getPassword()))) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy chuyển sang tab Đăng nhập.");
            } else {
                JOptionPane.showMessageDialog(this, "Đăng ký thất bại (kiểm tra trùng thông tin)!");
            }
        });

        return p;
    }
}
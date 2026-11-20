package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UITheme {
    public static final Color PRIMARY = new Color(24, 119, 242);
    public static final Color PRIMARY_DARK = new Color(15, 92, 196);
    public static final Color BG_LIGHT = new Color(244, 246, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER_GRAY = new Color(218, 224, 232);
    public static final Color TEXT_DARK = new Color(28, 30, 33);
    public static final Color TEXT_MUTED = new Color(101, 103, 107);
    public static final Color BUBBLE_ME = new Color(24, 119, 242);
    public static final Color BUBBLE_OTHER = new Color(235, 238, 243);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color DANGER = new Color(220, 38, 38);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    public static void applyNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        return btn;
    }

    public static JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(FONT_BODY);
        tf.setPreferredSize(new Dimension(180, 34));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        if (placeholder != null) tf.setToolTipText(placeholder);
        return tf;
    }
}
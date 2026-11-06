package client;

import javax.swing.*;
import java.awt.*;

/**
 * QUẢN LÝ THEME VÀ THIẾT LẬP GIAO DIỆN (THEME MANAGER)
 *
 * Khởi tạo FlatDarkLaf và cấu hình UIManager Defaults đồng bộ:
 * - Đảm bảo toàn bộ ứng dụng dùng chung bảng màu Modern Dark.
 * - Loại bỏ hoàn toàn các màu mặc định cũ kỹ của Swing (nền xám trắng, viền nổi 3D).
 */
public class ThemeManager {

    public static final Color CLR_BG        = new Color(0x11, 0x18, 0x27); // #111827
    public static final Color CLR_PANEL     = new Color(0x1F, 0x29, 0x37); // #1F2937
    public static final Color CLR_SURFACE   = new Color(0x27, 0x34, 0x49); // #273449
    public static final Color CLR_BORDER    = new Color(0x37, 0x41, 0x51); // #374151
    public static final Color CLR_TEXT      = new Color(0xF9, 0xFA, 0xFB); // #F9FAFB
    public static final Color CLR_MUTED     = new Color(0x9C, 0xA3, 0xAF); // #9CA3AF
    public static final Color CLR_PRIMARY   = new Color(0x3B, 0x82, 0xF6); // #3B82F6
    public static final Color CLR_SUCCESS   = new Color(0x22, 0xC5, 0x5E); // #22C55E
    public static final Color CLR_WARN      = new Color(0xF5, 0x9E, 0x0B); // #F59E0B
    public static final Color CLR_DANGER    = new Color(0xEF, 0x44, 0x44); // #EF4444

    private static boolean isInitialized = false;

    public static synchronized void applyTheme() {
        if (isInitialized) return;
        isInitialized = true;

        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            flatLafClass.getMethod("setup").invoke(null);
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Cấu hình UIManager defaults chống chọi mọi Look and Feel
        UIManager.put("Panel.background", CLR_BG);
        UIManager.put("Panel.foreground", CLR_TEXT);
        UIManager.put("Label.foreground", CLR_TEXT);
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));

        UIManager.put("Button.background", CLR_PANEL);
        UIManager.put("Button.foreground", CLR_TEXT);
        UIManager.put("Button.borderColor", CLR_BORDER);
        UIManager.put("Button.arc", 6);
        UIManager.put("Component.arc", 6);
        UIManager.put("TextComponent.arc", 6);

        UIManager.put("ScrollBar.thumbArc", 6);
        UIManager.put("ScrollBar.trackArc", 6);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbColor", new Color(0x37, 0x41, 0x51));
        UIManager.put("ScrollBar.trackColor", CLR_BG);

        UIManager.put("Table.background", CLR_SURFACE);
        UIManager.put("Table.foreground", CLR_TEXT);
        UIManager.put("Table.selectionBackground", CLR_PRIMARY);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", CLR_BORDER);
        UIManager.put("TableHeader.background", CLR_PANEL);
        UIManager.put("TableHeader.foreground", CLR_MUTED);

        UIManager.put("TextField.background", CLR_BG);
        UIManager.put("TextField.foreground", CLR_TEXT);
        UIManager.put("TextField.caretForeground", CLR_TEXT);
        UIManager.put("PasswordField.background", CLR_BG);
        UIManager.put("PasswordField.foreground", CLR_TEXT);
        UIManager.put("PasswordField.caretForeground", CLR_TEXT);

        UIManager.put("ProgressBar.background", CLR_PANEL);
        UIManager.put("ProgressBar.foreground", CLR_SUCCESS);
        UIManager.put("ProgressBar.arc", 6);

        UIManager.put("ScrollPane.background", CLR_BG);
        UIManager.put("Viewport.background", CLR_SURFACE);
        UIManager.put("CheckBox.foreground", CLR_MUTED);
        UIManager.put("TabbedPane.background", CLR_PANEL);
        UIManager.put("TabbedPane.foreground", CLR_TEXT);
    }
}


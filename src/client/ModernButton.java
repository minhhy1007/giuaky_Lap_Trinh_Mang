package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * COMPONENT NÚT BẤM HIỆN ĐẠI (MODERN BUTTON)
 *
 * Tự vẽ đồ họa 100% bằng Graphics2D (Anti-aliasing):
 * - Loại bỏ hoàn toàn lỗi nền trắng mặc định của Java Swing / Windows Look and Feel.
 * - Đảm bảo độ tương phản cao tuyệt đối: Chữ luôn nổi bật trên nền, không bao giờ bị chữ trắng trên nền trắng.
 * - Tự động tính toán màu Hover và Pressed tinh tế.
 * - Xử lý trạng thái Disabled rõ ràng (nền xám đậm #1F2937, chữ xám #6B7280).
 */
public class ModernButton extends JButton {

    private Color normalBg;
    private Color hoverBg;
    private Color pressedBg;
    private Color normalFg;
    private Color disabledBg;
    private Color disabledFg;
    private Color borderColor;
    private int cornerRadius = 6;
    private boolean isHovered = false;
    private boolean isPressed = false;

    public ModernButton(String text) {
        this(text, new Color(0x1F, 0x29, 0x37), new Color(0xF9, 0xFA, 0xFB), new Color(0x37, 0x41, 0x51));
    }

    public ModernButton(String text, Color bg, Color fg) {
        this(text, bg, fg, null);
    }

    public ModernButton(String text, Color bg, Color fg, Color border) {
        super(text);
        this.normalBg = bg;
        this.normalFg = fg;
        this.borderColor = border;

        this.hoverBg = calculateHover(bg);
        this.pressedBg = calculatePressed(bg);
        this.disabledBg = new Color(0x1F, 0x29, 0x37);
        this.disabledFg = new Color(0x6B, 0x72, 0x80);

        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(fg);
        setFont(new Font("Segoe UI", Font.BOLD, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    private Color calculateHover(Color c) {
        if (c == null) return new Color(0x37, 0x41, 0x51);
        if (c.equals(new Color(0x3B, 0x82, 0xF6))) return new Color(0x25, 0x63, 0xEB); // Primary Hover
        if (c.equals(new Color(0x22, 0xC5, 0x5E))) return new Color(0x16, 0xA3, 0x4A); // Success Hover
        if (c.equals(new Color(0xEF, 0x44, 0x44))) return new Color(0xDC, 0x26, 0x26); // Danger Hover
        if (c.equals(new Color(0xF5, 0x9E, 0x0B))) return new Color(0xD9, 0x77, 0x06); // Warning Hover
        return new Color(0x37, 0x41, 0x51); // Neutral Surface Hover
    }

    private Color calculatePressed(Color c) {
        if (c == null) return new Color(0x11, 0x18, 0x27);
        if (c.equals(new Color(0x3B, 0x82, 0xF6))) return new Color(0x1D, 0x4E, 0xD8);
        return c.darker();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setCustomColors(Color bg, Color fg, Color border) {
        this.normalBg = bg;
        this.normalFg = fg;
        this.borderColor = border;
        this.hoverBg = calculateHover(bg);
        this.pressedBg = calculatePressed(bg);
        setForeground(fg);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Nền nút
        Color currentBg;
        if (!isEnabled()) {
            currentBg = disabledBg;
        } else if (isPressed) {
            currentBg = pressedBg;
        } else if (isHovered) {
            currentBg = hoverBg;
        } else {
            currentBg = normalBg;
        }

        g2.setColor(currentBg);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // 2. Viền nút
        if (borderColor != null) {
            g2.setColor(isEnabled() ? borderColor : new Color(0x37, 0x41, 0x51));
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
        }

        // 3. Văn bản nút với màu tương phản cao
        Color currentFg = isEnabled() ? normalFg : disabledFg;
        g2.setColor(currentFg);
        g2.setFont(getFont());

        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(getText())) / 2;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}

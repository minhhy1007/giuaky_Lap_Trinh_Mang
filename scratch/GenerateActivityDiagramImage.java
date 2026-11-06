package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateActivityDiagramImage {
    public static void main(String[] args) {
        try {
            int w = 2338; // A4 Landscape 200 DPI
            int h = 1654;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            // Title
            g.setColor(Color.BLACK);
            g.setFont(new Font("Segoe UI", Font.BOLD, 32));
            String title = "SƠ ĐỒ HOẠT ĐỘNG (ACTIVITY DIAGRAM) — CHỨC NĂNG ĐĂNG NHẬP";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (w - fm.stringWidth(title)) / 2, 60);

            // Swimlanes
            int sy = 90;
            int sh = 1500;
            int l1_w = 640;
            int l2_w = 1050;
            int l3_w = 580;
            int l1_x = 34;
            int l2_x = l1_x + l1_w;
            int l3_x = l2_x + l2_w;

            g.setStroke(new BasicStroke(2.5f));
            // Lane 1
            g.drawRect(l1_x, sy, l1_w, sh);
            g.drawRect(l1_x, sy, l1_w, 45);
            // Lane 2
            g.drawRect(l2_x, sy, l2_w, sh);
            g.drawRect(l2_x, sy, l2_w, 45);
            // Lane 3
            g.drawRect(l3_x, sy, l3_w, sh);
            g.drawRect(l3_x, sy, l3_w, 45);

            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.drawString("NGƯỜI DÙNG (USER)", l1_x + 180, sy + 32);
            g.drawString("HỆ THỐNG (SYSTEM / SERVER)", l2_x + 350, sy + 32);
            g.drawString("CƠ SỞ DỮ LIỆU (DATABASE)", l3_x + 140, sy + 32);

            File out = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/activity_diagram_login.png");
            ImageIO.write(img, "PNG", out);
            System.out.println("Preview scaffold rendered successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


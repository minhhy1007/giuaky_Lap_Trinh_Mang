package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateUseCaseDiagramA4 {

    private static final int WIDTH = 3508;
    private static final int HEIGHT = 2480;

    // Font definitions
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 24);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 30);
    private static final Font FONT_ACTOR = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_UC = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_REL = new Font("Segoe UI", Font.ITALIC, 20);
    private static final Font FONT_ZONE = new Font("Segoe UI", Font.BOLD, 22);

    // Strokes
    private static final Stroke STROKE_BOX = new BasicStroke(4.0f);
    private static final Stroke STROKE_SOLID = new BasicStroke(2.8f);
    private static final Stroke STROKE_DASHED = new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{12.0f, 8.0f}, 0.0f);
    private static final Stroke STROKE_ACTOR = new BasicStroke(3.2f);

    public static void main(String[] args) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();

            // Antialiasing and High Quality Rendering
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // 1. Background: Pure White
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // 2. Main Title Banner at the top
            g2.setColor(Color.BLACK);
            g2.setFont(FONT_TITLE);
            String title = "SƠ ĐỒ USE CASE HỆ THỐNG SECURE FTP CLIENT PRO & SERVER";
            FontMetrics fm = g2.getFontMetrics(FONT_TITLE);
            g2.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 70);

            g2.setFont(FONT_SUBTITLE);
            String subtitle = "Hệ thống Quản lý Lưu trữ, Truyền nhận Tệp tin và Chia sẻ Đám mây (UML 2.x Standard Use Case Diagram)";
            fm = g2.getFontMetrics(FONT_SUBTITLE);
            g2.drawString(subtitle, (WIDTH - fm.stringWidth(subtitle)) / 2, 110);

            // 3. System Boundary Box
            int bx = 620, by = 140, bw = 2780, bh = 2260;
            g2.setColor(Color.BLACK);
            g2.setStroke(STROKE_BOX);
            g2.drawRect(bx, by, bw, bh);

            // Boundary Label Header Box
            g2.setFont(FONT_HEADER);
            String boundaryLabel = "  Ranh giới hệ thống: Secure FTP Client Pro System  ";
            FontMetrics fmh = g2.getFontMetrics(FONT_HEADER);
            int blw = fmh.stringWidth(boundaryLabel);
            g2.setColor(Color.WHITE);
            g2.fillRect(bx + 40, by - 22, blw, 44);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRect(bx + 40, by - 22, blw, 44);
            g2.drawString(boundaryLabel, bx + 40, by + 10);

            // Draw Actors on Left Side
            // Actor 1: Guest (x = 280, y = 460)
            Point actGuest = new Point(280, 430);
            drawActor(g2, actGuest.x, actGuest.y, "Khách vãng lai\n(Guest)");

            // Actor 2: Standard User (x = 280, y = 1200)
            Point actUser = new Point(280, 1170);
            drawActor(g2, actUser.x, actUser.y, "Người dùng\n(Standard User)");

            // Actor 3: Administrator (x = 280, y = 2050)
            Point actAdmin = new Point(280, 2020);
            drawActor(g2, actAdmin.x, actAdmin.y, "Quản trị viên\n(Administrator)");

            // Generalization Actor: Admin is-a User
            drawGeneralization(g2, actAdmin.x, actAdmin.y - 85, actUser.x, actUser.y + 115);

            // Definition of Use Cases (id, x_center, y_center, width, height, text)
            // Column 1: Primary Use Cases directly associated with Actors
            // Auth & Account
            UseCase uc01 = new UseCase("UC01", 1080, 260, 380, 95, "Đăng ký tài khoản");
            UseCase uc02 = new UseCase("UC02", 1080, 400, 380, 95, "Đăng nhập hệ thống");
            UseCase uc03 = new UseCase("UC03", 1080, 560, 380, 95, "Đổi mật khẩu cá nhân");

            // File & Directory Operations
            UseCase uc04 = new UseCase("UC04", 1080, 730, 390, 95, "Duyệt danh sách tệp & thư mục");
            UseCase uc05 = new UseCase("UC05", 1080, 900, 380, 95, "Tải tệp tin lên (Upload)");
            UseCase uc06 = new UseCase("UC06", 1080, 1070, 380, 95, "Tải tệp tin về (Download)");
            UseCase uc07 = new UseCase("UC07", 1080, 1240, 390, 105, "Quản lý thư mục\n(Tạo / Chuyển / Xóa)");
            UseCase uc08 = new UseCase("UC08", 1080, 1410, 380, 95, "Đổi tên & Xóa tệp tin");

            // Admin Operations (Col 1 Lower)
            UseCase uc16 = new UseCase("UC16", 1080, 1680, 390, 105, "Quản trị tài khoản\nngười dùng (User List)");
            UseCase uc17 = new UseCase("UC17", 1080, 1850, 380, 95, "Khóa / Mở khóa tài khoản");
            UseCase uc18 = new UseCase("UC18", 1080, 2010, 380, 95, "Phân quyền vai trò người dùng");
            UseCase uc19 = new UseCase("UC19", 1080, 2180, 380, 95, "Điều chỉnh hạn mức Quota");

            // Column 2: Secondary & Collaboration / Billing Use Cases
            // Sharing
            UseCase uc09 = new UseCase("UC09", 1820, 520, 380, 95, "Tạo liên kết chia sẻ tệp");
            UseCase uc10 = new UseCase("UC10", 1820, 680, 380, 95, "Quản lý & Thu hồi chia sẻ");
            UseCase uc11 = new UseCase("UC11", 1820, 860, 390, 105, "Tải tệp chia sẻ\nqua Token");

            // Billing & Mock Payment
            UseCase uc12 = new UseCase("UC12", 1820, 1100, 380, 95, "Xem & Đăng ký gói cước");
            UseCase uc13 = new UseCase("UC13", 1820, 1270, 380, 95, "Thanh toán mô phỏng (Mock)");
            UseCase uc14 = new UseCase("UC14", 1820, 1440, 380, 95, "Xem lịch sử đơn hàng");

            // Logs
            UseCase uc15 = new UseCase("UC15", 1820, 1610, 390, 95, "Xem nhật ký hoạt động cá nhân");

            // Admin System
            UseCase uc20 = new UseCase("UC20", 1820, 1860, 390, 100, "Quản lý toàn bộ đơn hàng\nhệ thống (Admin)");
            UseCase uc21 = new UseCase("UC21", 1820, 2030, 390, 100, "Kiểm toán nhật ký\ntoàn hệ thống (Admin Logs)");
            UseCase uc22 = new UseCase("UC22", 1820, 2200, 390, 100, "Xem thống kê KPI\n& Doanh thu (AdminStats)");

            // Column 3: Included & Extended Use Cases
            // Associated with Sharing
            UseCase ucIncToken = new UseCase("UC_INC1", 2620, 780, 420, 105, "Xác thực mã Token &\nQuyền truy cập (Validate)");
            UseCase ucExtView  = new UseCase("UC_EXT1", 2620, 940, 420, 105, "Từ chối tải với\nquyền xem (Deny VIEW)");

            // Associated with Upload / Storage
            UseCase ucIncQuota = new UseCase("UC_INC2", 2620, 1120, 430, 105, "Kiểm tra hạn mức Quota\n(Handshake 125/SIZE/150)");
            UseCase ucExtQuota = new UseCase("UC_EXT2", 2620, 1280, 430, 105, "Cảnh báo & Từ chối\nkhi vượt Quota (552)");

            // Associated with Mock Payment
            UseCase ucIncPayQuota = new UseCase("UC_INC3", 2620, 1450, 430, 105, "Cộng dung lượng Quota\ntheo chuẩn ACID");

            // Associated with Admin User Management
            UseCase ucIncProtect = new UseCase("UC_INC4", 2620, 1780, 430, 105, "Bảo vệ tài khoản quản trị\n(Chống tự khóa / hạ quyền)");

            // Draw Associations (Actor to Use Cases)
            // Guest Associations
            drawAssociation(g2, actGuest.x + 45, actGuest.y - 20, uc01.left());
            drawAssociation(g2, actGuest.x + 45, actGuest.y, uc02.left());
            drawAssociation(g2, actGuest.x + 45, actGuest.y + 30, uc11.left());

            // Standard User Associations
            drawAssociation(g2, actUser.x + 45, actUser.y - 90, uc03.left());
            drawAssociation(g2, actUser.x + 45, actUser.y - 65, uc04.left());
            drawAssociation(g2, actUser.x + 45, actUser.y - 40, uc05.left());
            drawAssociation(g2, actUser.x + 45, actUser.y - 15, uc06.left());
            drawAssociation(g2, actUser.x + 45, actUser.y + 15, uc07.left());
            drawAssociation(g2, actUser.x + 45, actUser.y + 45, uc08.left());

            // User to Secondary Col
            drawAssociation(g2, actUser.x + 45, actUser.y - 80, uc09.left());
            drawAssociation(g2, actUser.x + 45, actUser.y - 50, uc10.left());
            drawAssociation(g2, actUser.x + 45, actUser.y - 20, uc11.left());
            drawAssociation(g2, actUser.x + 45, actUser.y + 20, uc12.left());
            drawAssociation(g2, actUser.x + 45, actUser.y + 50, uc13.left());
            drawAssociation(g2, actUser.x + 45, actUser.y + 80, uc14.left());
            drawAssociation(g2, actUser.x + 45, actUser.y + 100, uc15.left());

            // Administrator Associations
            drawAssociation(g2, actAdmin.x + 45, actAdmin.y - 60, uc16.left());
            drawAssociation(g2, actAdmin.x + 45, actAdmin.y - 30, uc17.left());
            drawAssociation(g2, actAdmin.x + 45, actAdmin.y, uc18.left());
            drawAssociation(g2, actAdmin.x + 45, actAdmin.y + 35, uc19.left());

            drawAssociation(g2, actAdmin.x + 45, actAdmin.y - 20, uc20.left());
            drawAssociation(g2, actAdmin.x + 45, actAdmin.y + 15, uc21.left());
            drawAssociation(g2, actAdmin.x + 45, actAdmin.y + 50, uc22.left());

            // Draw Include & Extend Relationships
            // 1. Upload include Quota Check
            drawInclude(g2, uc05.right(), ucIncQuota.left());

            // 2. Upload extend Quota Rejection
            drawExtend(g2, ucExtQuota.left(), uc05.right());

            // 3. Download Shared include Token Check
            drawInclude(g2, uc11.right(), ucIncToken.left());

            // 4. Download Shared extend Deny View
            drawExtend(g2, ucExtView.left(), uc11.right());

            // 5. Mock Payment include ACID Quota Upgrade
            drawInclude(g2, uc13.right(), ucIncPayQuota.left());

            // 6. User Lock / Role include Self-Protect
            drawInclude(g2, uc17.right(), ucIncProtect.left());

            // Draw All Use Cases (Ellipses on top of lines)
            UseCase[] allUCs = {
                    uc01, uc02, uc03, uc04, uc05, uc06, uc07, uc08,
                    uc09, uc10, uc11, uc12, uc13, uc14, uc15,
                    uc16, uc17, uc18, uc19, uc20, uc21, uc22,
                    ucIncToken, ucExtView, ucIncQuota, ucExtQuota,
                    ucIncPayQuota, ucIncProtect
            };

            for (UseCase uc : allUCs) {
                drawUseCase(g2, uc);
            }

            // Legend / Chú thích góc dưới ranh giới
            drawLegend(g2, bx + 1600, by + bh - 160);

            g2.dispose();

            // Save Image
            File outputDir = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs");
            if (!outputDir.exists()) outputDir.mkdirs();
            File outputFile = new File(outputDir, "use_case_diagram_a4.png");
            ImageIO.write(image, "PNG", outputFile);
            System.out.println("SUCCESS: Use Case Diagram saved to " + outputFile.getAbsolutePath());

            // Also copy to brain artifacts directory for preview
            File artifactDir = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead");
            File artifactFile = new File(artifactDir, "use_case_diagram_a4.png");
            ImageIO.write(image, "PNG", artifactFile);
            System.out.println("SUCCESS: Artifact copy saved to " + artifactFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: Draw Stick Figure Actor
    private static void drawActor(Graphics2D g, int x, int y, String label) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_ACTOR);

        // Head (circle)
        int r = 26;
        g.drawOval(x - r, y - 75, r * 2, r * 2);

        // Spine (torso)
        g.drawLine(x, y - 75 + r * 2, x, y + 5);

        // Arms (horizontal line)
        g.drawLine(x - 38, y - 25, x + 38, y - 25);

        // Legs (inverted V)
        g.drawLine(x, y + 5, x - 32, y + 65);
        g.drawLine(x, y + 5, x + 32, y + 65);

        // Label
        g.setFont(FONT_ACTOR);
        FontMetrics fm = g.getFontMetrics(FONT_ACTOR);
        String[] lines = label.split("\n");
        int ly = y + 98;
        for (String l : lines) {
            int lx = x - fm.stringWidth(l) / 2;
            g.drawString(l, lx, ly);
            ly += fm.getHeight() - 4;
        }
    }

    // Helper: Draw Generalization Arrow (Hollow Triangle)
    private static void drawGeneralization(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_SOLID);
        // Arrow head at (x2, y2)
        int arrowHeight = 22;
        int arrowWidth = 14;

        g.drawLine(x1, y1, x2, y2 + arrowHeight);

        Polygon poly = new Polygon();
        poly.addPoint(x2, y2);
        poly.addPoint(x2 - arrowWidth, y2 + arrowHeight);
        poly.addPoint(x2 + arrowWidth, y2 + arrowHeight);

        g.setColor(Color.WHITE);
        g.fillPolygon(poly);
        g.setColor(Color.BLACK);
        g.drawPolygon(poly);

        // Label "Kế thừa (is-a)"
        g.setFont(FONT_REL);
        g.drawString("<<generalization>>", x1 - 85, (y1 + y2) / 2 + 10);
    }

    // Helper: Draw Association Line
    private static void drawAssociation(Graphics2D g, int x1, int y1, Point target) {
        g.setColor(new Color(40, 40, 40));
        g.setStroke(STROKE_SOLID);
        g.drawLine(x1, y1, target.x, target.y);
    }

    // Helper: Draw Include Relationship (Dashed line with open arrow from base to included)
    private static void drawInclude(Graphics2D g, Point from, Point to) {
        drawDashedArrow(g, from, to, "<<include>>");
    }

    // Helper: Draw Extend Relationship (Dashed line with open arrow from extending to base)
    private static void drawExtend(Graphics2D g, Point from, Point to) {
        drawDashedArrow(g, from, to, "<<extend>>");
    }

    private static void drawDashedArrow(Graphics2D g, Point from, Point to, String label) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_DASHED);
        g.drawLine(from.x, from.y, to.x, to.y);

        // Arrow head at 'to'
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowLength = 16;
        g.setStroke(STROKE_SOLID);
        g.drawLine(to.x, to.y, (int) (to.x - arrowLength * Math.cos(angle - Math.PI / 6)),
                (int) (to.y - arrowLength * Math.sin(angle - Math.PI / 6)));
        g.drawLine(to.x, to.y, (int) (to.x - arrowLength * Math.cos(angle + Math.PI / 6)),
                (int) (to.y - arrowLength * Math.sin(angle + Math.PI / 6)));

        // Label
        int mx = (from.x + to.x) / 2;
        int my = (from.y + to.y) / 2 - 12;
        g.setFont(FONT_REL);
        FontMetrics fm = g.getFontMetrics(FONT_REL);
        int lw = fm.stringWidth(label);

        g.setColor(Color.WHITE);
        g.fillRect(mx - lw / 2 - 4, my - fm.getAscent() + 2, lw + 8, fm.getHeight());
        g.setColor(Color.BLACK);
        g.drawString(label, mx - lw / 2, my);
    }

    // Helper: Draw Use Case (Ellipse with clean border and centered text)
    private static void drawUseCase(Graphics2D g, UseCase uc) {
        // Fill white to hide background lines
        g.setColor(Color.WHITE);
        g.fillOval(uc.x - uc.w / 2, uc.y - uc.h / 2, uc.w, uc.h);

        // Draw crisp border
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.6f));
        g.drawOval(uc.x - uc.w / 2, uc.y - uc.h / 2, uc.w, uc.h);

        // Draw centered text
        g.setFont(FONT_UC);
        FontMetrics fm = g.getFontMetrics(FONT_UC);
        String[] lines = uc.text.split("\n");
        int totalH = lines.length * (fm.getHeight() - 4);
        int startY = uc.y - totalH / 2 + fm.getAscent() - 2;

        for (String l : lines) {
            int lx = uc.x - fm.stringWidth(l) / 2;
            g.drawString(l, lx, startY);
            startY += fm.getHeight() - 4;
        }
    }

    // Helper: Draw Legend
    private static void drawLegend(Graphics2D g, int x, int y) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, 1100, 130);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.0f));
        g.drawRect(x, y, 1100, 130);

        g.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g.drawString("CHÚ THÍCH KÝ HIỆU UML 2.X (LEGEND):", x + 20, y + 32);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        // Solid line: Association
        g.setStroke(STROKE_SOLID);
        g.drawLine(x + 25, y + 68, x + 95, y + 68);
        g.drawString("Association (Liên kết tác nhân - ca sử dụng)", x + 110, y + 74);

        // Dashed line: include/extend
        g.setStroke(STROKE_DASHED);
        g.drawLine(x + 25, y + 105, x + 95, y + 105);
        g.setStroke(STROKE_SOLID);
        g.drawLine(x + 95, y + 105, x + 85, y + 100);
        g.drawLine(x + 95, y + 105, x + 85, y + 110);
        g.drawString("<<include>> / <<extend>> (Quan hệ phụ thuộc ca sử dụng)", x + 110, y + 111);

        // Hollow triangle: Generalization
        Polygon p = new Polygon();
        p.addPoint(x + 650, y + 68);
        p.addPoint(x + 630, y + 60);
        p.addPoint(x + 630, y + 76);
        g.setColor(Color.WHITE);
        g.fillPolygon(p);
        g.setColor(Color.BLACK);
        g.drawPolygon(p);
        g.drawLine(x + 590, y + 68, x + 630, y + 68);
        g.drawString("Generalization (Kế thừa Actor)", x + 665, y + 74);
    }

    // Helper Class: UseCase
    static class UseCase {
        final String id;
        final int x, y, w, h;
        final String text;

        UseCase(String id, int x, int y, int w, int h, String text) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
        }

        Point left() {
            return new Point(x - w / 2, y);
        }

        Point right() {
            return new Point(x + w / 2, y);
        }
    }
}


package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateActivityDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 24);
    private static final Font FONT_LANE_HEADER = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_ACTION = new Font("Segoe UI", Font.BOLD, 21);
    private static final Font FONT_DECISION = new Font("Segoe UI", Font.BOLD, 19);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD | Font.ITALIC, 19);
    private static final Font FONT_LEGEND = new Font("Segoe UI", Font.PLAIN, 18);

    // Strokes
    private static final Stroke STROKE_LANE = new BasicStroke(3.5f);
    private static final Stroke STROKE_BOX = new BasicStroke(2.6f);
    private static final Stroke STROKE_EDGE = new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke STROKE_LOOP = new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    public static void main(String[] args) {
        try {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            // Quality hints
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // 1. Background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, W, H);

            // 2. Main Title
            g.setColor(Color.BLACK);
            g.setFont(FONT_TITLE);
            String title = "SƠ ĐỒ HOẠT ĐỘNG (ACTIVITY DIAGRAM) — CHỨC NĂNG ĐĂNG NHẬP";
            FontMetrics fm = g.getFontMetrics(FONT_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 70);

            g.setFont(FONT_SUBTITLE);
            String sub = "Quy trình xác thực tài khoản 3 làn bơi (3 Swimlanes) theo chuẩn UML 2.x — Hệ thống Secure FTP Client Pro";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 115);

            // 3. Swimlanes Setup
            int sx = 80;
            int sy = 150;
            int l1_w = 900;
            int l2_w = 1480;
            int l3_w = 880;
            int sh = 2250;

            int l1_x = sx;
            int l2_x = l1_x + l1_w;
            int l3_x = l2_x + l2_w;

            int c1 = l1_x + l1_w / 2; // 530
            int c2 = l2_x + l2_w / 2; // 1720
            int c3 = l3_x + l3_w / 2; // 2900

            // Draw Lanes
            drawSwimlane(g, l1_x, sy, l1_w, sh, "NGƯỜI DÙNG (USER)");
            drawSwimlane(g, l2_x, sy, l2_w, sh, "HỆ THỐNG (SYSTEM / SERVER)");
            drawSwimlane(g, l3_x, sy, l3_w, sh, "CƠ SỞ DỮ LIỆU (DATABASE / MYSQL)");

            // --- Y COORD SCHEDULE ---
            int y_init = 250;
            int y_step12 = 360;
            int y_step3 = 510;
            int y_step45 = 670;
            int y_dec1 = 830;
            int y_step78 = 1010;
            int y_step9_10 = 1190;
            int y_dec2 = 1370;
            int y_step12_stat = 1540;
            int y_dec3 = 1710;
            int y_step15 = 1890;
            int y_step16 = 2070;
            int y_final = 2240;

            // Loop track X coord (outside boxes in Lane 2)
            int loop1_x = l2_x + 60;  // 1040
            int loop2_x = l2_x + 100; // 1080

            // --- DRAW CONNECTING EDGES FIRST (Underneath Nodes) ---
            // Init -> Step 1
            drawArrow(g, c1, y_init + 22, c1, y_step12 - 45);

            // Step 1 -> Step 2 (Lane 1 -> Lane 2)
            drawArrow(g, c1 + 225, y_step12, c2 - 225, y_step12);

            // Step 2 -> Step 3 (Lane 2 -> Lane 1)
            drawOrthogonalArrow(g, new Point(c2, y_step12 + 45), new Point(c1 + 225, y_step3),
                    new Point[]{new Point(c2, y_step3), new Point(c1 + 225, y_step3)});

            // Step 3 -> Step 4
            drawArrow(g, c1, y_step3 + 45, c1, y_step45 - 45);

            // Step 4 -> Step 5 (Lane 1 -> Lane 2)
            drawArrow(g, c1 + 225, y_step45, c2 - 225, y_step45);

            // Step 5 -> Decision 1
            drawArrow(g, c2, y_step45 + 45, c2, y_dec1 - 45);

            // Decision 1 -> [Không hợp lệ] -> Step 6 (Notify invalid)
            int s6_x = c2 - 430;
            drawArrowWithLabel(g, c2 - 60, y_dec1, s6_x + 210, y_dec1, "[Không hợp lệ]", -15);

            // Step 6 -> Loop back to Step 3 (Nhập lại)
            drawLoopBack(g, s6_x - 210, y_dec1, loop1_x, c1 + 225, y_step3 - 15);

            // Decision 1 -> [Hợp lệ] -> Step 7
            drawArrowWithLabel(g, c2, y_dec1 + 45, c2, y_step78 - 45, "[Hợp lệ]", 15);

            // Step 7 -> Step 7.1 (System -> DB)
            drawArrow(g, c2 + 225, y_step78, c3 - 225, y_step78);

            // Step 7.1 -> Step 7.2 (DB internal)
            drawArrow(g, c3, y_step78 + 45, c3, y_step9_10 - 45);

            // Step 7.2 -> Step 8 (DB -> System)
            drawArrow(g, c3 - 225, y_step9_10, c2 + 225, y_step9_10);

            // Step 8 -> Decision 2
            drawArrow(g, c2, y_step9_10 + 45, c2, y_dec2 - 45);

            // Decision 2 -> [Sai] -> Step 9 (Notify wrong creds)
            int s9_x = c2 - 430;
            drawArrowWithLabel(g, c2 - 60, y_dec2, s9_x + 210, y_dec2, "[Sai thông tin]", -15);

            // Step 9 -> Loop back to Step 3 (Nhập lại)
            drawLoopBack(g, s9_x - 210, y_dec2, loop2_x, c1 + 225, y_step3 + 15);

            // Decision 2 -> [Đúng] -> Step 10
            drawArrowWithLabel(g, c2, y_dec2 + 45, c2, y_step12_stat - 45, "[Đúng thông tin]", 15);

            // Step 10 -> Decision 3
            drawArrow(g, c2, y_step12_stat + 45, c2, y_dec3 - 45);

            // Decision 3 -> [Bị khóa] -> Step 11
            int s11_x = c2 - 430;
            drawArrowWithLabel(g, c2 - 60, y_dec3, s11_x + 210, y_dec3, "[Bị khóa - LOCKED]", -15);

            // Step 11 -> Final Node (Fail/Locked)
            drawArrow(g, s11_x, y_dec3 + 45, s11_x, y_step15);

            // Decision 3 -> [Hoạt động] -> Step 12
            int s12_x = c2 + 430;
            drawArrowWithLabel(g, c2 + 60, y_dec3, s12_x - 210, y_dec3, "[Hoạt động - ACTIVE]", -15);

            // Step 12 -> Step 13
            drawArrow(g, s12_x, y_dec3 + 45, s12_x, y_step15 - 45);

            // Step 13 -> Step 14 (System -> User)
            drawOrthogonalArrow(g, new Point(s12_x, y_step15 + 45), new Point(c1 + 225, y_step16),
                    new Point[]{new Point(s12_x, y_step16), new Point(c1 + 225, y_step16)});

            // Step 14 -> Final Node (Success)
            drawArrow(g, c1, y_step16 + 45, c1, y_final - 24);

            // --- DRAW ALL NODES (Action, Decision, Initial, Final) ---
            // Initial Node
            drawInitialNode(g, c1, y_init);

            // Action Nodes
            // Lane 1
            drawAction(g, c1, y_step12, 450, 90, "1. Truy cập giao diện\nđăng nhập (LoginFrame)");
            drawAction(g, c1, y_step3, 450, 90, "3. Nhập Email / Username\n& Mật khẩu");
            drawAction(g, c1, y_step45, 450, 90, "4. Nhấn nút 'Đăng nhập'");
            drawAction(g, c1, y_step16, 450, 90, "14. Truy cập giao diện chính\n(MainFrame / AdminDashboard)");
            drawFinalNode(g, c1, y_final, "Đăng nhập thành công");

            // Lane 2
            drawAction(g, c2, y_step12, 450, 90, "2. Hiển thị biểu mẫu\nđăng nhập cho người dùng");
            drawAction(g, c2, y_step45, 450, 90, "5. Kiểm tra dữ liệu đầu vào\n(Validation: trống, format)");
            drawDecision(g, c2, y_dec1, 120, 85, "Dữ liệu\nhợp lệ?");
            drawAction(g, s6_x, y_dec1, 420, 90, "6. Thông báo lỗi định dạng\n& yêu cầu nhập lại");

            drawAction(g, c2, y_step78, 450, 90, "7. Gửi truy vấn tìm kiếm\ntài khoản sang CSDL");
            drawAction(g, c2, y_step9_10, 450, 90, "8. Kiểm tra & đối soát\nmật khẩu băm (SHA-256)");
            drawDecision(g, c2, y_dec2, 120, 85, "Mật khẩu\nđúng?");
            drawAction(g, s9_x, y_dec2, 420, 90, "9. Thông báo sai tài khoản\nhoặc mật khẩu");

            drawAction(g, c2, y_step12_stat, 450, 90, "10. Kiểm tra trạng thái\ntài khoản (ACTIVE / LOCKED)");
            drawDecision(g, c2, y_dec3, 120, 85, "Trạng thái\ntài khoản?");
            drawAction(g, s11_x, y_dec3, 420, 90, "11. Thông báo tài khoản\nbị khóa & từ chối");
            drawFinalNode(g, s11_x, y_step15 + 24, "Từ chối / Kết thúc");

            drawAction(g, s12_x, y_dec3, 420, 90, "12. Khởi tạo phiên (Session)\n& Cấp quyền đăng nhập");
            drawAction(g, s12_x, y_step15, 420, 90, "13. Xác định vai trò (Role)\n& Điều hướng giao diện");

            // Lane 3
            drawAction(g, c3, y_step78, 450, 90, "7.1. Tìm kiếm bản ghi User\ntheo Username / Email");
            drawAction(g, c3, y_step9_10, 450, 90, "7.2. Trả về Hash, Salt,\nTrạng thái & Vai trò");

            // Draw Legend at the bottom right
            drawLegend(g, l3_x + 50, sy + sh - 210, 780, 180);

            g.dispose();

            // Save to files
            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/activity_diagram_login.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/activity_diagram_login.png");
            ImageIO.write(img, "PNG", outBrain);
            System.out.println("SUCCESS: Saved Brain artifact copy to " + outBrain.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: Draw Swimlane
    private static void drawSwimlane(Graphics2D g, int x, int y, int w, int h, String title) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_LANE);
        g.drawRect(x, y, w, h);

        // Header separator
        int headerH = 60;
        g.drawLine(x, y + headerH, x + w, y + headerH);

        g.setFont(FONT_LANE_HEADER);
        FontMetrics fm = g.getFontMetrics(FONT_LANE_HEADER);
        g.drawString(title, x + (w - fm.stringWidth(title)) / 2, y + (headerH + fm.getAscent() - 6) / 2);
    }

    // Helper: Draw Action Node
    private static void drawAction(Graphics2D g, int cx, int cy, int w, int h, String text) {
        int x = cx - w / 2;
        int y = cy - h / 2;

        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 28, 28);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_BOX);
        g.drawRoundRect(x, y, w, h, 28, 28);

        g.setFont(FONT_ACTION);
        FontMetrics fm = g.getFontMetrics(FONT_ACTION);
        String[] lines = text.split("\n");
        int totalH = lines.length * (fm.getHeight() - 2);
        int startY = cy - totalH / 2 + fm.getAscent();

        for (String line : lines) {
            int lx = cx - fm.stringWidth(line) / 2;
            g.drawString(line, lx, startY);
            startY += fm.getHeight() - 2;
        }
    }

    // Helper: Draw Decision Node (Rhombus)
    private static void drawDecision(Graphics2D g, int cx, int cy, int w, int h, String text) {
        Polygon p = new Polygon();
        p.addPoint(cx, cy - h / 2);
        p.addPoint(cx + w / 2, cy);
        p.addPoint(cx, cy + h / 2);
        p.addPoint(cx - w / 2, cy);

        g.setColor(Color.WHITE);
        g.fillPolygon(p);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_BOX);
        g.drawPolygon(p);

        g.setFont(FONT_DECISION);
        FontMetrics fm = g.getFontMetrics(FONT_DECISION);
        String[] lines = text.split("\n");
        int totalH = lines.length * (fm.getHeight() - 2);
        int startY = cy - totalH / 2 + fm.getAscent();

        for (String line : lines) {
            int lx = cx - fm.stringWidth(line) / 2;
            g.drawString(line, lx, startY);
            startY += fm.getHeight() - 2;
        }
    }

    // Helper: Draw Initial Node
    private static void drawInitialNode(Graphics2D g, int cx, int cy) {
        int r = 22;
        g.setColor(Color.BLACK);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    // Helper: Draw Final Node (Bullseye)
    private static void drawFinalNode(Graphics2D g, int cx, int cy, String label) {
        int r1 = 26;
        int r2 = 17;

        g.setColor(Color.WHITE);
        g.fillOval(cx - r1, cy - r1, r1 * 2, r1 * 2);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.8f));
        g.drawOval(cx - r1, cy - r1, r1 * 2, r1 * 2);

        g.fillOval(cx - r2, cy - r2, r2 * 2, r2 * 2);

        if (label != null && !label.isEmpty()) {
            g.setFont(FONT_LABEL);
            FontMetrics fm = g.getFontMetrics(FONT_LABEL);
            g.drawString(label, cx - fm.stringWidth(label) / 2, cy + r1 + 26);
        }
    }

    // Helper: Draw Straight Arrow
    private static void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_EDGE);
        g.drawLine(x1, y1, x2, y2);
        drawArrowHead(g, x1, y1, x2, y2);
    }

    // Helper: Draw Arrow with Label
    private static void drawArrowWithLabel(Graphics2D g, int x1, int y1, int x2, int y2, String label, int labelOffset) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_EDGE);
        g.drawLine(x1, y1, x2, y2);
        drawArrowHead(g, x1, y1, x2, y2);

        // Draw Label
        g.setFont(FONT_LABEL);
        FontMetrics fm = g.getFontMetrics(FONT_LABEL);
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2 + labelOffset;

        int lw = fm.stringWidth(label);
        g.setColor(Color.WHITE);
        g.fillRect(mx - lw / 2 - 4, my - fm.getAscent() + 2, lw + 8, fm.getHeight());
        g.setColor(Color.BLACK);
        g.drawString(label, mx - lw / 2, my);
    }

    // Helper: Draw Orthogonal Arrow
    private static void drawOrthogonalArrow(Graphics2D g, Point start, Point end, Point[] waypoints) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_EDGE);

        Point prev = start;
        for (Point pt : waypoints) {
            g.drawLine(prev.x, prev.y, pt.x, pt.y);
            prev = pt;
        }
        g.drawLine(prev.x, prev.y, end.x, end.y);
        drawArrowHead(g, prev.x, prev.y, end.x, end.y);
    }

    // Helper: Draw Loopback Arrow
    private static void drawLoopBack(Graphics2D g, int startX, int startY, int trackX, int targetX, int targetY) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_LOOP);

        // 1. Go left to trackX
        g.drawLine(startX, startY, trackX, startY);
        // 2. Go up to targetY
        g.drawLine(trackX, startY, trackX, targetY);
        // 3. Go left to targetX
        g.drawLine(trackX, targetY, targetX, targetY);

        drawArrowHead(g, trackX, targetY, targetX, targetY);
    }

    // Helper: Draw Filled Triangle Arrowhead
    private static void drawArrowHead(Graphics2D g, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int len = 15;
        Polygon poly = new Polygon();
        poly.addPoint(x2, y2);
        poly.addPoint((int) (x2 - len * Math.cos(angle - Math.PI / 6)), (int) (y2 - len * Math.sin(angle - Math.PI / 6)));
        poly.addPoint((int) (x2 - len * Math.cos(angle + Math.PI / 6)), (int) (y2 - len * Math.sin(angle + Math.PI / 6)));

        g.setColor(Color.BLACK);
        g.fillPolygon(poly);
    }

    // Helper: Draw Legend
    private static void drawLegend(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.0f));
        g.drawRect(x, y, w, h);

        g.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g.drawString("CHÚ THÍCH KÝ HIỆU (UML 2.X LEGEND):", x + 25, y + 34);

        g.setFont(FONT_LEGEND);
        // Initial node symbol
        g.fillOval(x + 30, y + 55, 20, 20);
        g.drawString("Initial Node (Bắt đầu)", x + 65, y + 71);

        // Action node symbol
        g.drawRoundRect(x + 300, y + 54, 75, 26, 10, 10);
        g.drawString("Action (Hoạt động)", x + 390, y + 71);

        // Decision symbol
        Polygon p = new Polygon();
        p.addPoint(x + 580, y + 53);
        p.addPoint(x + 595, y + 66);
        p.addPoint(x + 580, y + 79);
        p.addPoint(x + 565, y + 66);
        g.drawPolygon(p);
        g.drawString("Decision (Rẽ nhánh)", x + 610, y + 71);

        // Control flow arrow
        g.drawLine(x + 30, y + 120, x + 100, y + 120);
        drawArrowHead(g, x + 30, y + 120, x + 100, y + 120);
        g.drawString("Control Flow (Luồng điều khiển)", x + 115, y + 126);

        // Final node symbol
        g.drawOval(x + 400, y + 110, 24, 24);
        g.fillOval(x + 404, y + 114, 16, 16);
        g.drawString("Activity Final (Kết thúc)", x + 440, y + 126);
    }
}


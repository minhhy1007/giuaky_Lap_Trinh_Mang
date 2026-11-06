package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateUploadActivityDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 24);
    private static final Font FONT_LANE_HEADER = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_ACTION = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_DECISION = new Font("Segoe UI", Font.BOLD, 18);
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

            // Quality rendering hints
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // 1. Clean White Background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, W, H);

            // 2. Main Title & Subtitle
            g.setColor(Color.BLACK);
            g.setFont(FONT_TITLE);
            String title = "SƠ ĐỒ HOẠT ĐỘNG (ACTIVITY DIAGRAM) — CHỨC NĂNG TẢI TỆP (FILE UPLOAD)";
            FontMetrics fm = g.getFontMetrics(FONT_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 70);

            g.setFont(FONT_SUBTITLE);
            String sub = "Quy trình kiểm soát & truyền tải tệp 3 làn bơi (3 Swimlanes) theo chuẩn UML 2.x — Hệ thống Secure FTP Client Pro";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 115);

            // 3. Swimlanes Setup (A4 Landscape total: width=3348, height=2260, sx=80, sy=150)
            int sx = 80;
            int sy = 150;
            int l1_w = 900;
            int l2_w = 1560;
            int l3_w = 888;
            int sh = 2260;

            int l1_x = sx;
            int l2_x = l1_x + l1_w;
            int l3_x = l2_x + l2_w;

            int c1 = l1_x + l1_w / 2; // 530
            int c2 = l2_x + l2_w / 2; // 1760
            int c3 = l3_x + l3_w / 2; // 2984

            // Draw Lanes
            drawSwimlane(g, l1_x, sy, l1_w, sh, "NGƯỜI DÙNG (USER)");
            drawSwimlane(g, l2_x, sy, l2_w, sh, "HỆ THỐNG (SYSTEM / SERVER)");
            drawSwimlane(g, l3_x, sy, l3_w, sh, "CƠ SỞ DỮ LIỆU (DATABASE / MYSQL)");

            // --- Y COORD SCHEDULE ---
            int y_init = 230;
            int y_step1_2 = 330;
            int y_step3 = 475;
            int y_step4_5 = 625;
            int y_dec1 = 765;
            int y_step7 = 905;
            int y_dec2 = 1045;
            int y_step10 = 1185;
            int y_dec3 = 1325;
            int y_step12 = 1465;
            int y_dec4 = 1605;
            int y_step14 = 1745;
            int y_dec5 = 1885;
            int y_step17 = 2025;
            int y_step18 = 2175;
            int y_final = 2335;

            // Box dimensions
            int bw = 460;
            int bh = 82;

            // Error box X coordinate in Lane 2 (left of center c2)
            int err_x = c2 - 470;
            int abort_x = c2 + 480;

            // Loop track X coords (in left margin of Lane 1)
            int loop1_x = l1_x + 55;
            int loop2_x = l1_x + 90;
            int loop3_x = l1_x + 125;
            int loop4_x = l1_x + 160;

            // --- DRAW CONNECTING EDGES FIRST (Underneath Nodes) ---
            // Init -> Step 1
            drawArrow(g, c1, y_init + 22, c1, y_step1_2 - bh / 2);

            // Step 1 -> Step 2 (Lane 1 -> Lane 2)
            drawArrow(g, c1 + bw / 2, y_step1_2, c2 - bw / 2, y_step1_2);

            // Step 2 -> Step 3 (Lane 2 -> Lane 1)
            drawOrthogonalArrow(g, new Point(c2, y_step1_2 + bh / 2), new Point(c1 + bw / 2, y_step3),
                    new Point[]{new Point(c2, y_step3), new Point(c1 + bw / 2, y_step3)});

            // Step 3 -> Step 4
            drawArrow(g, c1, y_step3 + bh / 2, c1, y_step4_5 - bh / 2);

            // Step 4 -> Step 5 (Lane 1 -> Lane 2)
            drawArrow(g, c1 + bw / 2, y_step4_5, c2 - bw / 2, y_step4_5);

            // Step 5 -> Decision 1
            drawArrow(g, c2, y_step4_5 + bh / 2, c2, y_dec1 - 42);

            // Decision 1 -> [Chưa đăng nhập] -> Step 6 (Yêu cầu đăng nhập) -> Final Abort
            drawArrowWithLabel(g, c2 + 65, y_dec1, abort_x - bw / 2, y_dec1, "[Chưa đăng nhập]", -15);
            drawArrow(g, abort_x, y_dec1 + bh / 2, abort_x, y_dec1 + bh / 2 + 55);

            // Decision 1 -> [Đã đăng nhập] -> Step 7 (Kiểm tra tệp được chọn)
            drawArrowWithLabel(g, c2, y_dec1 + 42, c2, y_step7 - bh / 2, "[Đã đăng nhập]", 15);

            // Step 7 -> Decision 2 (Đã chọn tệp?)
            drawArrow(g, c2, y_step7 + bh / 2, c2, y_dec2 - 42);

            // Decision 2 -> [Chưa chọn] -> Step 9 (Cảnh báo yêu cầu chọn tệp)
            drawArrowWithLabel(g, c2 - 65, y_dec2, err_x + bw / 2, y_dec2, "[Chưa chọn tệp]", -15);
            // Step 9 -> Loop back to Step 3
            drawLoopBack(g, err_x - bw / 2, y_dec2, loop1_x, c1 - bw / 2, y_step3 - 15);

            // Decision 2 -> [Đã chọn] -> Step 10 (Kiểm tra định dạng)
            drawArrowWithLabel(g, c2, y_dec2 + 42, c2, y_step10 - bh / 2, "[Đã chọn tệp]", 15);

            // Step 10 -> Decision 3 (Định dạng hợp lệ?)
            drawArrow(g, c2, y_step10 + bh / 2, c2, y_dec3 - 42);

            // Decision 3 -> [Không hợp lệ] -> Step 11 (Báo lỗi định dạng)
            drawArrowWithLabel(g, c2 - 65, y_dec3, err_x + bw / 2, y_dec3, "[Không hợp lệ]", -15);
            // Step 11 -> Loop back to Step 3
            drawLoopBack(g, err_x - bw / 2, y_dec3, loop2_x, c1 - bw / 2, y_step3 - 5);

            // Decision 3 -> [Hợp lệ] -> Step 12 (Kiểm tra Quota)
            drawArrowWithLabel(g, c2, y_dec3 + 42, c2, y_step12 - bh / 2, "[Hợp lệ]", 15);

            // Step 12 -> Decision 4 (Dung lượng <= Quota?)
            drawArrow(g, c2, y_step12 + bh / 2, c2, y_dec4 - 42);

            // Decision 4 -> [Vượt Quota] -> Step 13 (Báo lỗi Quota)
            drawArrowWithLabel(g, c2 - 65, y_dec4, err_x + bw / 2, y_dec4, "[Vượt Quota]", -15);
            // Step 13 -> Loop back to Step 3
            drawLoopBack(g, err_x - bw / 2, y_dec4, loop3_x, c1 - bw / 2, y_step3 + 5);

            // Decision 4 -> [Đạt yêu cầu] -> Step 14 (Truyền luồng dữ liệu)
            drawArrowWithLabel(g, c2, y_dec4 + 42, c2, y_step14 - bh / 2, "[Đạt yêu cầu]", 15);

            // Step 14 -> Decision 5 (Tải lên thành công?)
            drawArrow(g, c2, y_step14 + bh / 2, c2, y_dec5 - 42);

            // Decision 5 -> [Thất bại] -> Step 16 (Báo lỗi & cho phép thử lại)
            drawArrowWithLabel(g, c2 - 65, y_dec5, err_x + bw / 2, y_dec5, "[Thất bại / Đứt kết nối]", -15);
            // Step 16 -> Loop back to Step 3
            drawLoopBack(g, err_x - bw / 2, y_dec5, loop4_x, c1 - bw / 2, y_step3 + 15);

            // Decision 5 -> [Thành công] -> Step 17 (Ghi metadata vào CSDL trong Lane 3)
            drawOrthogonalArrow(g, new Point(c2 + 65, y_dec5), new Point(c3 - bw / 2, y_step17),
                    new Point[]{new Point(c3 - bw / 2, y_dec5), new Point(c3 - bw / 2, y_step17)});

            // Step 17 (DB) -> Step 18 (System phản hồi thông báo thành công)
            drawOrthogonalArrow(g, new Point(c3, y_step17 + bh / 2), new Point(c2 + bw / 2, y_step18),
                    new Point[]{new Point(c3, y_step18), new Point(c2 + bw / 2, y_step18)});

            // Step 18 (System) -> Step 18 (User nhận kết quả & xem file)
            drawArrow(g, c2 - bw / 2, y_step18, c1 + bw / 2, y_step18);

            // Step 18 (User) -> Final Node Success
            drawArrow(g, c1, y_step18 + bh / 2, c1, y_final - 26);

            // --- DRAW ALL NODES (Action, Decision, Initial, Final) ---
            // Initial Node
            drawInitialNode(g, c1, y_init);

            // --- LANE 1: USER ---
            drawAction(g, c1, y_step1_2, bw, bh, "1. Truy cập chức năng tải tệp\n(Nhấn nút Tải lên / Kéo thả)");
            drawAction(g, c1, y_step3, bw, bh, "3. Chọn tệp từ thiết bị\n(Duyệt bộ nhớ cục bộ)");
            drawAction(g, c1, y_step4_5, bw, bh, "4. Nhấn nút 'Tải lên'\n(Xác nhận gửi tệp)");
            drawAction(g, c1, y_step18, bw, bh, "18. Nhận thông báo thành công\n& tệp hiển thị trong danh sách");
            drawFinalNode(g, c1, y_final, "Tải tệp hoàn tất");

            // --- LANE 2: SYSTEM ---
            drawAction(g, c2, y_step1_2, bw, bh, "2. Hiển thị giao diện tải tệp\n(Mở hộp thoại JFileChooser)");
            drawAction(g, c2, y_step4_5, bw, bh, "5. Kiểm tra trạng thái\nđăng nhập của người dùng");
            drawDecision(g, c2, y_dec1, 130, 84, "Đã đăng\nnhập?");

            drawAction(g, abort_x, y_dec1, bw - 40, bh, "6. Yêu cầu đăng nhập\n& điều hướng về LoginFrame");
            drawFinalNode(g, abort_x, y_dec1 + bh / 2 + 80, "Từ chối / Chấm dứt");

            drawAction(g, c2, y_step7, bw, bh, "7. Kiểm tra tệp được chọn\ntừ thiết bị người dùng");
            drawDecision(g, c2, y_dec2, 130, 84, "Đã chọn\ntệp?");
            drawAction(g, err_x, y_dec2, bw, bh, "9. Thông báo lỗi: Vui lòng chọn\nít nhất 1 tệp tin hợp lệ");

            drawAction(g, c2, y_step10, bw, bh, "10. Kiểm tra phần mở rộng\n& định dạng tệp (Blacklist)");
            drawDecision(g, c2, y_dec3, 130, 84, "Định dạng\nhợp lệ?");
            drawAction(g, err_x, y_dec3, bw, bh, "11. Thông báo lỗi định dạng\n(Tệp thực thi bị cấm / sai kiểu)");

            drawAction(g, c2, y_step12, bw, bh, "12. Kiểm tra kích thước tệp\n& đối soát dung lượng Quota");
            drawDecision(g, c2, y_dec4, 130, 84, "Dung lượng\n<= Quota?");
            drawAction(g, err_x, y_dec4, bw, bh, "13. Thông báo lỗi dung lượng\n(Vượt hạn mức Quota cho phép)");

            drawAction(g, c2, y_step14, bw, bh, "14. Truyền luồng dữ liệu (Socket)\n& Server ghi tệp vào Sandbox");
            drawDecision(g, c2, y_dec5, 130, 84, "Tải lên\nthành công?");
            drawAction(g, err_x, y_dec5, bw, bh, "16. Xóa tệp tạm (.tmp), báo lỗi\n& cho phép người dùng tải lại");

            drawAction(g, c2, y_step18, bw, bh, "18. Thông báo tải tệp thành công\n& cập nhật giao diện danh sách");

            // --- LANE 3: DATABASE ---
            drawAction(g, c3, y_step17, bw + 20, bh, "17. Ghi metadata vào bảng 'files'\n& cộng used_storage vào 'users'");

            // Draw Legend at the bottom right of Lane 3
            drawLegend(g, l3_x + 50, sy + sh - 215, 788, 185);

            g.dispose();

            // Save to files
            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/activity_diagram_upload.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/activity_diagram_upload.png");
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
        // 3. Go right to targetX
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

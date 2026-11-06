package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateRETRSequenceDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 24);
    private static final Font FONT_PARTICIPANT = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_STEREOTYPE = new Font("Segoe UI", Font.ITALIC, 16);
    private static final Font FONT_MSG = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_ALT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_GUARD = new Font("Segoe UI", Font.BOLD | Font.ITALIC, 19);

    // Strokes
    private static final Stroke STROKE_BOX = new BasicStroke(2.6f);
    private static final Stroke STROKE_LIFELINE = new BasicStroke(2.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 8.0f}, 0.0f);
    private static final Stroke STROKE_SYNC = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke STROKE_RETURN = new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f, 6.0f}, 0.0f);
    private static final Stroke STROKE_FRAME = new BasicStroke(2.4f);
    private static final Stroke STROKE_DIVIDER = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{12.0f, 8.0f}, 0.0f);

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

            // 1. Background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, W, H);

            // 2. Main Title & Subtitle
            g.setColor(Color.BLACK);
            g.setFont(FONT_TITLE);
            String title = "SƠ ĐỒ TUẦN TỰ (SEQUENCE DIAGRAM) — CHỨC NĂNG TẢI TỆP (RETR)";
            FontMetrics fm = g.getFontMetrics(FONT_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 70);

            g.setFont(FONT_SUBTITLE);
            String sub = "Trình tự giao tiếp giữa Client, Server và Bộ lưu trữ khi tải tệp xuống — Hệ thống Secure FTP Client Pro";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 115);

            // 3. Lifeline X coordinates (5 Participants)
            int p1_x = 340;   // Client
            int p2_x = 1060;  // FTP Server
            int p3_x = 1780;  // Authentication Service
            int p4_x = 2500;  // File Storage
            int p5_x = 3160;  // Database

            int y_header_box = 165;
            int h_header_box = 105;
            int w_header_box = 370;
            int y_line_start = y_header_box + h_header_box;
            int y_line_end = 2320;

            // --- DRAW LIFELINE DASHED LINES ---
            g.setColor(new Color(100, 100, 100));
            g.setStroke(STROKE_LIFELINE);
            g.drawLine(p1_x, y_line_start, p1_x, y_line_end);
            g.drawLine(p2_x, y_line_start, p2_x, y_line_end);
            g.drawLine(p3_x, y_line_start, p3_x, y_line_end);
            g.drawLine(p4_x, y_line_start, p4_x, y_line_end);
            g.drawLine(p5_x, y_line_start, p5_x, y_line_end);

            // --- DRAW COMBINED FRAGMENTS (alt frames) ---
            // ALT FRAME 1: XÁC THỰC TÀI KHOẢN
            int alt1_x = 160;
            int alt1_y = 590;
            int alt1_w = 1840;
            int alt1_h = 240;
            drawAltFrame(g, alt1_x, alt1_y, alt1_w, alt1_h, "alt [XÁC THỰC TÀI KHOẢN]");

            int div1_y = 710;
            drawAltDivider(g, alt1_x, div1_y, alt1_x + alt1_w);

            g.setFont(FONT_GUARD);
            g.setColor(Color.BLACK);
            g.drawString("[Xác thực thất bại]", alt1_x + 140, alt1_y + 35);
            g.drawString("[Xác thực thành công]", alt1_x + 140, div1_y + 35);

            // ALT FRAME 2: KIỂM TRA QUYỀN TRUY XUẤT TỆP
            int alt2_x = 160;
            int alt2_y = 1010;
            int alt2_w = 1840;
            int alt2_h = 230;
            drawAltFrame(g, alt2_x, alt2_y, alt2_w, alt2_h, "alt [KIỂM TRA QUYỀN TRUY XUẤT TỆP]");

            int div2_y = 1125;
            drawAltDivider(g, alt2_x, div2_y, alt2_x + alt2_w);

            g.setFont(FONT_GUARD);
            g.drawString("[Không đủ quyền truy xuất]", alt2_x + 140, alt2_y + 35);
            g.drawString("[Đủ quyền truy xuất]", alt2_x + 140, div2_y + 35);

            // ALT FRAME 3: KIỂM TRA TỒN TẠI VÀ TRUYỀN DỮ LIỆU TỆP TIN
            int alt3_x = 160;
            int alt3_y = 1480;
            int alt3_w = 3180;
            int alt3_h = 810;
            drawAltFrame(g, alt3_x, alt3_y, alt3_w, alt3_h, "alt [KIỂM TRA TỒN TẠI VÀ TRUYỀN DỮ LIỆU TỆP TIN]");

            int div3_y = 1610;
            drawAltDivider(g, alt3_x, div3_y, alt3_x + alt3_w);

            g.setFont(FONT_GUARD);
            g.drawString("[Tệp không tồn tại / Không thể đọc]", alt3_x + 140, alt3_y + 35);
            g.drawString("[Tệp tồn tại & Sẵn sàng truyền tải]", alt3_x + 140, div3_y + 35);

            // ALT FRAME 4: KẾT QUẢ TẢI TỆP TIN (Lồng bên trong Alt 3 Branch 2)
            int alt4_x = 220;
            int alt4_y = 1930;
            int alt4_w = 3060;
            int alt4_h = 330;
            drawAltFrame(g, alt4_x, alt4_y, alt4_w, alt4_h, "alt [KẾT QUẢ TẢI TỆP TIN]");

            int div4_y = 2090;
            drawAltDivider(g, alt4_x, div4_y, alt4_x + alt4_w);

            g.setFont(FONT_GUARD);
            g.drawString("[Tải tệp thành công & Đầy đủ dữ liệu]", alt4_x + 140, alt4_y + 35);
            g.drawString("[Lỗi mạng / Gián đoạn kết nối Socket]", alt4_x + 140, div4_y + 35);

            // --- DRAW ACTIVATION BARS ---
            int act_w = 30;
            drawActivation(g, p1_x, 370, act_w, 1870); // Client
            drawActivation(g, p2_x, 370, act_w, 1870); // Server
            drawActivation(g, p3_x, 490, act_w, 680);  // AuthService
            drawActivation(g, p5_x, 1270, act_w, 110); // DB Read
            drawActivation(g, p4_x, 1410, act_w, 440); // File Storage
            drawActivation(g, p5_x, 2010, act_w, 230); // DB Log

            // --- DRAW MESSAGES ---
            // 1. Client -> Server: Connect
            drawSyncCall(g, p1_x + act_w / 2, 400, p2_x - act_w / 2, 400,
                    "1: Kết nối TCP Socket (Port 6789)", -16);

            // 2. Client -> Server: Send Credentials
            drawSyncCall(g, p1_x + act_w / 2, 470, p2_x - act_w / 2, 470,
                    "2: Gửi thông tin xác thực (USER & PASS)", -16);

            // 3. Server -> AuthService: Verify
            drawSyncCall(g, p2_x + act_w / 2, 540, p3_x - act_w / 2, 540,
                    "3: Kiểm tra thông tin đăng nhập: authenticate(user, pass)", -16);

            // --- ALT 1 MESSAGES ---
            // 4a. AuthService -> Server -> Client (Fail)
            drawReturnMsg(g, p3_x - act_w / 2, 650, p2_x + act_w / 2, 650,
                    "4a: Phản hồi lỗi xác thực (530 Login incorrect)", -16);
            drawReturnMsg(g, p2_x - act_w / 2, 650, p1_x + act_w / 2, 650,
                    "Thông báo lỗi xác thực & đóng kết nối", -16);

            // 4b. AuthService -> Server -> Client (OK)
            drawReturnMsg(g, p3_x - act_w / 2, 760, p2_x + act_w / 2, 760,
                    "4b: Xác thực thành công (230 User logged in)", -16);
            drawReturnMsg(g, p2_x - act_w / 2, 760, p1_x + act_w / 2, 760,
                    "Khởi tạo phiên làm việc thành công", -16);

            // --- PHASE 2: RETR REQUEST ---
            // 5. Client -> Server: RETR request
            drawSyncCall(g, p1_x + act_w / 2, 870, p2_x - act_w / 2, 870,
                    "5: Gửi yêu cầu tải tệp: RETR <filename>", -16);

            // 6. Server -> AuthService: Check permission
            drawSyncCall(g, p2_x + act_w / 2, 950, p3_x - act_w / 2, 950,
                    "6: Kiểm tra quyền truy xuất tệp: checkPermission(userId, filename, READ)", -16);

            // --- ALT 2 MESSAGES ---
            // 7a. Denied
            drawReturnMsg(g, p3_x - act_w / 2, 1060, p2_x + act_w / 2, 1060,
                    "7a: Từ chối: 550 Permission denied (Không có quyền đọc)", -16);
            drawReturnMsg(g, p2_x - act_w / 2, 1060, p1_x + act_w / 2, 1060,
                    "Thông báo lỗi: Quyền truy xuất tệp bị từ chối", -16);

            // 7b. Permission OK
            drawReturnMsg(g, p3_x - act_w / 2, 1170, p2_x + act_w / 2, 1170,
                    "7b: Quyền hợp lệ: checkPermission = OK (Access Granted)", -16);

            // 8. Server -> Database: Query metadata
            drawSyncCall(g, p2_x + act_w / 2, 1290, p5_x - act_w / 2, 1290,
                    "8: Truy vấn metadata tệp: SELECT file_id, file_size, checksum FROM files WHERE ...", -16);
            drawReturnMsg(g, p5_x - act_w / 2, 1350, p2_x + act_w / 2, 1350,
                    "Trả về thông tin metadata tệp tin (size, checksum, status)", -16);

            // 9. Server -> File Storage: Access file on disk
            drawSyncCall(g, p2_x + act_w / 2, 1420, p4_x - act_w / 2, 1420,
                    "9: Truy xuất tệp từ ổ đĩa máy chủ: resolveSafe(userHome, filename)", -16);

            // --- ALT 3 MESSAGES ---
            // 10a. File Not Found
            drawReturnMsg(g, p4_x - act_w / 2, 1540, p2_x + act_w / 2, 1540,
                    "10a: File not found / Cannot read file (file.exists() == false)", -16);
            drawReturnMsg(g, p2_x - act_w / 2, 1540, p1_x + act_w / 2, 1540,
                    "10: Phản hồi lỗi: 550 File not found hoặc Cannot read file", -16);

            // 10b. File Exists
            drawReturnMsg(g, p4_x - act_w / 2, 1660, p2_x + act_w / 2, 1660,
                    "10b: Tệp tồn tại, cung cấp File Handle & Kích thước byte", -16);

            // 11. Server -> Client: Ready code 150
            drawReturnMsg(g, p2_x - act_w / 2, 1720, p1_x + act_w / 2, 1720,
                    "11: Phản hồi sẵn sàng: 150 Opening binary mode data connection (SIZE <fileSize>)", -16);

            // Binary Stream: Storage -> Server -> Client
            drawSyncCall(g, p4_x - act_w / 2, 1790, p2_x + act_w / 2, 1790,
                    "Đọc luồng nhị phân từ đĩa (FileInputStream buffer 4096B)", -16);
            drawSyncCall(g, p2_x - act_w / 2, 1790, p1_x + act_w / 2, 1790,
                    "Truyền luồng dữ liệu nhị phân về Client (Binary Socket Stream)", -16);

            // 12. Client writes file
            drawSelfCall(g, p1_x + act_w / 2, 1845, 90, 48,
                    "12: Nhận bytes & ghi vào FileOutputStream tại downloads/ (Cập nhật Progress)", -14);

            // --- ALT 4 MESSAGES ---
            // 13a. Server -> Client: 226 Transfer complete
            drawReturnMsg(g, p2_x - act_w / 2, 1990, p1_x + act_w / 2, 1990,
                    "13a: Phản hồi hoàn tất: 226 Transfer complete (File sent, SHA-256 Checksum)", -16);

            // 14a. Client closes & updates UI
            drawSelfCall(g, p1_x + act_w / 2, 2030, 90, 42,
                    "14a: Nhận kết quả thành công, đóng luồng & cập nhật UI hoàn tất", -14);

            // Log Success to DB
            drawSyncCall(g, p2_x + act_w / 2, 2050, p5_x - act_w / 2, 2050,
                    "Ghi nhật ký audit: INSERT transfer_logs (RETR, SUCCESS, duration)", -16);

            // 13b. Server -> Client: 426 Transfer aborted
            drawReturnMsg(g, p2_x - act_w / 2, 2150, p1_x + act_w / 2, 2150,
                    "13b: Phản hồi thất bại: 426 Connection closed; transfer aborted", -16);

            // 14b. Client cleans up on error
            drawSelfCall(g, p1_x + act_w / 2, 2190, 90, 42,
                    "14b: Xóa tệp tải dở dang (.part) & hiển thị cảnh báo lỗi", -14);

            // Log Failure to DB
            drawSyncCall(g, p2_x + act_w / 2, 2210, p5_x - act_w / 2, 2210,
                    "Ghi nhật ký audit: INSERT transfer_logs (RETR, FAILED, reason)", -16);

            // --- DRAW PARTICIPANTS HEADERS (Top) ---
            drawParticipantBox(g, p1_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«boundary»", "Client\n(FTP Client Pro)");

            drawParticipantBox(g, p2_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«control»", "FTP Server\n(TCPServer / Handler)");

            drawParticipantBox(g, p3_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«service»", "Auth Service\n(Xác thực & Phân quyền)");

            drawParticipantBox(g, p4_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«subsystem»", "File Storage\n(Storage Sandbox)");

            drawParticipantBox(g, p5_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«entity»", "Database\n(MySQL Database)");

            g.dispose();

            // Save images
            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/sequence_diagram_retr.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/sequence_diagram_retr.png");
            ImageIO.write(img, "PNG", outBrain);
            System.out.println("SUCCESS: Saved Brain artifact copy to " + outBrain.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: Draw Participant Header Box
    private static void drawParticipantBox(Graphics2D g, int x, int y, int w, int h, String stereotype, String name) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_BOX);
        g.drawRect(x, y, w, h);

        g.setFont(FONT_STEREOTYPE);
        FontMetrics fmSt = g.getFontMetrics(FONT_STEREOTYPE);
        int stW = fmSt.stringWidth(stereotype);
        g.drawString(stereotype, x + (w - stW) / 2, y + 26);

        g.setFont(FONT_PARTICIPANT);
        FontMetrics fmNm = g.getFontMetrics(FONT_PARTICIPANT);
        String[] lines = name.split("\n");
        int ly = y + 55;
        for (String line : lines) {
            int lineW = fmNm.stringWidth(line);
            g.drawString(line, x + (w - lineW) / 2, ly);
            ly += 26;
        }
    }

    // Helper: Draw Activation Bar
    private static void drawActivation(Graphics2D g, int cx, int y, int w, int h) {
        int x = cx - w / 2;
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.0f));
        g.drawRect(x, y, w, h);
    }

    // Helper: Draw Synchronous Call Message (Solid line with filled arrowhead)
    private static void drawSyncCall(Graphics2D g, int x1, int y1, int x2, int y2, String label, int labelOffset) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_SYNC);
        g.drawLine(x1, y1, x2, y2);
        drawArrowHead(g, x1, y1, x2, y2, true);

        // Label with background wipe
        g.setFont(FONT_MSG);
        FontMetrics fm = g.getFontMetrics(FONT_MSG);
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2 + labelOffset;
        int lw = fm.stringWidth(label);
        g.setColor(Color.WHITE);
        g.fillRect(mx - lw / 2 - 6, my - fm.getAscent() + 2, lw + 12, fm.getHeight());
        g.setColor(Color.BLACK);
        g.drawString(label, mx - lw / 2, my);
    }

    // Helper: Draw Return Message (Dashed line with open arrowhead)
    private static void drawReturnMsg(Graphics2D g, int x1, int y1, int x2, int y2, String label, int labelOffset) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_RETURN);
        g.drawLine(x1, y1, x2, y2);
        drawOpenArrowHead(g, x1, y1, x2, y2);

        // Label
        g.setFont(FONT_MSG);
        FontMetrics fm = g.getFontMetrics(FONT_MSG);
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2 + labelOffset;
        int lw = fm.stringWidth(label);
        g.setColor(Color.WHITE);
        g.fillRect(mx - lw / 2 - 6, my - fm.getAscent() + 2, lw + 12, fm.getHeight());
        g.setColor(Color.BLACK);
        g.drawString(label, mx - lw / 2, my);
    }

    // Helper: Draw Self Call Message (Loopback on same participant)
    private static void drawSelfCall(Graphics2D g, int x, int y, int w, int h, String label, int labelOffset) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_SYNC);

        g.drawLine(x, y, x + w, y);
        g.drawLine(x + w, y, x + w, y + h);
        g.drawLine(x + w, y + h, x, y + h);

        drawArrowHead(g, x + w, y + h, x, y + h, true);

        // Label
        g.setFont(FONT_MSG);
        FontMetrics fm = g.getFontMetrics(FONT_MSG);
        int lx = x + w + 16;
        int ly = y + h / 2 + 6;
        int lw = fm.stringWidth(label);
        g.setColor(Color.WHITE);
        g.fillRect(lx - 4, ly - fm.getAscent() + 2, lw + 8, fm.getHeight());
        g.setColor(Color.BLACK);
        g.drawString(label, lx, ly);
    }

    // Helper: Draw Alt Frame
    private static void drawAltFrame(Graphics2D g, int x, int y, int w, int h, String title) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_FRAME);
        g.drawRect(x, y, w, h);

        // Tab header on top left
        int tabW = 270;
        int tabH = 36;
        g.setColor(Color.WHITE);
        g.fillRect(x, y, tabW, tabH);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, tabW, tabH);

        g.setFont(FONT_ALT_TITLE);
        FontMetrics fm = g.getFontMetrics(FONT_ALT_TITLE);
        g.drawString(title, x + 16, y + (tabH + fm.getAscent() - 8) / 2);
    }

    // Helper: Draw Alt Divider (Dashed horizontal line)
    private static void drawAltDivider(Graphics2D g, int x1, int y, int x2) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_DIVIDER);
        g.drawLine(x1, y, x2, y);
    }

    // Helper: Draw Filled Arrowhead
    private static void drawArrowHead(Graphics2D g, int x1, int y1, int x2, int y2, boolean filled) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int len = 15;
        Polygon poly = new Polygon();
        poly.addPoint(x2, y2);
        poly.addPoint((int) (x2 - len * Math.cos(angle - Math.PI / 6)), (int) (y2 - len * Math.sin(angle - Math.PI / 6)));
        poly.addPoint((int) (x2 - len * Math.cos(angle + Math.PI / 6)), (int) (y2 - len * Math.sin(angle + Math.PI / 6)));

        if (filled) {
            g.setColor(Color.BLACK);
            g.fillPolygon(poly);
        } else {
            g.setColor(Color.BLACK);
            g.setStroke(STROKE_SYNC);
            g.drawPolygon(poly);
        }
    }

    // Helper: Draw Open Arrowhead (Return)
    private static void drawOpenArrowHead(Graphics2D g, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int len = 16;
        int ax1 = (int) (x2 - len * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (y2 - len * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (x2 - len * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (y2 - len * Math.sin(angle + Math.PI / 6));

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(x2, y2, ax1, ay1);
        g.drawLine(x2, y2, ax2, ay2);
    }
}


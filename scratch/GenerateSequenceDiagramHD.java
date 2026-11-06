package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateSequenceDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 24);
    private static final Font FONT_PARTICIPANT = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_STEREOTYPE = new Font("Segoe UI", Font.ITALIC, 16);
    private static final Font FONT_MSG = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_ALT_TITLE = new Font("Segoe UI", Font.BOLD, 21);
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
            String title = "SƠ ĐỒ TUẦN TỰ (SEQUENCE DIAGRAM) — CHỨC NĂNG ĐĂNG NHẬP";
            FontMetrics fm = g.getFontMetrics(FONT_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 70);

            g.setFont(FONT_SUBTITLE);
            String sub = "Mô tả tương tác tuần tự & xử lý rẽ nhánh xác thực theo chuẩn UML 2.x — Hệ thống Secure FTP Client Pro";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 115);

            // 3. Lifeline X coordinates (5 Participants)
            int p1_x = 330;   // User (Actor)
            int p2_x = 1000;  // Login Page (LoginFrame)
            int p3_x = 1700;  // Backend API (ClientHandler)
            int p4_x = 2400;  // Auth Service (CommandHandler / Security)
            int p5_x = 3100;  // Database (UserDAO / MySQL)

            int y_header_box = 170;
            int h_header_box = 110;
            int w_header_box = 380;
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

            // --- DRAW COMBINED FRAGMENT: alt [XỬ LÝ KẾT QUẢ ĐĂNG NHẬP] FIRST (Underneath) ---
            int alt_x = 160;
            int alt_y = 1480;
            int alt_w = 3180;
            int alt_h = 790;
            drawAltFrame(g, alt_x, alt_y, alt_w, alt_h, "alt [XỬ LÝ KẾT QUẢ XÁC THỰC]");

            int div1_y = 1740;
            int div2_y = 1990;
            drawAltDivider(g, alt_x, div1_y, alt_x + alt_w);
            drawAltDivider(g, alt_x, div2_y, alt_x + alt_w);

            // Guard labels
            g.setFont(FONT_GUARD);
            g.setColor(Color.BLACK);
            g.drawString("[Mật khẩu đúng & Tài khoản hoạt động - ACTIVE]", alt_x + 150, alt_y + 35);
            g.drawString("[Sai email/username hoặc mật khẩu không khớp]", alt_x + 150, div1_y + 35);
            g.drawString("[Tài khoản bị khóa - LOCKED]", alt_x + 150, div2_y + 35);

            // --- DRAW ACTIVATION BARS ---
            int act_w = 30;
            drawActivation(g, p1_x, 420, act_w, 1820); // User
            drawActivation(g, p2_x, 420, act_w, 1800); // LoginFrame
            drawActivation(g, p3_x, 680, act_w, 1420); // Backend
            drawActivation(g, p4_x, 780, act_w, 1260); // AuthService
            drawActivation(g, p5_x, 1020, act_w, 140);  // Database

            // --- DRAW MESSAGES ---
            // 1. User -> LoginFrame: Nhập thông tin
            drawSyncCall(g, p1_x + act_w / 2, 450, p2_x - act_w / 2, 450,
                    "1: Nhập Email/Username & Mật khẩu", -16);

            // 2. User -> LoginFrame: Nhấn nút Đăng nhập
            drawSyncCall(g, p1_x + act_w / 2, 570, p2_x - act_w / 2, 570,
                    "2: Nhấn nút 'Đăng nhập'", -16);

            // 3. LoginFrame -> ClientHandler: Gửi yêu cầu đăng nhập
            drawSyncCall(g, p2_x + act_w / 2, 700, p3_x - act_w / 2, 700,
                    "3: Gửi yêu cầu đăng nhập: USER & PASS (TCP Socket:6789)", -16);

            // 4. ClientHandler -> AuthService: Tiếp nhận và gọi xử lý
            drawSyncCall(g, p3_x + act_w / 2, 800, p4_x - act_w / 2, 800,
                    "4: Tiếp nhận & gọi authenticate(username, password)", -16);

            // 5. AuthService: Kiểm tra dữ liệu đầu vào (Self-call)
            drawSelfCall(g, p4_x + act_w / 2, 890, 80, 50,
                    "5: Kiểm tra định dạng dữ liệu đầu vào (Validation)", -14);

            // 6. AuthService -> Database: Truy vấn thông tin tài khoản
            drawSyncCall(g, p4_x + act_w / 2, 1040, p5_x - act_w / 2, 1040,
                    "6: Truy vấn tài khoản: findUserByUsername(username)", -16);

            // 7. Database -> AuthService: Trả về thông tin
            drawReturnMsg(g, p5_x - act_w / 2, 1140, p4_x + act_w / 2, 1140,
                    "7: Trả về bản ghi User (PasswordHash, Salt, Status, Role)", -16);

            // 8. AuthService: Băm mật khẩu và so khớp (Self-call)
            drawSelfCall(g, p4_x + act_w / 2, 1250, 80, 50,
                    "8: Băm mật khẩu (SHA-256 + Salt) & đối soát chuỗi Hash", -14);

            // 9. AuthService: Kiểm tra trạng thái tài khoản (Self-call)
            drawSelfCall(g, p4_x + act_w / 2, 1370, 80, 50,
                    "9: Kiểm tra trạng thái tài khoản (Status: ACTIVE / LOCKED)", -14);

            // --- ALT NHÁNH 1: THÀNH CÔNG ---
            // 10a. AuthService: Tạo session (Self-call)
            drawSelfCall(g, p4_x + act_w / 2, 1530, 80, 45,
                    "10a: Tạo phiên (Session) & cấp quyền đăng nhập", -14);

            // 11a. AuthService -> ClientHandler -> LoginFrame
            drawReturnMsg(g, p4_x - act_w / 2, 1610, p3_x + act_w / 2, 1610,
                    "11a: Phản hồi đăng nhập thành công (230 Login OK)", -16);
            drawReturnMsg(g, p3_x - act_w / 2, 1610, p2_x + act_w / 2, 1610,
                    "12a: Chuyển tiếp kết quả đăng nhập thành công", -16);

            // 13a. LoginFrame -> User: Điều hướng
            drawSyncCall(g, p2_x - act_w / 2, 1690, p1_x + act_w / 2, 1690,
                    "13a: Điều hướng tới giao diện chính (MainFrame / AdminDashboard)", -16);

            // --- ALT NHÁNH 2: SAI THÔNG TIN ---
            // 10b. AuthService -> ClientHandler -> LoginFrame
            drawReturnMsg(g, p4_x - act_w / 2, 1820, p3_x + act_w / 2, 1820,
                    "10b: Phản hồi lỗi: Sai tài khoản hoặc mật khẩu (530 Error)", -16);
            drawReturnMsg(g, p3_x - act_w / 2, 1820, p2_x + act_w / 2, 1820,
                    "11b: Chuyển tiếp thông báo lỗi xác thực", -16);

            // 12b. LoginFrame -> User: Báo lỗi & yêu cầu nhập lại
            drawSyncCall(g, p2_x - act_w / 2, 1920, p1_x + act_w / 2, 1920,
                    "12b: Hiển thị thông báo lỗi trên UI & yêu cầu nhập lại", -16);

            // --- ALT NHÁNH 3: TÀI KHOẢN BỊ KHÓA ---
            // 10c. AuthService -> ClientHandler -> LoginFrame
            drawReturnMsg(g, p4_x - act_w / 2, 2070, p3_x + act_w / 2, 2070,
                    "10c: Phản hồi lỗi: Tài khoản bị khóa (530 Account Locked)", -16);
            drawReturnMsg(g, p3_x - act_w / 2, 2070, p2_x + act_w / 2, 2070,
                    "11c: Chuyển tiếp thông báo tài khoản bị khóa", -16);

            // 12c. LoginFrame -> User: Cảnh báo tài khoản bị khóa
            drawSyncCall(g, p2_x - act_w / 2, 2180, p1_x + act_w / 2, 2180,
                    "12c: Hiển thị cảnh báo: Tài khoản bị khóa & từ chối truy cập", -16);

            // --- DRAW PARTICIPANTS HEADERS (Top) ---
            // User Actor
            drawActor(g, p1_x, y_header_box + 25, "Người dùng\n(User)");

            // Participants Boxes
            drawParticipantBox(g, p2_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«boundary»", "Giao diện đăng nhập\n(LoginFrame)");

            drawParticipantBox(g, p3_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«control»", "Backend API\n(ClientHandler)");

            drawParticipantBox(g, p4_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«service»", "Dịch vụ xác thực\n(AuthService / Security)");

            drawParticipantBox(g, p5_x - w_header_box / 2, y_header_box, w_header_box, h_header_box,
                    "«entity»", "Cơ sở dữ liệu\n(UserDAO / MySQL)");

            g.dispose();

            // Save images
            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/sequence_diagram_login.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/sequence_diagram_login.png");
            ImageIO.write(img, "PNG", outBrain);
            System.out.println("SUCCESS: Saved Brain artifact copy to " + outBrain.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: Draw Actor Stick Figure
    private static void drawActor(Graphics2D g, int cx, int cy, String label) {
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.6f));

        // Head
        int headR = 22;
        g.drawOval(cx - headR, cy - 35, headR * 2, headR * 2);

        // Body
        g.drawLine(cx, cy - 35 + headR * 2, cx, cy + 25);

        // Arms
        g.drawLine(cx - 30, cy - 2, cx + 30, cy - 2);

        // Legs
        g.drawLine(cx, cy + 25, cx - 25, cy + 65);
        g.drawLine(cx, cy + 25, cx + 25, cy + 65);

        // Label
        g.setFont(FONT_PARTICIPANT);
        FontMetrics fm = g.getFontMetrics(FONT_PARTICIPANT);
        String[] lines = label.split("\n");
        int dy = cy + 90;
        for (String line : lines) {
            g.drawString(line, cx - fm.stringWidth(line) / 2, dy);
            dy += fm.getHeight();
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
        FontMetrics fmStereo = g.getFontMetrics(FONT_STEREOTYPE);
        g.drawString(stereotype, x + (w - fmStereo.stringWidth(stereotype)) / 2, y + 30);

        g.setFont(FONT_PARTICIPANT);
        FontMetrics fm = g.getFontMetrics(FONT_PARTICIPANT);
        String[] lines = name.split("\n");
        int dy = y + 58;
        for (String line : lines) {
            g.drawString(line, x + (w - fm.stringWidth(line)) / 2, dy);
            dy += fm.getHeight();
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


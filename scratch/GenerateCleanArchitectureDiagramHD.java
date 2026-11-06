package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenerateCleanArchitectureDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 24);
    private static final Font FONT_TIER_HEADER = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_COMP_NAME = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_COMP_DESC = new Font("Segoe UI", Font.PLAIN, 18);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD | Font.ITALIC, 19);

    // Strokes
    private static final Stroke STROKE_TIER = new BasicStroke(3.5f);
    private static final Stroke STROKE_COMP = new BasicStroke(2.6f);
    private static final Stroke STROKE_EDGE = new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

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

            // 2. Titles
            g.setColor(Color.BLACK);
            g.setFont(FONT_TITLE);
            String title = "SƠ ĐỒ THÀNH PHẦN VÀ KIẾN TRÚC HỆ THỐNG (COMPONENT / ARCHITECTURE DIAGRAM)";
            FontMetrics fm = g.getFontMetrics(FONT_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 70);

            g.setFont(FONT_SUBTITLE);
            String sub = "Kiến trúc tổng thể 3 phân tầng (Multi-tier Architecture) chuẩn UML 2.x — Hệ thống Secure FTP Client Pro & Server";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 115);

            // 3. Main Tiers Layout (3 Columns from Left to Right)
            int sy = 150;
            int sh = 2260;

            int t1_x = 80;
            int t1_w = 820;

            int t2_x = 980;
            int t2_w = 1420;

            int t3_x = 2480;
            int t3_w = 948;

            drawTier(g, t1_x, sy, t1_w, sh, "CLIENT (ỨNG DỤNG KHÁCH)");
            drawTier(g, t2_x, sy, t2_w, sh, "APPLICATION SERVER (MÁY CHỦ FTP)");
            drawTier(g, t3_x, sy, t3_w, sh, "DATA & EXTERNAL (DỮ LIỆU & DỊCH VỤ NGOÀI)");

            // --- CONNECTING EDGES (Drawn underneath for clean graphics) ---
            // 1. UI -> FtpClientService
            drawArrowWithLabel(g, t1_x + t1_w / 2, 350 + 320, t1_x + t1_w / 2, 1200,
                    "Gọi phương thức nội bộ (Method Invocations)", 0);

            // 2. FtpClientService <-> TCPServer (The core network bridge)
            drawBiArrowWithLabel(g, t1_x + t1_w - 50, 1200 + 170, t2_x + 50, 260 + 120,
                    "Kênh Mạng: TCP Socket (Port 6789) — [Lệnh FTP ASCII & Luồng nhị phân]", -20);

            // 3. TCPServer -> CommandHandler
            drawArrow(g, t2_x + t2_w / 2, 260 + 240, t2_x + t2_w / 2, 660);

            // 4. CommandHandler <-> Security & Auth
            drawBiArrowWithLabel(g, t2_x + t2_w / 2, 660 + 240, t2_x + t2_w / 2, 1060,
                    "Xác thực & Kiểm tra Quyền", 0);

            // 5. Security <-> StorageManager
            drawBiArrowWithLabel(g, t2_x + t2_w / 2, 1060 + 240, t2_x + t2_w / 2, 1460,
                    "Kiểm tra Quota & Quyền tệp", 0);

            // 6. StorageManager -> DAO Layer
            drawArrow(g, t2_x + t2_w / 2, 1460 + 250, t2_x + t2_w / 2, 1880);

            // 7. StorageManager <-> File Sandbox (Java File I/O)
            drawBiArrowWithLabel(g, t2_x + t2_w - 50, 1460 + 125, t3_x + 50, 1100 + 180,
                    "Java File I/O (NIO) — [Luồng byte thô & Sandbox Chroot]", -20);

            // 8. DAO Layer <-> MySQL Database (JDBC SQL Port 3306)
            drawBiArrowWithLabel(g, t2_x + t2_w - 50, 1880 + 100, t3_x + 50, 280 + 210,
                    "JDBC Connection (Port 3306) — [SQL Queries & Giao dịch ACID]", -20);

            // 9. DAO Layer / Controller <-> External Payment Gateway
            drawBiArrowWithLabel(g, t2_x + t2_w - 50, 1880 + 180, t3_x + 50, 1850 + 170,
                    "Mock REST / Webhook — [VietQR & Xác nhận thanh toán]", -20);

            // --- DRAW COMPONENTS ---
            // --- COLUMN 1: CLIENT ---
            drawComponent(g, t1_x + 50, 350, t1_w - 100, 320,
                    "Giao diện Người dùng (Presentation UI)",
                    "• Java Swing Desktop UI: LoginFrame, MainFrame\n" +
                    "• AdminDashboardDialog: Quản lý người dùng, Quota & Logs\n" +
                    "• Sub Dialogs: PaymentDialog, SharedFilesDialog, ShareManager\n" +
                    "• ThemeManager & ModernButton: Giao diện phẳng trực quan, hiện đại\n" +
                    "• Action Toolbars & FileTable: Thao tác file/thư mục và hiển thị Quota");

            drawComponent(g, t1_x + 50, 1200, t1_w - 100, 340,
                    "Dịch vụ Khách & Socket (FtpClientService)",
                    "• Điều phối logic nghiệp vụ và quản lý trạng thái phiên client\n" +
                    "• Đóng gói và gửi nhận các lệnh giao thức FTP (Command Builder)\n" +
                    "• TCPClient: Quản lý kết nối TCP Socket tới máy chủ Port 6789\n" +
                    "• Binary Transfer Stream: Truyền tải luồng byte theo khối (Chunked I/O)\n" +
                    "• Lắng nghe sự kiện tiến độ (Progress Listener) cập nhật giao diện");

            // --- COLUMN 2: APPLICATION SERVER (BACKEND) ---
            drawComponent(g, t2_x + 50, 260, t2_w - 100, 240,
                    "Bộ Tiếp nhận & Đa luồng (TCPServer & Concurrency)",
                    "• ServerSocket(Port 6789): Mở cổng dịch vụ lắng nghe kết nối TCP đồng thời\n" +
                    "• Thread Pool Executor: Cấp phát luồng xử lý độc lập (ClientHandler) cho từng client\n" +
                    "• Quản lý phiên (Session): Lưu trữ định danh người dùng, thư mục hiện hành và Quota\n" +
                    "• ProtocolInputStream: Tách biệt đọc dòng lệnh và luồng nhị phân trực tiếp trên Socket");

            drawComponent(g, t2_x + 50, 660, t2_w - 100, 240,
                    "Bộ Điều khiển Giao thức (CommandHandler)",
                    "• Bộ phân tích cú pháp và điều phối lệnh (FTP Protocol Dispatcher)\n" +
                    "• Xác thực & Tài khoản: USER, PASS, REGISTER, LOGOUT, PASSWD\n" +
                    "• Thao tác tệp & thư mục: LIST, STOR, RETR, DELE, MKD, RMD, CWD, CDUP\n" +
                    "• Dịch vụ nâng cao: QUOTA, SHARE, REVOKE, ORDER, PAY, UPGRADE");

            drawComponent(g, t2_x + 50, 1060, t2_w - 100, 240,
                    "Xác thực & Bảo mật (Security & Authentication)",
                    "• Băm mật khẩu an toàn: Mã hóa một chiều SHA-256 kết hợp chuỗi muối (Salt)\n" +
                    "• Cơ chế phân quyền RBAC: Kiểm soát quyền hạn người dùng (USER) và quản trị (ADMIN)\n" +
                    "• Kiểm tra trạng thái tài khoản: Cho phép (ACTIVE) hoặc từ chối (LOCKED)\n" +
                    "• Làm sạch dữ liệu đầu vào (Input Sanitization) ngăn ngừa tấn công Injection");

            drawComponent(g, t2_x + 50, 1460, t2_w - 100, 250,
                    "Quản lý Tập tin & Quota (StorageManager)",
                    "• Cô lập không gian thư mục cục bộ (Chroot Sandbox): storage/<username>/\n" +
                    "• Phòng chống tấn công vượt đường dẫn (Path Traversal Defense: chặn ../, /)\n" +
                    "• Kiểm soát dung lượng Quota khả dụng trước khi chấp nhận ghi nhận tệp tải lên\n" +
                    "• Quản lý tệp tạm (.upload.tmp) và cơ chế tự động dọn dẹp khi ngắt kết nối");

            drawComponent(g, t2_x + 50, 1880, t2_w - 100, 250,
                    "Tầng Truy xuất Dữ liệu (Data Access Layer - DAO)",
                    "• UserDAO: Xác thực, phân quyền, trạng thái khóa, cập nhật dung lượng đã dùng\n" +
                    "• FileDAO & ShareDAO: Thao tác metadata tệp tin và token liên kết chia sẻ\n" +
                    "• OrderDAO, PlanDAO & PaymentDAO: Xử lý quy trình đơn hàng và thanh toán nâng gói\n" +
                    "• LogDAO: Lưu vết lịch sử truyền file và cung cấp dữ liệu báo cáo hệ thống\n" +
                    "• DBConnection: Quản lý kết nối JDBC MySQL và bảo đảm giao dịch ACID");

            // --- COLUMN 3: DATA & EXTERNAL SERVICES ---
            drawDatabase(g, t3_x + 50, 280, t3_w - 100, 420,
                    "Cơ sở Dữ liệu (MySQL Database Server)",
                    "Hệ quản trị CSDL quan hệ MySQL 8.x (Port 3306):\n\n" +
                    "• Bảng 'users': Thông tin tài khoản, mật khẩu băm, vai trò, quota\n" +
                    "• Bảng 'files': Metadata tệp tin, kích thước, đường dẫn, checksum\n" +
                    "• Bảng 'file_shares': Quyền chia sẻ tệp tin và mã token truy cập\n" +
                    "• Bảng 'plans', 'orders', 'payments': Gói cước và giao dịch dịch vụ\n" +
                    "• Bảng 'transfer_logs': Nhật ký upload/download, IP và thời gian\n" +
                    "• Ràng buộc toàn vẹn khóa ngoại (Foreign Keys) & Giao dịch ACID");

            drawFolder(g, t3_x + 50, 1100, t3_w - 100, 360,
                    "Hệ thống Tệp Lưu trữ (Storage Sandbox)",
                    "Thư mục lưu trữ vật lý: storage/<username>/\n\n" +
                    "• Phân vùng Chroot độc lập tuyệt đối giữa các tài khoản người dùng\n" +
                    "• Hỗ trợ cấu trúc thư mục con phân cấp nhiều tầng (Subdirectories)\n" +
                    "• Bảo vệ tệp đang truyền: Ghi vào .upload.tmp đến khi hoàn tất\n" +
                    "• Truyền trực tiếp qua Socket Stream (Zero OOM - Stream Pipeline)\n" +
                    "• Đồng bộ nhất quán giữa đĩa cứng và metadata CSDL");

            drawExternal(g, t3_x + 50, 1850, t3_w - 100, 340,
                    "Cổng Thanh toán Mô phỏng (VietQR Simulator)",
                    "Dịch vụ thanh toán bên ngoài (Simulated Payment Gateway):\n\n" +
                    "• VietQR Mock Service: Tạo mã QR động chuẩn ngân hàng theo đơn hàng\n" +
                    "• Webhook Callback Simulator: Giả lập tín hiệu chuyển khoản thành công\n" +
                    "• Transaction Validator: Đối soát mã đơn hàng ORD-xxx và số tiền\n" +
                    "• Tự động kích hoạt nâng gói cước PRO/VIP và mở rộng Quota tức thì");

            g.dispose();

            // Save images
            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/component_architecture_diagram.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/component_architecture_diagram.png");
            ImageIO.write(img, "PNG", outBrain);
            System.out.println("SUCCESS: Saved Brain artifact copy to " + outBrain.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: Draw Tier (Main Column)
    private static void drawTier(Graphics2D g, int x, int y, int w, int h, String title) {
        g.setColor(new Color(250, 250, 250));
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_TIER);
        g.drawRect(x, y, w, h);

        int headerH = 55;
        g.drawLine(x, y + headerH, x + w, y + headerH);

        g.setFont(FONT_TIER_HEADER);
        FontMetrics fm = g.getFontMetrics(FONT_TIER_HEADER);
        g.drawString(title, x + (w - fm.stringWidth(title)) / 2, y + (headerH + fm.getAscent() - 8) / 2);
    }

    // Helper: Draw UML Component Box
    private static void drawComponent(Graphics2D g, int x, int y, int w, int h, String compName, String details) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_COMP);
        g.drawRect(x, y, w, h);

        // Component Icon on top right
        int iconW = 26;
        int iconH = 20;
        int iconX = x + w - iconW - 16;
        int iconY = y + 14;
        g.drawRect(iconX, iconY, iconW, iconH);
        g.fillRect(iconX - 6, iconY + 3, 8, 5);
        g.fillRect(iconX - 6, iconY + 12, 8, 5);

        // Header: «component»
        g.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        g.drawString("«component»", x + 20, y + 26);

        // Component Name
        g.setFont(FONT_COMP_NAME);
        g.drawString(compName, x + 20, y + 54);

        // Details text (multi-line)
        g.setFont(FONT_COMP_DESC);
        FontMetrics fm = g.getFontMetrics(FONT_COMP_DESC);
        String[] lines = details.split("\n");
        int dy = y + 84;
        for (String line : lines) {
            g.drawString(line, x + 20, dy);
            dy += fm.getHeight() + 4;
        }
    }

    // Helper: Draw Database Cylinder Shape
    private static void drawDatabase(Graphics2D g, int x, int y, int w, int h, String dbName, String details) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_COMP);
        g.drawRoundRect(x, y, w, h, 20, 20);

        // Top cylinder ellipse
        int ellH = 34;
        g.drawOval(x + 10, y + 10, w - 20, ellH);

        g.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        g.drawString("«database»", x + 20, y + 72);

        g.setFont(FONT_COMP_NAME);
        g.drawString(dbName, x + 20, y + 100);

        g.setFont(FONT_COMP_DESC);
        FontMetrics fm = g.getFontMetrics(FONT_COMP_DESC);
        String[] lines = details.split("\n");
        int dy = y + 134;
        for (String line : lines) {
            g.drawString(line, x + 20, dy);
            dy += fm.getHeight() + 4;
        }
    }

    // Helper: Draw Folder Storage Shape
    private static void drawFolder(Graphics2D g, int x, int y, int w, int h, String folderName, String details) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_COMP);
        g.drawRect(x, y + 24, w, h - 24);

        // Folder Tab
        g.drawRect(x, y, 180, 24);

        g.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        g.drawString("«filesystem»", x + 20, y + 54);

        g.setFont(FONT_COMP_NAME);
        g.drawString(folderName, x + 20, y + 82);

        g.setFont(FONT_COMP_DESC);
        FontMetrics fm = g.getFontMetrics(FONT_COMP_DESC);
        String[] lines = details.split("\n");
        int dy = y + 114;
        for (String line : lines) {
            g.drawString(line, x + 20, dy);
            dy += fm.getHeight() + 4;
        }
    }

    // Helper: Draw External Service Rounded Box
    private static void drawExternal(Graphics2D g, int x, int y, int w, int h, String name, String details) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 24, 24);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_COMP);
        g.drawRoundRect(x, y, w, h, 24, 24);

        g.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        g.drawString("«external service»", x + 20, y + 30);

        g.setFont(FONT_COMP_NAME);
        g.drawString(name, x + 20, y + 58);

        g.setFont(FONT_COMP_DESC);
        FontMetrics fm = g.getFontMetrics(FONT_COMP_DESC);
        String[] lines = details.split("\n");
        int dy = y + 90;
        for (String line : lines) {
            g.drawString(line, x + 20, dy);
            dy += fm.getHeight() + 4;
        }
    }

    // Helper: Draw Arrow
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

        g.setFont(FONT_LABEL);
        FontMetrics fm = g.getFontMetrics(FONT_LABEL);
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2 + labelOffset;
        int lw = fm.stringWidth(label);
        g.setColor(Color.WHITE);
        g.fillRect(mx - lw / 2 - 6, my - fm.getAscent() + 2, lw + 12, fm.getHeight());
        g.setColor(Color.BLACK);
        g.drawString(label, mx - lw / 2, my);
    }

    // Helper: Draw Bi-directional Orthogonal Arrow with Label
    private static void drawBiArrowWithLabel(Graphics2D g, int x1, int y1, int x2, int y2, String label, int labelOffset) {
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_EDGE);

        int midX = (x1 + x2) / 2;
        g.drawLine(x1, y1, midX, y1);
        g.drawLine(midX, y1, midX, y2);
        g.drawLine(midX, y2, x2, y2);

        drawArrowHead(g, midX, y1, x1, y1);
        drawArrowHead(g, midX, y2, x2, y2);

        g.setFont(FONT_LABEL);
        FontMetrics fm = g.getFontMetrics(FONT_LABEL);
        int my = (y1 + y2) / 2 + labelOffset;
        int lw = fm.stringWidth(label);
        g.setColor(Color.WHITE);
        g.fillRect(midX - lw / 2 - 8, my - fm.getAscent() + 2, lw + 16, fm.getHeight());
        g.setColor(Color.BLACK);
        g.drawString(label, midX - lw / 2, my);
    }

    // Helper: Draw Arrowhead
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
}


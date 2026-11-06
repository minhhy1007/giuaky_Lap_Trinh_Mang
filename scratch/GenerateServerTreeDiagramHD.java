package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class GenerateServerTreeDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 22);
    private static final Font FONT_FOLDER_NAME = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_FOLDER_DESC = new Font("Segoe UI", Font.ITALIC, 16);
    private static final Font FONT_BOX_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_FILE_NAME = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_FILE_DESC = new Font("Segoe UI", Font.PLAIN, 16);

    // Strokes
    private static final Stroke STROKE_BORDER = new BasicStroke(2.4f);
    private static final Stroke STROKE_TREE = new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke STROKE_DIVIDER = new BasicStroke(1.4f);

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

            // 1. White Background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, W, H);

            // 2. Titles
            g.setColor(Color.BLACK);
            g.setFont(FONT_TITLE);
            String title = "SƠ ĐỒ CÂY THƯ MỤC BACKEND / SERVER (DIRECTORY TREE DIAGRAM)";
            FontMetrics fm = g.getFontMetrics(FONT_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 65);

            g.setFont(FONT_SUBTITLE);
            String sub = "Kiến trúc tổ chức mã nguồn, cấu hình, tầng truy cập CSDL và hệ thống lưu trữ vật lý của Secure FTP Server";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 105);

            // --- 3. NODES DEFINITION ---
            // Root Node
            int rx = 80, ry = 1080, rw = 520, rh = 125;

            // Level 1 Nodes
            int l1_x = 730;
            int srv_y = 480, srv_w = 460, srv_h = 120;
            int cfg_y = 1380, cfg_w = 460, cfg_h = 95;
            int sql_y = 1580, sql_w = 460, sql_h = 95;
            int sto_y = 1780, sto_w = 460, sto_h = 95;
            int tst_y = 2000, tst_w = 460, tst_h = 95;

            // Level 2 Nodes (from src/server/)
            int l2_x = 1340;
            int core_y = 160, core_w = 930, core_h = 490;
            int dao_y = 690, dao_w = 930, dao_h = 560;

            // Level 3 Nodes (from src/server/ to the right)
            int l3_x = 2410;
            int db_y = 160, db_w = 980, db_h = 280;
            int mdl_y = 480, mdl_w = 980, mdl_h = 560;
            int utl_y = 1080, utl_w = 980, utl_h = 170;

            // Other leaf boxes
            int cfg_box_y = 1340, cfg_box_w = 930, cfg_box_h = 170;
            int sql_box_y = 1550, sql_box_w = 930, sql_box_h = 140;
            int sto_box_y = 1730, sto_box_w = 930, sto_box_h = 190;
            int tst_box_y = 1960, tst_box_w = 930, tst_box_h = 430;

            // --- 4. DRAW TREE CONNECTORS (Underneath) ---
            g.setColor(new Color(60, 60, 60));
            g.setStroke(STROKE_TREE);

            // Root -> Level 1
            int r_out_x = rx + rw;
            int r_out_y = ry + rh / 2;
            int mid1_x = (r_out_x + l1_x) / 2;

            drawTreeBranch(g, r_out_x, r_out_y, mid1_x, srv_y + srv_h / 2, l1_x);
            drawTreeBranch(g, r_out_x, r_out_y, mid1_x, cfg_y + cfg_h / 2, l1_x);
            drawTreeBranch(g, r_out_x, r_out_y, mid1_x, sql_y + sql_h / 2, l1_x);
            drawTreeBranch(g, r_out_x, r_out_y, mid1_x, sto_y + sto_h / 2, l1_x);
            drawTreeBranch(g, r_out_x, r_out_y, mid1_x, tst_y + tst_h / 2, l1_x);

            // src/server/ -> Core & DAO
            int srv_out_x = l1_x + srv_w;
            int srv_out_y = srv_y + srv_h / 2;
            int mid2_x = (srv_out_x + l2_x) / 2;
            drawTreeBranch(g, srv_out_x, srv_out_y, mid2_x, core_y + 160, l2_x);
            drawTreeBranch(g, srv_out_x, srv_out_y, mid2_x, dao_y + 160, l2_x);

            // src/server/ -> db, model, util
            int mid3_x = (l2_x + core_w + l3_x) / 2;
            drawTreeBranch(g, srv_out_x, srv_out_y, mid3_x, db_y + 140, l3_x);
            drawTreeBranch(g, srv_out_x, srv_out_y, mid3_x, mdl_y + 140, l3_x);
            drawTreeBranch(g, srv_out_x, srv_out_y, mid3_x, utl_y + 85, l3_x);

            // config/ -> cfg_box
            drawStraightBranch(g, l1_x + cfg_w, cfg_y + cfg_h / 2, l2_x, cfg_box_y + cfg_box_h / 2);
            // sql/ -> sql_box
            drawStraightBranch(g, l1_x + sql_w, sql_y + sql_h / 2, l2_x, sql_box_y + sql_box_h / 2);
            // storage/ -> sto_box
            drawStraightBranch(g, l1_x + sto_w, sto_y + sto_h / 2, l2_x, sto_box_y + sto_box_h / 2);
            // test/ -> tst_box
            drawStraightBranch(g, l1_x + tst_w, tst_y + tst_h / 2, l2_x, tst_box_y + tst_box_h / 2);

            // --- 5. DRAW FOLDER & FILE BOXES ---
            // Root
            drawFolderBox(g, rx, ry, rw, rh, "📁 giuaky_Lap_Trinh_Mang/", "Thư mục gốc toàn bộ dự án hệ thống", true);

            // Level 1 Folders
            drawFolderBox(g, l1_x, srv_y, srv_w, srv_h, "📁 src/server/", "Mã nguồn xử lý Backend Server", false);
            drawFolderBox(g, l1_x, cfg_y, cfg_w, cfg_h, "📁 config/", "Tệp cấu hình kết nối CSDL", false);
            drawFolderBox(g, l1_x, sql_y, sql_w, sql_h, "📁 sql/", "Kịch bản DDL Cơ sở dữ liệu MySQL", false);
            drawFolderBox(g, l1_x, sto_y, sto_w, sto_h, "📁 storage/", "Thư mục lưu trữ vật lý (Sandbox)", false);
            drawFolderBox(g, l1_x, tst_y, tst_w, tst_h, "📁 test/", "Bộ kiểm thử tự động 7 Phase", false);

            // Level 2 Detail Boxes
            // 1. Core Server Engine
            drawDetailBox(g, l2_x, core_y, core_w, core_h, "📁 src/server/ (Core Engine & Protocols)",
                    List.of(
                            new Item("TCPServer.java", "Entry point, mở ServerSocket Port 6789, cấp phát ThreadPool"),
                            new Item("ClientHandler.java", "Thực thi Runnable, tiếp nhận & xử lý socket riêng từng Client"),
                            new Item("CommandHandler.java", "Router định tuyến 13 lệnh FTP RFC 959 (USER, PASS, STOR, RETR...)"),
                            new Item("ProtocolInputStream.java", "Đọc luồng byte-by-byte đến \\n, chống lỗi nuốt byte Buffer-Ahead"),
                            new Item("Session.java", "Quản lý phiên: lưu trạng thái xác thực, thư mục hiện tại, renameFrom"),
                            new Item("StorageManager.java", "Xử lý file vật lý, chuẩn hóa đường dẫn chống Path Traversal (resolveSafe)"),
                            new Item("UserDatabase.java", "Xác thực danh tính người dùng (In-Memory HashMap / MySQL)")
                    ));

            // 2. DAO Package
            drawDetailBox(g, l2_x, dao_y, dao_w, dao_h, "📁 src/server/dao/ (Data Access Objects)",
                    List.of(
                            new Item("UserDAO.java", "Truy vấn, xác thực mật khẩu BCrypt & cập nhật trạng thái bảng users"),
                            new Item("FileDAO.java", "Quản lý metadata bảng files, dung lượng byte, mã SHA-256 Checksum"),
                            new Item("LogDAO.java", "Ghi nhật ký lịch sử truyền tải tệp tin vào bảng transfer_logs"),
                            new Item("ShareDAO.java", "Tạo và kiểm soát token truy cập liên kết chia sẻ bảng shared_files"),
                            new Item("PlanDAO.java", "Quản lý danh mục gói cước dung lượng lưu trữ bảng plans"),
                            new Item("OrderDAO.java", "Xử lý đơn hàng nâng cấp gói cước bảng orders"),
                            new Item("PaymentDAO.java", "Ghi nhận hóa đơn & biên lai thanh toán giao dịch bảng payments"),
                            new Item("SubscriptionDAO.java", "Quản lý thời hạn & gia hạn gói dung lượng bảng subscriptions")
                    ));

            // 3. DB Package
            drawDetailBox(g, l3_x, db_y, db_w, db_h, "📁 src/server/db/ (Database & Connection Pool)",
                    List.of(
                            new Item("DBConnection.java", "Quản trị kết nối JDBC Connection Pool HikariCP tối ưu hiệu năng"),
                            new Item("DatabaseConfig.java", "Tải thông số cấu hình URL, user, pass từ config/db.properties"),
                            new Item("DataReconciler.java", "Đối soát & đồng bộ toàn vẹn dữ liệu đĩa vật lý với CSDL MySQL")
                    ));

            // 4. Model Package
            drawDetailBox(g, l3_x, mdl_y, mdl_w, mdl_h, "📁 src/server/model/ (Domain Entities)",
                    List.of(
                            new Item("User.java", "Mô hình người dùng, mật khẩu băm, hạn mức dung lượng quota"),
                            new Item("FileMetadata.java", "Mô hình tệp tin, kích thước byte, sha256_hash, chủ sở hữu"),
                            new Item("TransferLog.java", "Mô hình nhật ký truyền tải: hành động, thời gian, trạng thái"),
                            new Item("SharedFile.java", "Mô hình tệp chia sẻ: token public, quyền READ/WRITE, hạn dùng"),
                            new Item("Plan.java", "Mô hình gói dung lượng: tên gói, giá tiền, dung lượng GB"),
                            new Item("Order.java", "Mô hình đơn hàng nâng cấp: mã đơn, tổng tiền, trạng thái"),
                            new Item("Payment.java", "Mô hình thanh toán: phương thức, mã giao dịch, thời gian"),
                            new Item("Subscription.java", "Mô hình thuê bao: ngày bắt đầu, ngày hết hạn, quota")
                    ));

            // 5. Util Package
            drawDetailBox(g, l3_x, utl_y, utl_w, utl_h, "📁 src/server/util/ (Security & Encryption)",
                    List.of(
                            new Item("SecurityUtil.java", "Băm mật khẩu BCrypt, tính Checksum SHA-256, sinh token ngẫu nhiên")
                    ));

            // 6. Config Box
            drawDetailBox(g, l2_x, cfg_box_y, cfg_box_w, cfg_box_h, "📁 config/ (Configuration)",
                    List.of(
                            new Item("db.properties", "Thông số kết nối CSDL MySQL: host, port, dbname, user, password"),
                            new Item("db.properties.example", "Tệp mẫu tham chiếu môi trường triển khai")
                    ));

            // 7. SQL Box
            drawDetailBox(g, l2_x, sql_box_y, sql_box_w, sql_box_h, "📁 sql/ (Database DDL Scripts)",
                    List.of(
                            new Item("schema.sql", "Kịch bản DDL tạo 8 bảng quan hệ: users, files, transfer_logs, plans...")
                    ));

            // 8. Storage Box
            drawDetailBox(g, l2_x, sto_box_y, sto_box_w, sto_box_h, "📁 storage/ (Physical Storage Sandbox)",
                    List.of(
                            new Item("storage/<username>/", "Thư mục Sandbox độc lập phân quyền riêng cho từng tài khoản"),
                            new Item(".upload.tmp", "Tệp tạm trong quá trình ghi dữ liệu nhị phân của lệnh STOR")
                    ));

            // 9. Test Suite Box
            drawDetailBox(g, l2_x, tst_box_y, tst_box_w, tst_box_h, "📁 test/ (Automated System Test Suite - 7 Phases)",
                    List.of(
                            new Item("TestPhase1Database.java", "Kiểm thử kết nối JDBC Connection Pool và cấu hình CSDL"),
                            new Item("TestPhase2AuthAndSandbox.java", "Kiểm thử xác thực, băm BCrypt và cô lập Sandbox chống Path Traversal"),
                            new Item("TestPhase3StorageAndQuota.java", "Kiểm thử truyền tệp STOR/RETR và đối soát hạn mức Quota dung lượng"),
                            new Item("TestPhase4SharingAndHistory.java", "Kiểm thử sinh link chia sẻ tệp public và ghi nhật ký kiểm toán"),
                            new Item("TestPhase5PlansAndPayment.java", "Kiểm thử nâng cấp gói dung lượng, thanh toán và gia hạn thuê bao"),
                            new Item("TestPhase6AdminDashboard.java", "Kiểm thử tính năng quản trị Admin: quản lý user, mở khóa, reset pass"),
                            new Item("TestPhase7SystemReliabilitySecurity.java", "Kiểm thử độ tin cậy, an toàn bảo mật và khả năng phục hồi lỗi")
                    ));

            g.dispose();

            // Save images
            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/server_directory_tree.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/server_directory_tree.png");
            ImageIO.write(img, "PNG", outBrain);
            System.out.println("SUCCESS: Saved Brain artifact copy to " + outBrain.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Item {
        String name;
        String desc;
        Item(String name, String desc) { this.name = name; this.desc = desc; }
    }

    private static void drawFolderBox(Graphics2D g, int x, int y, int w, int h, String name, String desc, boolean isRoot) {
        g.setColor(isRoot ? new Color(240, 240, 240) : new Color(248, 248, 248));
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_BORDER);
        g.drawRect(x, y, w, h);

        // Tab on top
        int tabW = 160;
        int tabH = 26;
        g.setColor(new Color(225, 225, 225));
        g.fillRect(x, y - tabH, tabW, tabH);
        g.setColor(Color.BLACK);
        g.drawRect(x, y - tabH, tabW, tabH);

        g.setFont(FONT_FOLDER_NAME);
        g.setColor(Color.BLACK);
        g.drawString(name, x + 20, y + 42);

        g.setFont(FONT_FOLDER_DESC);
        g.setColor(new Color(80, 80, 80));
        g.drawString(desc, x + 20, y + 78);
    }

    private static void drawDetailBox(Graphics2D g, int x, int y, int w, int h, String title, List<Item> items) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_BORDER);
        g.drawRect(x, y, w, h);

        // Header
        int headerH = 46;
        g.setColor(new Color(238, 238, 238));
        g.fillRect(x + 1, y + 1, w - 2, headerH);
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_DIVIDER);
        g.drawLine(x, y + headerH, x + w, y + headerH);

        g.setFont(FONT_BOX_TITLE);
        g.setColor(Color.BLACK);
        g.drawString(title, x + 16, y + 31);

        // Items
        int curY = y + headerH + 34;
        for (Item item : items) {
            g.setFont(FONT_FILE_NAME);
            g.setColor(Color.BLACK);
            String bullet = "📄 " + item.name;
            g.drawString(bullet, x + 18, curY);

            FontMetrics fm = g.getFontMetrics(FONT_FILE_NAME);
            int nameW = fm.stringWidth(bullet);

            g.setFont(FONT_FILE_DESC);
            g.setColor(new Color(85, 85, 85));
            g.drawString(" — " + item.desc, x + 18 + nameW, curY);

            curY += 38;
        }
    }

    private static void drawTreeBranch(Graphics2D g, int x1, int y1, int midX, int y2, int x2) {
        g.drawLine(x1, y1, midX, y1);
        g.drawLine(midX, y1, midX, y2);
        g.drawLine(midX, y2, x2, y2);

        // Draw small anchor circle at join
        g.fillOval(x1 - 3, y1 - 3, 6, 6);
        g.fillOval(x2 - 4, y2 - 4, 8, 8);
    }

    private static void drawStraightBranch(Graphics2D g, int x1, int y1, int x2, int y2) {
        int midX = (x1 + x2) / 2;
        g.drawLine(x1, y1, midX, y1);
        g.drawLine(midX, y1, midX, y2);
        g.drawLine(midX, y2, x2, y2);

        g.fillOval(x1 - 3, y1 - 3, 6, 6);
        g.fillOval(x2 - 4, y2 - 4, 8, 8);
    }
}


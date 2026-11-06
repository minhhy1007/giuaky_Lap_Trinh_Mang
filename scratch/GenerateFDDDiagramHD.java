package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class GenerateFDDDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_MAIN_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 22);

    private static final Font FONT_ROOT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_ROOT_SUB = new Font("Segoe UI", Font.BOLD, 22);

    private static final Font FONT_G_NUM = new Font("Segoe UI", Font.BOLD, 23);
    private static final Font FONT_G_NAME = new Font("Segoe UI", Font.BOLD, 20);

    private static final Font FONT_ITEM_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_ITEM_DESC = new Font("Segoe UI", Font.PLAIN, 15);

    // Strokes
    private static final Stroke STROKE_ROOT = new BasicStroke(3.0f);
    private static final Stroke STROKE_GROUP = new BasicStroke(2.6f);
    private static final Stroke STROKE_ITEM = new BasicStroke(2.0f);
    private static final Stroke STROKE_BUS = new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke STROKE_BRANCH = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

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
            g.setFont(FONT_MAIN_TITLE);
            String title = "SƠ ĐỒ PHÂN RÃ CHỨC NĂNG (FUNCTIONAL DECOMPOSITION DIAGRAM — FDD)";
            FontMetrics fm = g.getFontMetrics(FONT_MAIN_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 60);

            g.setFont(FONT_SUBTITLE);
            String sub = "Cấu trúc phân cấp chức năng hệ thống Secure FTP Client Pro & Server — Phân hệ chức năng và tác vụ con";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 98);

            // 3. Level 0: Root Function Box
            int rootW = 1680;
            int rootH = 140;
            int rootX = (W - rootW) / 2;
            int rootY = 135;

            drawRootBox(g, rootX, rootY, rootW, rootH,
                    "HỆ THỐNG TRUYỀN TỆP TIN AN TOÀN",
                    "(SECURE FTP CLIENT PRO & SERVER)");

            // 4. Level 1: 5 Functional Groups
            int colW = 610;
            int gap = 54;
            int startX = 120;
            int groupY = 410;
            int groupH = 145;

            int[] colX = new int[5];
            for (int i = 0; i < 5; i++) {
                colX[i] = startX + i * (colW + gap);
            }

            // Connectors Level 0 -> Level 1 (Orthogonal Bus Tree, No arrows)
            int busY = 345;
            g.setColor(new Color(40, 40, 40));
            g.setStroke(STROKE_BUS);

            // Drop from Root
            g.drawLine(W / 2, rootY + rootH, W / 2, busY);
            // Horizontal bus line
            g.drawLine(colX[0] + colW / 2, busY, colX[4] + colW / 2, busY);
            // Drops to 5 groups
            for (int i = 0; i < 5; i++) {
                int cx = colX[i] + colW / 2;
                g.drawLine(cx, busY, cx, groupY);
                // Anchor dots
                g.fillOval(cx - 4, busY - 4, 8, 8);
                g.fillOval(cx - 4, groupY - 4, 8, 8);
            }

            // Draw Level 1 Boxes
            drawGroupBox(g, colX[0], groupY, colW, groupH, "1.0", "QUẢN LÝ TÀI KHOẢN & XÁC THỰC");
            drawGroupBox(g, colX[1], groupY, colW, groupH, "2.0", "QUẢN LÝ TỆP TIN & THƯ MỤC");
            drawGroupBox(g, colX[2], groupY, colW, groupH, "3.0", "TRUYỀN TẢI DỮ LIỆU (STOR & RETR)");
            drawGroupBox(g, colX[3], groupY, colW, groupH, "4.0", "CHIA SẺ TỆP & DỊCH VỤ QUOTA");
            drawGroupBox(g, colX[4], groupY, colW, groupH, "5.0", "QUẢN TRỊ HỆ THỐNG & KIỂM TOÁN");

            // 5. Level 2: Sub-functions for each group
            // Column 1: Auth & Account
            List<Item> g1Items = List.of(
                    new Item("1.1 Đăng ký tài khoản", "Đăng ký tài khoản người dùng mới với mật khẩu băm BCrypt"),
                    new Item("1.2 Đăng nhập hệ thống", "Xác thực danh tính USER & PASS, kiểm tra khóa tài khoản"),
                    new Item("1.3 Đổi mật khẩu cá nhân", "Cập nhật mật khẩu bảo mật & xác thực mật khẩu cũ"),
                    new Item("1.4 Tùy chỉnh Theme giao diện", "Chuyển đổi giao diện: Dark Custom, Nimbus, Windows, Flat"),
                    new Item("1.5 Đăng xuất & Hủy phiên", "Gửi lệnh QUIT, hủy token phiên làm việc & đóng socket")
            );

            // Column 2: File & Directory Management
            List<Item> g2Items = List.of(
                    new Item("2.1 Duyệt danh sách tệp & thư mục", "Lệnh LIST, hiển thị bảng tệp tin với kích thước và thời gian"),
                    new Item("2.2 Tạo thư mục mới", "Lệnh MKD tạo thư mục trong Sandbox cô lập của người dùng"),
                    new Item("2.3 Xóa thư mục rỗng", "Lệnh RMD xóa bỏ thư mục rỗng trên máy chủ"),
                    new Item("2.4 Chuyển đổi thư mục làm việc", "Lệnh CWD, CDUP, PWD thay đổi thư mục hiện hành"),
                    new Item("2.5 Đổi tên tệp tin an toàn", "Lệnh RNFR & RNTO kiểm tra trùng tên & cập nhật CSDL"),
                    new Item("2.6 Xóa tệp tin & Hoàn trả Quota", "Lệnh DELE xóa tệp vật lý & trừ dung lượng used_storage")
            );

            // Column 3: Data Transfer (STOR & RETR)
            List<Item> g3Items = List.of(
                    new Item("3.1 Tải tệp lên máy chủ (Upload)", "Lệnh STOR đẩy luồng nhị phân nhị phân qua Socket TCP"),
                    new Item("3.2 Đối soát & Quản lý Quota", "Handshake kiểm tra dung lượng còn lại, chặn quá tải"),
                    new Item("3.3 Tải tệp về máy trạm (Download)", "Lệnh RETR tải tệp từ Server lưu vào thư mục downloads/"),
                    new Item("3.4 Giám sát tiến độ truyền tệp", "Cập nhật thanh Progress Bar thời gian thực qua SwingWorker"),
                    new Item("3.5 Kiểm tra toàn vẹn Checksum", "Tính toán & đối chiếu mã băm SHA-256 sau khi truyền"),
                    new Item("3.6 Xử lý lỗi gián đoạn mạng", "Tự động xóa tệp tạm dở dang (.tmp/.part) khi đứt kết nối")
            );

            // Column 4: Sharing & Quota Services
            List<Item> g4Items = List.of(
                    new Item("4.1 Tạo liên kết chia sẻ tệp", "Sinh mã Token ngẫu nhiên công khai cho tệp tin"),
                    new Item("4.2 Thiết lập quyền & Hạn dùng", "Phân quyền: VIEW (chỉ xem) hoặc DOWNLOAD; ngày hết hạn"),
                    new Item("4.3 Tải tệp chia sẻ qua Token", "Lệnh GETSHARED tải tệp an toàn không cần tài khoản chủ"),
                    new Item("4.4 Quản lý & Thu hồi chia sẻ", "Xem danh sách liên kết đã chia sẻ, hủy hoặc gia hạn token"),
                    new Item("4.5 Xem danh mục gói cước Quota", "Xem thông số các gói: Free 50MB, VIP1 1GB, VIP2 5GB"),
                    new Item("4.6 Đăng ký & Thanh toán mô phỏng", "Thanh toán mô phỏng Mock, tự động nâng cấp hạn mức Quota")
            );

            // Column 5: Administration & Audit
            List<Item> g5Items = List.of(
                    new Item("5.1 Quản trị tài khoản người dùng", "Xem danh sách, tìm kiếm & quản lý tất cả user hệ thống"),
                    new Item("5.2 Khóa / Mở khóa tài khoản", "Vô hiệu hóa quyền truy cập của tài khoản vi phạm"),
                    new Item("5.3 Phân quyền vai trò hệ thống", "Cấp quyền Standard User hoặc Administrator"),
                    new Item("5.4 Điều chỉnh hạn mức Quota", "Can thiệp điều chỉnh dung lượng lưu trữ tối đa thủ công"),
                    new Item("5.5 Giám sát đơn hàng & Doanh thu", "Xem lịch sử giao dịch nâng cấp, thống kê đơn hàng"),
                    new Item("5.6 Kiểm toán nhật ký hệ thống", "Truy vấn lịch sử upload/download chi tiết từ transfer_logs")
            );

            // Draw Sub-functions
            int itemH = 185;
            int itemGap = 65;
            int startItemY = 640;

            drawColumnItems(g, colX[0], colW, groupY + groupH, startItemY, itemH, itemGap, g1Items);
            drawColumnItems(g, colX[1], colW, groupY + groupH, startItemY, itemH, itemGap, g2Items);
            drawColumnItems(g, colX[2], colW, groupY + groupH, startItemY, itemH, itemGap, g3Items);
            drawColumnItems(g, colX[3], colW, groupY + groupH, startItemY, itemH, itemGap, g4Items);
            drawColumnItems(g, colX[4], colW, groupY + groupH, startItemY, itemH, itemGap, g5Items);

            g.dispose();

            // Save images
            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/functional_decomposition_diagram.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/functional_decomposition_diagram.png");
            ImageIO.write(img, "PNG", outBrain);
            System.out.println("SUCCESS: Saved Brain artifact copy to " + outBrain.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Item {
        String title;
        String desc;
        Item(String title, String desc) { this.title = title; this.desc = desc; }
    }

    private static void drawRootBox(Graphics2D g, int x, int y, int w, int h, String line1, String line2) {
        g.setColor(new Color(245, 245, 245));
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_ROOT);
        g.drawRect(x, y, w, h);

        FontMetrics fm1 = g.getFontMetrics(FONT_ROOT_TITLE);
        FontMetrics fm2 = g.getFontMetrics(FONT_ROOT_SUB);

        g.setFont(FONT_ROOT_TITLE);
        g.drawString(line1, x + (w - fm1.stringWidth(line1)) / 2, y + 55);

        g.setFont(FONT_ROOT_SUB);
        g.drawString(line2, x + (w - fm2.stringWidth(line2)) / 2, y + 105);
    }

    private static void drawGroupBox(Graphics2D g, int x, int y, int w, int h, String num, String name) {
        g.setColor(new Color(248, 248, 248));
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_GROUP);
        g.drawRect(x, y, w, h);

        // Header band
        int bandH = 46;
        g.setColor(new Color(230, 230, 230));
        g.fillRect(x + 1, y + 1, w - 2, bandH);
        g.setColor(Color.BLACK);
        g.drawLine(x, y + bandH, x + w, y + bandH);

        // Text
        g.setFont(FONT_G_NUM);
        FontMetrics fmN = g.getFontMetrics(FONT_G_NUM);
        g.drawString("PHÂN HỆ " + num, x + (w - fmN.stringWidth("PHÂN HỆ " + num)) / 2, y + 33);

        g.setFont(FONT_G_NAME);
        FontMetrics fmNm = g.getFontMetrics(FONT_G_NAME);

        // Word wrap for group name if necessary
        String[] words = name.split(" ");
        if (words.length > 3) {
            String l1 = words[0] + " " + words[1] + " " + words[2];
            StringBuilder sb = new StringBuilder();
            for (int k = 3; k < words.length; k++) sb.append(words[k]).append(" ");
            String l2 = sb.toString().trim();

            g.drawString(l1, x + (w - fmNm.stringWidth(l1)) / 2, y + 80);
            g.drawString(l2, x + (w - fmNm.stringWidth(l2)) / 2, y + 118);
        } else {
            g.drawString(name, x + (w - fmNm.stringWidth(name)) / 2, y + 100);
        }
    }

    private static void drawColumnItems(Graphics2D g, int colX, int colW, int groupBottomY,
                                        int startY, int itemH, int gap, List<Item> items) {
        int cx = colX + colW / 2;

        // Vertical spine line down the column
        int lastCenterY = startY + (items.size() - 1) * (itemH + gap) + itemH / 2;
        g.setColor(new Color(80, 80, 80));
        g.setStroke(STROKE_BRANCH);
        g.drawLine(cx, groupBottomY, cx, lastCenterY);

        int curY = startY;
        for (Item item : items) {
            int itemCenterY = curY + itemH / 2;

            // Horizontal branch line to box
            g.drawLine(cx, itemCenterY, colX + 10, itemCenterY);
            g.drawLine(cx, itemCenterY, colX + colW - 10, itemCenterY);
            g.fillOval(cx - 3, itemCenterY - 3, 6, 6);

            // Draw Item Box
            drawItemBox(g, colX + 10, curY, colW - 20, itemH, item.title, item.desc);

            curY += itemH + gap;
        }
    }

    private static void drawItemBox(Graphics2D g, int x, int y, int w, int h, String title, String desc) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);

        g.setColor(Color.BLACK);
        g.setStroke(STROKE_ITEM);
        g.drawRect(x, y, w, h);

        // Header strip
        int headH = 48;
        g.setColor(new Color(245, 245, 245));
        g.fillRect(x + 1, y + 1, w - 2, headH);
        g.setColor(Color.BLACK);
        g.drawLine(x, y + headH, x + w, y + headH);

        // Title
        g.setFont(FONT_ITEM_TITLE);
        g.drawString(title, x + 16, y + 32);

        // Description
        g.setFont(FONT_ITEM_DESC);
        g.setColor(new Color(60, 60, 60));

        // Multi-line wrap
        wrapText(g, desc, x + 16, y + headH + 34, w - 32, 28);
    }

    private static void wrapText(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        int curY = y;
        for (String word : words) {
            if (fm.stringWidth(currentLine + " " + word) < maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                g.drawString(currentLine.toString(), x, curY);
                curY += lineHeight;
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            g.drawString(currentLine.toString(), x, curY);
        }
    }
}


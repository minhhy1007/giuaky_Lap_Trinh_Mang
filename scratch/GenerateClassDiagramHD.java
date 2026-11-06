package scratch;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class GenerateClassDiagramHD {

    private static final int W = 3508; // A4 Landscape 300 DPI
    private static final int H = 2480;

    // Fonts
    private static final Font FONT_MAIN_TITLE = new Font("Segoe UI", Font.BOLD, 42);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 22);
    private static final Font FONT_PKG_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONT_CLASS_NAME = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_STEREOTYPE = new Font("Segoe UI", Font.ITALIC, 17);
    private static final Font FONT_MEMBER = new Font("Segoe UI", Font.PLAIN, 17);
    private static final Font FONT_MEMBER_BOLD = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font FONT_REL_LABEL = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_MULT = new Font("Segoe UI", Font.BOLD, 17);

    // Strokes
    private static final Stroke STROKE_PKG = new BasicStroke(2.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 6.0f}, 0.0f);
    private static final Stroke STROKE_CLASS_BORDER = new BasicStroke(2.4f);
    private static final Stroke STROKE_INNER_DIVIDER = new BasicStroke(1.6f);
    private static final Stroke STROKE_SOLID_REL = new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke STROKE_DASHED_REL = new BasicStroke(2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f, 6.0f}, 0.0f);

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
            String title = "SƠ ĐỒ LỚP (CLASS DIAGRAM) — HỆ THỐNG SECURE FTP CLIENT PRO & SERVER";
            FontMetrics fm = g.getFontMetrics(FONT_MAIN_TITLE);
            g.drawString(title, (W - fm.stringWidth(title)) / 2, 65);

            g.setFont(FONT_SUBTITLE);
            String sub = "Mô hình hóa cấu trúc 11 lớp phần mềm, thuộc tính, phương thức và quan hệ theo chuẩn UML 2.x";
            fm = g.getFontMetrics(FONT_SUBTITLE);
            g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 105);

            // 3. Package Frames
            // Package Server
            int pkg1_x = 80, pkg1_y = 145, pkg1_w = 2080, pkg1_h = 2270;
            drawPackageFrame(g, pkg1_x, pkg1_y, pkg1_w, pkg1_h, "Package server (Máy chủ FTP)");

            // Package Client
            int pkg2_x = 2210, pkg2_y = 145, pkg2_w = 1220, pkg2_h = 2270;
            drawPackageFrame(g, pkg2_x, pkg2_y, pkg2_w, pkg2_h, "Package client (Máy trạm FTP)");

            // --- 4. DRAW CLASSES ---
            // ================= SERVER PACKAGE =================
            // 1. TCPServer
            int tcp_x = 130, tcp_y = 220, tcp_w = 580, tcp_h = 320;
            drawClass(g, tcp_x, tcp_y, tcp_w, tcp_h, null, "TCPServer",
                    List.of("- PORT : int = 6789",
                            "- clientCount : AtomicInteger",
                            "- threadPool : ExecutorService"),
                    List.of("+ main(args : String[]) : void",
                            "+ startServer() : void"));

            // 2. ClientHandler
            int ch_x = 130, ch_y = 660, ch_w = 580, ch_h = 420;
            drawClass(g, ch_x, ch_y, ch_w, ch_h, "«implements Runnable»", "ClientHandler",
                    List.of("- clientSocket : Socket",
                            "- handler : CommandHandler",
                            "- clientId : String",
                            "- lineReader : ProtocolInputStream"),
                    List.of("+ run() : void",
                            "- handleCommands() : void",
                            "- closeConnection() : void"));

            // 3. ProtocolInputStream
            int pis_x = 130, pis_y = 1200, pis_w = 580, pis_h = 320;
            drawClass(g, pis_x, pis_y, pis_w, pis_h, null, "ProtocolInputStream",
                    List.of("- in : InputStream"),
                    List.of("+ readLine() : String",
                            "+ getRawInputStream() : InputStream"));

            // 4. CommandHandler
            int cmd_x = 800, cmd_y = 570, cmd_w = 660, cmd_h = 600;
            drawClass(g, cmd_x, cmd_y, cmd_w, cmd_h, null, "CommandHandler",
                    List.of("- session : Session",
                            "- userDatabase : UserDatabase",
                            "- storageManager : StorageManager"),
                    List.of("+ handle(rawInput : String) : String",
                            "+ handleStor(filename, in, out) : String",
                            "+ handleRetr(filename, out, rawOut) : String",
                            "- handleList() : String",
                            "- handleDele(filename : String) : String",
                            "- handleRnfr(oldName : String) : String",
                            "- handleRnto(newName : String) : String",
                            "- handlePwd() : String",
                            "- handleCwd(dirName : String) : String"));

            // 5. Session
            int ses_x = 1530, ses_y = 220, ses_w = 580, ses_h = 430;
            drawClass(g, ses_x, ses_y, ses_w, ses_h, null, "Session",
                    List.of("- state : State (UNAUTH / AUTH)",
                            "- pendingUsername : String",
                            "- loggedInUser : String",
                            "- currentDirectory : String",
                            "- renameFrom : String"),
                    List.of("+ isAuthenticated() : boolean",
                            "+ loginSuccess(username : String) : void",
                            "+ reset() : void",
                            "+ setRenameFrom(name : String) : void",
                            "+ clearRenameFrom() : void"));

            // 6. UserDatabase
            int udb_x = 1530, udb_y = 730, udb_w = 580, udb_h = 320;
            drawClass(g, udb_x, udb_y, udb_w, udb_h, null, "UserDatabase",
                    List.of("- users : Map<String, String>"),
                    List.of("+ validate(user : String, pass : String) : boolean",
                            "+ userExists(username : String) : boolean"));

            // 7. StorageManager
            int sm_x = 1530, sm_y = 1130, sm_w = 580, sm_h = 580;
            drawClass(g, sm_x, sm_y, sm_w, sm_h, null, "StorageManager",
                    List.of("- storageRoot : Path",
                            "- DATE_FORMAT : SimpleDateFormat"),
                    List.of("- resolveSafe(currentDir, path) : Path",
                            "+ listDirectory(userHome, currentDir) : String",
                            "+ receiveFile(userHome, cDir, ...) : String",
                            "+ sendFile(userHome, cDir, ...) : String",
                            "+ deleteFile(userHome, cDir, file) : String",
                            "+ renameFile(userHome, cDir, oldN, newN) : String",
                            "+ makeDirectory(userHome, cDir, dir) : String"));

            // ================= CLIENT PACKAGE =================
            // 8. LoginFrame
            int lf_x = 2260, lf_y = 220, lf_w = 530, lf_h = 420;
            drawClass(g, lf_x, lf_y, lf_w, lf_h, "«extends JFrame»", "LoginFrame",
                    List.of("- tfServer, tfPort : JTextField",
                            "- tfUsername : JTextField",
                            "- pfPassword : JPasswordField",
                            "- btnConnect, btnLogin : JButton",
                            "- ftpService : FtpClientService"),
                    List.of("- doConnect() : void",
                            "- doLogin() : void",
                            "- applyTheme(themeName : String) : void"));

            // 9. MainFrame
            int mf_x = 2260, mf_y = 750, mf_w = 530, mf_h = 510;
            drawClass(g, mf_x, mf_y, mf_w, mf_h, "«extends JFrame»", "MainFrame",
                    List.of("- fileTable : JTable",
                            "- progressBar : JProgressBar",
                            "- logArea : JTextArea",
                            "- ftpService : FtpClientService",
                            "- currentRemoteDir : String"),
                    List.of("- doUpload() : void",
                            "- doDownload() : void",
                            "- refreshFiles() : void",
                            "+ setProgress(pct : int, msg : String) : void",
                            "- initToolbar() : void"));

            // 10. ProgressListener (Interface)
            int pl_x = 2860, pl_y = 220, pl_w = 530, pl_h = 280;
            drawClass(g, pl_x, pl_y, pl_w, pl_h, "«interface»", "ProgressListener",
                    List.of(),
                    List.of("+ onProgress(percent : int,",
                            "      transferred : long,",
                            "      totalBytes : long) : void"));

            // 11. FtpClientService
            int fcs_x = 2860, fcs_y = 610, fcs_w = 530, fcs_h = 650;
            drawClass(g, fcs_x, fcs_y, fcs_w, fcs_h, null, "FtpClientService",
                    List.of("- socket : Socket",
                            "- socketIn : InputStream",
                            "- writer : PrintWriter",
                            "- connected : boolean",
                            "- loggedIn : boolean"),
                    List.of("+ connect(host : String, port : int) : boolean",
                            "+ login(user : String, pass : String) : String",
                            "+ list() : List<FileEntry>",
                            "+ stor(localFile, listener) : String",
                            "+ retr(filename, saveDir, listener) : String",
                            "+ dele(filename : String) : String",
                            "+ rename(oldName, newName) : String",
                            "+ disconnect() : void"));

            // 12. TCPClient (Console Client)
            int cli_x = 2260, cli_y = 1380, cli_w = 530, cli_h = 330;
            drawClass(g, cli_x, cli_y, cli_w, cli_h, null, "TCPClient",
                    List.of("- DEFAULT_HOST : String = \"127.0.0.1\"",
                            "- DEFAULT_PORT : int = 6789"),
                    List.of("+ main(args : String[]) : void",
                            "- handleDownload(filename, ...) : void",
                            "- handleUpload(filename, ...) : void",
                            "- readLine(in : InputStream) : String"));

            // --- 5. DRAW RELATIONSHIPS ---
            // Server relationships
            // Rel 1: TCPServer -> ClientHandler (Composition)
            drawRelationship(g, tcp_x + tcp_w / 2, tcp_y + tcp_h, ch_x + ch_w / 2, ch_y,
                    "composition", "", "1", "0..*");

            // Rel 2: ClientHandler -> ProtocolInputStream (Composition)
            drawRelationship(g, ch_x + ch_w / 2, ch_y + ch_h, pis_x + pis_w / 2, pis_y,
                    "composition", "", "1", "1");

            // Rel 3: ClientHandler -> CommandHandler (Composition)
            drawRelationship(g, ch_x + ch_w, ch_y + 180, cmd_x, cmd_y + 270,
                    "composition", "", "1", "1");

            // Rel 4: CommandHandler -> Session (Composition)
            drawRelationship(g, cmd_x + cmd_w, cmd_y + 120, ses_x, ses_y + 250,
                    "composition", "", "1", "1");

            // Rel 5: CommandHandler -> UserDatabase (Aggregation)
            drawRelationship(g, cmd_x + cmd_w, cmd_y + 300, udb_x, udb_y + 160,
                    "aggregation", "", "*", "1");

            // Rel 6: CommandHandler -> StorageManager (Aggregation)
            drawRelationship(g, cmd_x + cmd_w, cmd_y + 480, sm_x, sm_y + 200,
                    "aggregation", "", "*", "1");

            // Client relationships
            // Rel 7: LoginFrame -> MainFrame (Dependency: creates & opens)
            drawRelationship(g, lf_x + lf_w / 2, lf_y + lf_h, mf_x + mf_w / 2, mf_y,
                    "dependency", "«creates & opens»", "", "");

            // Rel 8: LoginFrame -> FtpClientService (Association)
            drawRelationship(g, lf_x + lf_w, lf_y + 280, fcs_x, fcs_y + 150,
                    "association", "", "1", "1");

            // Rel 9: MainFrame -> FtpClientService (Association)
            drawRelationship(g, mf_x + mf_w, mf_y + 250, fcs_x, fcs_y + 390,
                    "association", "", "1", "1");

            // Rel 10: Fcs -> ProgressListener (Aggregation)
            drawRelationship(g, fcs_x + fcs_w / 2, fcs_y, pl_x + pl_w / 2, pl_y + pl_h,
                    "aggregation", "", "1", "0..1");

            // Rel 11: MainFrame -> ProgressListener (Dependency / implements)
            drawRelationship(g, mf_x + mf_w - 40, mf_y, pl_x + 50, pl_y + pl_h,
                    "dependency", "«callback»", "", "");

            // Save images
            g.dispose();

            File outDocs = new File("d:/Lap_Trinh_Mang/giuaky_Lap_Trinh_Mang/docs/class_diagram.png");
            ImageIO.write(img, "PNG", outDocs);
            System.out.println("SUCCESS: Saved HD image to " + outDocs.getAbsolutePath());

            File outBrain = new File("C:/Users/ADMIN/.gemini/antigravity/brain/8b2670ad-186f-4216-86ba-e88882841ead/class_diagram.png");
            ImageIO.write(img, "PNG", outBrain);
            System.out.println("SUCCESS: Saved Brain artifact copy to " + outBrain.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: Draw Package Frame
    private static void drawPackageFrame(Graphics2D g, int x, int y, int w, int h, String name) {
        g.setColor(new Color(248, 248, 248));
        g.fillRect(x, y, w, h);

        g.setColor(new Color(90, 90, 90));
        g.setStroke(STROKE_PKG);
        g.drawRect(x, y, w, h);

        // Tab header
        int tabW = 420;
        int tabH = 45;
        g.setColor(new Color(235, 235, 235));
        g.fillRect(x, y, tabW, tabH);
        g.setColor(new Color(90, 90, 90));
        g.drawRect(x, y, tabW, tabH);

        g.setFont(FONT_PKG_TITLE);
        g.setColor(Color.BLACK);
        g.drawString(name, x + 20, y + 31);
    }

    // Helper: Draw 3-Compartment UML Class Box
    private static void drawClass(Graphics2D g, int x, int y, int w, int h, String stereotype, String className,
                                  List<String> attributes, List<String> methods) {
        // Box background & border
        g.setColor(Color.WHITE);
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_CLASS_BORDER);
        g.drawRect(x, y, w, h);

        // Header height calculation
        int headerH = (stereotype != null && !stereotype.isEmpty()) ? 62 : 46;
        g.setColor(new Color(245, 245, 245));
        g.fillRect(x + 1, y + 1, w - 2, headerH);
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_INNER_DIVIDER);
        g.drawLine(x, y + headerH, x + w, y + headerH);

        // Class Name & Stereotype
        FontMetrics fm;
        if (stereotype != null && !stereotype.isEmpty()) {
            g.setFont(FONT_STEREOTYPE);
            fm = g.getFontMetrics(FONT_STEREOTYPE);
            g.drawString(stereotype, x + (w - fm.stringWidth(stereotype)) / 2, y + 24);

            g.setFont(FONT_CLASS_NAME);
            fm = g.getFontMetrics(FONT_CLASS_NAME);
            g.drawString(className, x + (w - fm.stringWidth(className)) / 2, y + 50);
        } else {
            g.setFont(FONT_CLASS_NAME);
            fm = g.getFontMetrics(FONT_CLASS_NAME);
            g.drawString(className, x + (w - fm.stringWidth(className)) / 2, y + 32);
        }

        // Attributes Compartment
        int attrH = Math.max(38, attributes.size() * 26 + 18);
        int curY = y + headerH + 22;
        g.setFont(FONT_MEMBER);
        for (String attr : attributes) {
            drawMemberLine(g, attr, x + 16, curY);
            curY += 26;
        }

        // Divider between attributes and methods
        int div2Y = y + headerH + attrH;
        g.setColor(Color.BLACK);
        g.setStroke(STROKE_INNER_DIVIDER);
        g.drawLine(x, div2Y, x + w, div2Y);

        // Methods Compartment
        curY = div2Y + 24;
        for (String meth : methods) {
            drawMemberLine(g, meth, x + 16, curY);
            curY += 26;
        }
    }

    private static void drawMemberLine(Graphics2D g, String line, int lx, int ly) {
        g.setColor(Color.BLACK);
        if (line.startsWith("+") || line.startsWith("-") || line.startsWith("#")) {
            String vis = line.substring(0, 1);
            String rest = line.substring(1);
            g.setFont(FONT_MEMBER_BOLD);
            g.drawString(vis, lx, ly);
            g.setFont(FONT_MEMBER);
            g.drawString(rest, lx + 14, ly);
        } else {
            g.setFont(FONT_MEMBER);
            g.drawString(line, lx, ly);
        }
    }

    // Helper: Draw Relationship Line with UML Arrows / Diamonds / Multiplicities
    private static void drawRelationship(Graphics2D g, int x1, int y1, int x2, int y2,
                                         String type, String label, String mult1, String mult2) {
        g.setColor(Color.BLACK);

        if ("dependency".equals(type)) {
            g.setStroke(STROKE_DASHED_REL);
        } else {
            g.setStroke(STROKE_SOLID_REL);
        }

        g.drawLine(x1, y1, x2, y2);

        // Arrow / Diamond decoration
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double angleRev = Math.atan2(y1 - y2, x1 - x2);

        if ("composition".equals(type)) {
            drawDiamond(g, x1, y1, angle, true);
        } else if ("aggregation".equals(type)) {
            drawDiamond(g, x1, y1, angle, false);
        } else if ("association".equals(type)) {
            drawOpenArrow(g, x2, y2, angleRev);
        } else if ("dependency".equals(type)) {
            drawOpenArrow(g, x2, y2, angleRev);
        }

        // Multiplicities
        g.setFont(FONT_MULT);
        if (mult1 != null && !mult1.isEmpty()) {
            int mx1 = x1 + (int) (40 * Math.cos(angle)) - 10;
            int my1 = y1 + (int) (40 * Math.sin(angle)) - 10;
            g.drawString(mult1, mx1, my1);
        }
        if (mult2 != null && !mult2.isEmpty()) {
            int mx2 = x2 - (int) (40 * Math.cos(angle)) + 12;
            int my2 = y2 - (int) (40 * Math.sin(angle)) - 10;
            g.drawString(mult2, mx2, my2);
        }

        // Center Label
        if (label != null && !label.isEmpty()) {
            g.setFont(FONT_REL_LABEL);
            FontMetrics fm = g.getFontMetrics(FONT_REL_LABEL);
            int midX = (x1 + x2) / 2;
            int midY = (y1 + y2) / 2 - 8;
            int lw = fm.stringWidth(label);
            g.setColor(Color.WHITE);
            g.fillRect(midX - lw / 2 - 4, midY - fm.getAscent() + 2, lw + 8, fm.getHeight());
            g.setColor(Color.BLACK);
            g.drawString(label, midX - lw / 2, midY);
        }
    }

    private static void drawDiamond(Graphics2D g, int x, int y, double angle, boolean filled) {
        int dLen = 22;
        int dW = 9;
        int xMid = x + (int) (dLen * Math.cos(angle));
        int yMid = y + (int) (dLen * Math.sin(angle));
        int xEnd = x + (int) (2 * dLen * Math.cos(angle));
        int yEnd = y + (int) (2 * dLen * Math.sin(angle));

        double normAngle = angle + Math.PI / 2;
        int xTop = xMid + (int) (dW * Math.cos(normAngle));
        int yTop = yMid + (int) (dW * Math.sin(normAngle));
        int xBot = xMid - (int) (dW * Math.cos(normAngle));
        int yBot = yMid - (int) (dW * Math.sin(normAngle));

        Polygon p = new Polygon();
        p.addPoint(x, y);
        p.addPoint(xTop, yTop);
        p.addPoint(xEnd, yEnd);
        p.addPoint(xBot, yBot);

        if (filled) {
            g.setColor(Color.BLACK);
            g.fillPolygon(p);
        } else {
            g.setColor(Color.WHITE);
            g.fillPolygon(p);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.2f));
            g.drawPolygon(p);
        }
    }

    private static void drawOpenArrow(Graphics2D g, int x, int y, double angle) {
        int aLen = 18;
        int x1 = x + (int) (aLen * Math.cos(angle - Math.PI / 6));
        int y1 = y + (int) (aLen * Math.sin(angle - Math.PI / 6));
        int x2 = x + (int) (aLen * Math.cos(angle + Math.PI / 6));
        int y2 = y + (int) (aLen * Math.sin(angle + Math.PI / 6));

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2.4f));
        g.drawLine(x, y, x1, y1);
        g.drawLine(x, y, x2, y2);
    }
}


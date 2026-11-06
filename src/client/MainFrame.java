package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * GIAO DIỆN CHÍNH (MODERN FTP CLIENT MAIN FRAME)
 *
 * Thiết kế lại theo chuẩn Modern Dark Desktop Application:
 * - Bảng màu thống nhất: #111827, #1F2937, #273449, #374151, #F9FAFB, #9CA3AF.
 * - Header: Tên ứng dụng, Host, User/Role badge, Quota bar, Trạng thái kết nối.
 * - Sidebar: Menu điều hướng chuyên nghiệp (Tệp tin, Chia sẻ, Quản lý, Gói cước, Logs, Admin).
 * - Toolbar: Thao tác tệp nhóm chuẩn, kèm thanh tìm kiếm tệp tin (Search Bar).
 * - JTable: Hỗ trợ Sort, Zebra striping, Empty state, không dùng emoji gây lỗi font.
 * - Terminal: JSplitPane thu gọn/mở rộng linh hoạt, phân biệt lệnh gửi (→) và nhận (←).
 * - Dialogs: Toàn bộ chuyển sang JTable hiện đại, không dùng chuỗi thô.
 */
public class MainFrame extends JFrame {

    static {
        ThemeManager.applyTheme();
    }

    // ── Bảng màu chuẩn Modern Dark Desktop ────────────────────────────
    public static final Color CLR_BG        = new Color(0x11, 0x18, 0x27); // #111827
    public static final Color CLR_SIDEBAR   = new Color(0x1F, 0x29, 0x37); // #1F2937
    public static final Color CLR_CARD      = new Color(0x1F, 0x29, 0x37); // #1F2937
    public static final Color CLR_TABLE_BG  = new Color(0x27, 0x34, 0x49); // #273449
    public static final Color CLR_TABLE_ALT = new Color(0x1F, 0x2A, 0x3C); // #1F2A3C
    public static final Color CLR_TABLE_SEL = new Color(0x3B, 0x82, 0xF6); // #3B82F6
    public static final Color CLR_BORDER    = new Color(0x37, 0x41, 0x51); // #374151
    public static final Color CLR_TEXT      = new Color(0xF9, 0xFA, 0xFB); // #F9FAFB
    public static final Color CLR_MUTED     = new Color(0x9C, 0xA3, 0xAF); // #9CA3AF
    public static final Color CLR_PRIMARY   = new Color(0x3B, 0x82, 0xF6); // #3B82F6
    public static final Color CLR_SUCCESS   = new Color(0x22, 0xC5, 0x5E); // #22C55E
    public static final Color CLR_WARN      = new Color(0xF5, 0x9E, 0x0B); // #F59E0B
    public static final Color CLR_DANGER    = new Color(0xEF, 0x44, 0x44); // #EF4444

    // ── Service Layer ────────────────────────────────────────────────
    private final FtpClientService ftpService;

    // ── Components Header ────────────────────────────────────────────
    private JLabel       lblServer;
    private JLabel       lblUser;
    private JLabel       lblDir;
    private JLabel       lblStatus;
    private JLabel       lblQuota;
    private ModernButton btnChangePass;
    private ModernButton btnLogout;
    private ModernButton btnDisconnect;

    // ── Components Toolbar & File Table ──────────────────────────────
    private ModernButton      btnRefresh;
    private ModernButton      btnParentDir;
    private ModernButton      btnNewFolder;
    private ModernButton      btnUpload;
    private ModernButton      btnDownload;
    private ModernButton      btnDelete;
    private ModernButton      btnRename;
    private JTextField        tfSearch;
    private DefaultTableModel tableModel;
    private JTable            fileTable;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JLabel            lblEmptyState;

    // ── Sidebar Nav Buttons ──────────────────────────────────────────
    private ModernButton btnNavFiles;
    private ModernButton btnNavShare;
    private ModernButton btnNavSharedWithMe;
    private ModernButton btnNavMyShares;
    private ModernButton btnNavGetShared;
    private ModernButton btnNavPlans;
    private ModernButton btnNavLogs;
    private ModernButton btnNavAdmin;

    // ── Legacy Button references for internal compatibility ──────────
    private ModernButton btnPlans;
    private ModernButton btnAdminDashboard;
    private ModernButton btnShare;
    private ModernButton btnSharedWithMe;
    private ModernButton btnMyShares;
    private ModernButton btnGetShared;
    private ModernButton btnLogs;

    // ── Terminal & Progress Bar ──────────────────────────────────────
    private JProgressBar progressBar;
    private JTextArea    logArea;
    private JSplitPane   splitPane;

    public MainFrame(FtpClientService service) {
        this.ftpService = service;
        initUI();
        loadInitialData();
    }

    private void initUI() {
        setTitle("FTP Client Pro — Quản Lý Tệp Tin & Lưu Trữ Đám Mây");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1180, 750);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int ans = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Bạn có chắc muốn thoát ứng dụng không?",
                        "Xác nhận thoát", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ans == JOptionPane.YES_OPTION) {
                    ftpService.disconnect();
                    System.exit(0);
                }
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(CLR_BG);

        // Header trên cùng
        root.add(buildHeader(), BorderLayout.NORTH);

        // Thân chính: Sidebar bên trái + Content Area bên phải
        JPanel bodyPanel = new JPanel(new BorderLayout(0, 0));
        bodyPanel.setBackground(CLR_BG);
        bodyPanel.add(buildSidebar(), BorderLayout.WEST);
        bodyPanel.add(buildContentArea(), BorderLayout.CENTER);
        root.add(bodyPanel, BorderLayout.CENTER);

        // Status bar dưới cùng
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
        setVisible(true);
    }

    // ================================================================
    // 1. HEADER (TOP BAR)
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CLR_SIDEBAR);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER),
                new EmptyBorder(10, 18, 10, 18)));

        // Trái: Brand logo & Server info
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        JLabel lblBrand = new JLabel("FTP CLIENT PRO");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblBrand.setForeground(CLR_PRIMARY);

        lblServer = new JLabel("Máy chủ: localhost:6789");
        lblServer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblServer.setForeground(CLR_MUTED);

        String roleBadge = ftpService.isAdmin() ? " [ADMIN]" : " [USER]";
        lblUser = new JLabel("Người dùng: " + ftpService.getLoggedUser() + roleBadge + "  ● Đã kết nối");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(ftpService.isAdmin() ? CLR_WARN : CLR_TEXT);

        left.add(lblBrand);
        left.add(createHeaderSep());
        left.add(lblServer);
        left.add(createHeaderSep());
        left.add(lblUser);

        header.add(left, BorderLayout.WEST);

        // Phải: Quota info & Logout buttons
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        lblQuota = new JLabel("Quota: Đang tải...");
        lblQuota.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblQuota.setForeground(CLR_TEXT);
        lblQuota.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblQuota.setToolTipText("Bấm vào đây để nâng cấp hoặc gia hạn gói cước");
        lblQuota.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { doPlansAndSubscriptions(); }
        });

        btnChangePass = createHeaderButton("Đổi mật khẩu", CLR_TEXT, CLR_BORDER);
        btnLogout = createHeaderButton("Đăng xuất", CLR_WARN, CLR_WARN);
        btnDisconnect = createHeaderButton("Ngắt kết nối", CLR_DANGER, CLR_DANGER);

        btnChangePass.addActionListener(e -> doChangePassword());
        btnLogout.addActionListener(e -> doLogout());
        btnDisconnect.addActionListener(e -> doDisconnect());

        right.add(lblQuota);
        right.add(createHeaderSep());
        right.add(btnChangePass);
        right.add(btnLogout);
        right.add(btnDisconnect);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JSeparator createHeaderSep() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setForeground(CLR_BORDER);
        s.setPreferredSize(new Dimension(1, 16));
        return s;
    }

    private ModernButton createHeaderButton(String text, Color fgColor, Color borderColor) {
        ModernButton btn = new ModernButton(text, CLR_CARD, fgColor, borderColor);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 14, 28));
        return btn;
    }

    // ================================================================
    // 2. SIDEBAR (THANH ĐIỀU HƯỚNG BÊN TRÁI)
    // ================================================================

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(CLR_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(215, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, CLR_BORDER),
                new EmptyBorder(14, 10, 14, 10)));

        JLabel lblNavTitle = new JLabel("ĐIỀU HƯỚNG CHỨC NĂNG");
        lblNavTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNavTitle.setForeground(CLR_MUTED);
        lblNavTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblNavTitle.setBorder(new EmptyBorder(0, 8, 10, 0));
        sidebar.add(lblNavTitle);

        btnNavFiles = createSidebarBtn("Tệp tin & Thư mục", true);
        btnNavShare = createSidebarBtn("Chia sẻ tệp tin", false);
        btnNavSharedWithMe = createSidebarBtn("Được chia sẻ cho tôi", false);
        btnNavMyShares = createSidebarBtn("Quản lý đã chia sẻ", false);
        btnNavGetShared = createSidebarBtn("Tải qua mã Token", false);
        btnNavPlans = createSidebarBtn("Gói cước & Nâng cấp", false);
        btnNavLogs = createSidebarBtn("Nhật ký hoạt động", false);

        // Gán tham chiếu legacy
        btnPlans = btnNavPlans;
        btnShare = btnNavShare;
        btnSharedWithMe = btnNavSharedWithMe;
        btnMyShares = btnNavMyShares;
        btnGetShared = btnNavGetShared;
        btnLogs = btnNavLogs;

        sidebar.add(btnNavFiles);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavShare);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavSharedWithMe);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavMyShares);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavGetShared);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavPlans);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavLogs);

        // Nếu là Admin thì thêm nút Admin Dashboard
        if (ftpService.isAdmin()) {
            sidebar.add(Box.createVerticalStrut(14));
            JSeparator sepAdmin = new JSeparator();
            sepAdmin.setForeground(CLR_BORDER);
            sepAdmin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            sidebar.add(sepAdmin);
            sidebar.add(Box.createVerticalStrut(10));

            btnNavAdmin = createSidebarBtn("Quản trị hệ thống", false);
            btnNavAdmin.setForeground(CLR_WARN);
            btnAdminDashboard = btnNavAdmin;
            btnNavAdmin.addActionListener(e -> new AdminDashboardDialog(MainFrame.this, ftpService).setVisible(true));
            sidebar.add(btnNavAdmin);
        }

        sidebar.add(Box.createVerticalGlue());

        // Gắn sự kiện các nút Sidebar
        btnNavFiles.addActionListener(e -> doRefresh());
        btnNavShare.addActionListener(e -> doShare());
        btnNavSharedWithMe.addActionListener(e -> doSharedWithMe());
        btnNavMyShares.addActionListener(e -> doMyShares());
        btnNavGetShared.addActionListener(e -> doGetShared());
        btnNavPlans.addActionListener(e -> doPlansAndSubscriptions());
        btnNavLogs.addActionListener(e -> doViewLogs());

        return sidebar;
    }

    private ModernButton createSidebarBtn(String text, boolean active) {
        Color bg = active ? CLR_PRIMARY : CLR_SIDEBAR;
        Color fg = active ? Color.WHITE : CLR_MUTED;
        ModernButton btn = new ModernButton(text, bg, fg, null);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    // ================================================================
    // 3. KHU VỰC NỘI DUNG CHÍNH (CONTENT AREA)
    // ================================================================

    private JPanel buildContentArea() {
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(CLR_BG);

        // ── Toolbar thao tác file phía trên ───────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBackground(CLR_CARD);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER),
                new EmptyBorder(8, 12, 8, 12)));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftActions.setOpaque(false);

        btnRefresh   = createToolBtn("Làm mới",     CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnParentDir = createToolBtn("Thư mục cha", CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnNewFolder = createToolBtn("+ Thư mục",   CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnUpload    = createToolBtn("Tải lên",     CLR_PRIMARY, Color.WHITE, null);
        btnDownload  = createToolBtn("Tải về",      CLR_SUCCESS, Color.WHITE, null);
        btnRename    = createToolBtn("Đổi tên",     CLR_CARD, CLR_WARN, CLR_WARN);
        btnDelete    = createToolBtn("Xóa",         CLR_CARD, CLR_DANGER, CLR_DANGER);

        leftActions.add(btnRefresh);
        leftActions.add(btnParentDir);
        leftActions.add(btnNewFolder);
        leftActions.add(btnUpload);
        leftActions.add(btnDownload);
        leftActions.add(btnRename);
        leftActions.add(btnDelete);

        toolbar.add(leftActions, BorderLayout.WEST);

        // Search Bar bên phải Toolbar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSearch.setForeground(CLR_MUTED);

        tfSearch = new JTextField(15);
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfSearch.setBackground(CLR_BG);
        tfSearch.setForeground(CLR_TEXT);
        tfSearch.setCaretColor(CLR_TEXT);
        tfSearch.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)));

        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterFiles(); }
            @Override public void removeUpdate(DocumentEvent e) { filterFiles(); }
            @Override public void changedUpdate(DocumentEvent e) { filterFiles(); }
        });

        searchPanel.add(lblSearch);
        searchPanel.add(tfSearch);
        toolbar.add(searchPanel, BorderLayout.EAST);

        // ── Path Breadcrumb bar ───────────────────────────────────────
        JPanel pathBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        pathBar.setBackground(CLR_BG);
        pathBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER));

        lblDir = new JLabel("Vị trí: /");
        lblDir.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDir.setForeground(CLR_PRIMARY);
        pathBar.add(lblDir);

        JPanel topHeaderContainer = new JPanel(new BorderLayout());
        topHeaderContainer.add(toolbar, BorderLayout.NORTH);
        topHeaderContainer.add(pathBar, BorderLayout.SOUTH);

        // ── Bảng danh sách file ───────────────────────────────────────
        String[] columns = {"Tên file / Thư mục", "Loại", "Kích thước", "Ngày sửa đổi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        fileTable = new JTable(tableModel);
        fileTable.setBackground(CLR_TABLE_BG);
        fileTable.setForeground(CLR_TEXT);
        fileTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileTable.setRowHeight(30);
        fileTable.setSelectionBackground(CLR_TABLE_SEL);
        fileTable.setSelectionForeground(Color.WHITE);
        fileTable.setGridColor(new Color(0x37, 0x41, 0x51));
        fileTable.setShowVerticalLines(false);

        fileTable.getTableHeader().setBackground(CLR_CARD);
        fileTable.getTableHeader().setForeground(CLR_MUTED);
        fileTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        fileTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER));
        fileTable.getTableHeader().setPreferredSize(new Dimension(0, 32));

        // Thiết lập bộ sắp xếp (Sorter) với so sánh dung lượng thực tế
        tableSorter = new TableRowSorter<>(tableModel);
        tableSorter.setComparator(2, (o1, o2) -> {
            long b1 = parseSizeToBytes(String.valueOf(o1));
            long b2 = parseSizeToBytes(String.valueOf(o2));
            return Long.compare(b1, b2);
        });
        fileTable.setRowSorter(tableSorter);

        // Chiều rộng các cột
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(360);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        // Render xen kẽ màu (Zebra Striping)
        fileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? CLR_TABLE_SEL : (row % 2 == 0 ? CLR_TABLE_BG : CLR_TABLE_ALT));
                setForeground(sel ? Color.WHITE : CLR_TEXT);
                setBorder(new EmptyBorder(0, 10, 0, 10));

                if (col == 1) { // Cột loại
                    setHorizontalAlignment(SwingConstants.CENTER);
                    String type = String.valueOf(v);
                    if ("Thư mục".equals(type)) {
                        setForeground(sel ? Color.WHITE : CLR_PRIMARY);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    }
                } else if (col == 2) { // Kích thước
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (col == 3) { // Ngày sửa đổi
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else { // Tên file / Thư mục
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return this;
            }
        });

        // Double-click mở thư mục con
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = fileTable.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = fileTable.convertRowIndexToModel(viewRow);
                        String type = (String) tableModel.getValueAt(modelRow, 1);
                        String name = (String) tableModel.getValueAt(modelRow, 0);
                        if ("Thư mục".equals(type)) {
                            doCwd(name);
                        } else {
                            doDownload();
                        }
                    }
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(fileTable);
        tableScroll.setBackground(CLR_TABLE_BG);
        tableScroll.getViewport().setBackground(CLR_TABLE_BG);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        // Panel bao quanh bảng + Empty State nếu không có tệp
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(CLR_TABLE_BG);
        tableContainer.add(tableScroll, BorderLayout.CENTER);

        lblEmptyState = new JLabel("Thư mục trống. Bạn có thể bấm nút 'Tải lên' để thêm tệp tin đầu tiên.", SwingConstants.CENTER);
        lblEmptyState.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblEmptyState.setForeground(CLR_MUTED);
        lblEmptyState.setVisible(false);
        tableContainer.add(lblEmptyState, BorderLayout.SOUTH);

        // ── Progress Bar ──────────────────────────────────────────────
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng");
        progressBar.setValue(0);
        progressBar.setBackground(CLR_CARD);
        progressBar.setForeground(CLR_SUCCESS);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressBar.setBorder(new EmptyBorder(3, 8, 3, 8));

        JPanel centerTablePanel = new JPanel(new BorderLayout());
        centerTablePanel.add(topHeaderContainer, BorderLayout.NORTH);
        centerTablePanel.add(tableContainer, BorderLayout.CENTER);
        centerTablePanel.add(progressBar, BorderLayout.SOUTH);

        // ── Terminal Log ở phía dưới ─────────────────────────────────
        JPanel terminalPanel = new JPanel(new BorderLayout());
        terminalPanel.setBackground(new Color(0x0F, 0x17, 0x2A));

        JPanel termHeader = new JPanel(new BorderLayout());
        termHeader.setBackground(new Color(0x1E, 0x29, 0x3B));
        termHeader.setBorder(new EmptyBorder(4, 10, 4, 10));

        JLabel lblTerm = new JLabel("NHẬT KÝ LỆNH MẠNG (TERMINAL LOG)");
        lblTerm.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTerm.setForeground(CLR_MUTED);

        ModernButton btnClearLog = new ModernButton("Xóa nhật ký", CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnClearLog.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClearLog.setPreferredSize(new Dimension(90, 24));
        btnClearLog.addActionListener(e -> logArea.setText(""));

        termHeader.add(lblTerm, BorderLayout.WEST);
        termHeader.add(btnClearLog, BorderLayout.EAST);
        terminalPanel.add(termHeader, BorderLayout.NORTH);

        logArea = new JTextArea(5, 0);
        logArea.setEditable(false);
        logArea.setBackground(new Color(0x0F, 0x17, 0x2A));
        logArea.setForeground(new Color(0x86, 0xEF, 0xAC)); // Xanh lá dịu mắt
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(6, 10, 6, 10));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        terminalPanel.add(logScroll, BorderLayout.CENTER);

        // Dùng JSplitPane dọc chia bảng và terminal
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerTablePanel, terminalPanel);
        splitPane.setDividerSize(4);
        splitPane.setResizeWeight(0.8);
        splitPane.setBorder(null);
        splitPane.setBackground(CLR_BG);

        content.add(splitPane, BorderLayout.CENTER);

        // Gắn sự kiện Toolbar
        btnRefresh.addActionListener(e -> doRefresh());
        btnParentDir.addActionListener(e -> doCdup());
        btnNewFolder.addActionListener(e -> doNewFolder());
        btnUpload.addActionListener(e -> doUpload());
        btnDownload.addActionListener(e -> doDownload());
        btnRename.addActionListener(e -> doRename());
        btnDelete.addActionListener(e -> doDelete());

        return content;
    }

    private ModernButton createToolBtn(String text, Color bg, Color fg, Color border) {
        ModernButton btn = new ModernButton(text, bg, fg, border);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 32));
        return btn;
    }

    // ================================================================
    // 4. STATUS BAR PHÍA DƯỚI
    // ================================================================

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CLR_SIDEBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CLR_BORDER),
                new EmptyBorder(4, 14, 4, 14)));

        JLabel lblVer = new JLabel("FTP Project — Client/Server Network Programming | Standard Charset: UTF-8");
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVer.setForeground(CLR_MUTED);

        lblStatus = new JLabel("● Sẵn sàng");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(CLR_SUCCESS);

        bar.add(lblVer, BorderLayout.WEST);
        bar.add(lblStatus, BorderLayout.EAST);
        return bar;
    }

    // ================================================================
    // LOGIC THAO TÁC FILE: REFRESH, UPLOAD, DOWNLOAD, DELETE, RENAME
    // ================================================================

    private void filterFiles() {
        String query = tfSearch.getText().trim();
        if (query.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 0));
        }
    }

    private void loadInitialData() {
        updateDirLabel();
        updateQuotaLabel();
        doRefresh();
    }

    private void doRefresh() {
        setButtons(false);
        setStatus("Đang tải danh sách tệp...", CLR_PRIMARY);
        appendLog("→ LIST");

        new SwingWorker<List<FtpClientService.FileEntry>, Void>() {
            @Override
            protected List<FtpClientService.FileEntry> doInBackground() throws Exception {
                return ftpService.list();
            }

            @Override
            protected void done() {
                try {
                    List<FtpClientService.FileEntry> entries = get();
                    tableModel.setRowCount(0);

                    for (FtpClientService.FileEntry entry : entries) {
                        String typeStr = entry.isDirectory ? "Thư mục" : "Tệp tin";
                        String sizeStr = entry.isDirectory ? "-" : formatBytes(entry.size);
                        String dateStr = entry.date;
                        tableModel.addRow(new Object[]{entry.name, typeStr, sizeStr, dateStr});
                    }

                    lblEmptyState.setVisible(entries.isEmpty());
                    setStatus("Sẵn sàng (" + entries.size() + " mục)", CLR_SUCCESS);
                    appendLog("← 226 OK. " + entries.size() + " mục.");
                } catch (Exception ex) {
                    setStatus("Lỗi tải danh sách: " + ex.getMessage(), CLR_DANGER);
                    appendLog("← Lỗi: " + ex.getMessage());
                } finally {
                    setButtons(true);
                    updateDirLabel();
                }
            }
        }.execute();
    }

    private void doUpload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn tệp tin để tải lên Server");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.exists()) return;

        setButtons(false);
        setStatus("Đang tải lên: " + file.getName() + "...", CLR_PRIMARY);
        appendLog("→ STOR " + file.getName() + " (" + formatBytes(file.length()) + ")");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.stor(file, (transferred, total) -> {
                    int pct = (total > 0) ? (int) (transferred * 100 / total) : 0;
                    MainFrame.this.setProgress(pct, "Đang tải lên... " + pct + "% (" + formatBytes(transferred) + "/" + formatBytes(total) + ")");
                });
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("226")) {
                        setStatus("Tải lên hoàn tất: " + file.getName(), CLR_SUCCESS);
                        appendLog("← " + resp);
                        updateQuotaLabel();
                        doRefresh();
                    } else {
                        String err = (resp != null) ? resp : "Tải lên thất bại";
                        setStatus("Tải lên thất bại: " + err, CLR_DANGER);
                        appendLog("← " + err);
                        showError(err);
                    }
                } catch (Exception ex) {
                    showError("Lỗi tải lên: " + ex.getMessage());
                } finally {
                    MainFrame.this.setProgress(0, "Sẵn sàng");
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doDownload() {
        int viewRow = fileTable.getSelectedRow();
        if (viewRow < 0) {
            showWarn("Vui lòng chọn một tệp tin trong bảng để tải về!");
            return;
        }

        int modelRow = fileTable.convertRowIndexToModel(viewRow);
        String filename = (String) tableModel.getValueAt(modelRow, 0);
        String type     = (String) tableModel.getValueAt(modelRow, 1);

        if ("Thư mục".equals(type)) {
            showWarn("Không thể tải về thư mục. Vui lòng chọn một tệp tin!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(filename));
        chooser.setDialogTitle("Lưu tệp tin tải về");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File dest = chooser.getSelectedFile();

        setButtons(false);
        setStatus("Đang tải về: " + filename + "...", CLR_PRIMARY);
        appendLog("→ RETR " + filename);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.retr(filename, dest, (transferred, total) -> {
                    int pct = (total > 0) ? (int) (transferred * 100 / total) : 0;
                    MainFrame.this.setProgress(pct, "Đang tải về... " + pct + "% (" + formatBytes(transferred) + "/" + formatBytes(total) + ")");
                });
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("226")) {
                        setStatus("Tải về hoàn tất: " + filename, CLR_SUCCESS);
                        appendLog("← " + resp);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Tệp tin đã được lưu thành công tại:\n" + dest.getAbsolutePath(),
                                "Tải về thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showError("Tải về thất bại: " + resp);
                    }
                } catch (Exception ex) {
                    showError("Lỗi tải về: " + ex.getMessage());
                } finally {
                    MainFrame.this.setProgress(0, "Sẵn sàng");
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doDelete() {
        int viewRow = fileTable.getSelectedRow();
        if (viewRow < 0) {
            showWarn("Vui lòng chọn một tệp hoặc thư mục cần xóa!");
            return;
        }

        int modelRow = fileTable.convertRowIndexToModel(viewRow);
        String filename = (String) tableModel.getValueAt(modelRow, 0);
        String type     = (String) tableModel.getValueAt(modelRow, 1);
        boolean isFolder = "Thư mục".equals(type);

        String promptMsg = isFolder
                ? "Bạn có chắc chắn muốn xóa thư mục:\n\n\"" + filename + "\"\n\nLưu ý: Thư mục phải rỗng mới có thể xóa!"
                : "Bạn có chắc chắn muốn xóa tệp tin:\n\n\"" + filename + "\"\n\nHành động này không thể hoàn tác!";

        int confirm = JOptionPane.showConfirmDialog(this, promptMsg,
                isFolder ? "Xác nhận xóa thư mục" : "Xác nhận xóa tệp tin",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        setButtons(false);
        setStatus("Đang xóa: " + filename, CLR_WARN);
        appendLog("→ " + (isFolder ? "RMD " : "DELE ") + filename);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return isFolder ? ftpService.rmd(filename) : ftpService.dele(filename);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && (resp.startsWith("250") || resp.startsWith("200") || resp.startsWith("226"))) {
                        setStatus("Đã xóa: " + filename, CLR_SUCCESS);
                        appendLog("← " + resp);
                        updateQuotaLabel();
                        doRefresh();
                    } else {
                        showError("Xóa thất bại: " + resp);
                    }
                } catch (Exception ex) {
                    showError("Lỗi xóa: " + ex.getMessage());
                } finally {
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doRename() {
        int viewRow = fileTable.getSelectedRow();
        if (viewRow < 0) {
            showWarn("Vui lòng chọn một mục để đổi tên!");
            return;
        }

        int modelRow = fileTable.convertRowIndexToModel(viewRow);
        String oldName = (String) tableModel.getValueAt(modelRow, 0);

        String newName = JOptionPane.showInputDialog(this, "Nhập tên mới cho \"" + oldName + "\":", oldName);
        if (newName == null || newName.trim().isEmpty() || newName.trim().equals(oldName)) return;

        setButtons(false);
        setStatus("Đang đổi tên: " + oldName + " → " + newName, CLR_PRIMARY);
        appendLog("→ RNFR " + oldName + " | RNTO " + newName);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.rename(oldName, newName.trim());
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && (resp.startsWith("250") || resp.startsWith("200"))) {
                        setStatus("Đã đổi tên thành công!", CLR_SUCCESS);
                        appendLog("← " + resp);
                        doRefresh();
                    } else {
                        showError("Đổi tên thất bại: " + resp);
                    }
                } catch (Exception ex) {
                    showError("Lỗi đổi tên: " + ex.getMessage());
                } finally {
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doCwd(String dirName) {
        setButtons(false);
        setStatus("Đang chuyển thư mục: " + dirName, CLR_PRIMARY);
        appendLog("→ CWD " + dirName);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.cwd(dirName);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && (resp.startsWith("250") || resp.startsWith("200"))) {
                        appendLog("← " + resp);
                        updateDirLabel();
                        doRefresh();
                    } else {
                        showError("Không thể vào thư mục: " + resp);
                    }
                } catch (Exception ex) {
                    showError("Lỗi CWD: " + ex.getMessage());
                } finally {
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doCdup() {
        setButtons(false);
        setStatus("Đang lùi về thư mục cha...", CLR_PRIMARY);
        appendLog("→ CDUP");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.cdup();
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && (resp.startsWith("250") || resp.startsWith("200"))) {
                        appendLog("← " + resp);
                        updateDirLabel();
                        doRefresh();
                    } else {
                        showError("Không thể lùi thư mục: " + resp);
                    }
                } catch (Exception ex) {
                    showError("Lỗi CDUP: " + ex.getMessage());
                } finally {
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doNewFolder() {
        String folderName = JOptionPane.showInputDialog(this, "Nhập tên thư mục mới cần tạo:");
        if (folderName == null || folderName.trim().isEmpty()) return;

        setButtons(false);
        setStatus("Đang tạo thư mục mới: " + folderName, CLR_PRIMARY);
        appendLog("→ MKD " + folderName.trim());

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.mkd(folderName.trim());
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && (resp.startsWith("257") || resp.startsWith("250") || resp.startsWith("200"))) {
                        setStatus("Tạo thư mục thành công!", CLR_SUCCESS);
                        appendLog("← " + resp);
                        doRefresh();
                    } else {
                        showError("Tạo thư mục thất bại: " + resp);
                    }
                } catch (Exception ex) {
                    showError("Lỗi tạo thư mục: " + ex.getMessage());
                } finally {
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doLogout() {
        int ans = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đăng xuất khỏi phiên làm việc hiện tại?",
                "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                ftpService.logout();
            } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> {
                new LoginFrame();
                MainFrame.this.dispose();
            });
        }).start();
    }

    private void doDisconnect() {
        new Thread(() -> {
            ftpService.disconnect();
            SwingUtilities.invokeLater(() -> {
                new LoginFrame();
                MainFrame.this.dispose();
            });
        }).start();
    }

    // ================================================================
    // 5. THIẾT KẾ LẠI CÁC DIALOG (CHIA SẺ, QUẢN LÝ, AUDIT LOG, GÓI CƯỚC)
    // ================================================================

    private void doShare() {
        int viewRow = fileTable.getSelectedRow();
        String defaultFile = "";
        if (viewRow >= 0) {
            int modelRow = fileTable.convertRowIndexToModel(viewRow);
            String type = (String) tableModel.getValueAt(modelRow, 1);
            if ("Tệp tin".equals(type)) {
                defaultFile = (String) tableModel.getValueAt(modelRow, 0);
            }
        }

        JDialog dlg = new JDialog(this, "Tạo liên kết chia sẻ tệp tin", true);
        dlg.setSize(480, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CLR_BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblTitle = new JLabel("CHIA SẺ TỆP TIN AN TOÀN");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(CLR_PRIMARY);
        root.add(lblTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 1, 0, 10));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(16, 0, 16, 0));

        JTextField tfFile = new JTextField(defaultFile);
        tfFile.setBackground(CLR_CARD);
        tfFile.setForeground(CLR_TEXT);
        tfFile.setBorder(BorderFactory.createCompoundBorder(new LineBorder(CLR_BORDER, 1), new EmptyBorder(6, 10, 6, 10)));

        JTextField tfRecipient = new JTextField();
        tfRecipient.setBackground(CLR_CARD);
        tfRecipient.setForeground(CLR_TEXT);
        tfRecipient.setBorder(BorderFactory.createCompoundBorder(new LineBorder(CLR_BORDER, 1), new EmptyBorder(6, 10, 6, 10)));

        JComboBox<String> cbPerm = new JComboBox<>(new String[]{"DOWNLOAD (Cho phép tải về)", "VIEW (Chỉ xem danh mục)"});
        JComboBox<String> cbExp = new JComboBox<>(new String[]{"1 ngày", "7 ngày", "30 ngày", "Vĩnh viễn"});

        form.add(createDialogFieldGroup("Tên tệp tin cần chia sẻ:", tfFile));
        form.add(createDialogFieldGroup("Người nhận đích danh (để trống nếu muốn chia sẻ công khai):", tfRecipient));
        form.add(createDialogFieldGroup("Quyền hạn truy cập:", cbPerm));
        form.add(createDialogFieldGroup("Thời gian hết hạn liên kết:", cbExp));

        root.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Hủy bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dlg.dispose());

        ModernButton btnConfirm = new ModernButton("Tạo chia sẻ ngay", CLR_PRIMARY, Color.WHITE);
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnConfirm.addActionListener(e -> {
            String fname = tfFile.getText().trim();
            if (fname.isEmpty()) {
                showWarn("Vui lòng nhập tên tệp cần chia sẻ!");
                return;
            }
            String recipient = tfRecipient.getText().trim();
            String perm = cbPerm.getSelectedIndex() == 0 ? "DOWNLOAD" : "VIEW";
            int days = (cbExp.getSelectedIndex() == 0) ? 1 : (cbExp.getSelectedIndex() == 1 ? 7 : (cbExp.getSelectedIndex() == 2 ? 30 : 0));
            int hours = days * 24;

            dlg.dispose();
            setButtons(false);
            setStatus("Đang tạo liên kết chia sẻ...", CLR_PRIMARY);

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return ftpService.share(fname, recipient, perm, hours, 0);
                }

                @Override
                protected void done() {
                    try {
                        String resp = get();
                        if (resp != null && resp.startsWith("250")) {
                            setStatus("Chia sẻ thành công!", CLR_SUCCESS);
                            appendLog("← " + resp);
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    "Đã tạo chia sẻ thành công!\n" + resp,
                                    "Chia sẻ thành công", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showError("Chia sẻ thất bại: " + resp);
                        }
                    } catch (Exception ex) {
                        showError("Lỗi chia sẻ: " + ex.getMessage());
                    } finally {
                        setButtons(true);
                    }
                }
            }.execute();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private JPanel createDialogFieldGroup(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(CLR_TEXT);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void doSharedWithMe() {
        setButtons(false);
        setStatus("Đang tải danh sách tệp được chia sẻ...", CLR_PRIMARY);

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ftpService.getSharedWithMe();
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    showSharedWithMeDialog(lines);
                } catch (Exception ex) {
                    showError("Lỗi tải danh sách: " + ex.getMessage());
                } finally {
                    setButtons(true);
                    setStatus("Sẵn sàng", CLR_SUCCESS);
                }
            }
        }.execute();
    }

    private void showSharedWithMeDialog(List<String> lines) {
        JDialog dlg = new JDialog(this, "Tệp tin được chia sẻ cho bạn", true);
        dlg.setSize(860, 460);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(CLR_BG);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel titleLbl = new JLabel("Danh sách các tệp tin người khác đã gửi chia sẻ cho bạn:");
        titleLbl.setForeground(CLR_TEXT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        root.add(titleLbl, BorderLayout.NORTH);

        String[] cols = {"ID", "Tên tệp tin", "Dung lượng", "Người gửi", "Quyền hạn", "Mã Token"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (String raw : lines) {
            String l = raw.trim();
            if (l.matches("^\\d+\\s+.*")) {
                String[] parts = l.split("\\s+");
                if (parts.length >= 6) {
                    String id = parts[0];
                    String token = parts[parts.length - 1];
                    String perm = parts[parts.length - 2];
                    String owner = parts[parts.length - 3];
                    String size;
                    int filenameEndIdx;
                    if ("B".equalsIgnoreCase(parts[parts.length - 4])) {
                        size = parts[parts.length - 5] + " B";
                        filenameEndIdx = parts.length - 5;
                    } else {
                        size = parts[parts.length - 4];
                        filenameEndIdx = parts.length - 4;
                    }
                    StringBuilder fn = new StringBuilder();
                    for (int i = 1; i < filenameEndIdx; i++) {
                        if (fn.length() > 0) fn.append(" ");
                        fn.append(parts[i]);
                    }
                    model.addRow(new Object[]{id, fn.toString(), size, owner, perm, token});
                }
            }
        }

        JTable tbl = new JTable(model);
        styleDialogTable(tbl);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        tbl.getColumnModel().getColumn(1).setPreferredWidth(210); // Tên tệp tin
        tbl.getColumnModel().getColumn(2).setPreferredWidth(90);  // Dung lượng
        tbl.getColumnModel().getColumn(3).setPreferredWidth(100); // Người gửi
        tbl.getColumnModel().getColumn(4).setPreferredWidth(90);  // Quyền hạn
        tbl.getColumnModel().getColumn(5).setPreferredWidth(260); // Mã Token

        JScrollPane sp = new JScrollPane(tbl);
        sp.getViewport().setBackground(CLR_TABLE_BG);
        root.add(sp, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton btnDl = new ModernButton("Tải về tệp đã chọn", CLR_SUCCESS, Color.WHITE);
        btnDl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDl.setEnabled(false);

        ModernButton btnCopy = new ModernButton("Sao chép Token", CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnCopy.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCopy.setEnabled(false);

        ModernButton btnClose = new ModernButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dlg.dispose());

        tbl.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = tbl.getSelectedRow() >= 0;
            btnDl.setEnabled(hasSel);
            btnCopy.setEnabled(hasSel);
        });

        btnDl.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) return;
            int modelRow = tbl.convertRowIndexToModel(row);
            String perm = (String) model.getValueAt(modelRow, 4);
            if ("VIEW".equalsIgnoreCase(perm) || "READ_ONLY".equalsIgnoreCase(perm)) {
                JOptionPane.showMessageDialog(dlg,
                        "Tệp tin này được chia sẻ với quyền 'VIEW' (Chỉ xem danh mục),\nbạn không được phép tải về!",
                        "Từ chối quyền tải về", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String fname = (String) model.getValueAt(modelRow, 1);
            String token = (String) model.getValueAt(modelRow, 5);
            dlg.dispose();
            doGetShared(token, fname);
        });

        btnCopy.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) return;
            int modelRow = tbl.convertRowIndexToModel(row);
            String token = (String) model.getValueAt(modelRow, 5);
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(token);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            JOptionPane.showMessageDialog(dlg, "Đã sao chép mã Token vào bộ nhớ tạm:\n" + token, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });

        btnPanel.add(btnDl);
        btnPanel.add(btnCopy);
        btnPanel.add(btnClose);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void doMyShares() {
        setButtons(false);
        setStatus("Đang tải danh sách tệp đã chia sẻ...", CLR_PRIMARY);

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ftpService.getMyShares();
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    showMySharesDialog(lines);
                } catch (Exception ex) {
                    showError("Lỗi tải danh sách: " + ex.getMessage());
                } finally {
                    setButtons(true);
                    setStatus("Sẵn sàng", CLR_SUCCESS);
                }
            }
        }.execute();
    }

    private void showMySharesDialog(List<String> lines) {
        JDialog dlg = new JDialog(this, "Quản lý các tệp bạn đã chia sẻ", true);
        dlg.setSize(920, 480);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(CLR_BG);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Danh sách tệp tin bạn đang chia sẻ. Chọn một dòng để Thu hồi quyền truy cập:");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(CLR_TEXT);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Tên tệp tin", "Người nhận", "Quyền", "Lượt tải", "Trạng thái", "Mã Token"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (String raw : lines) {
            String l = raw.trim();
            if (l.matches("^\\d+\\s+.*")) {
                String[] parts = l.split("\\s+");
                if (parts.length >= 7) {
                    String id = parts[0];
                    String token = parts[parts.length - 1];
                    String status = parts[parts.length - 2];
                    String downloads = parts[parts.length - 3];
                    String perm = parts[parts.length - 4];
                    String recipient = parts[parts.length - 5];
                    StringBuilder fn = new StringBuilder();
                    for (int i = 1; i <= parts.length - 6; i++) {
                        if (fn.length() > 0) fn.append(" ");
                        fn.append(parts[i]);
                    }
                    model.addRow(new Object[]{id, fn.toString(), recipient, perm, downloads, status, token});
                }
            }
        }

        JTable tbl = new JTable(model);
        styleDialogTable(tbl);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(45);  // ID
        tbl.getColumnModel().getColumn(1).setPreferredWidth(200); // Tên tệp tin
        tbl.getColumnModel().getColumn(2).setPreferredWidth(100); // Người nhận
        tbl.getColumnModel().getColumn(3).setPreferredWidth(80);  // Quyền
        tbl.getColumnModel().getColumn(4).setPreferredWidth(80);  // Lượt tải
        tbl.getColumnModel().getColumn(5).setPreferredWidth(95);  // Trạng thái
        tbl.getColumnModel().getColumn(6).setPreferredWidth(240); // Mã Token

        JScrollPane sp = new JScrollPane(tbl);
        sp.getViewport().setBackground(CLR_TABLE_BG);
        root.add(sp, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton btnRevoke = new ModernButton("Thu hồi chia sẻ đã chọn", CLR_DANGER, Color.WHITE);
        btnRevoke.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRevoke.setEnabled(false);

        ModernButton btnCopy = new ModernButton("Sao chép Token", CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnCopy.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCopy.setEnabled(false);

        ModernButton btnClose = new ModernButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dlg.dispose());

        tbl.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = tbl.getSelectedRow() >= 0;
            btnRevoke.setEnabled(hasSel);
            btnCopy.setEnabled(hasSel);
        });

        btnRevoke.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) return;
            int modelRow = tbl.convertRowIndexToModel(row);
            String shareIdStr = (String) model.getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(dlg,
                    "Bạn có chắc chắn muốn thu hồi quyền chia sẻ cho tệp này không?\nNgười khác sẽ không thể tải tệp này nữa.",
                    "Xác nhận thu hồi", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                try {
                    int sId = Integer.parseInt(shareIdStr);
                    String resp = ftpService.revoke(sId);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dlg, resp, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                        doMyShares();
                    });
                } catch (Exception exx) {
                    SwingUtilities.invokeLater(() -> showError(exx.getMessage()));
                }
            }).start();
        });

        btnCopy.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) return;
            int modelRow = tbl.convertRowIndexToModel(row);
            String token = (String) model.getValueAt(modelRow, 6);
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(token);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            JOptionPane.showMessageDialog(dlg, "Đã sao chép Token vào Clipboard:\n" + token, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        });

        btnPanel.add(btnRevoke);
        btnPanel.add(btnCopy);
        btnPanel.add(btnClose);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void doGetShared() {
        doGetShared("", "");
    }

    private void doGetShared(String defaultToken, String defaultFilename) {
        String token = JOptionPane.showInputDialog(this,
                "Nhập chuỗi mã Token chia sẻ để tải tệp về máy:", defaultToken);
        if (token == null || token.trim().isEmpty()) return;

        JFileChooser chooser = new JFileChooser();
        if (!defaultFilename.isEmpty()) {
            chooser.setSelectedFile(new File(defaultFilename));
        } else {
            chooser.setSelectedFile(new File("shared_file_" + token.substring(0, Math.min(8, token.length()))));
        }
        chooser.setDialogTitle("Chọn vị trí lưu tệp tin chia sẻ");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File dest = chooser.getSelectedFile();

        setButtons(false);
        setStatus("Đang tải tệp chia sẻ qua Token...", CLR_PRIMARY);
        appendLog("→ GETSHARED " + token);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return ftpService.downloadShared(token.trim(), dest, (transferred, total) -> {
                    int pct = (total > 0) ? (int) (transferred * 100 / total) : 0;
                    MainFrame.this.setProgress(pct, "Đang tải tệp chia sẻ... " + pct + "% (" + formatBytes(transferred) + "/" + formatBytes(total) + ")");
                });
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        setStatus("Tải tệp chia sẻ thành công!", CLR_SUCCESS);
                        appendLog("← 226 OK. Tải tệp thành công.");
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Tệp tin chia sẻ đã được tải về thành công tại:\n" + dest.getAbsolutePath(),
                                "Tải về thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        showError("Tải tệp thất bại!");
                    }
                } catch (Exception ex) {
                    showError("Lỗi tải tệp: " + ex.getMessage());
                } finally {
                    MainFrame.this.setProgress(0, "Sẵn sàng");
                    setButtons(true);
                }
            }
        }.execute();
    }

    private void doViewLogs() {
        setButtons(false);
        setStatus("Đang tải nhật ký kiểm toán...", CLR_PRIMARY);

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ftpService.getAuditLogs("ALL", 1);
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    showAuditLogsDialog(lines);
                } catch (Exception ex) {
                    showError("Lỗi tải nhật ký: " + ex.getMessage());
                } finally {
                    setButtons(true);
                    setStatus("Sẵn sàng", CLR_SUCCESS);
                }
            }
        }.execute();
    }

    private void showAuditLogsDialog(List<String> lines) {
        JDialog dlg = new JDialog(this, "Nhật ký hoạt động hệ thống (Audit Logs)", true);
        dlg.setSize(940, 520);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(CLR_BG);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Thanh tìm kiếm và bộ lọc phía trên
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.setOpaque(false);

        JLabel lblFilter = new JLabel("Lọc hành động:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFilter.setForeground(CLR_TEXT);

        String[] actions = {"Tất cả", "UPLOAD", "DOWNLOAD", "DELETE", "MKD", "RMD", "SHARE", "LOGIN"};
        JComboBox<String> cbAction = new JComboBox<>(actions);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(CLR_TEXT);

        JTextField tfLogSearch = new JTextField(16);
        tfLogSearch.setBackground(CLR_CARD);
        tfLogSearch.setForeground(CLR_TEXT);
        tfLogSearch.setCaretColor(CLR_TEXT);
        tfLogSearch.setBorder(BorderFactory.createCompoundBorder(new LineBorder(CLR_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));

        filterBar.add(lblFilter);
        filterBar.add(cbAction);
        filterBar.add(lblSearch);
        filterBar.add(tfLogSearch);
        root.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"Thời gian", "Người dùng", "Hành động", "Đối tượng / Tệp", "Trạng thái", "Thông báo"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (String raw : lines) {
            String l = raw.trim();
            if (l.matches("^\\d{4}-\\d{2}-\\d{2}\\s+.*")) {
                String[] parts = l.split("\\s+", 7);
                if (parts.length >= 6) {
                    String time = parts[0] + " " + parts[1];
                    String user = parts[2];
                    String act = parts[3];
                    String target = parts[4];
                    String status = parts[5];
                    String msg = parts.length >= 7 ? parts[6] : "";
                    model.addRow(new Object[]{time, user, act, target, status, msg});
                }
            }
        }

        JTable tbl = new JTable(model);
        styleDialogTable(tbl);
        tbl.getColumnModel().getColumn(0).setPreferredWidth(150); // Thời gian
        tbl.getColumnModel().getColumn(1).setPreferredWidth(95);  // Người dùng
        tbl.getColumnModel().getColumn(2).setPreferredWidth(140); // Hành động
        tbl.getColumnModel().getColumn(3).setPreferredWidth(170); // Đối tượng / Tệp
        tbl.getColumnModel().getColumn(4).setPreferredWidth(90);  // Trạng thái
        tbl.getColumnModel().getColumn(5).setPreferredWidth(240); // Thông báo
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        tbl.setRowSorter(sorter);

        // Filter event
        Runnable applyFilter = () -> {
            String selectedAct = (String) cbAction.getSelectedItem();
            String query = tfLogSearch.getText().trim();
            String safeQuery = java.util.regex.Pattern.quote(query);
            if ("Tất cả".equals(selectedAct) && query.isEmpty()) {
                sorter.setRowFilter(null);
            } else if (!"Tất cả".equals(selectedAct) && query.isEmpty()) {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + selectedAct, 2));
            } else if ("Tất cả".equals(selectedAct) && !query.isEmpty()) {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + safeQuery));
            } else {
                sorter.setRowFilter(RowFilter.andFilter(List.of(
                        RowFilter.regexFilter("(?i)" + selectedAct, 2),
                        RowFilter.regexFilter("(?i)" + safeQuery)
                )));
            }
        };

        cbAction.addActionListener(e -> applyFilter.run());
        tfLogSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter.run(); }
        });

        JScrollPane sp = new JScrollPane(tbl);
        sp.getViewport().setBackground(CLR_TABLE_BG);
        root.add(sp, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        ModernButton btnClose = new ModernButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dlg.dispose());
        btnPanel.add(btnClose);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void doPlansAndSubscriptions() {
        setButtons(false);
        setStatus("Đang tải thông tin gói cước...", CLR_PRIMARY);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.getMyPlan();
            }

            @Override
            protected void done() {
                try {
                    String myPlan = get();
                    showPlansDialog(myPlan);
                } catch (Exception ex) {
                    showError("Lỗi tải thông tin gói: " + ex.getMessage());
                } finally {
                    setButtons(true);
                    setStatus("Sẵn sàng", CLR_SUCCESS);
                }
            }
        }.execute();
    }

    private void showPlansDialog(String myPlanInfo) {
        JDialog dlg = new JDialog(this, "Quản lý gói cước & Đăng ký dịch vụ", true);
        dlg.setSize(940, 620);
        dlg.setLocationRelativeTo(this);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // ── TAB 1: GÓI CƯỚC & NÂNG CẤP ────────────────────────────────
        JPanel tab1 = new JPanel(new BorderLayout(12, 12));
        tab1.setBackground(CLR_BG);
        tab1.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Thông tin gói hiện tại
        JPanel currentPanel = new JPanel(new BorderLayout(6, 6));
        currentPanel.setBackground(CLR_CARD);
        currentPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(12, 16, 12, 16)));

        JLabel curTitle = new JLabel("GÓI DỊCH VỤ HIỆN TẠI CỦA BẠN:");
        curTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        curTitle.setForeground(CLR_WARN);

        JLabel curInfo = new JLabel("<html>" + (myPlanInfo != null ? myPlanInfo.replace("200 ", "").replace("|", "<br>• ") : "Đang cập nhật...") + "</html>");
        curInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        curInfo.setForeground(CLR_TEXT);

        currentPanel.add(curTitle, BorderLayout.NORTH);
        currentPanel.add(curInfo, BorderLayout.CENTER);
        tab1.add(currentPanel, BorderLayout.NORTH);

        // Khởi tạo ô nhập mã đơn hàng dùng chung
        final JTextField txtOrderCode = new JTextField();
        txtOrderCode.setFont(new Font("Consolas", Font.BOLD, 14));
        txtOrderCode.setBackground(CLR_CARD);
        txtOrderCode.setForeground(Color.WHITE);
        txtOrderCode.setCaretColor(Color.WHITE);
        txtOrderCode.setBorder(BorderFactory.createCompoundBorder(new LineBorder(CLR_BORDER, 1), new EmptyBorder(6, 10, 6, 10)));

        // 3 Thẻ gói cước (Cards)
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        cardsPanel.setBackground(CLR_BG);

        cardsPanel.add(createModernPlanCard(dlg, tabs, txtOrderCode, 1, "GÓI FREE", "100 MB", "Miễn phí", "Vĩnh viễn",
                "Gói tiêu chuẩn mặc định cho mọi tài khoản sinh viên. Lưu trữ bài tập và tài liệu học tập cơ bản.", CLR_BORDER, false));

        cardsPanel.add(createModernPlanCard(dlg, tabs, txtOrderCode, 2, "GÓI PRO", "1 GB", "50.000 VNĐ", "30 ngày",
                "Tăng gấp 10 lần dung lượng. Phù hợp lưu trữ dự án lập trình, tài liệu nhóm và mã nguồn bài tập lớn.", CLR_PRIMARY, true));

        cardsPanel.add(createModernPlanCard(dlg, tabs, txtOrderCode, 3, "GÓI VIP", "10 GB", "200.000 VNĐ", "90 ngày",
                "Hạn mức tối đa 10GB tốc độ cao trong 3 tháng. Thoải mái lưu trữ máy ảo, tệp ISO và đồ án dung lượng lớn.", CLR_WARN, true));

        tab1.add(cardsPanel, BorderLayout.CENTER);
        tabs.addTab("Gói cước & Nâng cấp", tab1);

        // ── TAB 2: THANH TOÁN MÔ PHỎNG ────────────────────────────────
        JPanel tab2 = new JPanel(new BorderLayout(12, 12));
        tab2.setBackground(CLR_BG);
        tab2.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel formPanel = new JPanel(new GridLayout(5, 1, 8, 8));
        formPanel.setBackground(CLR_BG);

        JLabel lblPayTitle = new JLabel("CỔNG THANH TOÁN MÔ PHỎNG (MOCK PAYMENT)");
        lblPayTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPayTitle.setForeground(CLR_PRIMARY);

        JLabel lblOrderInput = new JLabel("Nhập mã đơn hàng hoặc Order ID (ví dụ: ORD-...):");
        lblOrderInput.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblOrderInput.setForeground(CLR_TEXT);

        JPanel payBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        payBtnPanel.setOpaque(false);

        ModernButton btnMockSuccess = new ModernButton("Thanh toán Thành công (Mô phỏng)", CLR_SUCCESS, Color.WHITE);
        btnMockSuccess.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnMockSuccess.setPreferredSize(new Dimension(300, 38));

        ModernButton btnMockFail = new ModernButton("Giả lập Thanh toán Thất bại", CLR_DANGER, Color.WHITE);
        btnMockFail.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnMockFail.setPreferredSize(new Dimension(260, 38));

        payBtnPanel.add(btnMockSuccess);
        payBtnPanel.add(btnMockFail);

        formPanel.add(lblPayTitle);
        formPanel.add(lblOrderInput);
        formPanel.add(txtOrderCode);
        formPanel.add(new JSeparator());
        formPanel.add(payBtnPanel);

        tab2.add(formPanel, BorderLayout.NORTH);

        JTextArea payLogArea = new JTextArea("Hướng dẫn quy trình:\n1. Chọn gói cước ở Tab 1 để tạo đơn hàng mới.\n2. Mã đơn hàng sẽ được điền tự động vào ô trên.\n3. Bấm 'Thanh toán Thành công' để kích hoạt ngay hạn mức Quota mới theo chuẩn ACID Transaction!\n4. Bấm 'Giả lập Thất bại' để kiểm tra kịch bản ngân hàng từ chối giao dịch.\n");
        payLogArea.setEditable(false);
        payLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        payLogArea.setBackground(new Color(0x0F, 0x17, 0x2A));
        payLogArea.setForeground(new Color(0x86, 0xEF, 0xAC));
        payLogArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        tab2.add(new JScrollPane(payLogArea), BorderLayout.CENTER);

        tabs.addTab("Thanh toán mô phỏng", tab2);

        // ── TAB 3: LỊCH SỬ ĐƠN HÀNG ────────────────────────────────────
        JPanel tab3 = new JPanel(new BorderLayout(8, 8));
        tab3.setBackground(CLR_BG);
        tab3.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] orderCols = {"Order ID", "Mã đơn hàng", "Gói cước", "Số tiền", "Trạng thái", "Ngày tạo"};
        DefaultTableModel orderModel = new DefaultTableModel(orderCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable orderTable = new JTable(orderModel);
        styleDialogTable(orderTable);
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(65);  // Order ID
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(210); // Mã đơn hàng
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(85);  // Gói cước
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(110); // Số tiền
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Trạng thái
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(160); // Ngày tạo

        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.getViewport().setBackground(CLR_TABLE_BG);
        tab3.add(orderScroll, BorderLayout.CENTER);

        JPanel orderActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        orderActionPanel.setOpaque(false);

        ModernButton btnRefreshOrders = new ModernButton("Làm mới đơn");
        btnRefreshOrders.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ModernButton btnPaySelected = new ModernButton("Thanh toán đơn đã chọn", CLR_PRIMARY, Color.WHITE);
        btnPaySelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ModernButton btnCancelOrder = new ModernButton("Hủy đơn đang chờ", CLR_DANGER, Color.WHITE);
        btnCancelOrder.setFont(new Font("Segoe UI", Font.BOLD, 12));

        orderActionPanel.add(btnRefreshOrders);
        orderActionPanel.add(btnPaySelected);
        orderActionPanel.add(btnCancelOrder);
        tab3.add(orderActionPanel, BorderLayout.SOUTH);

        tabs.addTab("Lịch sử đơn hàng", tab3);

        // ── SỰ KIỆN GÓI CƯỚC & THANH TOÁN ──────────────────────────────
        btnMockSuccess.addActionListener(e -> {
            String code = txtOrderCode.getText().trim();
            if (code.isEmpty()) {
                showWarn("Vui lòng nhập Order ID hoặc Mã đơn hàng!");
                return;
            }
            try {
                String resp = ftpService.payOrder(code, true);
                payLogArea.append("\n[THANH TOÁN THÀNH CÔNG] " + resp);
                if (resp.startsWith("200")) {
                    JOptionPane.showMessageDialog(dlg, resp, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    updateQuotaLabel();
                    doRefresh();
                } else {
                    JOptionPane.showMessageDialog(dlg, resp, "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                showError("Lỗi thanh toán: " + ex.getMessage());
            }
        });

        btnMockFail.addActionListener(e -> {
            String code = txtOrderCode.getText().trim();
            if (code.isEmpty()) {
                showWarn("Vui lòng nhập Order ID hoặc Mã đơn hàng!");
                return;
            }
            try {
                String resp = ftpService.payOrder(code, false);
                payLogArea.append("\n[GIAO DỊCH BỊ TỪ CHỐI] " + resp);
                JOptionPane.showMessageDialog(dlg, resp, "Thông báo thất bại", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                showError("Lỗi: " + ex.getMessage());
            }
        });

        Runnable loadOrders = () -> {
            new Thread(() -> {
                try {
                    List<String> orders = ftpService.getMyOrders();
                    SwingUtilities.invokeLater(() -> {
                        orderModel.setRowCount(0);
                        for (String l : orders) {
                            String trimmed = l.trim();
                            if (trimmed.matches("^\\d+\\s+.*")) {
                                String[] parts = trimmed.split("\\s+");
                                if (parts.length >= 6) {
                                    String oid = parts[0];
                                    String code = parts[1];
                                    String plan = parts[2];
                                    String amt;
                                    String st;
                                    String dt;
                                    if (parts[4].equalsIgnoreCase("VND") || parts[4].equalsIgnoreCase("VNĐ")) {
                                        amt = parts[3] + " " + parts[4];
                                        st = parts[5];
                                        dt = parts.length > 7 ? (parts[6] + " " + parts[7]) : (parts.length > 6 ? parts[6] : "");
                                    } else {
                                        amt = parts[3];
                                        st = parts[4];
                                        dt = parts.length > 6 ? (parts[5] + " " + parts[6]) : (parts.length > 5 ? parts[5] : "");
                                    }
                                    if (dt.endsWith(".0")) dt = dt.substring(0, dt.length() - 2);
                                    orderModel.addRow(new Object[]{oid, code, plan, amt, st, dt});
                                }
                            }
                        }
                    });
                } catch (Exception ignored) {}
            }).start();
        };

        btnRefreshOrders.addActionListener(e -> loadOrders.run());
        loadOrders.run();

        btnPaySelected.addActionListener(e -> {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                showWarn("Vui lòng chọn một đơn hàng cần thanh toán!");
                return;
            }
            int modelRow = orderTable.convertRowIndexToModel(row);
            String code = (String) orderModel.getValueAt(modelRow, 1);
            txtOrderCode.setText(code);
            tabs.setSelectedIndex(1);
        });

        btnCancelOrder.addActionListener(e -> {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                showWarn("Vui lòng chọn đơn hàng cần hủy!");
                return;
            }
            int modelRow = orderTable.convertRowIndexToModel(row);
            String code = (String) orderModel.getValueAt(modelRow, 1);
            int ans = JOptionPane.showConfirmDialog(dlg, "Hủy đơn hàng " + code + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (ans != JOptionPane.YES_OPTION) return;

            new Thread(() -> {
                try {
                    String resp = ftpService.cancelOrder(code);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dlg, resp);
                        loadOrders.run();
                    });
                } catch (Exception exx) {
                    SwingUtilities.invokeLater(() -> showError(exx.getMessage()));
                }
            }).start();
        });

        dlg.setContentPane(tabs);
        dlg.setVisible(true);
    }

    private JPanel createModernPlanCard(JDialog parentDlg, JTabbedPane tabs, JTextField txtOrderCode, int planId, String name, String quota, String price, String duration, String desc, Color highlight, boolean canUpgrade) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(CLR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(highlight, 2, true),
                new EmptyBorder(16, 16, 16, 16)));

        JPanel top = new JPanel(new GridLayout(4, 1, 0, 4));
        top.setOpaque(false);

        JLabel lblName = new JLabel(name, SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblName.setForeground(highlight);

        JLabel lblQuota = new JLabel(quota, SwingConstants.CENTER);
        lblQuota.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblQuota.setForeground(CLR_TEXT);

        JLabel lblPrice = new JLabel(price + " / " + duration, SwingConstants.CENTER);
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrice.setForeground(CLR_SUCCESS);

        top.add(lblName);
        top.add(lblQuota);
        top.add(lblPrice);
        card.add(top, BorderLayout.NORTH);

        JTextArea descArea = new JTextArea(desc);
        descArea.setWrapStyleWord(true);
        descArea.setLineWrap(true);
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descArea.setForeground(CLR_MUTED);
        card.add(descArea, BorderLayout.CENTER);

        ModernButton btnAction = new ModernButton(canUpgrade ? "Đăng ký ngay" : "Gói mặc định",
                canUpgrade ? highlight : CLR_CARD,
                canUpgrade ? Color.WHITE : CLR_MUTED,
                canUpgrade ? null : CLR_BORDER);
        btnAction.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAction.setEnabled(canUpgrade);
        btnAction.setPreferredSize(new Dimension(0, 36));

        if (canUpgrade) {
            btnAction.addActionListener(e -> {
                int ans = JOptionPane.showConfirmDialog(parentDlg,
                        "Bạn có muốn tạo đơn hàng nâng cấp lên " + name + " (" + price + ") không?",
                        "Xác nhận đặt hàng", JOptionPane.YES_NO_OPTION);
                if (ans != JOptionPane.YES_OPTION) return;

                new Thread(() -> {
                    try {
                        String resp = ftpService.createOrder(planId);
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(parentDlg, resp, "Đã tạo đơn hàng", JOptionPane.INFORMATION_MESSAGE);
                            if (txtOrderCode != null && resp != null) {
                                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(ORD-[A-Za-z0-9_-]+)").matcher(resp);
                                if (m.find()) {
                                    txtOrderCode.setText(m.group(1));
                                } else {
                                    java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("OrderID:\\s*(\\d+)").matcher(resp);
                                    if (m2.find()) {
                                        txtOrderCode.setText(m2.group(1));
                                    }
                                }
                            }
                            tabs.setSelectedIndex(1);
                        });
                    } catch (Exception exx) {
                        SwingUtilities.invokeLater(() -> showError(exx.getMessage()));
                    }
                }).start();
            });
        }

        card.add(btnAction, BorderLayout.SOUTH);
        return card;
    }

    private void styleDialogTable(JTable tbl) {
        tbl.setBackground(CLR_TABLE_BG);
        tbl.setForeground(CLR_TEXT);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tbl.setRowHeight(28);
        tbl.setSelectionBackground(CLR_PRIMARY);
        tbl.setSelectionForeground(Color.WHITE);
        tbl.setShowVerticalLines(false);
        tbl.setShowHorizontalLines(true);
        tbl.setGridColor(new Color(0x37, 0x41, 0x51));
        tbl.getTableHeader().setBackground(CLR_CARD);
        tbl.getTableHeader().setForeground(CLR_MUTED);
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tbl.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER));

        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? CLR_PRIMARY : (row % 2 == 0 ? CLR_TABLE_BG : CLR_TABLE_ALT));
                setForeground(sel ? Color.WHITE : CLR_TEXT);
                setBorder(new EmptyBorder(0, 8, 0, 8));

                String colName = t.getColumnName(col).toLowerCase();
                // Tự động căn lề theo ngữ cảnh cột
                if (colName.contains("id") || colName.contains("mã") || colName.contains("ngày") || colName.contains("thời gian") ||
                    colName.contains("trạng thái") || colName.contains("quyền") || colName.contains("lượt") || colName.contains("vai trò") ||
                    colName.contains("gói") || colName.contains("loại")) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (colName.contains("tiền") || colName.contains("dung lượng") || colName.contains("kích thước") ||
                           colName.contains("hạn mức") || colName.contains("đã dùng") || colName.contains("price") || colName.contains("size")) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Tô màu trạng thái & vai trò
                if (val != null) {
                    String str = val.toString().trim();
                    if ("COMPLETED".equalsIgnoreCase(str) || "SUCCESS".equalsIgnoreCase(str) || "ACTIVE".equalsIgnoreCase(str) ||
                        "YES".equalsIgnoreCase(str) || "DOWNLOAD".equalsIgnoreCase(str)) {
                        setForeground(sel ? Color.WHITE : CLR_SUCCESS);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if ("FAILED".equalsIgnoreCase(str) || "CANCELLED".equalsIgnoreCase(str) || "LOCKED".equalsIgnoreCase(str) ||
                               "NO".equalsIgnoreCase(str) || "REVOKED".equalsIgnoreCase(str)) {
                        setForeground(sel ? Color.WHITE : CLR_DANGER);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if ("PENDING".equalsIgnoreCase(str) || "EXPIRED".equalsIgnoreCase(str) || str.contains("ADMIN")) {
                        setForeground(sel ? Color.WHITE : CLR_WARN);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if ("VIEW".equalsIgnoreCase(str) || "READ_ONLY".equalsIgnoreCase(str)) {
                        setForeground(sel ? Color.WHITE : CLR_PRIMARY);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    }
                }
                return this;
            }
        });
    }

    // ================================================================
    // TIỆN ÍCH HỆ THỐNG
    // ================================================================

    private void updateDirLabel() {
        try {
            String pwd = ftpService.pwd();
            if (pwd != null && pwd.contains("\"")) {
                lblDir.setText("Vị trí: " + pwd.split("\"")[1]);
            } else {
                lblDir.setText("Vị trí: /");
            }
        } catch (Exception ignored) {
            lblDir.setText("Vị trí: /");
        }
    }

    private void updateQuotaLabel() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.whoami();
            }

            @Override
            protected void done() {
                try {
                    String info = get();
                    if (info != null && info.startsWith("200")) {
                        java.util.regex.Matcher mq = java.util.regex.Pattern.compile("Quota=(\\d+)MB.*Used=(\\d+)KB").matcher(info);
                        if (mq.find()) {
                            long qMb = Long.parseLong(mq.group(1));
                            long uKb = Long.parseLong(mq.group(2));
                            String qText = (qMb >= 1024) ? String.format("%.1f GB", qMb / 1024.0).replace(".0 GB", " GB") : (qMb + " MB");
                            String uText = (uKb >= 1024 * 1024) ? String.format("%.1f GB", uKb / (1024.0 * 1024)).replace(".0 GB", " GB")
                                         : (uKb >= 1024) ? String.format("%.1f MB", uKb / 1024.0).replace(".0 MB", " MB")
                                         : (uKb + " KB");
                            if (lblQuota != null) {
                                lblQuota.setText("Dung lượng: " + uText + " / " + qText);
                            }
                        } else if (info.contains("Quota=") && info.contains("Used=")) {
                            String quotaPart = info.substring(info.indexOf("Quota="));
                            if (lblQuota != null) {
                                lblQuota.setText("Dung lượng: " + quotaPart);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("● " + text);
            lblStatus.setForeground(color);
        });
    }

    private void setProgress(int pct, String text) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(pct);
            progressBar.setString(text);
        });
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + time + "]  " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setButtons(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            btnRefresh.setEnabled(enabled);
            if (btnParentDir != null) btnParentDir.setEnabled(enabled);
            if (btnNewFolder != null) btnNewFolder.setEnabled(enabled);
            btnUpload.setEnabled(enabled);
            btnDownload.setEnabled(enabled);
            btnDelete.setEnabled(enabled);
            btnRename.setEnabled(enabled);

            btnNavFiles.setEnabled(enabled);
            btnNavShare.setEnabled(enabled);
            btnNavSharedWithMe.setEnabled(enabled);
            btnNavMyShares.setEnabled(enabled);
            btnNavGetShared.setEnabled(enabled);
            btnNavPlans.setEnabled(enabled);
            btnNavLogs.setEnabled(enabled);
            if (btnNavAdmin != null) btnNavAdmin.setEnabled(enabled);

            btnLogout.setEnabled(enabled);
            btnDisconnect.setEnabled(enabled);
        });
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return String.format("%.1f GB", (double) bytes / (1024 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.1f MB", (double) bytes / (1024 * 1024));
        } else if (bytes >= 1024) {
            return String.format("%.1f KB", (double) bytes / 1024);
        } else {
            return bytes + " B";
        }
    }

    private long parseSizeToBytes(String sizeStr) {
        if (sizeStr == null || sizeStr.equals("-") || sizeStr.trim().isEmpty()) return -1L;
        try {
            String[] parts = sizeStr.trim().split("\\s+");
            double num = Double.parseDouble(parts[0].replace(",", "."));
            if (parts.length > 1) {
                String unit = parts[1].toUpperCase();
                if (unit.contains("GB")) return (long) (num * 1024L * 1024L * 1024L);
                if (unit.contains("MB")) return (long) (num * 1024L * 1024L);
                if (unit.contains("KB")) return (long) (num * 1024L);
            }
            return (long) num;
        } catch (Exception e) {
            return 0L;
        }
    }

    private void doChangePassword() {
        JDialog dlg = new JDialog(this, "Đổi mật khẩu tài khoản", true);
        dlg.setSize(400, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CLR_BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("ĐỔI MẬT KHẨU TÀI KHOẢN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(CLR_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 1, 0, 10));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(14, 0, 14, 0));

        JPasswordField pfOld = new JPasswordField();
        JPasswordField pfNew = new JPasswordField();
        JPasswordField pfConfirm = new JPasswordField();

        styleDialogPassword(pfOld);
        styleDialogPassword(pfNew);
        styleDialogPassword(pfConfirm);

        form.add(createDialogFieldGroup("Mật khẩu hiện tại:", pfOld));
        form.add(createDialogFieldGroup("Mật khẩu mới (tối thiểu 6 ký tự):", pfNew));
        form.add(createDialogFieldGroup("Xác nhận mật khẩu mới:", pfConfirm));
        root.add(form, BorderLayout.CENTER);

        JPanel btnPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPnl.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dlg.dispose());

        ModernButton btnSubmit = new ModernButton("Xác nhận đổi", CLR_PRIMARY, Color.WHITE);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnSubmit.addActionListener(e -> {
            String oldP = new String(pfOld.getPassword());
            String newP = new String(pfNew.getPassword());
            String cfP  = new String(pfConfirm.getPassword());

            if (oldP.isEmpty() || newP.isEmpty()) {
                showWarn("Vui lòng điền đầy đủ thông tin mật khẩu!");
                return;
            }
            if (newP.length() < 6) {
                showWarn("Mật khẩu mới phải từ 6 ký tự trở lên!");
                return;
            }
            if (!newP.equals(cfP)) {
                showWarn("Xác nhận mật khẩu mới không khớp!");
                return;
            }

            btnSubmit.setEnabled(false);
            new Thread(() -> {
                try {
                    String resp = ftpService.changePassword(oldP, newP);
                    SwingUtilities.invokeLater(() -> {
                        btnSubmit.setEnabled(true);
                        if (resp != null && resp.startsWith("200")) {
                            JOptionPane.showMessageDialog(dlg, "Đổi mật khẩu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            dlg.dispose();
                        } else {
                            showError("Đổi mật khẩu thất bại: " + resp);
                        }
                    });
                } catch (Exception exx) {
                    SwingUtilities.invokeLater(() -> {
                        btnSubmit.setEnabled(true);
                        showError("Lỗi: " + exx.getMessage());
                    });
                }
            }).start();
        });

        btnPnl.add(btnCancel);
        btnPnl.add(btnSubmit);
        root.add(btnPnl, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void styleDialogPassword(JPasswordField pf) {
        pf.setBackground(CLR_CARD);
        pf.setForeground(CLR_TEXT);
        pf.setCaretColor(CLR_TEXT);
        pf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CLR_BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)));
    }

}

# -*- coding: utf-8 -*-
with open(r'src/client/MainFrame.java', 'r', encoding='utf-8') as f:
    c = f.read()

# Add static initializer to ensure ThemeManager is applied
static_theme = """public class MainFrame extends JFrame {

    static {
        ThemeManager.applyTheme();
    }"""
c = c.replace("public class MainFrame extends JFrame {", static_theme)

# Update fields to ModernButton
old_btn_fields = """    // ── Components Header ────────────────────────────────────────────
    private JLabel       lblServer;
    private JLabel       lblUser;
    private JLabel       lblDir;
    private JLabel       lblStatus;
    private JLabel       lblQuota;
    private JButton      btnChangePass;
    private JButton      btnLogout;
    private JButton      btnDisconnect;

    // ── Components Toolbar & File Table ──────────────────────────────
    private JButton           btnRefresh;
    private JButton           btnParentDir;
    private JButton           btnNewFolder;
    private JButton           btnUpload;
    private JButton           btnDownload;
    private JButton           btnDelete;
    private JButton           btnRename;
    private JTextField        tfSearch;
    private DefaultTableModel tableModel;
    private JTable            fileTable;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JLabel            lblEmptyState;

    // ── Sidebar Nav Buttons ──────────────────────────────────────────
    private JButton btnNavFiles;
    private JButton btnNavShare;
    private JButton btnNavSharedWithMe;
    private JButton btnNavMyShares;
    private JButton btnNavGetShared;
    private JButton btnNavPlans;
    private JButton btnNavLogs;
    private JButton btnNavAdmin;

    // ── Legacy Button references for internal compatibility ──────────
    private JButton btnPlans;
    private JButton btnAdminDashboard;
    private JButton btnShare;
    private JButton btnSharedWithMe;
    private JButton btnMyShares;
    private JButton btnGetShared;
    private JButton btnLogs;"""

new_btn_fields = """    // ── Components Header ────────────────────────────────────────────
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
    private ModernButton btnLogs;"""

c = c.replace(old_btn_fields, new_btn_fields)

# Replace createHeaderButton implementation
old_createHeaderButton = """    private JButton createHeaderButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(new Color(0x37, 0x41, 0x51));
        btn.setForeground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(bg, 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 12, 26));
        return btn;
    }"""

new_createHeaderButton = """    private ModernButton createHeaderButton(String text, Color fgColor, Color borderColor) {
        ModernButton btn = new ModernButton(text, CLR_CARD, fgColor, borderColor);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 14, 28));
        return btn;
    }"""

c = c.replace(old_createHeaderButton, new_createHeaderButton)

# Update header buttons initialization
old_head_init = """        btnChangePass = createHeaderButton("Đổi mật khẩu", CLR_TEXT);
        btnLogout = createHeaderButton("Đăng xuất", CLR_WARN);
        btnDisconnect = createHeaderButton("Ngắt kết nối", CLR_DANGER);"""

new_head_init = """        btnChangePass = createHeaderButton("Đổi mật khẩu", CLR_TEXT, CLR_BORDER);
        btnLogout = createHeaderButton("Đăng xuất", CLR_WARN, CLR_WARN);
        btnDisconnect = createHeaderButton("Ngắt kết nối", CLR_DANGER, CLR_DANGER);"""

c = c.replace(old_head_init, new_head_init)

# Replace createSidebarBtn implementation
old_createSidebarBtn = """    private JButton createSidebarBtn(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        btn.setForeground(active ? Color.WHITE : CLR_TEXT);
        btn.setBackground(active ? CLR_PRIMARY : CLR_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(CLR_PRIMARY)) {
                    btn.setBackground(new Color(0x27, 0x34, 0x49));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(CLR_PRIMARY)) {
                    btn.setBackground(CLR_SIDEBAR);
                }
            }
        });
        return btn;
    }"""

new_createSidebarBtn = """    private ModernButton createSidebarBtn(String text, boolean active) {
        Color bg = active ? CLR_PRIMARY : CLR_SIDEBAR;
        Color fg = active ? Color.WHITE : CLR_MUTED;
        ModernButton btn = new ModernButton(text, bg, fg, null);
        btn.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }"""

c = c.replace(old_createSidebarBtn, new_createSidebarBtn)

# Replace createToolBtn implementation
old_createToolBtn = """    private JButton createToolBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(CLR_BORDER, 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 14, 30));

        Color hover = bg.equals(CLR_PRIMARY) ? new Color(0x25, 0x63, 0xEB) :
                     (bg.equals(CLR_SUCCESS) ? new Color(0x16, 0xA3, 0x4A) : new Color(0x4B, 0x55, 0x63));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hover); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }"""

new_createToolBtn = """    private ModernButton createToolBtn(String text, Color bg, Color fg, Color border) {
        ModernButton btn = new ModernButton(text, bg, fg, border);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 32));
        return btn;
    }"""

c = c.replace(old_createToolBtn, new_createToolBtn)

# Update Toolbar Buttons initialization with high contrast
old_tb_init = """        btnRefresh   = createToolBtn("Làm mới",     CLR_BORDER, CLR_TEXT);
        btnParentDir = createToolBtn("Thư mục cha", CLR_BORDER, CLR_TEXT);
        btnNewFolder = createToolBtn("+ Thư mục",   CLR_BORDER, CLR_TEXT);
        btnUpload    = createToolBtn("Tải lên",     CLR_PRIMARY, Color.WHITE);
        btnDownload  = createToolBtn("Tải về",      CLR_SUCCESS, Color.WHITE);
        btnRename    = createToolBtn("Đổi tên",     CLR_BORDER, CLR_WARN);
        btnDelete    = createToolBtn("Xóa",         CLR_BORDER, CLR_DANGER);"""

new_tb_init = """        btnRefresh   = createToolBtn("Làm mới",     CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnParentDir = createToolBtn("Thư mục cha", CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnNewFolder = createToolBtn("+ Thư mục",   CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnUpload    = createToolBtn("Tải lên",     CLR_PRIMARY, Color.WHITE, null);
        btnDownload  = createToolBtn("Tải về",      CLR_SUCCESS, Color.WHITE, null);
        btnRename    = createToolBtn("Đổi tên",     CLR_CARD, CLR_WARN, CLR_WARN);
        btnDelete    = createToolBtn("Xóa",         CLR_CARD, CLR_DANGER, CLR_DANGER);"""

c = c.replace(old_tb_init, new_tb_init)

# Replace btnClearLog in Terminal
old_clear_log = """        JButton btnClearLog = new JButton("Xóa nhật ký");
        btnClearLog.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnClearLog.setBackground(CLR_CARD);
        btnClearLog.setForeground(CLR_TEXT);
        btnClearLog.setFocusPainted(false);"""

new_clear_log = """        ModernButton btnClearLog = new ModernButton("Xóa nhật ký", CLR_CARD, CLR_TEXT, CLR_BORDER);
        btnClearLog.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClearLog.setPreferredSize(new Dimension(90, 24));"""

c = c.replace(old_clear_log, new_clear_log)

with open(r'src/client/MainFrame.java', 'w', encoding='utf-8') as f:
    f.write(c)

print("[DONE] MainFrame.java updated with ModernButton - zero white-on-white contrast issues!")


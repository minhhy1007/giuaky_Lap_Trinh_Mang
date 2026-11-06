package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * PHASE 6: BẢNG ĐIỀU KHIỂN QUẢN TRỊ VIÊN TOÀN DIỆN (ADMIN DASHBOARD DIALOG)
 *
 * Chức năng:
 *   1. Thống kê KPI hệ thống: Users, Storage Quota/Used, Files, Orders, Revenue.
 *   2. Quản trị tài khoản & Phân quyền RBAC (Khóa/Mở tài khoản, Đổi quyền, Set Quota).
 *   3. Giám sát toàn bộ đơn hàng và doanh thu gói cước.
 *   4. Tra cứu lịch sử kiểm toán hệ thống (Audit Logs) đa tiêu chí.
 *
 * Mọi thao tác quản trị đều được xác minh quyền hạn nghiêm ngặt ở Server (Server-side RBAC).
 */
public class AdminDashboardDialog extends JDialog {

    private final FtpClientService ftpService;

    // Màu sắc chuẩn hóa Dark UI
    private static final Color CLR_BG       = new Color(0x11, 0x18, 0x27);
    private static final Color CLR_PANEL    = new Color(0x1F, 0x29, 0x37);
    private static final Color CLR_CARD     = new Color(0x1F, 0x29, 0x37);
    private static final Color CLR_TABLE_H  = new Color(0x1F, 0x29, 0x37);
    private static final Color CLR_TABLE_R  = new Color(0x27, 0x34, 0x49);
    private static final Color CLR_TABLE_A  = new Color(0x1F, 0x2A, 0x3C);
    private static final Color CLR_TABLE_S  = new Color(0x3B, 0x82, 0xF6);
    private static final Color CLR_ACCENT   = new Color(0x3B, 0x82, 0xF6);
    private static final Color CLR_SUCCESS  = new Color(0x22, 0xC5, 0x5E);
    private static final Color CLR_WARN     = new Color(0xF5, 0x9E, 0x0B);
    private static final Color CLR_ERROR    = new Color(0xEF, 0x44, 0x44);
    private static final Color CLR_TEXT     = new Color(0xF9, 0xFA, 0xFB);
    private static final Color CLR_MUTED    = new Color(0x9C, 0xA3, 0xAF);
    private static final Color CLR_BORDER   = new Color(0x37, 0x41, 0x51);

    // Components
    private JLabel lblTotalUsers, lblActiveUsers, lblLockedUsers;
    private JLabel lblTotalStorage, lblTotalUsed, lblFillRatio;
    private JLabel lblTotalFiles, lblTotalOrders, lblTotalRevenue;
    private JProgressBar barStorageHealth;

    private DefaultTableModel userTableModel;
    private JTable userTable;

    private DefaultTableModel orderTableModel;
    private JTable orderTable;

    private DefaultTableModel logTableModel;
    private JTable logTable;
    private JComboBox<String> cbLogFilter;

    public AdminDashboardDialog(Frame parent, FtpClientService service) {
        super(parent, "BẢNG ĐIỀU KHIỂN QUẢN TRỊ VIÊN (ADMIN DASHBOARD)", true);
        this.ftpService = service;

        initUI();
        setLocationRelativeTo(parent);

        // Load dữ liệu ban đầu
        refreshOverview();
        refreshUsers();
    }

    private void initUI() {
        setSize(1000, 660);
        setMinimumSize(new Dimension(880, 560));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(CLR_BG);

        // Header Title Banner
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(18, 22, 30));
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel titleLbl = new JLabel("HỆ THỐNG QUẢN TRỊ MÁY CHỦ (ADMIN DASHBOARD)");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLbl.setForeground(new Color(245, 185, 55));

        JLabel subTitleLbl = new JLabel("Phân hệ quản trị bảo mật RBAC, giám sát dung lượng, đơn hàng và lịch sử kiểm toán");
        subTitleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subTitleLbl.setForeground(CLR_MUTED);

        header.add(titleLbl, BorderLayout.NORTH);
        header.add(subTitleLbl, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(CLR_PANEL);
        tabs.setForeground(CLR_TEXT);

        tabs.addTab("Tổng quan & KPI", buildTabOverview());
        tabs.addTab("Quản lý Người dùng & Quyền hạn", buildTabUsers());
        tabs.addTab("💰 Đơn hàng & Doanh thu", buildTabOrders());
        tabs.addTab("📜 Nhật ký Kiểm toán Toàn hệ thống", buildTabAuditLogs());

        tabs.addChangeListener(e -> {
            int idx = tabs.getSelectedIndex();
            if (idx == 0) refreshOverview();
            else if (idx == 1) refreshUsers();
            else if (idx == 2) refreshOrders();
            else if (idx == 3) refreshLogs();
        });

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    // =========================================================================
    // TAB 1: TỔNG QUAN & KPI HỆ THỐNG
    // =========================================================================

    private JPanel buildTabOverview() {
        JPanel tab = new JPanel(new BorderLayout(14, 14));
        tab.setBackground(CLR_BG);
        tab.setBorder(new EmptyBorder(16, 18, 16, 18));

        // 4 KPI Cards Grid
        JPanel cardsGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        cardsGrid.setBackground(CLR_BG);

        // Card 1: Users
        JPanel cardUsers = createCard("👥  QUẢN TRỊ TÀI KHOẢN (USERS)", CLR_ACCENT);
        lblTotalUsers  = cardValueLabel("0");
        lblActiveUsers = cardSubLabel("• Hoạt động (Active): 0");
        lblLockedUsers = cardSubLabel("• Bị khóa (Locked): 0");
        cardUsers.add(lblTotalUsers);
        cardUsers.add(lblActiveUsers);
        cardUsers.add(lblLockedUsers);

        // Card 2: Storage
        JPanel cardStorage = createCard("💾  DUNG LƯỢNG LƯU TRỮ (STORAGE)", CLR_SUCCESS);
        lblTotalUsed    = cardValueLabel("0 MB");
        lblTotalStorage = cardSubLabel("• Tổng hạn mức cấp: 0 MB");
        lblFillRatio    = cardSubLabel("• Tỷ lệ sử dụng: 0%");
        barStorageHealth = new JProgressBar(0, 100);
        barStorageHealth.setStringPainted(true);
        barStorageHealth.setBackground(CLR_PANEL);
        barStorageHealth.setForeground(CLR_SUCCESS);
        cardStorage.add(lblTotalUsed);
        cardStorage.add(lblTotalStorage);
        cardStorage.add(lblFillRatio);
        cardStorage.add(barStorageHealth);

        // Card 3: Files
        JPanel cardFiles = createCard("📁  TỆP TIN HỆ THỐNG (FILES)", new Color(180, 120, 240));
        lblTotalFiles = cardValueLabel("0 tệp");
        JLabel lblFileNote = cardSubLabel("• Kiểm soát toàn vẹn Checksum SHA-256");
        JLabel lblSandNote = cardSubLabel("• Phân vùng Sandbox cô lập an toàn");
        cardFiles.add(lblTotalFiles);
        cardFiles.add(lblFileNote);
        cardFiles.add(lblSandNote);

        // Card 4: Orders & Revenue
        JPanel cardOrders = createCard("💳  DOANH THU & ĐƠN HÀNG (REVENUE)", CLR_WARN);
        lblTotalRevenue = cardValueLabel("0 VND");
        lblTotalOrders  = cardSubLabel("• Tổng số đơn hàng: 0");
        JLabel lblGatewayNote = cardSubLabel("• Cổng thanh toán: Mock Banking / QR");
        cardOrders.add(lblTotalRevenue);
        cardOrders.add(lblTotalOrders);
        cardOrders.add(lblGatewayNote);

        cardsGrid.add(cardUsers);
        cardsGrid.add(cardStorage);
        cardsGrid.add(cardFiles);
        cardsGrid.add(cardOrders);

        tab.add(cardsGrid, BorderLayout.CENTER);

        // Nút làm mới dưới cùng
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setBackground(CLR_BG);
        JButton btnRefresh = createStyledBtn("Làm mới số liệu KPI", CLR_ACCENT);
        btnRefresh.addActionListener(e -> refreshOverview());
        bottomBar.add(btnRefresh);
        tab.add(bottomBar, BorderLayout.SOUTH);

        return tab;
    }

    private void refreshOverview() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.getAdminStats();
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("200")) {
                        // 200 TotalUsers: 4 | Active: 4 | Locked: 0 | TotalStorage: 10.3 GB | TotalUsed: 221.0 KB | TotalFiles: 2 | TotalOrders: 1 | Revenue: 50,000 VND
                        String[] parts = resp.substring(4).split("\\|");
                        for (String p : parts) {
                            String item = p.trim();
                            if (item.startsWith("TotalUsers:")) lblTotalUsers.setText(item.split(":")[1].trim() + " tài khoản");
                            else if (item.startsWith("Active:")) lblActiveUsers.setText("• Đang hoạt động (Active): " + item.split(":")[1].trim());
                            else if (item.startsWith("Locked:")) lblLockedUsers.setText("• Bị khóa (Locked): " + item.split(":")[1].trim());
                            else if (item.startsWith("TotalUsed:")) lblTotalUsed.setText(item.split(":")[1].trim());
                            else if (item.startsWith("TotalStorage:")) lblTotalStorage.setText("• Tổng hạn mức cấp: " + item.split(":")[1].trim());
                            else if (item.startsWith("TotalFiles:")) lblTotalFiles.setText(item.split(":")[1].trim() + " tệp tin");
                            else if (item.startsWith("TotalOrders:")) lblTotalOrders.setText("• Tổng đơn hàng: " + item.split(":")[1].trim());
                            else if (item.startsWith("Revenue:")) lblTotalRevenue.setText(item.split(":")[1].trim());
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // =========================================================================
    // TAB 2: QUẢN LÝ NGƯỜI DÙNG & PHÂN QUYỀN RBAC
    // =========================================================================

    private JPanel buildTabUsers() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.setBackground(CLR_BG);
        tab.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"ID", "Tên tài khoản", "Vai trò (Role)", "Hạn mức (MB)", "Đã dùng (KB)", "Trạng thái"};
        userTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = createStyledTable(userTableModel);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        userTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Tên tài khoản
        userTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Vai trò
        userTable.getColumnModel().getColumn(3).setPreferredWidth(110); // Hạn mức (MB)
        userTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Đã dùng (KB)
        userTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Trạng thái
        tab.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // Thanh công cụ quản trị
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionPanel.setBackground(CLR_BG);

        JButton btnToggleStatus = createStyledBtn("Khóa / Mở khóa", CLR_ERROR);
        JButton btnToggleRole   = createStyledBtn("Đổi vai trò (Role)", CLR_WARN);
        JButton btnSetQuota     = createStyledBtn("Đặt lại Quota (MB)", CLR_ACCENT);
        JButton btnRefresh      = createStyledBtn("Tải lại", CLR_CARD);

        btnToggleStatus.addActionListener(e -> doToggleUserStatus());
        btnToggleRole.addActionListener(e   -> doToggleUserRole());
        btnSetQuota.addActionListener(e     -> doSetUserQuota());
        btnRefresh.addActionListener(e      -> refreshUsers());

        actionPanel.add(btnToggleStatus);
        actionPanel.add(btnToggleRole);
        actionPanel.add(btnSetQuota);
        actionPanel.add(btnRefresh);

        tab.add(actionPanel, BorderLayout.SOUTH);
        return tab;
    }

    private void refreshUsers() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ftpService.getUserList();
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    userTableModel.setRowCount(0);
                    for (String raw : lines) {
                        String l = raw.trim();
                        if (l.matches("^\\d+\\s+.*")) {
                            String[] parts = l.split("\\s+");
                            if (parts.length >= 6) {
                                userTableModel.addRow(new Object[]{
                                        parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void doToggleUserStatus() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = userTable.convertRowIndexToModel(row);
        String uname = (String) userTableModel.getValueAt(modelRow, 1);
        String currentActive = (String) userTableModel.getValueAt(modelRow, 5);
        boolean newActive = !"YES".equalsIgnoreCase(currentActive);

        int cf = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn " + (newActive ? "MỞ KHÓA" : "KHÓA") + " tài khoản [" + uname + "]?",
                "Xác nhận thay đổi trạng thái", JOptionPane.YES_NO_OPTION);
        if (cf != JOptionPane.YES_OPTION) return;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.setUserStatus(uname, newActive);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    JOptionPane.showMessageDialog(AdminDashboardDialog.this, resp);
                    refreshUsers();
                    refreshOverview();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboardDialog.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doToggleUserRole() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = userTable.convertRowIndexToModel(row);
        String uname = (String) userTableModel.getValueAt(modelRow, 1);
        String currentRole = (String) userTableModel.getValueAt(modelRow, 2);
        String newRole = currentRole.contains("ADMIN") ? "ROLE_USER" : "ROLE_ADMIN";

        int cf = JOptionPane.showConfirmDialog(this,
                "Bạn có muốn đổi vai trò của [" + uname + "] từ " + currentRole + " sang " + newRole + "?",
                "Xác nhận đổi quyền", JOptionPane.YES_NO_OPTION);
        if (cf != JOptionPane.YES_OPTION) return;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.setUserRole(uname, newRole);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    JOptionPane.showMessageDialog(AdminDashboardDialog.this, resp);
                    refreshUsers();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboardDialog.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doSetUserQuota() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = userTable.convertRowIndexToModel(row);
        String uname = (String) userTableModel.getValueAt(modelRow, 1);
        String curQuota = (String) userTableModel.getValueAt(modelRow, 3);

        String input = JOptionPane.showInputDialog(this,
                "Nhập hạn mức Quota mới cho tài khoản [" + uname + "] (đơn vị: MB):", curQuota);
        if (input == null || input.trim().isEmpty()) return;

        long mb;
        try {
            mb = Long.parseLong(input.trim());
            if (mb <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Dung lượng phải là số nguyên dương lớn hơn 0 MB!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.setUserQuota(uname, mb);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    JOptionPane.showMessageDialog(AdminDashboardDialog.this, resp);
                    refreshUsers();
                    refreshOverview();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboardDialog.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // =========================================================================
    // TAB 3: QUẢN LÝ ĐƠN HÀNG TOÀN HỆ THỐNG
    // =========================================================================

    private JPanel buildTabOrders() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.setBackground(CLR_BG);
        tab.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {"Order ID", "Mã đơn hàng", "Gói cước", "Số tiền", "Trạng thái", "Ngày tạo"};
        orderTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        orderTable = createStyledTable(orderTableModel);
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(65);  // Order ID
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(210); // Mã đơn hàng
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(85);  // Gói cước
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(110); // Số tiền
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Trạng thái
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(160); // Ngày tạo
        tab.add(new JScrollPane(orderTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bottomBar.setBackground(CLR_BG);
        JButton btnRefresh = createStyledBtn("Làm mới danh sách đơn", CLR_ACCENT);
        btnRefresh.addActionListener(e -> refreshOrders());
        bottomBar.add(btnRefresh);

        tab.add(bottomBar, BorderLayout.SOUTH);
        return tab;
    }

    private void refreshOrders() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ftpService.getAdminAllOrders();
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    orderTableModel.setRowCount(0);
                    for (String raw : lines) {
                        String l = raw.trim();
                        if (l.matches("^\\d+\\s+.*")) {
                            String[] parts = l.split("\\s+");
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
                                orderTableModel.addRow(new Object[]{oid, code, plan, amt, st, dt});
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // =========================================================================
    // TAB 4: NHẬT KÝ KIỂM TOÁN HỆ THỐNG (AUDIT LOGS)
    // =========================================================================

    private JPanel buildTabAuditLogs() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.setBackground(CLR_BG);
        tab.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.setBackground(CLR_BG);
        JLabel lblFilter = new JLabel("Lọc theo loại hành động:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFilter.setForeground(CLR_TEXT);

        String[] filters = {"ALL", "LOGIN", "UPLOAD", "DOWNLOAD", "DELE", "SHARE", "PAYMENT_SUCCESS", "PAYMENT_FAILED", "CANCEL_ORDER", "ADMIN_SET_STATUS", "ADMIN_SET_ROLE", "ADMIN_SET_QUOTA"};
        cbLogFilter = new JComboBox<>(filters);
        cbLogFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton btnFilter = createStyledBtn("Lọc nhật ký", CLR_ACCENT);
        btnFilter.addActionListener(e -> refreshLogs());

        filterBar.add(lblFilter);
        filterBar.add(cbLogFilter);
        filterBar.add(btnFilter);
        tab.add(filterBar, BorderLayout.NORTH);

        // Table Logs
        String[] cols = {"Thời gian", "Tài khoản", "Hành động", "Đối tượng / Tệp", "Trạng thái", "Chi tiết / Thông báo"};
        logTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        logTable = createStyledTable(logTableModel);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Thời gian
        logTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Tài khoản
        logTable.getColumnModel().getColumn(2).setPreferredWidth(140); // Hành động
        logTable.getColumnModel().getColumn(3).setPreferredWidth(160); // Đối tượng / Tệp
        logTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Trạng thái
        logTable.getColumnModel().getColumn(5).setPreferredWidth(240); // Chi tiết / Thông báo
        tab.add(new JScrollPane(logTable), BorderLayout.CENTER);

        return tab;
    }

    private void refreshLogs() {
        String filter = (String) cbLogFilter.getSelectedItem();
        if (filter == null) filter = "ALL";
        final String finalFilter = filter;

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ftpService.getAuditLogs(finalFilter, 1);
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    logTableModel.setRowCount(0);
                    for (String raw : lines) {
                        String l = raw.trim();
                        if (l.matches("^\\d{4}-\\d{2}-\\d{2}\\s+.*")) {
                            String[] parts = l.split("\\s+", 7);
                            if (parts.length >= 6) {
                                String time = parts[0] + " " + parts[1];
                                String user = parts[2];
                                String act = parts[3];
                                String file = parts[4];
                                String status = parts[5];
                                String msg = parts.length >= 7 ? parts[6] : "";
                                logTableModel.addRow(new Object[]{time, user, act, file, status, msg});
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================

    private JPanel createCard(String title, Color accentColor) {
        JPanel card = new JPanel(new GridLayout(4, 1, 3, 3));
        card.setBackground(CLR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                new EmptyBorder(10, 14, 10, 14)
        ));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(accentColor);
        card.add(lblTitle);
        return card;
    }

    private JLabel cardValueLabel(String val) {
        JLabel l = new JLabel(val);
        l.setFont(new Font("Segoe UI", Font.BOLD, 17));
        l.setForeground(Color.WHITE);
        return l;
    }

    private JLabel cardSubLabel(String sub) {
        JLabel l = new JLabel(sub);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(CLR_MUTED);
        return l;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(CLR_TABLE_R);
        table.setForeground(CLR_TEXT);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setSelectionBackground(CLR_TABLE_S);
        table.setSelectionForeground(Color.WHITE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(45, 52, 70));
        table.getTableHeader().setBackground(CLR_TABLE_H);
        table.getTableHeader().setForeground(CLR_TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, CLR_BORDER));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? CLR_TABLE_S : (row % 2 == 0 ? CLR_TABLE_R : CLR_TABLE_A));
                setForeground(sel ? Color.WHITE : CLR_TEXT);
                setBorder(new EmptyBorder(0, 8, 0, 8));

                String colName = t.getColumnName(col).toLowerCase();
                // Tự động căn lề theo ngữ cảnh cột
                if (colName.contains("id") || colName.contains("mã") || colName.contains("ngày") || colName.contains("thời gian") ||
                    colName.contains("trạng thái") || colName.contains("quyền") || colName.contains("lượt") || colName.contains("vai trò") ||
                    colName.contains("gói") || colName.contains("active")) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else if (colName.contains("tiền") || colName.contains("dung lượng") || colName.contains("kích thước") ||
                           colName.contains("hạn mức") || colName.contains("đã dùng") || colName.contains("quota") || colName.contains("used")) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Tô màu trạng thái & vai trò
                if (val != null) {
                    String str = val.toString().trim();
                    if ("YES".equalsIgnoreCase(str) || "ACTIVE".equalsIgnoreCase(str) || "COMPLETED".equalsIgnoreCase(str) || "SUCCESS".equalsIgnoreCase(str)) {
                        setForeground(sel ? Color.WHITE : CLR_SUCCESS);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if ("NO".equalsIgnoreCase(str) || "LOCKED".equalsIgnoreCase(str) || "FAILED".equalsIgnoreCase(str) || "CANCELLED".equalsIgnoreCase(str)) {
                        setForeground(sel ? Color.WHITE : CLR_ERROR);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if ("PENDING".equalsIgnoreCase(str) || str.contains("ADMIN")) {
                        setForeground(sel ? Color.WHITE : CLR_WARN);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    }
                }
                return this;
            }
        });
        return table;
    }

    private ModernButton createStyledBtn(String text, Color bg) {
        ModernButton btn = new ModernButton(text, bg, Color.WHITE, bg.equals(CLR_CARD) ? CLR_BORDER : null);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 32));
        return btn;
    }
}


package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * PHASE 7 — Màn hình chính FTP Client
 *
 * Kiến trúc:
 *   MainFrame  ──gọi──►  FtpClientService  ──Socket──►  Server
 *
 * MainFrame KHÔNG chứa bất kỳ code Socket nào.
 * Mọi thao tác network đều thông qua FtpClientService.
 *
 * Upload/Download chạy trong SwingWorker (Thread riêng),
 * KHÔNG block Event Dispatch Thread (EDT) tránh đóng băng UI.
 */
public class MainFrame extends JFrame {

    // ── Service Layer ─────────────────────────────────────────────────
    private final FtpClientService ftpService;

    // ── Màu sắc ───────────────────────────────────────────────────────
    private static final Color CLR_BG      = new Color(22, 27, 38);
    private static final Color CLR_PANEL   = new Color(30, 36, 50);
    private static final Color CLR_TOOLBAR = new Color(26, 32, 44);
    private static final Color CLR_ACCENT  = new Color(99, 179, 237);
    private static final Color CLR_SUCCESS = new Color(72, 199, 116);
    private static final Color CLR_ERROR   = new Color(252, 100, 100);
    private static final Color CLR_WARN    = new Color(255, 193, 70);
    private static final Color CLR_TEXT    = new Color(226, 232, 240);
    private static final Color CLR_MUTED   = new Color(100, 116, 139);
    private static final Color CLR_TABLE_H = new Color(44, 52, 70);
    private static final Color CLR_TABLE_S = new Color(55, 90, 130);
    private static final Color CLR_TABLE_R = new Color(34, 40, 54);
    private static final Color CLR_TABLE_A = new Color(38, 45, 60);

    // ── Thành phần UI ─────────────────────────────────────────────────
    private JLabel          lblServer;
    private JLabel          lblUser;
    private JLabel          lblDir;
    private JLabel          lblStatus;
    private DefaultTableModel tableModel;
    private JTable          fileTable;
    private JProgressBar    progressBar;
    private JTextArea       logArea;
    private JButton         btnRefresh;
    private JButton         btnUpload;
    private JButton         btnDownload;
    private JButton         btnDelete;
    private JButton         btnRename;
    private JButton         btnLogout;
    private JButton         btnDisconnect;

    public MainFrame(FtpClientService service) {
        this.ftpService = service;
        initUI();
        setVisible(true);
        // Load danh sách file ngay khi mở
        doRefresh();
    }

    // ================================================================
    // Khởi tạo giao diện
    // ================================================================

    private void initUI() {
        setTitle("FTP Client — " + ftpService.getLoggedUser()
                + " @ " + "localhost:6789");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(920, 640);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(720, 500));

        // Xác nhận khi đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Bạn muốn ngắt kết nối và thoát chương trình?",
                        "Xác nhận thoát", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    ftpService.disconnect();
                    System.exit(0);
                }
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(CLR_BG);

        root.add(buildTopBar(),    BorderLayout.NORTH);
        root.add(buildCenter(),    BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Thanh thông tin trên cùng ─────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CLR_TOOLBAR);
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Bên trái: thông tin kết nối
        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        info.setBackground(CLR_TOOLBAR);

        lblServer = infoLabel("🌐  localhost:6789");
        lblUser   = infoLabel("👤  " + ftpService.getLoggedUser());
        lblDir    = infoLabel("📂  /");
        lblStatus = statusLabel("🟢  Đã kết nối");

        info.add(lblServer);
        info.add(sep());
        info.add(lblUser);
        info.add(sep());
        info.add(lblDir);
        info.add(sep());
        info.add(lblStatus);
        bar.add(info, BorderLayout.WEST);

        // Bên phải: nút Logout + Disconnect
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setBackground(CLR_TOOLBAR);
        btnLogout     = toolBtn("⬅  Logout",    CLR_WARN);
        btnDisconnect = toolBtn("✖  Ngắt kết nối", CLR_ERROR);
        rightBtns.add(btnLogout);
        rightBtns.add(btnDisconnect);
        bar.add(rightBtns, BorderLayout.EAST);

        btnLogout.addActionListener(e     -> doLogout());
        btnDisconnect.addActionListener(e -> doDisconnect());

        return bar;
    }

    // ── Phần trung tâm: File Explorer + Log ───────────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(CLR_BG);

        // ── Toolbar nút bấm chức năng ─────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(CLR_PANEL);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(50, 60, 80)));

        btnRefresh  = funcBtn("🔄  Làm mới",  CLR_ACCENT);
        btnUpload   = funcBtn("⬆  Upload",    new Color(72, 167, 255));
        btnDownload = funcBtn("⬇  Download",  CLR_SUCCESS);
        btnDelete   = funcBtn("🗑  Xóa",      CLR_ERROR);
        btnRename   = funcBtn("✏  Đổi tên",  CLR_WARN);

        toolbar.add(btnRefresh);
        toolbar.add(btnUpload);
        toolbar.add(btnDownload);
        toolbar.add(btnDelete);
        toolbar.add(btnRename);

        // ── Bảng danh sách file (JTable) ──────────────────────────────
        String[] columns = {"  ", "Tên file / Thư mục", "Loại", "Kích thước", "Ngày sửa"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        fileTable = new JTable(tableModel);
        fileTable.setBackground(CLR_TABLE_R);
        fileTable.setForeground(CLR_TEXT);
        fileTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileTable.setRowHeight(28);
        fileTable.setSelectionBackground(CLR_TABLE_S);
        fileTable.setSelectionForeground(Color.WHITE);
        fileTable.setGridColor(new Color(45, 52, 70));
        fileTable.setIntercellSpacing(new Dimension(0, 1));
        fileTable.setShowVerticalLines(false);
        fileTable.getTableHeader().setBackground(CLR_TABLE_H);
        fileTable.getTableHeader().setForeground(CLR_MUTED);
        fileTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        fileTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // Điều chỉnh độ rộng cột
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        fileTable.getColumnModel().getColumn(0).setMaxWidth(40);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(280);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        fileTable.getColumnModel().getColumn(4).setPreferredWidth(140);

        // Zebra striping (xen kẽ màu dòng)
        fileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? CLR_TABLE_S : (row % 2 == 0 ? CLR_TABLE_R : CLR_TABLE_A));
                setForeground(sel ? Color.WHITE  : CLR_TEXT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                setFont(new Font("Segoe UI", col == 0 ? Font.PLAIN : Font.PLAIN, 13));
                return this;
            }
        });

        // Double-click trên thư mục (mở rộng ra Phase 8)
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fileTable.getSelectedRow();
                    if (row >= 0) {
                        String type = (String) tableModel.getValueAt(row, 2);
                        if ("Thư mục".equals(type)) {
                            appendLog("ℹ️  Mở thư mục chưa được hỗ trợ trong phiên bản này.");
                        }
                    }
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(fileTable);
        tableScroll.setBackground(CLR_TABLE_R);
        tableScroll.getViewport().setBackground(CLR_TABLE_R);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        // ── Progress Bar ──────────────────────────────────────────────
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng");
        progressBar.setValue(0);
        progressBar.setBackground(CLR_PANEL);
        progressBar.setForeground(CLR_SUCCESS);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressBar.setBorder(new EmptyBorder(4, 8, 4, 8));

        // ── Log area ──────────────────────────────────────────────────
        logArea = new JTextArea(5, 0);
        logArea.setEditable(false);
        logArea.setBackground(new Color(18, 22, 30));
        logArea.setForeground(new Color(150, 210, 150));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBorder(new EmptyBorder(6, 10, 6, 10));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                new Color(45, 55, 72)));
        logScroll.setPreferredSize(new Dimension(0, 120));

        // ── Ghép layout ───────────────────────────────────────────────
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(CLR_BG);
        tablePanel.add(toolbar,     BorderLayout.NORTH);
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        tablePanel.add(progressBar, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                tablePanel, logScroll);
        split.setDividerSize(3);
        split.setResizeWeight(0.78);
        split.setBorder(null);
        split.setBackground(CLR_BG);

        center.add(split, BorderLayout.CENTER);

        // ── Gắn sự kiện nút ──────────────────────────────────────────
        btnRefresh.addActionListener(e  -> doRefresh());
        btnUpload.addActionListener(e   -> doUpload());
        btnDownload.addActionListener(e -> doDownload());
        btnDelete.addActionListener(e   -> doDelete());
        btnRename.addActionListener(e   -> doRename());

        return center;
    }

    // ── Status bar dưới cùng ─────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        bar.setBackground(CLR_TOOLBAR);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(45, 55, 72)));
        JLabel lbl = new JLabel("FTP Project — Phase 7 GUI  |  Java Swing  |  Lập Trình Mạng");
        lbl.setForeground(CLR_MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bar.add(lbl);
        return bar;
    }

    // ================================================================
    // CHỨC NĂNG: Refresh (LIST)
    // ================================================================

    private void doRefresh() {
        setButtons(false);
        setStatus("🔄  Đang tải danh sách...", CLR_ACCENT);
        appendLog("→  LIST");

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
                    for (FtpClientService.FileEntry e : entries) {
                        tableModel.addRow(new Object[]{
                                e.getIcon(), e.name, e.getType(), e.getSizeText(), e.date
                        });
                    }
                    updateDirLabel();
                    setStatus("🟢  Đã tải: " + entries.size() + " mục", CLR_SUCCESS);
                    appendLog("←  226 OK. " + entries.size() + " item(s).");
                } catch (Exception ex) {
                    setStatus("🔴  " + ex.getMessage(), CLR_ERROR);
                    appendLog("←  Lỗi: " + ex.getMessage());
                }
                setButtons(true);
            }
        }.execute();
    }

    // ================================================================
    // CHỨC NĂNG: Upload (STOR)
    // ================================================================

    private void doUpload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn file để Upload lên Server");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File localFile = chooser.getSelectedFile();
        setButtons(false);
        setProgress(0, "Uploading: " + localFile.getName() + " ...");
        setStatus("⬆  Đang upload: " + localFile.getName(), CLR_ACCENT);
        appendLog("→  STOR " + localFile.getName() + " (" + localFile.length() + " bytes)");

        new SwingWorker<String, Integer>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.stor(localFile, (transferred, total) ->
                        SwingUtilities.invokeLater(() -> {
                            int pct = total > 0 ? (int)(transferred * 100 / total) : 100;
                            MainFrame.this.setProgress(pct, "Uploading... " + pct + "%");
                        })
                );
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("226")) {
                        setStatus("✅  Upload thành công: " + localFile.getName(), CLR_SUCCESS);
                        appendLog("←  " + resp);
                        MainFrame.this.setProgress(100, "Upload thành công!");
                        doRefresh();
                    } else {
                        setStatus("🔴  Upload thất bại: " + resp, CLR_ERROR);
                        appendLog("←  " + resp);
                        MainFrame.this.setProgress(0, "Upload thất bại");
                        setButtons(true);
                    }
                } catch (Exception ex) {
                    setStatus("🔴  Lỗi upload: " + ex.getMessage(), CLR_ERROR);
                    appendLog("←  Lỗi: " + ex.getMessage());
                    MainFrame.this.setProgress(0, "Lỗi");
                    setButtons(true);
                }
            }
        }.execute();
    }

    // ================================================================
    // CHỨC NĂNG: Download (RETR)
    // ================================================================

    private void doDownload() {
        int row = fileTable.getSelectedRow();
        if (row < 0) {
            showWarn("Vui lòng chọn file cần Download.");
            return;
        }
        String type = (String) tableModel.getValueAt(row, 2);
        if ("Thư mục".equals(type)) {
            showWarn("Không thể download thư mục. Hãy chọn một file.");
            return;
        }
        String filename = (String) tableModel.getValueAt(row, 1);

        setButtons(false);
        setProgress(0, "Downloading: " + filename + " ...");
        setStatus("⬇  Đang download: " + filename, CLR_ACCENT);
        appendLog("→  RETR " + filename);

        File saveDir = new File("downloads");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.retr(filename, saveDir, (transferred, total) ->
                        SwingUtilities.invokeLater(() -> {
                            int pct = total > 0 ? (int)(transferred * 100 / total) : 100;
                            MainFrame.this.setProgress(pct, "Downloading... " + pct + "%");
                        })
                );
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("226")) {
                        setStatus("✅  Download thành công: " + filename, CLR_SUCCESS);
                        appendLog("←  " + resp);
                        MainFrame.this.setProgress(100, "Download thành công!");
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "✅  Đã tải xong: " + filename
                                        + "\nLưu tại: downloads/" + filename,
                                "Download thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        setStatus("🔴  " + resp, CLR_ERROR);
                        appendLog("←  " + resp);
                        MainFrame.this.setProgress(0, "Download thất bại");
                    }
                } catch (Exception ex) {
                    setStatus("🔴  Lỗi download: " + ex.getMessage(), CLR_ERROR);
                    appendLog("←  Lỗi: " + ex.getMessage());
                    MainFrame.this.setProgress(0, "Lỗi");
                }
                setButtons(true);
            }
        }.execute();
    }

    // ================================================================
    // CHỨC NĂNG: Xóa file (DELE)
    // ================================================================

    private void doDelete() {
        int row = fileTable.getSelectedRow();
        if (row < 0) { showWarn("Vui lòng chọn file cần xóa."); return; }

        String filename = (String) tableModel.getValueAt(row, 1);
        String type     = (String) tableModel.getValueAt(row, 2);
        if ("Thư mục".equals(type)) { showWarn("Không thể xóa thư mục."); return; }

        // Confirm dialog
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️  Bạn có chắc muốn xóa file:\n\n\"" + filename + "\"\n\nHành động này không thể hoàn tác!",
                "Xác nhận xóa file", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        setButtons(false);
        setStatus("🗑  Đang xóa: " + filename, CLR_WARN);
        appendLog("→  DELE " + filename);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.dele(filename);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("250")) {
                        setStatus("✅  Đã xóa: " + filename, CLR_SUCCESS);
                        appendLog("←  " + resp);
                        doRefresh();
                    } else {
                        setStatus("🔴  " + resp, CLR_ERROR);
                        appendLog("←  " + resp);
                        setButtons(true);
                    }
                } catch (Exception ex) {
                    setStatus("🔴  Lỗi: " + ex.getMessage(), CLR_ERROR);
                    appendLog("←  Lỗi: " + ex.getMessage());
                    setButtons(true);
                }
            }
        }.execute();
    }

    // ================================================================
    // CHỨC NĂNG: Đổi tên (RNFR + RNTO)
    // ================================================================

    private void doRename() {
        int row = fileTable.getSelectedRow();
        if (row < 0) { showWarn("Vui lòng chọn file cần đổi tên."); return; }

        String oldName = (String) tableModel.getValueAt(row, 1);
        String type    = (String) tableModel.getValueAt(row, 2);
        if ("Thư mục".equals(type)) { showWarn("Không thể đổi tên thư mục."); return; }

        String newName = (String) JOptionPane.showInputDialog(
                this,
                "Nhập tên mới cho file:\n\"" + oldName + "\"",
                "Đổi tên file", JOptionPane.PLAIN_MESSAGE,
                null, null, oldName);

        if (newName == null || newName.trim().isEmpty() || newName.trim().equals(oldName)) return;
        newName = newName.trim();

        String finalNewName = newName;
        setButtons(false);
        setStatus("✏  Đang đổi tên: " + oldName + " → " + newName, CLR_WARN);
        appendLog("→  RNFR " + oldName + "  →  RNTO " + newName);

        String fn = finalNewName;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.rename(oldName, fn);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    if (resp != null && resp.startsWith("250")) {
                        setStatus("✅  Đổi tên thành công: " + fn, CLR_SUCCESS);
                        appendLog("←  " + resp);
                        doRefresh();
                    } else {
                        setStatus("🔴  " + resp, CLR_ERROR);
                        appendLog("←  " + resp);
                        setButtons(true);
                    }
                } catch (Exception ex) {
                    setStatus("🔴  Lỗi: " + ex.getMessage(), CLR_ERROR);
                    appendLog("←  Lỗi: " + ex.getMessage());
                    setButtons(true);
                }
            }
        }.execute();
    }

    // ================================================================
    // CHỨC NĂNG: Logout + Disconnect
    // ================================================================

    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Đăng xuất và quay lại màn hình Login?\n(Kết nối Socket vẫn được giữ)",
                "Xác nhận Logout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try { ftpService.logout(); } catch (Exception ignored) {}
            SwingUtilities.invokeLater(() -> {
                new LoginFrame();
                MainFrame.this.dispose();
            });
        }).start();
    }

    private void doDisconnect() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Ngắt kết nối hoàn toàn và quay về màn hình Login?",
                "Xác nhận ngắt kết nối", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            ftpService.disconnect();
            SwingUtilities.invokeLater(() -> {
                new LoginFrame();
                MainFrame.this.dispose();
            });
        }).start();
    }

    // ================================================================
    // Hàm hỗ trợ UI
    // ================================================================

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(text);
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
            logArea.append("[" + now() + "]  " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setButtons(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            btnRefresh.setEnabled(enabled);
            btnUpload.setEnabled(enabled);
            btnDownload.setEnabled(enabled);
            btnDelete.setEnabled(enabled);
            btnRename.setEnabled(enabled);
            btnLogout.setEnabled(enabled);
            btnDisconnect.setEnabled(enabled);
        });
    }

    private void updateDirLabel() {
        try { lblDir.setText("📂  " + ftpService.pwd().split("\"")[1]); }
        catch (Exception ignored) { lblDir.setText("📂  /"); }
    }

    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cảnh báo",
                JOptionPane.WARNING_MESSAGE);
    }

    private String now() {
        return new java.text.SimpleDateFormat("HH:mm:ss")
                .format(new java.util.Date());
    }

    // ── Helpers tạo component ─────────────────────────────────────────

    private JLabel infoLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(CLR_TEXT);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return lbl;
    }

    private JLabel statusLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(CLR_SUCCESS);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return lbl;
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setForeground(new Color(60, 70, 90));
        s.setPreferredSize(new Dimension(1, 16));
        return s;
    }

    private JButton toolBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg.darker());
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 28));
        return btn;
    }

    private JButton funcBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 32));
        Color hov = bg.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hov); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }
}

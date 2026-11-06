# -*- coding: utf-8 -*-
import sys

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update window dimensions
old_size = "        setSize(1080, 680);\n        setLocationRelativeTo(null);\n        setMinimumSize(new Dimension(720, 500));"
new_size = "        setSize(1100, 720);\n        setLocationRelativeTo(null);\n        setMinimumSize(new Dimension(950, 580));"

if old_size in content:
    content = content.replace(old_size, new_size)
    print("[OK] Replaced window size")
else:
    print("[WARN] old_size not found")

# 2. Update toolbar definition and tablePanel add
old_toolbar = """        // ── Toolbar nút bấm chức năng ─────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(CLR_PANEL);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(50, 60, 80)));

        btnRefresh   = funcBtn("🔄  Làm mới",     CLR_ACCENT);
        btnParentDir = funcBtn("⬆  Thư mục cha", CLR_ACCENT);
        btnNewFolder = funcBtn("📁  + Thư mục",   new Color(155, 110, 240));
        btnUpload    = funcBtn("⬆  Upload",       new Color(72, 167, 255));
        btnDownload  = funcBtn("⬇  Download",     CLR_SUCCESS);
        btnDelete    = funcBtn("🗑  Xóa",         CLR_ERROR);
        btnRename    = funcBtn("✏  Đổi tên",     CLR_WARN);

        btnShare        = funcBtn("🔗  Chia sẻ",        new Color(40, 180, 130));
        btnSharedWithMe = funcBtn("📥  Được chia sẻ",    new Color(30, 144, 255));
        btnMyShares     = funcBtn("📋  Quản lý chia sẻ", new Color(180, 100, 220));
        btnGetShared    = funcBtn("🎟  Tải qua Token",   new Color(240, 140, 30));
        btnLogs         = funcBtn("📜  Nhật ký",        new Color(100, 120, 150));

        toolbar.add(btnRefresh);
        toolbar.add(btnParentDir);
        toolbar.add(btnNewFolder);
        toolbar.add(btnUpload);
        toolbar.add(btnDownload);
        toolbar.add(btnDelete);
        toolbar.add(btnRename);
        toolbar.add(btnShare);
        toolbar.add(btnSharedWithMe);
        toolbar.add(btnMyShares);
        toolbar.add(btnGetShared);
        toolbar.add(btnLogs);"""

new_toolbar = """        // ── Container 2 hàng Toolbar phân nhóm rõ ràng ─────────────────
        JPanel toolbarContainer = new JPanel();
        toolbarContainer.setLayout(new BoxLayout(toolbarContainer, BoxLayout.Y_AXIS));
        toolbarContainer.setBackground(CLR_PANEL);
        toolbarContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(50, 60, 80)));

        // Hàng 1: Thao tác Tệp & Thư mục cá nhân
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        row1.setBackground(CLR_PANEL);

        btnRefresh   = funcBtn("🔄  Làm mới",     CLR_ACCENT, 110);
        btnParentDir = funcBtn("⬆  Thư mục cha", CLR_ACCENT, 125);
        btnNewFolder = funcBtn("📁  + Thư mục",   new Color(155, 110, 240), 115);
        btnUpload    = funcBtn("⬆  Upload",       new Color(72, 167, 255), 110);
        btnDownload  = funcBtn("⬇  Download",     CLR_SUCCESS, 110);
        btnDelete    = funcBtn("🗑  Xóa",         CLR_ERROR, 95);
        btnRename    = funcBtn("✏  Đổi tên",     CLR_WARN, 105);

        row1.add(btnRefresh);
        row1.add(btnParentDir);
        row1.add(btnNewFolder);
        row1.add(btnUpload);
        row1.add(btnDownload);
        row1.add(btnDelete);
        row1.add(btnRename);

        // Hàng 2: Tính năng Chia sẻ & Nhật ký kiểm toán
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        row2.setBackground(CLR_PANEL);

        btnShare        = funcBtn("🔗  Chia sẻ tệp",            new Color(40, 180, 130), 130);
        btnSharedWithMe = funcBtn("📥  Được chia sẻ cho tôi",   new Color(30, 144, 255), 180);
        btnMyShares     = funcBtn("📋  Quản lý đã chia sẻ",     new Color(180, 100, 220), 160);
        btnGetShared    = funcBtn("🎟  Tải qua Token",         new Color(240, 140, 30), 140);
        btnLogs         = funcBtn("📜  Nhật ký hoạt động",      new Color(100, 120, 150), 155);

        row2.add(btnShare);
        row2.add(btnSharedWithMe);
        row2.add(btnMyShares);
        row2.add(btnGetShared);
        row2.add(btnLogs);

        toolbarContainer.add(row1);
        toolbarContainer.add(row2);"""

if old_toolbar in content:
    content = content.replace(old_toolbar, new_toolbar)
    print("[OK] Replaced toolbar with 2-row toolbarContainer")
else:
    print("[WARN] old_toolbar not found")

content = content.replace("tablePanel.add(toolbar,     BorderLayout.NORTH);", "tablePanel.add(toolbarContainer, BorderLayout.NORTH);")

# 3. Update funcBtn helper
old_funcBtn = """    private JButton funcBtn(String text, Color bg) {
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
    }"""

new_funcBtn = """    private JButton funcBtn(String text, Color bg) {
        return funcBtn(text, bg, 120);
    }

    private JButton funcBtn(String text, Color bg, int width) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(width, 32));
        Color hov = bg.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hov); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }"""

if old_funcBtn in content:
    content = content.replace(old_funcBtn, new_funcBtn)
    print("[OK] Replaced funcBtn with overloaded version")
else:
    print("[WARN] old_funcBtn not found")

# 4. Enhance doSharedWithMe and doGetShared
old_shared = """    private void doSharedWithMe() {
        setButtons(false);
        setStatus("📥  Đang tải danh sách tệp được chia sẻ...", CLR_ACCENT);

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ftpService.getSharedWithMe();
            }

            @Override
            protected void done() {
                try {
                    List<String> lines = get();
                    StringBuilder sb = new StringBuilder();
                    for (String l : lines) sb.append(l).append("\\n");

                    JTextArea area = new JTextArea(sb.toString(), 15, 75);
                    area.setEditable(false);
                    area.setFont(new Font("Consolas", Font.PLAIN, 12));
                    
                    JPanel panel = new JPanel(new BorderLayout(8, 8));
                    panel.add(new JScrollPane(area), BorderLayout.CENTER);

                    JButton btnQuickDl = new JButton("⬇  Tải tệp này về máy (Nhập mã Token từ bảng trên)...");
                    btnQuickDl.setBackground(new Color(40, 180, 130));
                    btnQuickDl.setForeground(Color.WHITE);
                    btnQuickDl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    btnQuickDl.addActionListener(ev -> doGetShared());
                    panel.add(btnQuickDl, BorderLayout.SOUTH);

                    JOptionPane.showMessageDialog(MainFrame.this, panel, "Tệp Được Chia Sẻ Cho Bạn (Shared With Me)", JOptionPane.PLAIN_MESSAGE);
                } catch (Exception ex) {
                    showError("Lỗi tải danh sách chia sẻ: " + ex.getMessage());
                }
                setButtons(true);
                setStatus("🟢  Sẵn sàng", CLR_SUCCESS);
            }
        }.execute();
    }"""

new_shared = """    private void doSharedWithMe() {
        setButtons(false);
        setStatus("📥  Đang tải danh sách tệp được chia sẻ...", CLR_ACCENT);

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
                    showError("Lỗi tải danh sách chia sẻ: " + ex.getMessage());
                }
                setButtons(true);
                setStatus("🟢  Sẵn sàng", CLR_SUCCESS);
            }
        }.execute();
    }

    private void showSharedWithMeDialog(List<String> lines) {
        String[] cols = {"ID", "Tên tệp tin", "Dung lượng", "Người gửi", "Quyền", "Mã Token"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (String raw : lines) {
            String l = raw.trim();
            if (l.matches("^\\\\d+\\\\s+.*")) {
                String[] parts = l.split("\\\\s+");
                if (parts.length >= 6) {
                    String id = parts[0];
                    String fname = parts[1];
                    String size = parts[2] + " " + parts[3];
                    String owner = parts[4];
                    String perm = parts[5];
                    String token = parts[parts.length - 1];
                    model.addRow(new Object[]{id, fname, size, owner, perm, token});
                }
            }
        }

        JDialog dlg = new JDialog(MainFrame.this, "📥 Tệp Được Chia Sẻ Cho Bạn (Shared With Me)", true);
        dlg.setSize(880, 420);
        dlg.setLocationRelativeTo(MainFrame.this);

        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBackground(CLR_BG);
        pnl.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel titleLbl = new JLabel("Danh sách các tệp người khác đã chia sẻ cho bạn. Chọn một tệp rồi bấm 'Tải tệp đã chọn về máy':");
        titleLbl.setForeground(CLR_TEXT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pnl.add(titleLbl, BorderLayout.NORTH);

        JTable tbl = new JTable(model);
        tbl.setBackground(CLR_TABLE_R);
        tbl.setForeground(CLR_TEXT);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbl.setRowHeight(28);
        tbl.setSelectionBackground(CLR_TABLE_S);
        tbl.setSelectionForeground(Color.WHITE);
        tbl.getTableHeader().setBackground(CLR_TABLE_H);
        tbl.getTableHeader().setForeground(CLR_MUTED);
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        tbl.getColumnModel().getColumn(0).setPreferredWidth(40);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(220);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(90);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(90);
        tbl.getColumnModel().getColumn(5).setPreferredWidth(260);

        JScrollPane sp = new JScrollPane(tbl);
        sp.getViewport().setBackground(CLR_TABLE_R);
        pnl.add(sp, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setBackground(CLR_BG);

        JButton btnDl = new JButton("⬇  Tải tệp đã chọn về máy");
        btnDl.setBackground(new Color(40, 180, 130));
        btnDl.setForeground(Color.WHITE);
        btnDl.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnCopy = new JButton("📋  Sao chép Token");
        btnCopy.setBackground(new Color(100, 120, 150));
        btnCopy.setForeground(Color.WHITE);
        btnCopy.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        btnDl.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhấp chọn một dòng tệp trong bảng trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String fname = (String) model.getValueAt(row, 1);
            String token = (String) model.getValueAt(row, 5);
            dlg.dispose();
            doGetShared(token, fname);
        });

        btnCopy.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhấp chọn một dòng để sao chép mã Token!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String token = (String) model.getValueAt(row, 5);
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(token);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
            JOptionPane.showMessageDialog(dlg, "Đã sao chép Token vào bộ nhớ tạm (Clipboard):\\n" + token, "Sao chép thành công", JOptionPane.INFORMATION_MESSAGE);
        });

        btnClose.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnDl);
        btnPanel.add(btnCopy);
        btnPanel.add(btnClose);
        pnl.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(pnl);
        dlg.setVisible(true);
    }"""

if old_shared in content:
    content = content.replace(old_shared, new_shared)
    print("[OK] Replaced doSharedWithMe with showSharedWithMeDialog")
else:
    print("[WARN] old_shared not found")

# 5. Update doGetShared to support defaultToken and defaultFilename
old_get_shared = """    private void doGetShared() {
        String token = JOptionPane.showInputDialog(this,
                "Nhập mã Token chia sẻ (hoặc mã liên kết):",
                "Tải Tệp Qua Mã Chia Sẻ (Token)", JOptionPane.QUESTION_MESSAGE);
        if (token == null || token.trim().isEmpty()) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn nơi lưu tệp tải về");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dest = chooser.getSelectedFile();

        setButtons(false);
        setStatus("🎟  Đang tải tệp qua Token...", CLR_ACCENT);
        progressBar.setValue(0);

        new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return ftpService.downloadShared(token.trim(), dest, (transferred, total) -> {
                    if (total > 0) {
                        int pct = (int) (transferred * 100 / total);
                        publish(pct);
                    }
                });
            }

            @Override
            protected void process(List<Integer> chunks) {
                int last = chunks.get(chunks.size() - 1);
                progressBar.setValue(last);
                progressBar.setString("Đang tải: " + last + "%");
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        progressBar.setValue(100);
                        progressBar.setString("Tải xong: 100%");
                        setStatus("✅  Tải qua Token thành công: " + dest.getName(), CLR_SUCCESS);
                        appendLog("←  226 OK. Download shared complete: " + dest.getName());
                        JOptionPane.showMessageDialog(MainFrame.this, "Tải tệp thành công lưu tại:\\n" + dest.getAbsolutePath());
                    } else {
                        showError("Tải tệp qua Token thất bại!");
                    }
                } catch (Exception ex) {
                    showError("Lỗi tải qua Token: " + ex.getMessage());
                }
                setButtons(true);
            }
        }.execute();
    }"""

new_get_shared = """    private void doGetShared() {
        doGetShared(null, null);
    }

    private void doGetShared(String defaultToken, String defaultFilename) {
        String token = defaultToken;
        if (token == null || token.trim().isEmpty()) {
            token = JOptionPane.showInputDialog(this,
                    "Nhập mã Token chia sẻ (hoặc mã liên kết):",
                    "Tải Tệp Qua Mã Chia Sẻ (Token)", JOptionPane.QUESTION_MESSAGE);
            if (token == null || token.trim().isEmpty()) return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn nơi lưu tệp tải về");
        if (defaultFilename != null && !defaultFilename.trim().isEmpty()) {
            chooser.setSelectedFile(new File(defaultFilename.trim()));
        }
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File dest = chooser.getSelectedFile();

        setButtons(false);
        setStatus("🎟  Đang tải tệp qua Token: " + dest.getName() + " ...", CLR_ACCENT);
        progressBar.setValue(0);

        final String finalToken = token.trim();
        new SwingWorker<Boolean, Integer>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return ftpService.downloadShared(finalToken, dest, (transferred, total) -> {
                    if (total > 0) {
                        int pct = (int) (transferred * 100 / total);
                        publish(pct);
                    }
                });
            }

            @Override
            protected void process(List<Integer> chunks) {
                int last = chunks.get(chunks.size() - 1);
                progressBar.setValue(last);
                progressBar.setString("Đang tải: " + last + "%");
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        progressBar.setValue(100);
                        progressBar.setString("Tải xong: 100%");
                        setStatus("✅  Tải qua Token thành công: " + dest.getName(), CLR_SUCCESS);
                        appendLog("←  226 OK. Download shared complete: " + dest.getName());
                        JOptionPane.showMessageDialog(MainFrame.this, "Tải tệp thành công lưu tại:\\n" + dest.getAbsolutePath());
                    } else {
                        showError("Tải tệp qua Token thất bại!");
                    }
                } catch (Exception ex) {
                    showError("Lỗi tải qua Token: " + ex.getMessage());
                }
                setButtons(true);
            }
        }.execute();
    }"""

if old_get_shared in content:
    content = content.replace(old_get_shared, new_get_shared)
    print("[OK] Replaced doGetShared with overloaded version")
else:
    print("[WARN] old_get_shared not found")

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] MainFrame.java patched successfully!")


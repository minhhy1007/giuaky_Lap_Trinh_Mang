# -*- coding: utf-8 -*-
with open(r'src/client/MainFrame.java', 'r', encoding='utf-8') as f:
    c = f.read()

# 1. Add btnChangePass field and reference
old_fields = "    private JButton      btnLogout;\n    private JButton      btnDisconnect;"
new_fields = "    private JButton      btnChangePass;\n    private JButton      btnLogout;\n    private JButton      btnDisconnect;"
c = c.replace(old_fields, new_fields)

# 2. Add btnChangePass to Header right panel
old_header_btns = """        btnLogout = createHeaderButton("Đăng xuất", CLR_WARN);
        btnDisconnect = createHeaderButton("Ngắt kết nối", CLR_DANGER);

        btnLogout.addActionListener(e -> doLogout());
        btnDisconnect.addActionListener(e -> doDisconnect());

        right.add(lblQuota);
        right.add(createHeaderSep());
        right.add(btnLogout);
        right.add(btnDisconnect);"""

new_header_btns = """        btnChangePass = createHeaderButton("Đổi mật khẩu", CLR_TEXT);
        btnLogout = createHeaderButton("Đăng xuất", CLR_WARN);
        btnDisconnect = createHeaderButton("Ngắt kết nối", CLR_DANGER);

        btnChangePass.addActionListener(e -> doChangePassword());
        btnLogout.addActionListener(e -> doLogout());
        btnDisconnect.addActionListener(e -> doDisconnect());

        right.add(lblQuota);
        right.add(createHeaderSep());
        right.add(btnChangePass);
        right.add(btnLogout);
        right.add(btnDisconnect);"""
c = c.replace(old_header_btns, new_header_btns)

# 3. Enhance Table Sorter with Numeric Size Comparator
old_sorter = """        // Thiết lập bộ sắp xếp (Sorter)
        tableSorter = new TableRowSorter<>(tableModel);
        fileTable.setRowSorter(tableSorter);"""

new_sorter = """        // Thiết lập bộ sắp xếp (Sorter) với so sánh dung lượng thực tế
        tableSorter = new TableRowSorter<>(tableModel);
        tableSorter.setComparator(2, (o1, o2) -> {
            long b1 = parseSizeToBytes(String.valueOf(o1));
            long b2 = parseSizeToBytes(String.valueOf(o2));
            return Long.compare(b1, b2);
        });
        fileTable.setRowSorter(tableSorter);"""
c = c.replace(old_sorter, new_sorter)

# 4. Enhance Double Click on File Table to download files
old_double_click = """                        if ("Thư mục".equals(type)) {
                            doCwd(name);
                        }"""

new_double_click = """                        if ("Thư mục".equals(type)) {
                            doCwd(name);
                        } else {
                            doDownload();
                        }"""
c = c.replace(old_double_click, new_double_click)

# 5. Fix Regex Pattern.quote in filterFiles
old_filter = """    private void filterFiles() {
        String query = tfSearch.getText().trim();
        if (query.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 0));
        }
    }"""

new_filter = """    private void filterFiles() {
        String query = tfSearch.getText().trim();
        if (query.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 0));
        }
    }"""
c = c.replace(old_filter, new_filter)

# 6. Fix Regex Pattern.quote in showAuditLogsDialog
old_audit_filter = """            if ("Tất cả".equals(selectedAct) && query.isEmpty()) {
                sorter.setRowFilter(null);
            } else if (!"Tất cả".equals(selectedAct) && query.isEmpty()) {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + selectedAct, 2));
            } else if ("Tất cả".equals(selectedAct) && !query.isEmpty()) {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
            } else {
                sorter.setRowFilter(RowFilter.andFilter(List.of(
                        RowFilter.regexFilter("(?i)" + selectedAct, 2),
                        RowFilter.regexFilter("(?i)" + query)
                )));
            }"""

new_audit_filter = """            String safeQuery = java.util.regex.Pattern.quote(query);
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
            }"""
c = c.replace(old_audit_filter, new_audit_filter)

# 7. Add helper method parseSizeToBytes and doChangePassword before end of class
methods_to_add = """
    private long parseSizeToBytes(String sizeStr) {
        if (sizeStr == null || sizeStr.equals("-") || sizeStr.trim().isEmpty()) return -1L;
        try {
            String[] parts = sizeStr.trim().split("\\\\s+");
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

        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dlg.dispose());

        JButton btnSubmit = new JButton("Xác nhận đổi");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSubmit.setBackground(CLR_PRIMARY);
        btnSubmit.setForeground(Color.WHITE);

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
"""

last_brace_idx = c.rfind('}')
if last_brace_idx != -1:
    c = c[:last_brace_idx] + methods_to_add + "\n}\n"

with open(r'src/client/MainFrame.java', 'w', encoding='utf-8') as f:
    f.write(c)

print("[DONE] MainFrame.java enhanced with robust regex, numeric size sorting, double-click download, and Change Password feature!")


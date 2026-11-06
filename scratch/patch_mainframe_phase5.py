# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add field
old_fields = "    private JLabel          lblQuota;\n    private JButton         btnShare;"
new_fields = "    private JLabel          lblQuota;\n    private JButton         btnPlans;\n    private JButton         btnShare;"
content = content.replace(old_fields, new_fields)

# 2. Add btnPlans to row2
old_row2 = """        btnShare        = funcBtn("🔗  Chia sẻ tệp",            new Color(40, 180, 130), 130);
        btnSharedWithMe = funcBtn("📥  Được chia sẻ cho tôi",   new Color(30, 144, 255), 180);
        btnMyShares     = funcBtn("📋  Quản lý đã chia sẻ",     new Color(180, 100, 220), 160);
        btnGetShared    = funcBtn("🎟  Tải qua Token",         new Color(240, 140, 30), 140);
        btnLogs         = funcBtn("📜  Nhật ký hoạt động",      new Color(100, 120, 150), 155);

        row2.add(btnShare);
        row2.add(btnSharedWithMe);
        row2.add(btnMyShares);
        row2.add(btnGetShared);
        row2.add(btnLogs);"""

new_row2 = """        btnPlans        = funcBtn("💎  Gói cước",              new Color(230, 140, 20), 125);
        btnShare        = funcBtn("🔗  Chia sẻ tệp",            new Color(40, 180, 130), 120);
        btnSharedWithMe = funcBtn("📥  Được chia sẻ cho tôi",   new Color(30, 144, 255), 175);
        btnMyShares     = funcBtn("📋  Quản lý đã chia sẻ",     new Color(180, 100, 220), 155);
        btnGetShared    = funcBtn("🎟  Tải qua Token",         new Color(240, 140, 30), 135);
        btnLogs         = funcBtn("📜  Nhật ký hoạt động",      new Color(100, 120, 150), 150);

        row2.add(btnPlans);
        row2.add(btnShare);
        row2.add(btnSharedWithMe);
        row2.add(btnMyShares);
        row2.add(btnGetShared);
        row2.add(btnLogs);"""
content = content.replace(old_row2, new_row2)

# 3. Add listener
old_listeners = "        btnShare.addActionListener(e     -> doShare());"
new_listeners = "        btnPlans.addActionListener(e     -> doPlansAndSubscriptions());\n        btnShare.addActionListener(e     -> doShare());"
content = content.replace(old_listeners, new_listeners)

# 4. Add setButtons
old_set_btn = "            if (btnShare != null) btnShare.setEnabled(enabled);"
new_set_btn = "            if (btnPlans != null) btnPlans.setEnabled(enabled);\n            if (btnShare != null) btnShare.setEnabled(enabled);"
content = content.replace(old_set_btn, new_set_btn)

# 5. Add doPlansAndSubscriptions method before the last brace
new_method = """
    // ================================================================
    // PHASE 5: GIAO DIỆN GÓI CƯỚC, ĐƠN HÀNG VÀ THANH TOÁN MÔ PHỎNG
    // ================================================================

    private void doPlansAndSubscriptions() {
        setButtons(false);
        setStatus("💎  Đang nạp thông tin gói cước và dịch vụ...", CLR_ACCENT);

        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                String myPlan = ftpService.getMyPlan();
                return new String[]{myPlan};
            }

            @Override
            protected void done() {
                setButtons(true);
                setStatus("🟢  Sẵn sàng", CLR_SUCCESS);
                try {
                    String[] res = get();
                    showPlansDialog(res[0]);
                } catch (Exception ex) {
                    showError("Lỗi tải thông tin gói cước: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showPlansDialog(String myPlanInfo) {
        JDialog dlg = new JDialog(this, "💎 Quản Lý Gói Cước & Đăng Ký Dịch Vụ Lưu Trữ", true);
        dlg.setSize(920, 600);
        dlg.setLocationRelativeTo(this);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // ── TAB 1: GÓI CƯỚC & NÂNG CẤP ────────────────────────────────
        JPanel tab1 = new JPanel(new BorderLayout(10, 10));
        tab1.setBackground(CLR_BG);
        tab1.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Panel Gói hiện tại
        JPanel currentPanel = new JPanel(new BorderLayout(5, 5));
        currentPanel.setBackground(CLR_PANEL);
        currentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90)),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel curTitle = new JLabel("📌 GÓI CƯỚC HIỆN TẠI CỦA BẠN:");
        curTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        curTitle.setForeground(new Color(240, 180, 50));

        JLabel curInfo = new JLabel("<html>" + (myPlanInfo != null ? myPlanInfo.replace("200 ", "").replace("|", "<br>• ") : "Đang cập nhật...") + "</html>");
        curInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        curInfo.setForeground(CLR_TEXT);

        currentPanel.add(curTitle, BorderLayout.NORTH);
        currentPanel.add(curInfo, BorderLayout.CENTER);
        tab1.add(currentPanel, BorderLayout.NORTH);

        // Panel các gói nâng cấp (3 Cards)
        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        cardsPanel.setBackground(CLR_BG);

        // Card FREE
        cardsPanel.add(createPlanCard(dlg, tabs, 1, "FREE", "100 MB", "Miễn phí", "365 ngày",
                "Gói tiêu chuẩn mặc định cho mọi tài khoản sinh viên. Hạn mức 100MB.", new Color(50, 150, 200)));

        // Card PRO
        cardsPanel.add(createPlanCard(dlg, tabs, 2, "PRO", "1 GB", "50,000 VND", "30 ngày",
                "Dung lượng 1GB tốc độ cao, phù hợp lưu trữ bài tập lớn, mã nguồn dự án.", new Color(40, 180, 120)));

        // Card VIP
        cardsPanel.add(createPlanCard(dlg, tabs, 3, "VIP", "10 GB", "200,000 VND", "30 ngày",
                "Hạn mức 10GB lưu trữ máy ảo, tệp ISO, đồ án thực hành dung lượng lớn.", new Color(180, 100, 240)));

        tab1.add(cardsPanel, BorderLayout.CENTER);
        tabs.addTab("💎 Gói cước & Nâng cấp", tab1);

        // ── TAB 2: THANH TOÁN MÔ PHỎNG (MOCK PAYMENT) ──────────────────
        JPanel tab2 = new JPanel(new BorderLayout(10, 10));
        tab2.setBackground(CLR_BG);
        tab2.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel formPanel = new JPanel(new GridLayout(6, 1, 8, 8));
        formPanel.setBackground(CLR_BG);

        JLabel lblPayTitle = new JLabel("💳 CỔNG THANH TOÁN MÔ PHỎNG (MOCK PAYMENT GATEWAY)");
        lblPayTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPayTitle.setForeground(new Color(240, 180, 50));

        JLabel lblOrderInput = new JLabel("Nhập Order ID hoặc Mã đơn hàng (ví dụ: ORD-...):");
        lblOrderInput.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblOrderInput.setForeground(CLR_TEXT);

        JTextField txtOrderCode = new JTextField();
        txtOrderCode.setFont(new Font("Consolas", Font.BOLD, 14));
        txtOrderCode.setBackground(CLR_PANEL);
        txtOrderCode.setForeground(Color.WHITE);
        txtOrderCode.setCaretColor(Color.WHITE);

        JLabel lblMethod = new JLabel("Phương thức thanh toán: 🏦 Mock Internet Banking / QR Transfer");
        lblMethod.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMethod.setForeground(CLR_MUTED);

        JPanel payBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        payBtnPanel.setBackground(CLR_BG);

        JButton btnMockSuccess = new JButton("✅  Mô phỏng Thanh toán Thành công (Approved)");
        btnMockSuccess.setBackground(new Color(40, 180, 130));
        btnMockSuccess.setForeground(Color.WHITE);
        btnMockSuccess.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnMockSuccess.setPreferredSize(new Dimension(320, 38));

        JButton btnMockFail = new JButton("❌  Giả lập Giao dịch Thất bại (Decline)");
        btnMockFail.setBackground(new Color(220, 60, 60));
        btnMockFail.setForeground(Color.WHITE);
        btnMockFail.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnMockFail.setPreferredSize(new Dimension(280, 38));

        payBtnPanel.add(btnMockSuccess);
        payBtnPanel.add(btnMockFail);

        formPanel.add(lblPayTitle);
        formPanel.add(lblOrderInput);
        formPanel.add(txtOrderCode);
        formPanel.add(lblMethod);
        formPanel.add(new JSeparator());
        formPanel.add(payBtnPanel);

        tab2.add(formPanel, BorderLayout.NORTH);

        JTextArea payLogArea = new JTextArea("Hướng dẫn:\\n1. Chọn gói cước ở Tab 1 để tạo đơn hàng.\\n2. Mã đơn hàng sẽ được tự động điền vào ô trên.\\n3. Bấm nút màu xanh để mô phỏng thanh toán thành công và kích hoạt ngay Quota mới!\\n4. Bấm nút màu đỏ để kiểm tra kịch bản ngân hàng từ chối giao dịch.\\n");
        payLogArea.setEditable(false);
        payLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        payLogArea.setBackground(new Color(20, 24, 32));
        payLogArea.setForeground(new Color(160, 210, 160));
        payLogArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        tab2.add(new JScrollPane(payLogArea), BorderLayout.CENTER);

        tabs.addTab("💳 Thanh toán mô phỏng", tab2);

        // ── TAB 3: LỊCH SỬ ĐƠN HÀNG ────────────────────────────────────
        JPanel tab3 = new JPanel(new BorderLayout(8, 8));
        tab3.setBackground(CLR_BG);
        tab3.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] orderCols = {"Order ID", "Mã giao dịch", "Gói cước", "Số tiền", "Trạng thái", "Ngày tạo"};
        DefaultTableModel orderModel = new DefaultTableModel(orderCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable orderTable = new JTable(orderModel);
        orderTable.setBackground(CLR_TABLE_R);
        orderTable.setForeground(CLR_TEXT);
        orderTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        orderTable.setRowHeight(26);
        orderTable.setSelectionBackground(CLR_TABLE_S);
        orderTable.setSelectionForeground(Color.WHITE);
        orderTable.getTableHeader().setBackground(CLR_TABLE_H);
        orderTable.getTableHeader().setForeground(CLR_MUTED);

        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.getViewport().setBackground(CLR_TABLE_R);
        tab3.add(orderScroll, BorderLayout.CENTER);

        JPanel orderActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        orderActionPanel.setBackground(CLR_BG);

        JButton btnRefreshOrders = new JButton("🔄  Làm mới");
        JButton btnPaySelected = new JButton("💳  Thanh toán đơn đã chọn");
        btnPaySelected.setBackground(new Color(40, 180, 130));
        btnPaySelected.setForeground(Color.WHITE);
        JButton btnCancelOrder = new JButton("❌  Hủy đơn đang chờ (Cancel)");
        btnCancelOrder.setBackground(CLR_ERROR);
        btnCancelOrder.setForeground(Color.WHITE);

        orderActionPanel.add(btnRefreshOrders);
        orderActionPanel.add(btnPaySelected);
        orderActionPanel.add(btnCancelOrder);
        tab3.add(orderActionPanel, BorderLayout.SOUTH);

        tabs.addTab("📋 Lịch sử đơn hàng", tab3);

        // ── SỰ KIỆN NÚT THANH TOÁN ────────────────────────────────────
        btnMockSuccess.addActionListener(e -> {
            String code = txtOrderCode.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập Order ID hoặc Mã đơn hàng!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String resp = ftpService.payOrder(code, true);
                payLogArea.append("\\n[THANH TOÁN THÀNH CÔNG] " + resp);
                if (resp.startsWith("200")) {
                    JOptionPane.showMessageDialog(dlg, resp, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    refreshQuotaLabel();
                    doRefresh();
                } else {
                    JOptionPane.showMessageDialog(dlg, resp, "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi thanh toán: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnMockFail.addActionListener(e -> {
            String code = txtOrderCode.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập Order ID hoặc Mã đơn hàng!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String resp = ftpService.payOrder(code, false);
                payLogArea.append("\\n[MÔ PHỎNG THẤT BẠI] " + resp);
                JOptionPane.showMessageDialog(dlg, resp, "Kết quả mô phỏng", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        Runnable loadOrdersAction = () -> {
            try {
                List<String> rawOrders = ftpService.getMyOrders();
                orderModel.setRowCount(0);
                for (String line : rawOrders) {
                    String l = line.trim();
                    if (l.matches("^\\\\d+\\\\s+.*")) {
                        String[] parts = l.split("\\\\s+");
                        if (parts.length >= 6) {
                            String oid = parts[0];
                            String code = parts[1];
                            String plan = parts[2];
                            String amt = parts[3] + " " + parts[4];
                            String st = parts[5];
                            String dt = parts.length > 6 ? parts[6] : "";
                            orderModel.addRow(new Object[]{oid, code, plan, amt, st, dt});
                        }
                    }
                }
            } catch (Exception ignored) {}
        };

        btnRefreshOrders.addActionListener(e -> loadOrdersAction.run());

        btnPaySelected.addActionListener(e -> {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn một đơn hàng trong bảng!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String code = (String) orderModel.getValueAt(row, 1);
            txtOrderCode.setText(code);
            tabs.setSelectedIndex(1); // Chuyển sang Tab Thanh toán
        });

        btnCancelOrder.addActionListener(e -> {
            int row = orderTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chọn một đơn hàng để hủy!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String oid = (String) orderModel.getValueAt(row, 0);
            String st = (String) orderModel.getValueAt(row, 4);
            if (!"PENDING".equalsIgnoreCase(st)) {
                JOptionPane.showMessageDialog(dlg, "Chỉ có thể hủy đơn hàng ở trạng thái PENDING!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int cf = JOptionPane.showConfirmDialog(dlg, "Bạn có chắc chắn muốn hủy đơn hàng ID " + oid + "?", "Xác nhận hủy", JOptionPane.YES_NO_OPTION);
            if (cf == JOptionPane.YES_OPTION) {
                try {
                    String resp = ftpService.cancelOrder(oid);
                    JOptionPane.showMessageDialog(dlg, resp, "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                    loadOrdersAction.run();
                } catch (Exception ex) {
                    showError("Lỗi hủy đơn: " + ex.getMessage());
                }
            }
        });

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2) loadOrdersAction.run();
        });

        dlg.setContentPane(tabs);
        dlg.setVisible(true);
    }

    private JPanel createPlanCard(JDialog parentDlg, JTabbedPane tabs, int planId, String name, String quota,
                                  String price, String duration, String desc, Color headerColor) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(CLR_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 90), 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 2, 2));
        header.setBackground(CLR_PANEL);
        JLabel lblName = new JLabel(name, SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblName.setForeground(headerColor);

        JLabel lblPrice = new JLabel(price + " / " + duration, SwingConstants.CENTER);
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrice.setForeground(Color.WHITE);
        header.add(lblName);
        header.add(lblPrice);
        card.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new GridLayout(3, 1, 4, 4));
        body.setBackground(CLR_PANEL);
        JLabel lblQuota = new JLabel("💾 Hạn mức: " + quota, SwingConstants.CENTER);
        lblQuota.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblQuota.setForeground(CLR_SUCCESS);

        JLabel lblDur = new JLabel("⏳ Thời hạn: " + duration, SwingConstants.CENTER);
        lblDur.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDur.setForeground(CLR_TEXT);

        JLabel lblDesc = new JLabel("<html><center style='color:#a0aec0;font-size:10px;'>" + desc + "</center></html>", SwingConstants.CENTER);
        body.add(lblQuota);
        body.add(lblDur);
        body.add(lblDesc);
        card.add(body, BorderLayout.CENTER);

        // Button Đăng ký
        JButton btnRegister = new JButton(planId == 1 ? "Gói Mặc Định" : "🛒 Đăng ký Gói này");
        btnRegister.setBackground(headerColor.darker());
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRegister.setFocusPainted(false);

        if (planId == 1) {
            btnRegister.setEnabled(false);
        } else {
            btnRegister.addActionListener(e -> {
                int cf = JOptionPane.showConfirmDialog(parentDlg,
                        "Bạn có muốn tạo đơn hàng đăng ký gói " + name + " (" + price + ")?",
                        "Xác nhận tạo đơn hàng", JOptionPane.YES_NO_OPTION);
                if (cf == JOptionPane.YES_OPTION) {
                    try {
                        String resp = ftpService.createOrder(planId);
                        if (resp.startsWith("250")) {
                            JOptionPane.showMessageDialog(parentDlg, resp, "Đơn hàng đã tạo", JOptionPane.INFORMATION_MESSAGE);
                            // Tìm orderId hoặc transaction_code trong resp
                            String code = "";
                            if (resp.contains("Order created: ")) {
                                String sub = resp.substring(resp.indexOf("Order created: ") + 15);
                                code = sub.split("\\\\|")[0].trim();
                            }
                            // Điền vào ô thanh toán ở tab 2
                            JPanel tab2 = (JPanel) tabs.getComponentAt(1);
                            JPanel form = (JPanel) tab2.getComponent(0);
                            JTextField txt = (JTextField) form.getComponent(2);
                            txt.setText(code);
                            tabs.setSelectedIndex(1); // Mở Tab Thanh toán ngay lập tức
                        } else {
                            showError("Không thể tạo đơn: " + resp);
                        }
                    } catch (Exception ex) {
                        showError("Lỗi kết nối: " + ex.getMessage());
                    }
                }
            });
        }
        card.add(btnRegister, BorderLayout.SOUTH);

        return card;
    }
"""

last_brace = content.rfind("}")
if last_brace != -1:
    content = content[:last_brace] + new_method

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] MainFrame.java patched for Phase 5!")


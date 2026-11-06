# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add btnAdminDashboard field
old_field = "    private JButton         btnPlans;"
new_field = "    private JButton         btnPlans;\n    private JButton         btnAdminDashboard;"
content = content.replace(old_field, new_field)

# 2. Update Top Bar Role badge
old_lblUser = 'lblUser   = infoLabel("👤  " + ftpService.getLoggedUser());'
new_lblUser = 'lblUser   = infoLabel("👤  " + ftpService.getLoggedUser() + (ftpService.isAdmin() ? "  [👑 ADMIN]" : "  [USER]"));'
content = content.replace(old_lblUser, new_lblUser)

# 3. Add btnAdminDashboard to Row 2
old_row2_add = """        row2.add(btnPlans);
        row2.add(btnShare);
        row2.add(btnSharedWithMe);
        row2.add(btnMyShares);
        row2.add(btnGetShared);
        row2.add(btnLogs);"""

new_row2_add = """        row2.add(btnPlans);
        row2.add(btnShare);
        row2.add(btnSharedWithMe);
        row2.add(btnMyShares);
        row2.add(btnGetShared);
        row2.add(btnLogs);

        if (ftpService.isAdmin()) {
            btnAdminDashboard = funcBtn("🛡️  Admin Dashboard", new Color(220, 50, 80), 160);
            row2.add(btnAdminDashboard);
            btnAdminDashboard.addActionListener(e -> new AdminDashboardDialog(MainFrame.this, ftpService).setVisible(true));
        }"""
content = content.replace(old_row2_add, new_row2_add)

# 4. Update setButtons
old_set_btn = "            if (btnPlans != null) btnPlans.setEnabled(enabled);"
new_set_btn = """            if (btnPlans != null) btnPlans.setEnabled(enabled);
            if (btnAdminDashboard != null) btnAdminDashboard.setEnabled(enabled);"""
content = content.replace(old_set_btn, new_set_btn)

# 5. Enhance doDelete to support folders (RMD) as well
old_do_delete = """        String filename = (String) tableModel.getValueAt(row, 1);
        String type     = (String) tableModel.getValueAt(row, 2);
        if ("Thư mục".equals(type)) { showWarn("Không thể xóa thư mục."); return; }

        // Confirm dialog
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️  Bạn có chắc muốn xóa file:\\n\\n\\"" + filename + "\\"\\n\\nHành động này không thể hoàn tác!",
                "Xác nhận xóa file", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        setButtons(false);
        setStatus("🗑  Đang xóa: " + filename, CLR_WARN);
        appendLog("→  DELE " + filename);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ftpService.dele(filename);
            }"""

new_do_delete = """        String filename = (String) tableModel.getValueAt(row, 1);
        String type     = (String) tableModel.getValueAt(row, 2);
        boolean isFolder = "Thư mục".equals(type);

        // Confirm dialog với cảnh báo nguy hiểm rõ ràng
        String promptMsg = isFolder
                ? "⚠️  Bạn có chắc muốn xóa thư mục:\\n\\n\\"" + filename + "\\"\\n\\nLưu ý: Thư mục phải rỗng mới có thể xóa!"
                : "⚠️  Bạn có chắc muốn xóa tệp tin:\\n\\n\\"" + filename + "\\"\\n\\nHành động này không thể hoàn tác!";

        int confirm = JOptionPane.showConfirmDialog(this, promptMsg,
                isFolder ? "Xác nhận xóa thư mục (RMD)" : "Xác nhận xóa tệp tin (DELE)",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        setButtons(false);
        setStatus("🗑  Đang xóa: " + filename, CLR_WARN);
        appendLog("→  " + (isFolder ? "RMD " : "DELE ") + filename);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return isFolder ? ftpService.rmd(filename) : ftpService.dele(filename);
            }"""
content = content.replace(old_do_delete, new_do_delete)

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] MainFrame.java updated for Phase 6 Admin Dashboard & UX!")


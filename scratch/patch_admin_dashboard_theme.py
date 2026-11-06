# -*- coding: utf-8 -*-
with open(r'src/client/AdminDashboardDialog.java', 'r', encoding='utf-8') as f:
    c = f.read()

# Replace Colors
c = c.replace('private static final Color CLR_BG       = new Color(22, 26, 36);',
              'private static final Color CLR_BG       = new Color(0x11, 0x18, 0x27);')

c = c.replace('private static final Color CLR_PANEL    = new Color(30, 36, 50);',
              'private static final Color CLR_PANEL    = new Color(0x1F, 0x29, 0x37);')

c = c.replace('private static final Color CLR_CARD     = new Color(38, 46, 64);',
              'private static final Color CLR_CARD     = new Color(0x1F, 0x29, 0x37);')

c = c.replace('private static final Color CLR_TABLE_H  = new Color(42, 52, 72);',
              'private static final Color CLR_TABLE_H  = new Color(0x1F, 0x29, 0x37);')

c = c.replace('private static final Color CLR_TABLE_R  = new Color(28, 33, 46);',
              'private static final Color CLR_TABLE_R  = new Color(0x27, 0x34, 0x49);')

c = c.replace('private static final Color CLR_TABLE_A  = new Color(24, 28, 40);',
              'private static final Color CLR_TABLE_A  = new Color(0x1F, 0x2A, 0x3C);')

c = c.replace('private static final Color CLR_TABLE_S  = new Color(40, 90, 160);',
              'private static final Color CLR_TABLE_S  = new Color(0x3B, 0x82, 0xF6);')

c = c.replace('private static final Color CLR_ACCENT   = new Color(99, 179, 237);',
              'private static final Color CLR_ACCENT   = new Color(0x3B, 0x82, 0xF6);')

c = c.replace('private static final Color CLR_SUCCESS  = new Color(72, 199, 116);',
              'private static final Color CLR_SUCCESS  = new Color(0x22, 0xC5, 0x5E);')

c = c.replace('private static final Color CLR_WARN     = new Color(245, 175, 45);',
              'private static final Color CLR_WARN     = new Color(0xF5, 0x9E, 0x0B);')

c = c.replace('private static final Color CLR_ERROR    = new Color(250, 90, 90);',
              'private static final Color CLR_ERROR    = new Color(0xEF, 0x44, 0x44);')

c = c.replace('private static final Color CLR_TEXT     = new Color(226, 232, 240);',
              'private static final Color CLR_TEXT     = new Color(0xF9, 0xFA, 0xFB);')

c = c.replace('private static final Color CLR_MUTED    = new Color(140, 150, 170);',
              'private static final Color CLR_MUTED    = new Color(0x9C, 0xA3, 0xAF);')

# Replace emojis
c = c.replace('🛡️ BẢNG ĐIỀU KHIỂN QUẢN TRỊ VIÊN — ADMIN DASHBOARD', 'BẢNG ĐIỀU KHIỂN QUẢN TRỊ VIÊN (ADMIN DASHBOARD)')
c = c.replace('🛡️  HỆ THỐNG QUẢN TRỊ MÁY CHỦ (ADMIN DASHBOARD)', 'HỆ THỐNG QUẢN TRỊ MÁY CHỦ (ADMIN DASHBOARD)')
c = c.replace('tabs.addTab("📊 Tổng quan & KPI", buildTabOverview());', 'tabs.addTab("Tổng quan & KPI", buildTabOverview());')
c = c.replace('tabs.addTab("👥 Quản lý Người dùng & Quyền hạn", buildTabUsers());', 'tabs.addTab("Quản lý Người dùng & Quyền hạn", buildTabUsers());')
c = c.replace('tabs.addTab("💰 Đơn hàng Toàn hệ thống", buildTabOrders());', 'tabs.addTab("Đơn hàng Toàn hệ thống", buildTabOrders());')
c = c.replace('tabs.addTab("📜 Nhật ký Kiểm toán (Audit Logs)", buildTabLogs());', 'tabs.addTab("Nhật ký Kiểm toán (Audit Logs)", buildTabLogs());')

with open(r'src/client/AdminDashboardDialog.java', 'w', encoding='utf-8') as f:
    f.write(c)

print("[DONE] AdminDashboardDialog.java modernized with unified theme and clean text!")


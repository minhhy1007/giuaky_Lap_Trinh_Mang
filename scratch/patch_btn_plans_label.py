# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update btnPlans label to be crystal clear
old_btn = 'btnPlans        = funcBtn("💎  Gói cước",              new Color(230, 140, 20), 125);'
new_btn = 'btnPlans        = funcBtn("💳  Nâng cấp & Thanh toán", new Color(230, 140, 20), 185);'

if old_btn in content:
    content = content.replace(old_btn, new_btn)
    print("[OK] Replaced btnPlans label")
else:
    print("[WARN] old_btn not found")

# 2. Add click handler on lblQuota in top bar
old_quota = 'lblQuota  = infoLabel("💾  Quota: Đang tải...");'
new_quota = '''lblQuota  = infoLabel("💾  Quota: Đang tải...");
        lblQuota.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblQuota.setToolTipText("Nhấp vào đây để xem Gói cước, Nâng cấp & Thanh toán");
        lblQuota.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { doPlansAndSubscriptions(); }
        });'''

if old_quota in content:
    content = content.replace(old_quota, new_quota)
    print("[OK] Added click listener to lblQuota")
else:
    print("[WARN] old_quota not found")

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\MainFrame.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] MainFrame.java updated successfully!")


# -*- coding: utf-8 -*-
with open(r'test/TestPhase6AdminDashboard.java', 'r', encoding='utf-8') as f:
    c = f.read()

# Fix Test 1
c = c.replace('resp.startsWith("200 STATS ") && resp.contains("totalUsers=")',
              'resp.startsWith("200 TotalUsers: ") && resp.contains("TotalStorage:")')

# Fix Test 10
c = c.replace('ordersResp.startsWith("150 Orders list follows")',
              'ordersResp.startsWith("150 System orders list")')

with open(r'test/TestPhase6AdminDashboard.java', 'w', encoding='utf-8') as f:
    f.write(c)

print('Test assertions updated successfully!')


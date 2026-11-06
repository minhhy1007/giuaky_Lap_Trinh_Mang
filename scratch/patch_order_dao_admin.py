# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\dao\OrderDAO.java', 'r', encoding='utf-8') as f:
    content = f.read()

new_method = """
    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.order_id, o.user_id, o.plan_id, p.plan_name, o.amount, o.payment_method, " +
                     "o.status, o.transaction_code, o.created_at " +
                     "FROM orders o JOIN plans p ON o.plan_id = p.plan_id ORDER BY o.order_id DESC LIMIT 100";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) {
            System.err.println("[OrderDAO] Error getAllOrders: " + e.getMessage());
        }
        return list;
    }
"""

last_brace = content.rfind("}")
if last_brace != -1:
    content = content[:last_brace] + new_method + "\n}\n"

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\dao\OrderDAO.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] Added getAllOrders to OrderDAO.java")


# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\CommandHandler.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update switch cases for Admin commands
old_cases = """            // ── Commands DÀNH RIÊNG CHO QUẢN TRỊ VIÊN (ADMIN ONLY) ────────
            case "USERLIST":
                return handleUserList();"""

new_cases = """            // ── Commands DÀNH RIÊNG CHO QUẢN TRỊ VIÊN (ADMIN ONLY) ────────
            case "USERLIST":
                return handleUserList();

            case "SETUSERSTATUS":
                return handleSetUserStatus(argument);

            case "SETUSERROLE":
                return handleSetUserRole(argument);

            case "SETUSERQUOTA":
                return handleSetUserQuota(argument);

            case "ADMINSTATS":
                return handleAdminStats();

            case "ADMINALLORDERS":
                return handleAdminAllOrders();"""

content = content.replace(old_cases, new_cases)

# 2. Update handleUserList to query userDAO directly
old_userlist = """    private String handleUserList() {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }

        List<User> users = userDatabase.getAllUsers();
        StringBuilder sb = new StringBuilder();
        sb.append("150 User list (").append(users.size()).append(" users):\\n");
        sb.append(String.format("  %-5s %-15s %-15s %-12s %-10s%n", "ID", "USERNAME", "ROLE", "QUOTA(MB)", "ACTIVE"));
        sb.append("  ------------------------------------------------------------\\n");

        for (User u : users) {
            sb.append(String.format("  %-5d %-15s %-15s %-12d %-10s%n",
                    u.getUserId(), u.getUsername(), u.getRoleName(),
                    u.getQuotaBytes() / (1024 * 1024), u.isActive() ? "YES" : "NO"));
        }
        sb.append("226 User list completed.");
        return sb.toString();
    }"""

new_userlist = """    private String handleUserList() {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }

        List<User> users = userDAO.getAllUsers();
        StringBuilder sb = new StringBuilder();
        sb.append("150 User list (").append(users.size()).append(" users):\\n");
        sb.append(String.format("  %-5s %-16s %-15s %-12s %-12s %-10s%n", "ID", "USERNAME", "ROLE", "QUOTA(MB)", "USED(KB)", "ACTIVE"));
        sb.append("  -----------------------------------------------------------------------------\\n");

        for (User u : users) {
            sb.append(String.format("  %-5d %-16s %-15s %-12d %-12d %-10s%n",
                    u.getUserId(), u.getUsername(), u.getRoleName(),
                    u.getQuotaBytes() / (1024 * 1024), u.getUsedBytes() / 1024,
                    u.isActive() ? "YES" : "NO"));
        }
        sb.append("226 User list completed.");
        return sb.toString();
    }"""

content = content.replace(old_userlist, new_userlist)

# 3. Add handler methods before the last closing brace
new_admin_methods = """
    // =========================================================================
    // PHASE 6: ADMIN DASHBOARD COMMANDS (SERVER-SIDE RBAC VALIDATION)
    // =========================================================================

    private String handleSetUserStatus(String argument) {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: SETUSERSTATUS <username_or_id> <ACTIVE|LOCKED>";
        }
        String[] parts = argument.trim().split("\\\\s+");
        if (parts.length < 2) {
            return "501 Syntax error: SETUSERSTATUS <username_or_id> <ACTIVE|LOCKED>";
        }

        String target = parts[0];
        boolean active = "ACTIVE".equalsIgnoreCase(parts[1]) || "TRUE".equalsIgnoreCase(parts[1]);

        User user = null;
        try {
            int uid = Integer.parseInt(target);
            user = userDAO.findById(uid);
        } catch (NumberFormatException e) {
            user = userDAO.findByUsername(target);
        }

        if (user == null) {
            return "550 User not found: " + target;
        }

        // Không cho phép Admin tự khóa tài khoản của chính mình
        if (user.getUserId() == getUserId() && !active) {
            return "550 Cannot lock your own administrator account";
        }

        boolean ok = userDAO.updateUserActive(user.getUserId(), active);
        if (ok) {
            logDAO.log(getUserId(), clientIp, "ADMIN_SET_STATUS", "User: " + user.getUsername(), 0, "SUCCESS", "Set active = " + active);
            return "200 User " + user.getUsername() + " status updated to " + (active ? "ACTIVE" : "LOCKED");
        } else {
            return "550 Failed to update user status.";
        }
    }

    private String handleSetUserRole(String argument) {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: SETUSERROLE <username_or_id> <ROLE_ADMIN|ROLE_USER>";
        }
        String[] parts = argument.trim().split("\\\\s+");
        if (parts.length < 2) {
            return "501 Syntax error: SETUSERROLE <username_or_id> <ROLE_ADMIN|ROLE_USER>";
        }

        String target = parts[0];
        String roleStr = parts[1].toUpperCase();
        int roleId = roleStr.contains("ADMIN") ? 1 : 2;

        User user = null;
        try {
            int uid = Integer.parseInt(target);
            user = userDAO.findById(uid);
        } catch (NumberFormatException e) {
            user = userDAO.findByUsername(target);
        }

        if (user == null) {
            return "550 User not found: " + target;
        }

        if (user.getUserId() == getUserId() && roleId != 1) {
            return "550 Cannot demote your own administrator role";
        }

        boolean ok = userDAO.updateRole(user.getUserId(), roleId);
        if (ok) {
            logDAO.log(getUserId(), clientIp, "ADMIN_SET_ROLE", "User: " + user.getUsername(), 0, "SUCCESS", "Set role = " + (roleId == 1 ? "ROLE_ADMIN" : "ROLE_USER"));
            return "200 User " + user.getUsername() + " role updated to " + (roleId == 1 ? "ROLE_ADMIN" : "ROLE_USER");
        } else {
            return "550 Failed to update user role.";
        }
    }

    private String handleSetUserQuota(String argument) {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: SETUSERQUOTA <username_or_id> <quota_mb>";
        }
        String[] parts = argument.trim().split("\\\\s+");
        if (parts.length < 2) {
            return "501 Syntax error: SETUSERQUOTA <username_or_id> <quota_mb>";
        }

        String target = parts[0];
        long quotaMb;
        try {
            quotaMb = Long.parseLong(parts[1]);
            if (quotaMb <= 0) return "501 Quota must be greater than 0 MB";
        } catch (NumberFormatException e) {
            return "501 Invalid quota number: " + parts[1];
        }

        User user = null;
        try {
            int uid = Integer.parseInt(target);
            user = userDAO.findById(uid);
        } catch (NumberFormatException e) {
            user = userDAO.findByUsername(target);
        }

        if (user == null) {
            return "550 User not found: " + target;
        }

        long bytes = quotaMb * 1024L * 1024L;
        boolean ok = userDAO.updateUserQuota(user.getUserId(), bytes);
        if (ok) {
            logDAO.log(getUserId(), clientIp, "ADMIN_SET_QUOTA", "User: " + user.getUsername(), 0, "SUCCESS", "Set quota = " + quotaMb + " MB");
            return "200 User " + user.getUsername() + " quota updated to " + quotaMb + " MB (" + bytes + " bytes)";
        } else {
            return "550 Failed to update user quota.";
        }
    }

    private String handleAdminStats() {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }

        UserDAO.SystemStats stats = userDAO.getSystemStats();
        return String.format("200 TotalUsers: %d | Active: %d | Locked: %d | TotalStorage: %s | TotalUsed: %s | TotalFiles: %d | TotalOrders: %d | Revenue: %,.0f VND",
                stats.totalUsers, stats.activeUsers, stats.lockedUsers,
                formatBytes(stats.totalQuotaBytes), formatBytes(stats.totalUsedBytes),
                stats.totalFiles, stats.totalOrders, stats.totalRevenue);
    }

    private String handleAdminAllOrders() {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }

        List<Order> orders = orderDAO.getAllOrders();
        StringBuilder sb = new StringBuilder();
        sb.append("150 System orders list (").append(orders.size()).append(" orders):\\n");
        sb.append(String.format("  %-8s %-26s %-10s %-16s %-12s %-20s%n", "ORDER_ID", "CODE", "PLAN", "AMOUNT", "STATUS", "DATE"));
        sb.append("  ------------------------------------------------------------------------------------------------\\n");
        for (Order o : orders) {
            sb.append(String.format("  %-8d %-26s %-10s %-16s %-12s %-20s%n",
                    o.getOrderId(), o.getTransactionCode(), o.getPlanName(),
                    o.getFormattedAmount(), o.getStatus(), o.getCreatedAt().toString()));
        }
        sb.append("226 End of orders list.");
        return sb.toString();
    }
"""

last_brace = content.rfind("}")
if last_brace != -1:
    content = content[:last_brace] + new_admin_methods + "\n}\n"

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\CommandHandler.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] CommandHandler.java patched with Admin commands!")


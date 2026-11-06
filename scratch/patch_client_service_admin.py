# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\FtpClientService.java', 'r', encoding='utf-8') as f:
    content = f.read()

new_admin_methods = """
    // ================================================================
    // PHASE 6: ADMIN DASHBOARD COMMANDS
    // ================================================================

    public List<String> getUserList() throws IOException {
        writer.println("USERLIST");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500") || line.startsWith("530")) break;
        }
        return lines;
    }

    public String setUserStatus(String targetUser, boolean active) throws IOException {
        writer.println("SETUSERSTATUS " + targetUser + " " + (active ? "ACTIVE" : "LOCKED"));
        return readLine(socketIn);
    }

    public String setUserRole(String targetUser, String role) throws IOException {
        writer.println("SETUSERROLE " + targetUser + " " + role);
        return readLine(socketIn);
    }

    public String setUserQuota(String targetUser, long quotaMb) throws IOException {
        writer.println("SETUSERQUOTA " + targetUser + " " + quotaMb);
        return readLine(socketIn);
    }

    public String getAdminStats() throws IOException {
        writer.println("ADMINSTATS");
        return readLine(socketIn);
    }

    public List<String> getAdminAllOrders() throws IOException {
        writer.println("ADMINALLORDERS");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500") || line.startsWith("530")) break;
        }
        return lines;
    }
"""

last_brace = content.rfind("}")
if last_brace != -1:
    content = content[:last_brace] + new_admin_methods + "\n}\n"

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\FtpClientService.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] FtpClientService.java patched with Admin methods!")


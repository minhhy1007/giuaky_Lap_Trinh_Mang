# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\FtpClientService.java', 'r', encoding='utf-8') as f:
    content = f.read()

new_methods = """
    // ================================================================
    // PHASE 5: GÓI CƯỚC, ĐƠN HÀNG VÀ THANH TOÁN MÔ PHỎNG (MOCK PAYMENT)
    // ================================================================

    public List<String> getPlans() throws IOException {
        writer.println("PLANS");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500")) break;
        }
        return lines;
    }

    public String getMyPlan() throws IOException {
        writer.println("MYPLAN");
        return readLine(socketIn);
    }

    public String createOrder(int planId) throws IOException {
        writer.println("ORDER " + planId);
        return readLine(socketIn);
    }

    public String cancelOrder(String orderIdOrCode) throws IOException {
        writer.println("CANCELORDER " + orderIdOrCode);
        return readLine(socketIn);
    }

    public String payOrder(String orderIdOrCode, boolean simulateSuccess) throws IOException {
        writer.println("PAY " + orderIdOrCode + (simulateSuccess ? " SUCCESS" : " FAIL"));
        return readLine(socketIn);
    }

    public List<String> getMyOrders() throws IOException {
        writer.println("MYORDERS");
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = readLine(socketIn)) != null) {
            lines.add(line);
            if (line.startsWith("226") || line.startsWith("550") || line.startsWith("500")) break;
        }
        return lines;
    }
}
"""

last_brace = content.rfind("}")
if last_brace != -1:
    content = content[:last_brace] + new_methods

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\FtpClientService.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] FtpClientService.java patched for Phase 5!")


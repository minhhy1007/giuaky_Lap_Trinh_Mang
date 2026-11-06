# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\CommandHandler.java', 'r', encoding='utf-8') as f:
    text = f.read()

# Replace any multiline string literals in handlePlans and handleMyOrders
bad1 = 'append("150-Available service plans (").append(plans.size()).append("):\n");'
bad1_crlf = 'append("150-Available service plans (").append(plans.size()).append("):\r\n");'
good1 = 'append("150-Available service plans (").append(plans.size()).append("):\\n");'

bad2 = 'append("  -----------------------------------------------------------------------------------------\n");'
bad2_crlf = 'append("  -----------------------------------------------------------------------------------------\r\n");'
good2 = 'append("  -----------------------------------------------------------------------------------------\\n");'

bad3 = 'append("150-Your orders list (").append(orders.size()).append("):\n");'
bad3_crlf = 'append("150-Your orders list (").append(orders.size()).append("):\r\n");'
good3 = 'append("150-Your orders list (").append(orders.size()).append("):\\n");'

bad4 = 'append("  ------------------------------------------------------------------------------------------------\n");'
bad4_crlf = 'append("  ------------------------------------------------------------------------------------------------\r\n");'
good4 = 'append("  ------------------------------------------------------------------------------------------------\\n");'

for b, g in [(bad1, good1), (bad1_crlf, good1), (bad2, good2), (bad2_crlf, good2),
            (bad3, good3), (bad3_crlf, good3), (bad4, good4), (bad4_crlf, good4)]:
    text = text.replace(b, g)

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\server\CommandHandler.java', 'w', encoding='utf-8') as f:
    f.write(text)

print("[OK] Newlines fixed in CommandHandler.java")


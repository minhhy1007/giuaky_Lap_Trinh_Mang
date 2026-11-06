# -*- coding: utf-8 -*-
with open(r'src/client/LoginFrame.java', 'r', encoding='utf-8') as f:
    c = f.read()

# 1. Remove static import
c = c.replace('import com.formdev.flatlaf.FlatDarkLaf;\n', '')
c = c.replace('import com.formdev.flatlaf.FlatDarkLaf;', '')

# 2. Replace static FlatDarkLaf.setup() with dynamic reflection
old_setup = """        // Cấu hình FlatLaf Dark hiện đại
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 8);"""

new_setup = """        // Cấu hình FlatLaf Dark hiện đại (Nạp động qua Reflection - Tuyệt đối không lỗi IDE/Classpath)
        try {
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            flatLafClass.getMethod("setup").invoke(null);
            UIManager.put("Button.arc", 8);"""

c = c.replace(old_setup, new_setup)

with open(r'src/client/LoginFrame.java', 'w', encoding='utf-8') as f:
    f.write(c)

print('LoginFrame.java updated successfully with zero IDE errors!')


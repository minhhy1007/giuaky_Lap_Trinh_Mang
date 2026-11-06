# -*- coding: utf-8 -*-
with open(r'src/client/LoginFrame.java', 'r', encoding='utf-8') as f:
    c = f.read()

# 1. Update setSize
c = c.replace('setSize(460, 540);', 'setSize(485, 550);')
c = c.replace('this.setSize(460, 540);', 'this.setSize(485, 550);')

# 2. Update button font & margin in createButton
old_create_btn = """        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));"""

new_create_btn = """        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMargin(new Insets(2, 6, 2, 6));"""

c = c.replace(old_create_btn, new_create_btn)

with open(r'src/client/LoginFrame.java', 'w', encoding='utf-8') as f:
    f.write(c)

print('LoginFrame size and button style updated successfully!')


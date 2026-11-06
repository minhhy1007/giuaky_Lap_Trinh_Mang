# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\LoginFrame.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Enable btnLogin by default
content = content.replace("btnLogin.setEnabled(false); // Chua connect chua du?c login", "btnLogin.setEnabled(true);")
content = content.replace("btnLogin.setEnabled(false);", "btnLogin.setEnabled(true);")

# 2. Add Enter key listener on tfUsername to jump to pfPassword
old_listeners = """        // Enter trong pfPassword = nhấn Login
        pfPassword.addActionListener(e -> {
            if (btnLogin.isEnabled())
                doLogin();
        });"""

new_listeners = """        tfUsername.addActionListener(e -> pfPassword.requestFocusInWindow());

        // Enter trong pfPassword = nhấn Login
        pfPassword.addActionListener(e -> {
            if (btnLogin.isEnabled())
                doLogin();
        });"""

content = content.replace(old_listeners, new_listeners)

# 3. Update doLogin to auto-connect if not connected yet
old_do_login = """    private void doLogin() {
        if (!ftpService.isConnected()) {
            setStatus("⚠️  Vui lòng kết nối trước!", CLR_ERROR);
            return;
        }

        String user = tfUsername.getText().trim();
        String pass = new String(pfPassword.getPassword());

        setStatus("🔄  Đang đăng nhập...", CLR_MUTED);
        btnLogin.setEnabled(false);

        new Thread(() -> {
            try {
                String resp = ftpService.login(user, pass);
                SwingUtilities.invokeLater(() -> {
                    if (resp != null && resp.startsWith("230")) {
                        setStatus("✅  Đăng nhập thành công! Chào " + user, CLR_SUCCESS);
                        // Mở MainFrame sau 500ms để người dùng thấy thông báo
                        Timer t = new Timer(500, ev -> {
                            new MainFrame(ftpService);
                            LoginFrame.this.dispose();
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        setStatus("🔴  " + (resp != null ? resp : "Đăng nhập thất bại"), CLR_ERROR);
                        btnLogin.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("🔴  Lỗi: " + ex.getMessage(), CLR_ERROR);
                    btnLogin.setEnabled(true);
                });
            }
        }).start();
    }"""

new_do_login = """    private void doLogin() {
        String host = tfServer.getText().trim();
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException ex) {
            setStatus("❌  Cổng không hợp lệ!", CLR_ERROR);
            return;
        }

        String user = tfUsername.getText().trim();
        String pass = new String(pfPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("⚠️  Vui lòng nhập tên đăng nhập và mật khẩu!", CLR_ERROR);
            return;
        }

        setStatus("🔄  Đang kết nối & đăng nhập...", CLR_MUTED);
        btnLogin.setEnabled(false);
        btnConnect.setEnabled(false);

        new Thread(() -> {
            try {
                if (!ftpService.isConnected()) {
                    boolean ok = ftpService.connect(host, port);
                    if (!ok) {
                        SwingUtilities.invokeLater(() -> {
                            setStatus("🔴  Không thể kết nối đến Server " + host + ":" + port, CLR_ERROR);
                            btnLogin.setEnabled(true);
                            btnConnect.setEnabled(true);
                        });
                        return;
                    }
                }

                String resp = ftpService.login(user, pass);
                SwingUtilities.invokeLater(() -> {
                    if (resp != null && resp.startsWith("230")) {
                        setStatus("✅  Đăng nhập thành công! Chào " + user, CLR_SUCCESS);
                        Timer t = new Timer(350, ev -> {
                            new MainFrame(ftpService);
                            LoginFrame.this.dispose();
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        String errMsg = resp != null ? resp : "Đăng nhập thất bại";
                        if (errMsg.contains("530")) errMsg = "Tên đăng nhập hoặc mật khẩu không chính xác!";
                        setStatus("🔴  " + errMsg, CLR_ERROR);
                        btnLogin.setEnabled(true);
                        btnConnect.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("🔴  Lỗi: " + ex.getMessage(), CLR_ERROR);
                    btnLogin.setEnabled(true);
                    btnConnect.setEnabled(true);
                });
            }
        }).start();
    }"""

if old_do_login in content:
    content = content.replace(old_do_login, new_do_login)
    print("[OK] Replaced doLogin with seamless auto-connect version")
else:
    print("[WARN] old_do_login not found")

with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\LoginFrame.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("[DONE] LoginFrame.java updated successfully!")


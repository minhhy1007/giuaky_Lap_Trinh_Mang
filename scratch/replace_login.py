# -*- coding: utf-8 -*-
with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\LoginFrame.java', 'r', encoding='utf-8') as f:
    text = f.read()

idx1 = text.find("private void doLogin() {")
idx2 = text.find("private void setStatus(", idx1)

new_do_login = """private void doLogin() {
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
    }

    // ================================================================
    // Hàm hỗ trợ tạo UI
    // ================================================================

    """

if idx1 != -1 and idx2 != -1:
    text = text[:idx1] + new_do_login + text[idx2:]
    with open(r'd:\Lap_Trinh_Mang\giuaky_Lap_Trinh_Mang\src\client\LoginFrame.java', 'w', encoding='utf-8') as f:
        f.write(text)
    print("[OK] Replaced doLogin successfully!")
else:
    print("[ERROR] Could not find indices")


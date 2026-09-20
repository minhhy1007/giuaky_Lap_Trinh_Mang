package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * PHASE 7 — Màn hình Đăng nhập (Login Screen)
 *
 * Kiến trúc:
 *   LoginFrame  ──gọi──►  FtpClientService.connect() + .login()
 *   LoginFrame  KHÔNG biết gì về Socket, InputStream hay PrintWriter.
 *
 * Luồng sử dụng:
 *   1. Người dùng điền Server, Port, Username, Password.
 *   2. Nhấn "Kết nối" → FtpClientService.connect().
 *   3. Nhấn "Đăng nhập" → FtpClientService.login().
 *   4. Login thành công → ẩn LoginFrame, mở MainFrame.
 */
public class LoginFrame extends JFrame {

    // ── Components giao diện ─────────────────────────────────────────
    private JTextField     tfServer;
    private JTextField     tfPort;
    private JTextField     tfUsername;
    private JPasswordField pfPassword;
    private JButton        btnConnect;
    private JButton        btnLogin;
    private JLabel         lblStatus;

    // ── Service Layer (không biết Socket) ────────────────────────────
    private final FtpClientService ftpService = new FtpClientService();

    // ── Màu sắc giao diện ────────────────────────────────────────────
    private static final Color CLR_BG      = new Color(30, 34, 45);
    private static final Color CLR_PANEL   = new Color(42, 47, 62);
    private static final Color CLR_ACCENT  = new Color(99, 179, 237);
    private static final Color CLR_SUCCESS = new Color(72, 199, 116);
    private static final Color CLR_ERROR   = new Color(252, 100, 100);
    private static final Color CLR_TEXT    = new Color(226, 232, 240);
    private static final Color CLR_MUTED   = new Color(130, 140, 160);
    private static final Color CLR_INPUT_BG= new Color(55, 62, 80);

    public LoginFrame() {
        initUI();
        setVisible(true);
    }

    // ================================================================
    // Khởi tạo giao diện
    // ================================================================

    private void initUI() {
        setTitle("FTP Client — Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 480);
        setLocationRelativeTo(null); // giữa màn hình
        setResizable(false);

        // ── Panel nền chính ──────────────────────────────────────────
        JPanel bgPanel = new JPanel(new BorderLayout());
        bgPanel.setBackground(CLR_BG);
        bgPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // ── Tiêu đề ──────────────────────────────────────────────────
        JLabel lblTitle = new JLabel("🌐 FTP Client", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(CLR_ACCENT);
        lblTitle.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel lblSubtitle = new JLabel("Lập Trình Mạng — FTP Project", SwingConstants.CENTER);
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setForeground(CLR_MUTED);
        lblSubtitle.setBorder(new EmptyBorder(0, 0, 24, 0));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CLR_BG);
        headerPanel.add(lblTitle,    BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.CENTER);

        // ── Panel form ───────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(CLR_PANEL);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 100), 1),
                new EmptyBorder(24, 28, 24, 28)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.insets    = new Insets(6, 0, 6, 0);
        gbc.weightx   = 1.0;

        // ── Dòng Server ──────────────────────────────────────────────
        tfServer = createTextField("localhost");
        tfPort   = createTextField("6789");

        JPanel serverRow = new JPanel(new GridLayout(1, 2, 8, 0));
        serverRow.setBackground(CLR_PANEL);
        serverRow.add(tfServer);
        serverRow.add(tfPort);

        // ── Các dòng input ───────────────────────────────────────────
        tfUsername = createTextField("admin");
        pfPassword = new JPasswordField("123");
        stylePasswordField(pfPassword);

        // ── Layout các dòng ──────────────────────────────────────────
        addFormRow(formPanel, gbc, 0, "🖥  Địa chỉ Server & Port:", serverRow);
        addFormRow(formPanel, gbc, 1, "👤 Tên đăng nhập:",         tfUsername);
        addFormRow(formPanel, gbc, 2, "🔑 Mật khẩu:",              pfPassword);

        // ── Nút bấm ──────────────────────────────────────────────────
        btnConnect = createButton("Kết nối",   CLR_ACCENT);
        btnLogin   = createButton("Đăng nhập", CLR_SUCCESS);
        btnLogin.setEnabled(false); // Chưa connect chưa được login

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setBackground(CLR_PANEL);
        btnPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        gbc.gridy    = 3;
        gbc.gridwidth = 2;
        gbc.insets   = new Insets(14, 0, 0, 0);
        btnPanel.add(btnConnect);
        btnPanel.add(btnLogin);
        formPanel.add(btnPanel, gbc);

        // ── Nhãn trạng thái ──────────────────────────────────────────
        lblStatus = new JLabel("⚪  Chưa kết nối", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setForeground(CLR_MUTED);
        lblStatus.setBorder(new EmptyBorder(10, 0, 0, 0));

        // ── Theme Selector ───────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.setBackground(CLR_BG);
        bottomPanel.add(lblStatus, BorderLayout.NORTH);

        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        themeRow.setBackground(CLR_BG);
        JLabel themeLabel = new JLabel("Giao di\u1ec7n:");
        themeLabel.setForeground(CLR_MUTED);
        themeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        String[] themes = {"Dark Custom", "Nimbus", "Windows", "Metal"};
        JComboBox<String> themeBox = new JComboBox<>(themes);
        themeBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        themeBox.setSelectedItem("Dark Custom");
        themeBox.addActionListener(e -> {
            String selected = (String) themeBox.getSelectedItem();
            applyTheme(selected);
        });

        themeRow.add(themeLabel);
        themeRow.add(themeBox);
        bottomPanel.add(themeRow, BorderLayout.SOUTH);

        // ── Ghép layout ──────────────────────────────────────────────
        bgPanel.add(headerPanel,  BorderLayout.NORTH);
        bgPanel.add(formPanel,    BorderLayout.CENTER);
        bgPanel.add(bottomPanel,  BorderLayout.SOUTH);
        setContentPane(bgPanel);

        // ── Gắn sự kiện ──────────────────────────────────────────────
        btnConnect.addActionListener(e -> doConnect());
        btnLogin.addActionListener(e   -> doLogin());

        // Enter trong pfPassword = nhấn Login
        pfPassword.addActionListener(e -> {
            if (btnLogin.isEnabled()) doLogin();
        });
    }

    // ================================================================
    // Chuyển đổi Theme (Look and Feel)
    // ================================================================

    private void applyTheme(String themeName) {
        try {
            switch (themeName) {
                case "Nimbus":
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                    break;
                case "Windows":
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Metal":
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    break;
                case "Dark Custom":
                default:
                    // Dark theme đã được set sẵn bởi màu tùy chỉnh của chúng ta
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    break;
            }
            // Cập nhật giao diện toàn bộ cửa sổ
            SwingUtilities.updateComponentTreeUI(this);
            this.pack();
            this.setSize(440, 520);
            this.setLocationRelativeTo(null);
        } catch (Exception ex) {
            lblStatus.setText("Lỗi đổi theme: " + ex.getMessage());
            lblStatus.setForeground(CLR_ERROR);
        }
    }

    // ================================================================
    // Hành động kết nối
    // ================================================================

    private void doConnect() {
        String host = tfServer.getText().trim();
        int    port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException ex) {
            setStatus("❌  Cổng không hợp lệ!", CLR_ERROR);
            return;
        }

        setStatus("🔄  Đang kết nối đến " + host + ":" + port + "...", CLR_MUTED);
        btnConnect.setEnabled(false);

        // Kết nối trong Thread riêng để không block UI
        new Thread(() -> {
            boolean ok = ftpService.connect(host, port);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    setStatus("🟢  Đã kết nối thành công!", CLR_SUCCESS);
                    btnLogin.setEnabled(true);
                    btnConnect.setText("Kết nối lại");
                } else {
                    setStatus("🔴  Kết nối thất bại! Kiểm tra Server đang chạy.", CLR_ERROR);
                }
                btnConnect.setEnabled(true);
            });
        }).start();
    }

    // ================================================================
    // Hành động đăng nhập
    // ================================================================

    private void doLogin() {
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
    }

    // ================================================================
    // Hàm hỗ trợ tạo UI
    // ================================================================

    private void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
    }

    private JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setBackground(CLR_INPUT_BG);
        tf.setForeground(CLR_TEXT);
        tf.setCaretColor(CLR_TEXT);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 100)),
                new EmptyBorder(6, 10, 6, 10)));
        return tf;
    }

    private void stylePasswordField(JPasswordField pf) {
        pf.setBackground(CLR_INPUT_BG);
        pf.setForeground(CLR_TEXT);
        pf.setCaretColor(CLR_TEXT);
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 100)),
                new EmptyBorder(6, 10, 6, 10)));
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 38));
        // Hover effect
        Color hoverColor = bgColor.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hoverColor);
            }
            @Override public void mouseExited(MouseEvent e)  {
                btn.setBackground(bgColor);
            }
        });
        return btn;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            int row, String labelText, JComponent field) {
        gbc.gridy     = row * 2;
        gbc.gridwidth = 2;
        gbc.insets    = new Insets(row == 0 ? 0 : 10, 0, 2, 0);
        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(CLR_MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridy  = row * 2 + 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(field, gbc);
    }

    // ================================================================
    // Entry point
    // ================================================================

    public static void main(String[] args) {
        // Dùng Look and Feel của hệ điều hành (trông native hơn)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(LoginFrame::new);
    }
}

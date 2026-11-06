package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * GIAO DIỆN ĐĂNG NHẬP CHUYÊN NGHIỆP (MODERN DESKTOP LOGIN SCREEN)
 *
 * Chuẩn hóa màu sắc & độ tương phản:
 * - Nút "Đăng nhập": Nền xanh Primary #3B82F6, chữ trắng #FFFFFF (tương phản cao).
 * - Nút "Kiểm tra kết nối": Nút phụ nền #1F2937, viền #374151, chữ #F9FAFB dễ đọc.
 * - Nút "Đăng ký tài khoản mới": Nút phụ nền #1F2937, viền #374151, chữ #3B82F6 đồng nhất theme.
 * - Sử dụng ModernButton: Tự vẽ 100% bằng Graphics2D, triệt tiêu hoàn toàn lỗi nền trắng chữ trắng.
 * - Checkbox "Hiện mật khẩu" đồng bộ, chữ rõ ràng.
 */
public class LoginFrame extends JFrame {

    static {
        ThemeManager.applyTheme();
    }

    // ── Components ───────────────────────────────────────────────────
    private JTextField     tfServer;
    private JTextField     tfPort;
    private JTextField     tfUsername;
    private JPasswordField pfPassword;
    private JCheckBox      chkShowPass;
    private ModernButton   btnConnect;
    private ModernButton   btnLogin;
    private ModernButton   btnRegister;
    private JLabel         lblStatus;

    // ── Service Layer ────────────────────────────────────────────────
    private final FtpClientService ftpService = new FtpClientService();

    public LoginFrame() {
        initUI();
        setVisible(true);
    }

    private void initUI() {
        setTitle("FTP Client — Đăng nhập hệ thống");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        // Nền chính
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeManager.CLR_BG);
        root.setBorder(new EmptyBorder(30, 36, 30, 36));

        // ── Header: Logo & Tên ứng dụng ──────────────────────────────
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(ThemeManager.CLR_BG);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel lblBrand = new JLabel("FTP CLOUD CLIENT");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblBrand.setForeground(ThemeManager.CLR_PRIMARY);
        lblBrand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDesc = new JLabel("Hệ thống truyền nhận và lưu trữ tệp tin bảo mật");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(ThemeManager.CLR_MUTED);
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblDesc.setBorder(new EmptyBorder(4, 0, 0, 0));

        headerPanel.add(lblBrand);
        headerPanel.add(lblDesc);
        root.add(headerPanel, BorderLayout.NORTH);

        // ── Card Panel chứa form đăng nhập ───────────────────────────
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(ThemeManager.CLR_PANEL);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ThemeManager.CLR_BORDER, 1, true),
                new EmptyBorder(22, 24, 22, 24)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.weightx = 1.0;

        // 1. Dòng Server & Port trên cùng một hàng
        JLabel lblServerHost = createFieldLabel("Máy chủ FTP:");
        JLabel lblServerPort = createFieldLabel("Cổng Port:");

        JPanel hostPortLabelRow = new JPanel(new GridLayout(1, 2, 10, 0));
        hostPortLabelRow.setOpaque(false);
        hostPortLabelRow.add(lblServerHost);
        hostPortLabelRow.add(lblServerPort);

        tfServer = createStyledTextField("localhost");
        tfPort   = createStyledTextField("6789");

        JPanel serverRow = new JPanel(new GridLayout(1, 2, 10, 0));
        serverRow.setOpaque(false);
        serverRow.add(tfServer);
        serverRow.add(tfPort);

        gbc.gridy = 0; cardPanel.add(hostPortLabelRow, gbc);
        gbc.gridy = 1; cardPanel.add(serverRow, gbc);

        // 2. Dòng Tên đăng nhập
        JLabel lblUser = createFieldLabel("Tên đăng nhập:");
        tfUsername = createStyledTextField("admin");
        gbc.gridy = 2; cardPanel.add(lblUser, gbc);
        gbc.gridy = 3; cardPanel.add(tfUsername, gbc);

        // 3. Dòng Mật khẩu & Hiện/Ẩn mật khẩu
        JLabel lblPass = createFieldLabel("Mật khẩu:");
        pfPassword = new JPasswordField("123");
        stylePasswordField(pfPassword);

        chkShowPass = new JCheckBox("Hiện mật khẩu");
        chkShowPass.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkShowPass.setForeground(ThemeManager.CLR_MUTED);
        chkShowPass.setOpaque(false);
        chkShowPass.setFocusPainted(false);
        chkShowPass.addActionListener(e -> {
            if (chkShowPass.isSelected()) {
                pfPassword.setEchoChar((char) 0);
            } else {
                pfPassword.setEchoChar('•');
            }
        });

        JPanel passRow = new JPanel(new BorderLayout(0, 4));
        passRow.setOpaque(false);
        passRow.add(pfPassword, BorderLayout.CENTER);
        passRow.add(chkShowPass, BorderLayout.SOUTH);

        gbc.gridy = 4; cardPanel.add(lblPass, gbc);
        gbc.gridy = 5; cardPanel.add(passRow, gbc);

        // 4. Nút bấm chính & phụ bằng ModernButton (Chống 100% chữ trắng nền trắng)
        btnLogin = new ModernButton("Đăng nhập", ThemeManager.CLR_PRIMARY, Color.WHITE, null);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setPreferredSize(new Dimension(0, 40));

        btnConnect = new ModernButton("Kiểm tra kết nối", ThemeManager.CLR_PANEL, ThemeManager.CLR_TEXT, ThemeManager.CLR_BORDER);
        btnConnect.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnConnect.setPreferredSize(new Dimension(0, 36));

        btnRegister = new ModernButton("Đăng ký tài khoản mới", ThemeManager.CLR_PANEL, ThemeManager.CLR_PRIMARY, ThemeManager.CLR_BORDER);
        btnRegister.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRegister.setPreferredSize(new Dimension(0, 36));

        JPanel btnActionPanel = new JPanel();
        btnActionPanel.setLayout(new BoxLayout(btnActionPanel, BoxLayout.Y_AXIS));
        btnActionPanel.setOpaque(false);
        btnActionPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnActionPanel.add(btnLogin);
        btnActionPanel.add(Box.createVerticalStrut(10));

        JPanel secondaryRow = new JPanel(new GridLayout(1, 2, 10, 0));
        secondaryRow.setOpaque(false);
        secondaryRow.add(btnConnect);
        secondaryRow.add(btnRegister);
        btnActionPanel.add(secondaryRow);

        gbc.gridy = 6;
        cardPanel.add(btnActionPanel, gbc);

        root.add(cardPanel, BorderLayout.CENTER);

        // ── Trạng thái kết nối phía dưới ─────────────────────────────
        JPanel footerPanel = new JPanel(new BorderLayout(0, 6));
        footerPanel.setBackground(ThemeManager.CLR_BG);
        footerPanel.setBorder(new EmptyBorder(14, 0, 0, 0));

        lblStatus = new JLabel("Trạng thái: Chưa kết nối máy chủ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ThemeManager.CLR_MUTED);
        footerPanel.add(lblStatus, BorderLayout.CENTER);

        root.add(footerPanel, BorderLayout.SOUTH);
        setContentPane(root);

        // ── Sự kiện ──────────────────────────────────────────────────
        btnConnect.addActionListener(e -> doConnect());
        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> doOpenRegister());

        tfUsername.addActionListener(e -> pfPassword.requestFocusInWindow());
        pfPassword.addActionListener(e -> doLogin());
    }

    // ================================================================
    // Logic kết nối & đăng nhập
    // ================================================================

    private void doConnect() {
        String host = tfServer.getText().trim();
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException ex) {
            setStatus("Cổng Port không hợp lệ!", ThemeManager.CLR_DANGER);
            return;
        }

        setStatus("Đang kết nối đến " + host + ":" + port + "...", ThemeManager.CLR_MUTED);
        btnConnect.setEnabled(false);

        new Thread(() -> {
            boolean ok = ftpService.connect(host, port);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    setStatus("Đã kết nối thành công đến máy chủ!", ThemeManager.CLR_SUCCESS);
                    btnConnect.setText("Kết nối lại");
                } else {
                    setStatus("Kết nối thất bại! Vui lòng kiểm tra Server đang chạy.", ThemeManager.CLR_DANGER);
                }
                btnConnect.setEnabled(true);
            });
        }).start();
    }

    private void doLogin() {
        String host = tfServer.getText().trim();
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException ex) {
            setStatus("Cổng Port không hợp lệ!", ThemeManager.CLR_DANGER);
            return;
        }

        String user = tfUsername.getText().trim();
        String pass = new String(pfPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!", ThemeManager.CLR_WARN);
            return;
        }

        setStatus("Đang kết nối & xác thực tài khoản...", ThemeManager.CLR_MUTED);
        btnLogin.setEnabled(false);
        btnConnect.setEnabled(false);

        new Thread(() -> {
            try {
                if (!ftpService.isConnected()) {
                    boolean ok = ftpService.connect(host, port);
                    if (!ok) {
                        SwingUtilities.invokeLater(() -> {
                            setStatus("Không thể kết nối đến máy chủ " + host + ":" + port, ThemeManager.CLR_DANGER);
                            btnLogin.setEnabled(true);
                            btnConnect.setEnabled(true);
                        });
                        return;
                    }
                }

                String resp = ftpService.login(user, pass);
                SwingUtilities.invokeLater(() -> {
                    if (resp != null && resp.startsWith("230")) {
                        setStatus("Đăng nhập thành công! Đang vào hệ thống...", ThemeManager.CLR_SUCCESS);
                        Timer t = new Timer(250, ev -> {
                            new MainFrame(ftpService);
                            LoginFrame.this.dispose();
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        String errMsg = resp != null ? resp : "Đăng nhập thất bại";
                        if (errMsg.contains("530")) {
                            errMsg = "Tên đăng nhập hoặc mật khẩu không chính xác (hoặc tài khoản đã bị khóa)!";
                        }
                        setStatus(errMsg, ThemeManager.CLR_DANGER);
                        btnLogin.setEnabled(true);
                        btnConnect.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Lỗi kết nối: " + ex.getMessage(), ThemeManager.CLR_DANGER);
                    btnLogin.setEnabled(true);
                    btnConnect.setEnabled(true);
                });
            }
        }).start();
    }

    private void doOpenRegister() {
        new RegisterDialog(this, ftpService).setVisible(true);
    }

    public void fillCredentials(String user, String pass) {
        if (tfUsername != null) tfUsername.setText(user);
        if (pfPassword != null) pfPassword.setText(pass);
    }

    private void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
    }

    // ================================================================
    // Helper tạo component chuẩn Modern Dark
    // ================================================================

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(ThemeManager.CLR_TEXT);
        lbl.setBorder(new EmptyBorder(3, 0, 3, 0));
        return lbl;
    }

    private JTextField createStyledTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setBackground(ThemeManager.CLR_BG);
        tf.setForeground(ThemeManager.CLR_TEXT);
        tf.setCaretColor(ThemeManager.CLR_TEXT);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ThemeManager.CLR_BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)));
        return tf;
    }

    private void stylePasswordField(JPasswordField pf) {
        pf.setBackground(ThemeManager.CLR_BG);
        pf.setForeground(ThemeManager.CLR_TEXT);
        pf.setCaretColor(ThemeManager.CLR_TEXT);
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ThemeManager.CLR_BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)));
    }

    // ================================================================
    // Main Entry Point
    // ================================================================

    public static void main(String[] args) {
        ThemeManager.applyTheme();
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}

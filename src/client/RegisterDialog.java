package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * HỘP THOẠI ĐĂNG KÝ TÀI KHOẢN MỚI (MODERN REGISTER DIALOG)
 *
 * Chuẩn Modern Dark:
 * - Bảng màu: #111827, #1F2937, #374151, #3B82F6, #F9FAFB, #9CA3AF.
 * - Sử dụng ModernButton: Triệt tiêu hoàn toàn lỗi nền trắng chữ trắng.
 * - Validation đầy đủ trước khi gửi.
 * - Hỗ trợ hiện/ẩn mật khẩu.
 */
public class RegisterDialog extends JDialog {

    private final FtpClientService ftpService;
    private final LoginFrame parentFrame;

    private JTextField     tfUsername;
    private JPasswordField pfPassword;
    private JPasswordField pfConfirm;
    private JCheckBox      chkShowPass;
    private JTextField     tfEmail;
    private JLabel         lblStatus;
    private ModernButton   btnSubmit;
    private ModernButton   btnCancel;

    public RegisterDialog(LoginFrame parent, FtpClientService service) {
        super(parent, "Đăng ký tài khoản mới", true);
        this.parentFrame = parent;
        this.ftpService  = service;

        initUI();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setSize(440, 540);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeManager.CLR_BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Tiêu đề
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel lblTitle = new JLabel("TẠO TÀI KHOẢN MỚI");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(ThemeManager.CLR_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Nhập thông tin bên dưới để đăng ký tài khoản");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(ThemeManager.CLR_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(lblSub);
        root.add(titlePanel, BorderLayout.NORTH);

        // Form điền thông tin trong Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeManager.CLR_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ThemeManager.CLR_BORDER, 1, true),
                new EmptyBorder(16, 20, 16, 20)));

        tfUsername = createStyledTextField();
        pfPassword = createStyledPasswordField();
        pfConfirm  = createStyledPasswordField();
        tfEmail    = createStyledTextField();

        chkShowPass = new JCheckBox("Hiện mật khẩu");
        chkShowPass.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkShowPass.setForeground(ThemeManager.CLR_MUTED);
        chkShowPass.setOpaque(false);
        chkShowPass.setFocusPainted(false);
        chkShowPass.addActionListener(e -> {
            char echo = chkShowPass.isSelected() ? (char) 0 : '•';
            pfPassword.setEchoChar(echo);
            pfConfirm.setEchoChar(echo);
        });

        card.add(createFieldGroup("Tên đăng nhập (3 - 30 ký tự):", tfUsername));
        card.add(Box.createVerticalStrut(8));
        card.add(createFieldGroup("Mật khẩu (tối thiểu 6 ký tự):", pfPassword));
        card.add(Box.createVerticalStrut(8));
        card.add(createFieldGroup("Xác nhận mật khẩu:", pfConfirm));
        card.add(Box.createVerticalStrut(4));
        card.add(chkShowPass);
        card.add(Box.createVerticalStrut(8));
        card.add(createFieldGroup("Email liên hệ (tùy chọn):", tfEmail));

        root.add(card, BorderLayout.CENTER);

        // Nút bấm & Status
        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(14, 0, 0, 0));

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ThemeManager.CLR_MUTED);
        bottom.add(lblStatus, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);

        btnCancel = new ModernButton("Hủy bỏ", ThemeManager.CLR_PANEL, ThemeManager.CLR_TEXT, ThemeManager.CLR_BORDER);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setPreferredSize(new Dimension(0, 38));

        btnSubmit = new ModernButton("Đăng ký ngay", ThemeManager.CLR_PRIMARY, Color.WHITE, null);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSubmit.setPreferredSize(new Dimension(0, 38));

        btnSubmit.addActionListener(this::handleRegister);
        btnCancel.addActionListener(e -> dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);
        bottom.add(btnPanel, BorderLayout.SOUTH);

        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(ThemeManager.CLR_TEXT);

        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setBackground(ThemeManager.CLR_BG);
        tf.setForeground(ThemeManager.CLR_TEXT);
        tf.setCaretColor(ThemeManager.CLR_TEXT);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ThemeManager.CLR_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        return tf;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setBackground(ThemeManager.CLR_BG);
        pf.setForeground(ThemeManager.CLR_TEXT);
        pf.setCaretColor(ThemeManager.CLR_TEXT);
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ThemeManager.CLR_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        return pf;
    }

    private void handleRegister(ActionEvent e) {
        String username = tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword());
        String confirm  = new String(pfConfirm.getPassword());
        String email    = tfEmail.getText().trim();

        // Validation
        if (username.isEmpty()) {
            setStatus("Vui lòng nhập tên đăng nhập!", ThemeManager.CLR_WARN);
            tfUsername.requestFocus();
            return;
        }
        if (username.length() < 3 || username.length() > 30) {
            setStatus("Tên đăng nhập phải từ 3 đến 30 ký tự!", ThemeManager.CLR_WARN);
            tfUsername.requestFocus();
            return;
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            setStatus("Tên đăng nhập chỉ gồm chữ, số và dấu gạch dưới!", ThemeManager.CLR_WARN);
            tfUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            setStatus("Vui lòng nhập mật khẩu!", ThemeManager.CLR_WARN);
            pfPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            setStatus("Mật khẩu phải từ 6 ký tự trở lên!", ThemeManager.CLR_WARN);
            pfPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            setStatus("Xác nhận mật khẩu không khớp!", ThemeManager.CLR_DANGER);
            pfConfirm.requestFocus();
            return;
        }
        if (!email.isEmpty() && !email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            setStatus("Địa chỉ Email không đúng định dạng!", ThemeManager.CLR_WARN);
            tfEmail.requestFocus();
            return;
        }

        setStatus("Đang gửi yêu cầu đăng ký...", ThemeManager.CLR_MUTED);
        btnSubmit.setEnabled(false);

        new Thread(() -> {
            try {
                if (!ftpService.isConnected()) {
                    boolean ok = ftpService.connect("localhost", 6789);
                    if (!ok) {
                        SwingUtilities.invokeLater(() -> {
                            setStatus("Không thể kết nối đến máy chủ!", ThemeManager.CLR_DANGER);
                            btnSubmit.setEnabled(true);
                        });
                        return;
                    }
                }

                String resp = ftpService.register(username, password, email);
                SwingUtilities.invokeLater(() -> {
                    btnSubmit.setEnabled(true);
                    if (resp != null && resp.startsWith("200")) {
                        setStatus("Đăng ký thành công!", ThemeManager.CLR_SUCCESS);
                        JOptionPane.showMessageDialog(this,
                                "Tài khoản [" + username + "] đã được tạo thành công!\nBạn có thể đăng nhập ngay bây giờ.",
                                "Đăng ký thành công", JOptionPane.INFORMATION_MESSAGE);
                        parentFrame.fillCredentials(username, password);
                        dispose();
                    } else {
                        String errMsg = resp != null ? resp : "Đăng ký thất bại";
                        if (errMsg.contains("550")) {
                            errMsg = "Tên đăng nhập hoặc Email đã tồn tại trong hệ thống!";
                        }
                        setStatus(errMsg, ThemeManager.CLR_DANGER);
                    }
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    btnSubmit.setEnabled(true);
                    setStatus("Lỗi mạng: " + ex.getMessage(), ThemeManager.CLR_DANGER);
                });
            }
        }).start();
    }

    private void setStatus(String text, Color color) {
        lblStatus.setText(text);
        lblStatus.setForeground(color);
    }
}

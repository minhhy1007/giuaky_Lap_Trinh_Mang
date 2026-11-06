-- ============================================================================
-- DỰ ÁN: CUSTOM FTP CLIENT - SERVER OVER TCP SOCKET
-- HỆ THỐNG CƠ SỞ DỮ LIỆU QUAN HỆ CHUẨN HÓA 3NF (MYSQL 8.0+)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS custom_ftp_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE custom_ftp_db;

-- ----------------------------------------------------------------------------
-- 1. BẢNG ROLES (Vai trò phân quyền trong hệ thống)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. BẢNG USERS (Tài khoản người dùng, Quota dung lượng & Thư mục Sandbox)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NULL,
    email VARCHAR(100) NULL UNIQUE,
    role_id INT NOT NULL DEFAULT 2,
    quota_bytes BIGINT NOT NULL DEFAULT 104857600, -- Mặc định 100MB (100 * 1024 * 1024)
    used_bytes BIGINT NOT NULL DEFAULT 0,
    home_dir VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON UPDATE CASCADE,
    CONSTRAINT chk_user_quota CHECK (quota_bytes >= 0),
    CONSTRAINT chk_user_used CHECK (used_bytes >= 0),
    INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. BẢNG FILES (Metadata tệp tin, đường dẫn Sandbox, dung lượng & Checksum)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS files (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    relative_path VARCHAR(500) NOT NULL, -- Đường dẫn tương đối trong Sandbox của User (ví dụ: "/docs/report.pdf")
    file_size BIGINT NOT NULL DEFAULT 0,
    sha256_hash VARCHAR(64) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_files_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_file_size CHECK (file_size >= 0),
    UNIQUE KEY uk_user_file_path (user_id, relative_path),
    INDEX idx_files_user (user_id),
    INDEX idx_files_hash (sha256_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. BẢNG TRANSFER_LOGS (Nhật ký kiểm toán, phiên kết nối và lịch sử I/O)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transfer_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    client_ip VARCHAR(45) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    filename VARCHAR(255) NULL,
    bytes_transferred BIGINT DEFAULT 0,
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    message VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_logs_user (user_id),
    INDEX idx_logs_action (action_type),
    INDEX idx_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. BẢNG PLANS (Gói cước dung lượng dịch vụ)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS plans (
    plan_id INT AUTO_INCREMENT PRIMARY KEY,
    plan_name VARCHAR(50) NOT NULL UNIQUE,
    max_quota_bytes BIGINT NOT NULL,
    price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    duration_days INT NOT NULL DEFAULT 30,
    description VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. BẢNG SUBSCRIPTIONS (Lịch sử đăng ký và thời hạn gói cước của User)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscriptions (
    sub_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    plan_id INT NOT NULL,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP NOT NULL,
    status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED', 'UPGRADED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_sub_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES plans(plan_id),
    INDEX idx_sub_user (user_id),
    INDEX idx_sub_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. BẢNG ORDERS (Đơn hàng và giao dịch thanh toán mô phỏng Mock Payment)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    plan_id INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'MOCK_BANKING',
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    transaction_code VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_plan FOREIGN KEY (plan_id) REFERENCES plans(plan_id),
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_code (transaction_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. BẢNG PAYMENTS (Lịch sử thanh toán mô phỏng & Idempotency Key)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    payment_code VARCHAR(100) UNIQUE NOT NULL, -- Idempotency Key / Reference code
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'MOCK_BANKING',
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    raw_response VARCHAR(255) NULL,
    paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_code (payment_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. BẢNG SHARED_FILES (Chia sẻ tệp tin qua Token và phân quyền truy cập)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shared_files (
    share_id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT NOT NULL,
    owner_id INT NOT NULL,
    shared_with_user_id INT NULL,
    share_token VARCHAR(64) UNIQUE NOT NULL,
    permission VARCHAR(20) NOT NULL DEFAULT 'DOWNLOAD',
    expires_at TIMESTAMP NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    access_count INT NOT NULL DEFAULT 0,
    max_downloads INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_share_file FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE,
    CONSTRAINT fk_share_owner FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_share_recipient FOREIGN KEY (shared_with_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_share_token (share_token),
    INDEX idx_share_recipient (shared_with_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- DỮ LIỆU KHỞI TẠO (SEED DATA)
-- Di chuyển toàn bộ tài khoản cũ từ UserDatabase.java để giữ tính tương thích
-- ============================================================================

-- Nạp Roles
INSERT INTO roles (role_id, role_name, description) VALUES
(1, 'ROLE_ADMIN', 'Quản trị viên hệ thống có toàn quyền'),
(2, 'ROLE_USER',  'Người dùng thông thường với thư mục Sandbox riêng')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- Nạp Plans
INSERT INTO plans (plan_id, plan_name, max_quota_bytes, price, duration_days, description) VALUES
(1, 'FREE', 104857600, 0.00, 365, 'Gói miễn phí cơ bản 100MB dung lượng'),
(2, 'PRO',  1073741824, 50000.00, 30, 'Gói nâng cấp chuyên nghiệp 1GB dung lượng'),
(3, 'VIP',  10737418240, 200000.00, 30, 'Gói cao cấp VIP 10GB dung lượng')
ON DUPLICATE KEY UPDATE price=VALUES(price), max_quota_bytes=VALUES(max_quota_bytes);

-- Nạp Users mẫu (Mật khẩu mã hóa SHA-256 kèm Salt hoặc tương thích ngược)
-- SHA-256 của '123' với salt rỗng = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3'
-- SHA-256 của 'alice123' = '2c21966270e59ee65b77c5950d268d069b2d86f765790be4ff48f07ff6759ce6'
-- SHA-256 của 'bob456'   = 'e3e70682c20941703e6f6fa07991d3b447fb709f79249772b539d76ed68c65b4'
-- SHA-256 của 'guest'    = '84983c60f7daadc1cb8698621f802c0d9f9a3c3c295c810748fb048115c186ec'

INSERT INTO users (user_id, username, password_hash, salt, email, role_id, quota_bytes, home_dir) VALUES
(1, 'admin', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', '', 'admin@customftp.local', 1, 10737418240, 'admin'),
(2, 'alice', '2c21966270e59ee65b77c5950d268d069b2d86f765790be4ff48f07ff6759ce6', '', 'alice@customftp.local', 2, 104857600,   'alice'),
(3, 'bob',   'e3e70682c20941703e6f6fa07991d3b447fb709f79249772b539d76ed68c65b4', '', 'bob@customftp.local',   2, 104857600,   'bob'),
(4, 'guest', '84983c60f7daadc1cb8698621f802c0d9f9a3c3c295c810748fb048115c186ec', '', 'guest@customftp.local', 2, 52428800,    'guest')
ON DUPLICATE KEY UPDATE home_dir=VALUES(home_dir);


package server;

import server.dao.FileDAO;
import server.dao.LogDAO;
import server.dao.ShareDAO;
import server.dao.UserDAO;
import server.dao.OrderDAO;
import server.dao.PaymentDAO;
import server.dao.PlanDAO;
import server.dao.SubscriptionDAO;
import server.model.FileMetadata;
import server.model.SharedFile;
import server.model.TransferLog;
import server.model.User;
import server.model.Order;
import server.model.Plan;
import server.model.Subscription;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * ĐIỀU PHỐI VÀ XỬ LÝ LỆNH GIAO THỨC FTP (COMMAND HANDLER)
 *
 * Nâng cấp Phase 3:
 *   - Quản trị Cây Thư mục Con: MKD, RMD, CWD, CDUP
 *   - Quản trị Quota và Toàn vẹn Checksum khi upload (STOR) và download (RETR)
 *   - Tích hợp ghi nhật ký kiểm toán tự động qua LogDAO vào bảng 'transfer_logs'
 */
public class CommandHandler {

    private final Session         session;
    private final UserDatabase    userDatabase;
    private final StorageManager  storageManager;
    private final LogDAO          logDAO;
    private final ShareDAO        shareDAO;
    private final FileDAO         fileDAO;
    private final UserDAO         userDAO;
    private final PlanDAO         planDAO;
    private final SubscriptionDAO subscriptionDAO;
    private final OrderDAO        orderDAO;
    private final PaymentDAO      paymentDAO;
    private String                clientIp = "127.0.0.1";

    public CommandHandler(Session session, UserDatabase userDatabase,
                          StorageManager storageManager) {
        this.session        = session;
        this.userDatabase   = userDatabase;
        this.storageManager = storageManager;
        this.logDAO         = new LogDAO();
        this.shareDAO       = new ShareDAO();
        this.fileDAO        = new FileDAO();
        this.userDAO        = new UserDAO();
        this.planDAO         = new PlanDAO();
        this.subscriptionDAO = new SubscriptionDAO();
        this.orderDAO        = new OrderDAO();
        this.paymentDAO      = new PaymentDAO();
    }

    public void setClientIp(String ip) {
        if (ip != null && !ip.trim().isEmpty()) {
            this.clientIp = ip.trim();
        }
    }

    /**
     * Router phân tích cú pháp và điều phối lệnh FTP
     */
    public String handle(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return "500 Empty command";
        }
        String input = rawInput.trim();

        String[] parts   = input.split("\\s+", 2);
        String command   = parts[0].toUpperCase();
        String argument  = (parts.length > 1) ? parts[1].trim() : "";

        // ── Commands KHÔNG cần authentication ─────────────────────────
        switch (command) {
            case "USER":     return handleUser(argument);
            case "PASS":     return handlePass(argument);
            case "REGISTER": return handleRegister(argument);
            case "QUIT":     return handleQuit();
        }

        // ── Tất cả commands còn lại → CẦN authentication ─────────────
        if (!session.isAuthenticated()) {
            return "530 Not logged in";
        }

        // ── Commands cần authentication ────────────────────────────────
        switch (command) {
            case "LOGOUT":
                return handleLogout();

            case "PING":
                return "200 PONG";

            case "ECHO":
                if (argument.isEmpty()) return "500 ECHO requires a message";
                return "200 " + argument;

            case "WHOAMI":
                return handleWhoami();

            case "PASSWD":
                return handleChangePassword(argument);

            // ── Directory Navigation & Management (Phase 3) ──────────────
            case "PWD":
                return "257 \"" + storageManager.formatDir(session.getCurrentDirectory()) + "\" is current directory";

            case "LIST":
                return storageManager.listDirectory(session.getHomeDir(), session.getCurrentDirectory());

            case "MKD":
                return handleMkd(argument);

            case "RMD":
                return handleRmd(argument);

            case "CWD":
                return handleCwd(argument);

            case "CDUP":
                return handleCdup();

            // ── File Management ──────────────────────────────────────────
            case "DELE":
                return handleDele(argument);

            case "RNFR":
                if (argument.isEmpty()) return "501 Syntax error: RNFR requires a filename";
                return handleRnfr(argument);

            case "RNTO":
                return handleRnto(argument);

            // ── File Sharing & Access Control (Phase 4) ──────────────────
            case "SHARE":
                return handleShare(argument);

            case "REVOKE":
                return handleRevoke(argument);

            case "SHAREDLIST":
                return handleSharedList();

            case "MYSHARES":
                return handleMyShares();

            case "AUDITLOGS":
                return handleAuditLogs(argument);

            // ── Phase 5: Plans, Subscriptions, Orders & Mock Payment ──────
            case "PLANS":
                return handlePlans();

            case "MYPLAN":
                return handleMyPlan();

            case "ORDER":
                return handleOrder(argument);

            case "CANCELORDER":
                return handleCancelOrder(argument);

            case "PAY":
                return handlePay(argument);

            case "MYORDERS":
                return handleMyOrders();

            // ── Commands DÀNH RIÊNG CHO QUẢN TRỊ VIÊN (ADMIN ONLY) ────────
            case "USERLIST":
                return handleUserList();

            case "SETUSERSTATUS":
                return handleSetUserStatus(argument);

            case "SETUSERROLE":
                return handleSetUserRole(argument);

            case "SETUSERQUOTA":
                return handleSetUserQuota(argument);

            case "ADMINSTATS":
                return handleAdminStats();

            case "ADMINALLORDERS":
                return handleAdminAllOrders();

            // ── Các lệnh cần direct stream access ──────────────────────────
            case "STOR":
                return "500 Internal error: STOR should use handleStor()";

            case "RETR":
                return "500 Internal error: RETR should use handleRetr()";

            default:
                return "500 Unknown command: " + parts[0];
        }
    }

    // =========================================================================
    // XỬ LÝ UPLOAD VÀ DOWNLOAD (FILE STREAM) VỚI QUOTA VÀ LOG
    // =========================================================================

    public String handleStor(String filename, ProtocolInputStream lineReader, PrintWriter writer) {
        if (!session.isAuthenticated()) {
            return "530 Not logged in";
        }
        if (filename == null || filename.isEmpty()) {
            return "501 Syntax error: STOR requires a filename. Usage: STOR <filename>";
        }

        writer.println("125 Ready for '" + filename + "'. Send: SIZE <bytes>");

        String sizeLine;
        try {
            sizeLine = lineReader.readLine();
        } catch (IOException e) {
            logDAO.log(getUserId(), clientIp, "UPLOAD", filename, 0, "FAILED", "Mất kết nối trước khi nhận SIZE");
            return "426 Connection closed; transfer aborted";
        }

        if (sizeLine == null || !sizeLine.toUpperCase().startsWith("SIZE ")) {
            logDAO.log(getUserId(), clientIp, "UPLOAD", filename, 0, "FAILED", "Sai định dạng SIZE");
            return "501 Expected 'SIZE <bytes>', got: " + sizeLine;
        }

        long fileSize;
        try {
            fileSize = Long.parseLong(sizeLine.substring(5).trim());
        } catch (NumberFormatException e) {
            logDAO.log(getUserId(), clientIp, "UPLOAD", filename, 0, "FAILED", "Invalid file size");
            return "501 Invalid file size: " + sizeLine;
        }

        if (fileSize < 0) {
            return "501 File size cannot be negative";
        }

        // Tự động kiểm tra và giáng cấp gói nếu đã hết hạn
        subscriptionDAO.checkAndExpireSubscriptions();

        int userId = getUserId();

        // 1. Kiểm tra Quota TRƯỚC KHI cho phép truyền file
        if (userId > 0 && fileSize > 0) {
            boolean quotaReserved = userDAO.checkAndReserveQuota(userId, fileSize);
            if (!quotaReserved) {
                long[] qInfo = userDAO.getQuotaInfo(userId);
                long remaining = Math.max(0, qInfo[1] - qInfo[0]);
                String quotaErr = String.format("552 Storage quota exceeded: File size %d bytes exceeds remaining quota (%d bytes).",
                        fileSize, remaining);
                logDAO.log(userId, clientIp, "UPLOAD", filename, fileSize, "FAILED", quotaErr);
                return quotaErr;
            }
        }

        // 2. Quota hợp lệ -> Phản hồi 150 để Client bắt đầu bơm bytes
        writer.println("150 Ready to receive " + fileSize + " bytes. Binary mode.");

        InputStream rawIn = lineReader.getRawInputStream();

        // Nhận file vào thư mục Sandbox riêng của User, quota đã được đặt trước
        String response = storageManager.receiveFile(
                userId, session.getHomeDir(), session.getCurrentDirectory(), filename, fileSize, rawIn, true);

        // Ghi nhật ký kiểm toán
        if (response != null && response.startsWith("226")) {
            logDAO.log(userId, clientIp, "UPLOAD", filename, fileSize, "SUCCESS", "Tải lên hoàn tất");
        } else {
            logDAO.log(userId, clientIp, "UPLOAD", filename, fileSize, "FAILED", response);
        }

        return response;
    }

    public String handleRetr(String filename, PrintWriter writer, OutputStream rawOut) {
        if (!session.isAuthenticated()) {
            return "530 Not logged in";
        }
        if (filename == null || filename.isEmpty()) {
            return "501 Syntax error: RETR requires a filename. Usage: RETR <filename>";
        }

        String response = storageManager.sendFile(
                session.getHomeDir(), session.getCurrentDirectory(), filename, rawOut, writer);

        int userId = getUserId();
        if (response != null && response.startsWith("226")) {
            logDAO.log(userId, clientIp, "DOWNLOAD", filename, 0, "SUCCESS", "Tải về hoàn tất");
        } else {
            logDAO.log(userId, clientIp, "DOWNLOAD", filename, 0, "FAILED", response);
        }

        return response;
    }

    // =========================================================================
    // XỬ LÝ CÂY THƯ MỤC: MKD, RMD, CWD, CDUP
    // =========================================================================

    private String handleMkd(String dirName) {
        if (dirName.isEmpty()) return "501 Syntax error: MKD requires directory name";
        String resp = storageManager.makeDirectory(session.getHomeDir(), session.getCurrentDirectory(), dirName);
        if (resp.startsWith("257")) {
            logDAO.log(getUserId(), clientIp, "MKDIR", dirName, 0, "SUCCESS", "Tạo thư mục: " + dirName);
        } else {
            logDAO.log(getUserId(), clientIp, "MKDIR", dirName, 0, "FAILED", resp);
        }
        return resp;
    }

    private String handleRmd(String dirName) {
        if (dirName.isEmpty()) return "501 Syntax error: RMD requires directory name";
        String resp = storageManager.removeDirectory(session.getHomeDir(), session.getCurrentDirectory(), dirName);
        if (resp.startsWith("250")) {
            logDAO.log(getUserId(), clientIp, "RMDIR", dirName, 0, "SUCCESS", "Xóa thư mục: " + dirName);
        } else {
            logDAO.log(getUserId(), clientIp, "RMDIR", dirName, 0, "FAILED", resp);
        }
        return resp;
    }

    private String handleCwd(String dirName) {
        if (dirName.isEmpty() || dirName.equals("/")) {
            session.cd("/");
            return "250 Directory successfully changed to \"/\"";
        }

        try {
            Path target = storageManager.resolveSafe(session.getHomeDir(), session.getCurrentDirectory(), dirName);
            if (!Files.exists(target) || !Files.isDirectory(target)) {
                return "550 No such directory: " + dirName;
            }
            session.cd(dirName);
            return "250 Directory successfully changed to \"" + storageManager.formatDir(session.getCurrentDirectory()) + "\"";
        } catch (SecurityException e) {
            return "550 Access denied: " + e.getMessage();
        }
    }

    private String handleCdup() {
        session.cdup();
        return "200 Directory changed to parent: \"" + storageManager.formatDir(session.getCurrentDirectory()) + "\"";
    }

    // =========================================================================
    // XỬ LÝ FILE: DELE, RNFR, RNTO
    // =========================================================================

    private String handleDele(String filename) {
        if (filename.isEmpty()) return "501 Syntax error: DELE requires a filename";
        int userId = getUserId();
        String resp = storageManager.deleteFile(userId, session.getHomeDir(), session.getCurrentDirectory(), filename);
        if (resp.startsWith("250")) {
            logDAO.log(userId, clientIp, "DELETE", filename, 0, "SUCCESS", "Xóa file thành công");
        } else {
            logDAO.log(userId, clientIp, "DELETE", filename, 0, "FAILED", resp);
        }
        return resp;
    }

    private String handleRnfr(String filename) {
        session.setRenameFrom(filename);
        return "350 File exists, ready for destination name (RNTO)";
    }

    private String handleRnto(String newName) {
        if (!session.hasRenameFrom()) {
            return "503 Bad sequence of commands — send RNFR first";
        }
        String oldName = session.getRenameFrom();
        session.clearRenameFrom();
        int userId = getUserId();
        String resp = storageManager.renameFile(userId, session.getHomeDir(), session.getCurrentDirectory(), oldName, newName);
        if (resp.startsWith("250")) {
            logDAO.log(userId, clientIp, "RENAME", oldName + " -> " + newName, 0, "SUCCESS", "Đổi tên thành công");
        } else {
            logDAO.log(userId, clientIp, "RENAME", oldName, 0, "FAILED", resp);
        }
        return resp;
    }

    // =========================================================================
    // AUTH, PASSWD, REGISTER & WHOAMI
    // =========================================================================

    private String handleUser(String username) {
        if (username.isEmpty()) return "501 Syntax error: USER requires a username";
        session.setPendingUsername(username);
        return "331 Username accepted, enter password";
    }

    private String handlePass(String password) {
        if (session.getState() != Session.State.WAITING_PASSWORD) {
            return "503 Bad sequence — send USER first";
        }
        if (password.isEmpty()) return "501 Syntax error: PASS requires a password";

        String username = session.getPendingUsername();
        User user = userDatabase.authenticate(username, password);

        if (user != null) {
            session.loginSuccess(user);
            logDAO.log(user.getUserId(), clientIp, "LOGIN", null, 0, "SUCCESS", "Đăng nhập thành công");
            return "230 Login successful. Welcome, " + username + "! [Role: " + user.getRoleName() + "]";
        } else {
            session.reset();
            logDAO.log(null, clientIp, "LOGIN", username, 0, "FAILED", "Sai mật khẩu hoặc tài khoản không tồn tại");
            return "530 Login incorrect";
        }
    }

    private String handleRegister(String argument) {
        if (argument.isEmpty()) {
            return "501 Syntax error: REGISTER <username> <password> [email]";
        }

        String[] tokens = argument.split("\\s+");
        if (tokens.length < 2) {
            return "501 Syntax error: REGISTER <username> <password> [email]";
        }

        String username = tokens[0].trim();
        String password = tokens[1].trim();
        String email    = (tokens.length > 2) ? tokens[2].trim() : null;

        if (username.length() < 3 || username.length() > 30) {
            return "501 Username must be between 3 and 30 characters";
        }
        if (password.length() < 6) {
            return "501 Password must be at least 6 characters";
        }

        boolean ok = userDatabase.register(username, password, email);
        if (ok) {
            logDAO.log(null, clientIp, "LOGIN", username, 0, "SUCCESS", "Đăng ký tài khoản mới: " + username);
            return "200 Registration successful for '" + username + "'. You can now login.";
        } else {
            logDAO.log(null, clientIp, "LOGIN", username, 0, "FAILED", "Đăng ký thất bại (trùng lặp)");
            return "450 Username already exists or registration failed";
        }
    }

    private String handleChangePassword(String argument) {
        if (argument.isEmpty()) {
            return "501 Syntax error: PASSWD <old_password> <new_password>";
        }

        String[] tokens = argument.split("\\s+");
        if (tokens.length < 2) {
            return "501 Syntax error: PASSWD <old_password> <new_password>";
        }

        String oldPass = tokens[0];
        String newPass = tokens[1];

        if (newPass.length() < 6) {
            return "501 New password must be at least 6 characters";
        }

        User current = session.getCurrentUser();
        int userId = (current != null) ? current.getUserId() : -1;
        String username = session.getLoggedInUser();

        boolean ok = userDatabase.changePassword(userId, username, oldPass, newPass);
        if (ok) {
            return "200 Password changed successfully";
        } else {
            return "530 Incorrect current password or change failed";
        }
    }

    private String handleWhoami() {
        User u = session.getCurrentUser();
        if (u != null) {
            int userId = u.getUserId();
            long quotaMb = u.getQuotaBytes() / (1024 * 1024);
            long usedKb = u.getUsedBytes() / 1024;
            if (userId > 0) {
                long[] qInfo = userDAO.getQuotaInfo(userId);
                if (qInfo != null) {
                    usedKb = qInfo[0] / 1024;
                    quotaMb = qInfo[1] / (1024 * 1024);
                    u.setUsedBytes(qInfo[0]);
                    u.setQuotaBytes(qInfo[1]);
                }
            }
            return String.format("200 User=%s | Role=%s | Sandbox=storage/%s | Quota=%dMB | Used=%dKB",
                    u.getUsername(), u.getRoleName(), u.getHomeDir(),
                    quotaMb, usedKb);
        }
        return "200 User=" + session.getLoggedInUser();
    }

    private String handleUserList() {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }

        List<User> users = userDAO.getAllUsers();
        StringBuilder sb = new StringBuilder();
        sb.append("150 User list (").append(users.size()).append(" users):\n");
        sb.append(String.format("  %-5s %-16s %-15s %-12s %-12s %-10s%n", "ID", "USERNAME", "ROLE", "QUOTA(MB)", "USED(KB)", "ACTIVE"));
        sb.append("  -----------------------------------------------------------------------------\n");

        for (User u : users) {
            sb.append(String.format("  %-5d %-16s %-15s %-12d %-12d %-10s%n",
                    u.getUserId(), u.getUsername(), u.getRoleName(),
                    u.getQuotaBytes() / (1024 * 1024), u.getUsedBytes() / 1024,
                    u.isActive() ? "YES" : "NO"));
        }
        sb.append("226 User list completed.");
        return sb.toString();
    }

    private String handleLogout() {
        String who = session.getLoggedInUser();
        int userId = getUserId();
        logDAO.log(userId, clientIp, "LOGOUT", who, 0, "SUCCESS", "Đăng xuất tài khoản");
        session.reset();
        return "230 Goodbye, " + who + ". Logged out (connection stays open)";
    }

    private String handleQuit() {
        return "221 Goodbye";
    }

    public boolean isQuitResponse(String response) {
        return response != null && response.startsWith("221 Goodbye");
    }


    // =========================================================================
    // PHASE 4: SHARING, ACCESS CONTROL VÀ ACTIVITY HISTORY
    // =========================================================================

    public String handleGetShared(String token, PrintWriter writer, OutputStream rawOut) {
        if (token == null || token.trim().isEmpty()) {
            return "501 Syntax error: GETSHARED requires a share token";
        }

        int requestingUserId = getUserId();
        SharedFile sf = shareDAO.validateAndGetShare(token.trim(), requestingUserId > 0 ? requestingUserId : null);
        if (sf == null) {
            logDAO.log(requestingUserId > 0 ? requestingUserId : null, clientIp, "ACCESS_DENIED", null, 0, "FAILED", "Invalid, expired, or revoked share token");
            return "550 Access denied: Invalid, expired, or revoked share link";
        }

        if ("READ_ONLY".equalsIgnoreCase(sf.getPermission()) || "VIEW".equalsIgnoreCase(sf.getPermission())) {
            return "550 Permission denied: This share link has VIEW permission (download not permitted)";
        }

        String ownerHome = sf.getOwnerUsername();
        String relPath = sf.getRelativePath();
        String filename = sf.getFilename();

        String dir = "";
        if (relPath != null && relPath.startsWith("/")) {
            relPath = relPath.substring(1);
        }
        int lastSlash = (relPath != null) ? relPath.lastIndexOf('/') : -1;
        if (lastSlash >= 0) {
            dir = relPath.substring(0, lastSlash);
        }

        String response = storageManager.sendFile(ownerHome, dir, filename, rawOut, writer);
        if (response != null && response.startsWith("226")) {
            shareDAO.incrementAccessCount(sf.getShareId());
            logDAO.log(requestingUserId > 0 ? requestingUserId : null, clientIp, "DOWNLOAD_SHARED", filename, sf.getFileSize(), "SUCCESS", "Tải file chia sẻ thành công");
        } else {
            logDAO.log(requestingUserId > 0 ? requestingUserId : null, clientIp, "DOWNLOAD_SHARED", filename, sf.getFileSize(), "FAILED", response);
        }
        return response;
    }

    private String handleShare(String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: SHARE <filename> <recipient_or_PUBLIC> [permission] [expires_hours] [max_downloads]";
        }

        String input = argument.trim();
        String filename;
        String recipientStr;
        String permission = "DOWNLOAD";
        int expiresHours = 0;
        int maxDownloads = 0;

        if (input.startsWith("\"")) {
            int closeQuote = input.indexOf("\"", 1);
            if (closeQuote == -1) return "501 Syntax error: Missing closing quote for filename";
            filename = input.substring(1, closeQuote).trim();
            String rest = input.substring(closeQuote + 1).trim();
            String[] restParts = rest.isEmpty() ? new String[0] : rest.split("\\s+");
            if (restParts.length < 1) {
                return "501 Syntax error: SHARE <filename> <recipient_or_PUBLIC> [permission] [expires_hours] [max_downloads]";
            }
            recipientStr = restParts[0].trim();
            if (restParts.length > 1) permission = restParts[1].trim().toUpperCase();
            if (restParts.length > 2) expiresHours = parsePositiveInt(restParts[2], 0);
            if (restParts.length > 3) maxDownloads = parsePositiveInt(restParts[3], 0);
        } else {
            String[] tokens = input.split("\\s+");
            if (tokens.length < 2) {
                return "501 Syntax error: SHARE <filename> <recipient_or_PUBLIC> [permission] [expires_hours] [max_downloads]";
            }
            int idx = tokens.length - 1;
            if (idx >= 2 && tokens[idx].matches("^\\d+$")) {
                maxDownloads = parsePositiveInt(tokens[idx], 0);
                idx--;
            }
            if (idx >= 2 && tokens[idx].matches("^\\d+$")) {
                expiresHours = parsePositiveInt(tokens[idx], 0);
                idx--;
            }
            if (idx >= 2 && (tokens[idx].equalsIgnoreCase("DOWNLOAD") || tokens[idx].equalsIgnoreCase("VIEW") || tokens[idx].equalsIgnoreCase("READ_ONLY"))) {
                permission = tokens[idx].toUpperCase();
                idx--;
            }
            recipientStr = tokens[idx].trim();
            StringBuilder fnSb = new StringBuilder();
            for (int i = 0; i < idx; i++) {
                if (fnSb.length() > 0) fnSb.append(" ");
                fnSb.append(tokens[i]);
            }
            filename = fnSb.toString().trim();
        }

        if (filename.isEmpty()) {
            return "501 Syntax error: Empty filename";
        }

        int ownerId = getUserId();
        FileMetadata file = fileDAO.findByFilename(ownerId, session.getCurrentDirectory(), filename);
        if (file == null) {
            return "550 File not found in current directory: " + filename;
        }

        Integer recipientUserId = null;
        if (!"PUBLIC".equalsIgnoreCase(recipientStr)) {
            User recipientUser = userDAO.findByUsername(recipientStr);
            if (recipientUser == null) {
                return "550 Recipient user not found: " + recipientStr;
            }
            if (recipientUser.getUserId() == ownerId) {
                return "501 Cannot share file with yourself";
            }
            recipientUserId = recipientUser.getUserId();
        }

        Timestamp expiresAt = null;
        if (expiresHours > 0) {
            expiresAt = new Timestamp(System.currentTimeMillis() + expiresHours * 3600 * 1000L);
        }

        String token = shareDAO.createShare(file.getFileId(), ownerId, recipientUserId, permission, expiresAt, maxDownloads);
        if (token != null) {
            logDAO.log(ownerId, clientIp, "SHARE", filename, file.getFileSize(), "SUCCESS", "Chia sẻ cho " + recipientStr + " (Quyền: " + permission + ")");
            return "250 Share created successfully. Token: " + token + " | Recipient: " + recipientStr + " | Perm: " + permission;
        } else {
            logDAO.log(ownerId, clientIp, "SHARE", filename, file.getFileSize(), "FAILED", "Lỗi tạo lượt chia sẻ");
            return "451 Failed to create share";
        }
    }

    private String handleRevoke(String argument) {
        if (argument.isEmpty()) return "501 Syntax error: REVOKE <share_id>";
        int shareId;
        try {
            shareId = Integer.parseInt(argument.trim());
        } catch (NumberFormatException e) {
            return "501 Invalid share_id: " + argument;
        }

        int ownerId = getUserId();
        boolean ok = shareDAO.revokeShare(shareId, ownerId);
        if (ok) {
            logDAO.log(ownerId, clientIp, "REVOKE", "share_id=" + shareId, 0, "SUCCESS", "Đã thu hồi chia sẻ #" + shareId);
            return "250 Share #" + shareId + " revoked successfully";
        } else {
            logDAO.log(ownerId, clientIp, "ACCESS_DENIED", "share_id=" + shareId, 0, "FAILED", "Từ chối thu hồi (không phải chủ sở hữu hoặc không tồn tại)");
            return "550 Access denied: Not the owner or share not found";
        }
    }

    private String handleSharedList() {
        int userId = getUserId();
        List<SharedFile> list = shareDAO.getSharedWithUser(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("150-Shared with you (").append(list.size()).append(" file(s)):\n");
        sb.append(String.format("  %-5s %-20s %-12s %-15s %-12s %-32s%n", "ID", "FILENAME", "SIZE", "OWNER", "PERM", "TOKEN"));
        sb.append("  --------------------------------------------------------------------------------------------------------\n");

        for (SharedFile sf : list) {
            sb.append(String.format("  %-5d %-20s %-12s %-15s %-12s %-32s%n",
                    sf.getShareId(), sf.getFilename(), sf.getFileSize() + " B",
                    sf.getOwnerUsername(), sf.getPermission(), sf.getShareToken()));
        }
        sb.append("226 End of shared list.");
        return sb.toString();
    }

    private String handleMyShares() {
        int userId = getUserId();
        List<SharedFile> list = shareDAO.getSharedByOwner(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("150-Shared by you (").append(list.size()).append(" item(s)):\n");
        sb.append(String.format("  %-5s %-20s %-15s %-10s %-10s %-10s %-32s%n", "ID", "FILENAME", "RECIPIENT", "PERM", "DOWNLOADS", "STATUS", "TOKEN"));
        sb.append("  ------------------------------------------------------------------------------------------------------------------\n");

        for (SharedFile sf : list) {
            String recipient = (sf.getRecipientUsername() != null) ? sf.getRecipientUsername() : "PUBLIC";
            String dl = sf.getAccessCount() + "/" + (sf.getMaxDownloads() > 0 ? sf.getMaxDownloads() : "∞");
            String status = sf.isRevoked() ? "REVOKED" : (sf.isValid() ? "ACTIVE" : "EXPIRED");
            sb.append(String.format("  %-5d %-20s %-15s %-10s %-10s %-10s %-32s%n",
                    sf.getShareId(), sf.getFilename(), recipient, sf.getPermission(), dl, status, sf.getShareToken()));
        }
        sb.append("226 End of my shares list.");
        return sb.toString();
    }

    private String handleAuditLogs(String argument) {
        String filter = "ALL";
        int page = 1;
        int limit = 20;

        if (!argument.isEmpty()) {
            String[] parts = argument.split("\s+");
            if (parts.length >= 1 && !parts[0].isEmpty()) filter = parts[0].trim();
            if (parts.length >= 2) page = parsePositiveInt(parts[1], 1);
        }

        int offset = (page - 1) * limit;
        List<TransferLog> logs;
        int total;

        if (session.isAdmin()) {
            logs = logDAO.getAllLogs(limit, offset, filter, null);
            total = logs.size(); // Simplified count for list display
        } else {
            logs = logDAO.getLogsByUser(getUserId(), limit, offset, filter);
            total = logDAO.countLogsByUser(getUserId(), filter);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("150-Activity Audit Logs [Page ").append(page).append("] (").append(logs.size()).append(" records):\n");
        sb.append(String.format("  %-18s %-12s %-15s %-20s %-10s %-25s%n", "TIME", "USER", "ACTION", "TARGET", "STATUS", "MESSAGE"));
        sb.append("  ----------------------------------------------------------------------------------------------------------\n");

        for (TransferLog l : logs) {
            String timeStr = (l.getCreatedAt() != null) ? l.getCreatedAt().toString().substring(0, 19) : "";
            String userStr = (l.getUsername() != null) ? l.getUsername() : (l.getUserId() != null ? "ID:" + l.getUserId() : "GUEST");
            String target = (l.getFilename() != null) ? l.getFilename() : "-";
            String msg = (l.getMessage() != null) ? l.getMessage() : "";
            if (msg.length() > 25) msg = msg.substring(0, 22) + "...";

            sb.append(String.format("  %-18s %-12s %-15s %-20s %-10s %-25s%n",
                    timeStr, userStr, l.getActionType(), target, l.getStatus(), msg));
        }
        sb.append("226 End of audit logs.");
        return sb.toString();
    }

    private int parsePositiveInt(String str, int defaultVal) {
        try {
            int val = Integer.parseInt(str.trim());
            return Math.max(0, val);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private int getUserId() {
        return (session.getCurrentUser() != null) ? session.getCurrentUser().getUserId() : 0;
    }

    // =========================================================================
    // PHASE 5: GÓI CƯỚC, ĐĂNG KÝ, ĐƠN HÀNG VÀ THANH TOÁN MÔ PHỎNG (MOCK PAYMENT)
    // =========================================================================

    private String handlePlans() {
        List<Plan> plans = planDAO.getAllPlans();
        StringBuilder sb = new StringBuilder();
        sb.append("150-Available service plans (").append(plans.size()).append("):\n");
        sb.append(String.format("  %-5s %-10s %-12s %-18s %-10s %-30s%n", "ID", "PLAN", "QUOTA", "PRICE", "DURATION", "DESCRIPTION"));
        sb.append("  -----------------------------------------------------------------------------------------\n");
        for (Plan p : plans) {
            sb.append(String.format("  %-5d %-10s %-12s %-18s %-10s %-30s%n",
                    p.getPlanId(), p.getPlanName(), p.getFormattedQuota(), p.getFormattedPrice(),
                    p.getDurationDays() + " days", p.getDescription()));
        }
        sb.append("226 End of plans list.");
        return sb.toString();
    }

    private String handleMyPlan() {
        if (!session.isAuthenticated()) return "530 Not logged in";
        int userId = getUserId();
        subscriptionDAO.checkAndExpireSubscriptions();
        User user = userDAO.findByUsername(session.getCurrentUser().getUsername());
        if (user == null) return "550 User not found";

        Subscription activeSub = subscriptionDAO.getActiveSubscription(userId);
        String planName = (activeSub != null) ? activeSub.getPlanName() : "FREE (Default)";
        String status = (activeSub != null) ? activeSub.getStatus() : "ACTIVE";
        String expiry = (activeSub != null && activeSub.getEndDate() != null) ? activeSub.getEndDate().toString() : "Vĩnh viễn (Default)";

        long quota = user.getQuotaBytes();
        long used = user.getUsedBytes();
        long remaining = Math.max(0, quota - used);
        boolean overQuota = (used > quota);

        return String.format("200 Plan: %s | Status: %s%s | Expiry: %s | Quota: %s | Used: %s | Remaining: %s%s",
                planName, status, (overQuota ? " [OVER_QUOTA]" : ""),
                expiry, formatBytes(quota), formatBytes(used), formatBytes(remaining),
                (overQuota ? " (Cảnh báo: Đã vượt quá hạn mức dung lượng!)" : ""));
    }

    private String handleOrder(String argument) {
        if (!session.isAuthenticated()) return "530 Not logged in";
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: ORDER <plan_id> [payment_method]";
        }
        String[] parts = argument.trim().split("\\s+", 2);
        try {
            int planId = Integer.parseInt(parts[0]);
            String method = parts.length > 1 ? parts[1] : "MOCK_BANKING";
            int userId = getUserId();
            Order order = orderDAO.createOrder(userId, planId, method);
            if (order == null) {
                return "550 Failed to create order. Plan might be inactive.";
            }
            logDAO.log(userId, clientIp, "CREATE_ORDER", "Order: " + order.getTransactionCode(), 0, "SUCCESS",
                    "Plan: " + order.getPlanName() + " | Amount: " + order.getFormattedAmount());
            return String.format("250 Order created: %s | OrderID: %d | Plan: %s | Amount: %s | Status: %s. Use PAY %d SUCCESS to simulate payment.",
                    order.getTransactionCode(), order.getOrderId(), order.getPlanName(),
                    order.getFormattedAmount(), order.getStatus(), order.getOrderId());
        } catch (NumberFormatException e) {
            return "501 Invalid plan ID: " + parts[0];
        } catch (IllegalArgumentException e) {
            return "550 " + e.getMessage();
        } catch (Exception e) {
            return "550 Error creating order: " + e.getMessage();
        }
    }

    private String handleCancelOrder(String argument) {
        if (!session.isAuthenticated()) return "530 Not logged in";
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: CANCELORDER <order_id_or_code>";
        }
        int userId = getUserId();
        Order order = null;
        try {
            int orderId = Integer.parseInt(argument.trim());
            order = orderDAO.getOrderById(orderId);
        } catch (NumberFormatException e) {
            order = orderDAO.getOrderByCode(argument.trim());
        }

        if (order == null) {
            return "550 Order not found: " + argument.trim();
        }
        if (order.getUserId() != userId) {
            return "550 Access denied: Order belongs to another user (Anti-IDOR)";
        }
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            return "550 Cannot cancel order with status: " + order.getStatus();
        }

        boolean ok = orderDAO.cancelOrder(order.getOrderId(), userId);
        if (ok) {
            logDAO.log(userId, clientIp, "CANCEL_ORDER", "Order: " + order.getTransactionCode(), 0, "SUCCESS", "Status changed to CANCELLED");
            return "200 Order " + order.getTransactionCode() + " cancelled successfully.";
        } else {
            return "550 Failed to cancel order.";
        }
    }

    private String handlePay(String argument) {
        if (!session.isAuthenticated()) return "530 Not logged in";
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: PAY <order_id_or_code> [SUCCESS|FAIL]";
        }
        String[] parts = argument.trim().split("\\s+");
        String idOrCode = parts[0];
        boolean simulateSuccess = true;
        if (parts.length > 1 && ("FAIL".equalsIgnoreCase(parts[1]) || "FAILED".equalsIgnoreCase(parts[1]))) {
            simulateSuccess = false;
        }

        int userId = getUserId();
        Order order = null;
        try {
            int orderId = Integer.parseInt(idOrCode);
            order = orderDAO.getOrderById(orderId);
        } catch (NumberFormatException e) {
            order = orderDAO.getOrderByCode(idOrCode);
        }

        if (order == null) {
            return "550 Order not found: " + idOrCode;
        }

        PaymentDAO.PaymentResult result = paymentDAO.processMockPayment(order.getOrderId(), userId, simulateSuccess, "MOCK_BANKING");
        if (result.success) {
            logDAO.log(userId, clientIp, "PAYMENT_SUCCESS", "Order: " + order.getTransactionCode(), 0, "SUCCESS",
                    result.message + " [PaymentCode: " + result.paymentCode + "]");
            return "200 " + result.message + " (Transaction: " + result.paymentCode + ")";
        } else {
            logDAO.log(userId, clientIp, "PAYMENT_FAILED", "Order: " + order.getTransactionCode(), 0, "FAILED",
                    result.message);
            return "550 " + result.message;
        }
    }

    private String handleMyOrders() {
        if (!session.isAuthenticated()) return "530 Not logged in";
        int userId = getUserId();
        List<Order> orders = orderDAO.getUserOrders(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("150-Your orders list (").append(orders.size()).append("):\n");
        sb.append(String.format("  %-8s %-26s %-10s %-16s %-12s %-20s%n", "ORDER_ID", "CODE", "PLAN", "AMOUNT", "STATUS", "DATE"));
        sb.append("  ------------------------------------------------------------------------------------------------\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Order o : orders) {
            String dateStr = (o.getCreatedAt() != null) ? sdf.format(o.getCreatedAt()) : "";
            sb.append(String.format("  %-8d %-26s %-10s %-16s %-12s %-20s%n",
                    o.getOrderId(), o.getTransactionCode(), o.getPlanName(),
                    o.getFormattedAmount(), o.getStatus(), dateStr));
        }
        sb.append("226 End of orders list.");
        return sb.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return String.format("%.1f GB", (double) bytes / (1024 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%d MB", bytes / (1024 * 1024));
        } else {
            return String.format("%d KB", bytes / 1024);
        }
    }

    // =========================================================================
    // PHASE 6: ADMIN DASHBOARD COMMANDS (SERVER-SIDE RBAC VALIDATION)
    // =========================================================================

    private String handleSetUserStatus(String argument) {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: SETUSERSTATUS <username_or_id> <ACTIVE|LOCKED>";
        }
        String[] parts = argument.trim().split("\\s+");
        if (parts.length < 2) {
            return "501 Syntax error: SETUSERSTATUS <username_or_id> <ACTIVE|LOCKED>";
        }

        String target = parts[0];
        boolean active = "ACTIVE".equalsIgnoreCase(parts[1]) || "TRUE".equalsIgnoreCase(parts[1]);

        User user = null;
        try {
            int uid = Integer.parseInt(target);
            user = userDAO.findById(uid);
        } catch (NumberFormatException e) {
            user = userDAO.findByUsername(target);
        }

        if (user == null) {
            return "550 User not found: " + target;
        }

        // Không cho phép Admin tự khóa tài khoản của chính mình
        if (user.getUserId() == getUserId() && !active) {
            return "550 Cannot lock your own administrator account";
        }

        boolean ok = userDAO.updateUserActive(user.getUserId(), active);
        if (ok) {
            logDAO.log(getUserId(), clientIp, "ADMIN_SET_STATUS", "User: " + user.getUsername(), 0, "SUCCESS", "Set active = " + active);
            return "200 User " + user.getUsername() + " status updated to " + (active ? "ACTIVE" : "LOCKED");
        } else {
            return "550 Failed to update user status.";
        }
    }

    private String handleSetUserRole(String argument) {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: SETUSERROLE <username_or_id> <ROLE_ADMIN|ROLE_USER>";
        }
        String[] parts = argument.trim().split("\\s+");
        if (parts.length < 2) {
            return "501 Syntax error: SETUSERROLE <username_or_id> <ROLE_ADMIN|ROLE_USER>";
        }

        String target = parts[0];
        String roleStr = parts[1].toUpperCase();
        int roleId = roleStr.contains("ADMIN") ? 1 : 2;

        User user = null;
        try {
            int uid = Integer.parseInt(target);
            user = userDAO.findById(uid);
        } catch (NumberFormatException e) {
            user = userDAO.findByUsername(target);
        }

        if (user == null) {
            return "550 User not found: " + target;
        }

        if (user.getUserId() == getUserId() && roleId != 1) {
            return "550 Cannot demote your own administrator role";
        }

        boolean ok = userDAO.updateRole(user.getUserId(), roleId);
        if (ok) {
            logDAO.log(getUserId(), clientIp, "ADMIN_SET_ROLE", "User: " + user.getUsername(), 0, "SUCCESS", "Set role = " + (roleId == 1 ? "ROLE_ADMIN" : "ROLE_USER"));
            return "200 User " + user.getUsername() + " role updated to " + (roleId == 1 ? "ROLE_ADMIN" : "ROLE_USER");
        } else {
            return "550 Failed to update user role.";
        }
    }

    private String handleSetUserQuota(String argument) {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }
        if (argument == null || argument.trim().isEmpty()) {
            return "501 Syntax error: SETUSERQUOTA <username_or_id> <quota_mb>";
        }
        String[] parts = argument.trim().split("\\s+");
        if (parts.length < 2) {
            return "501 Syntax error: SETUSERQUOTA <username_or_id> <quota_mb>";
        }

        String target = parts[0];
        long quotaMb;
        try {
            quotaMb = Long.parseLong(parts[1]);
            if (quotaMb <= 0) return "501 Quota must be greater than 0 MB";
        } catch (NumberFormatException e) {
            return "501 Invalid quota number: " + parts[1];
        }

        User user = null;
        try {
            int uid = Integer.parseInt(target);
            user = userDAO.findById(uid);
        } catch (NumberFormatException e) {
            user = userDAO.findByUsername(target);
        }

        if (user == null) {
            return "550 User not found: " + target;
        }

        long bytes = quotaMb * 1024L * 1024L;
        boolean ok = userDAO.updateUserQuota(user.getUserId(), bytes);
        if (ok) {
            logDAO.log(getUserId(), clientIp, "ADMIN_SET_QUOTA", "User: " + user.getUsername(), 0, "SUCCESS", "Set quota = " + quotaMb + " MB");
            return "200 User " + user.getUsername() + " quota updated to " + quotaMb + " MB (" + bytes + " bytes)";
        } else {
            return "550 Failed to update user quota.";
        }
    }

    private String handleAdminStats() {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }

        UserDAO.SystemStats stats = userDAO.getSystemStats();
        return String.format("200 TotalUsers: %d | Active: %d | Locked: %d | TotalStorage: %s | TotalUsed: %s | TotalFiles: %d | TotalOrders: %d | Revenue: %,.0f VND",
                stats.totalUsers, stats.activeUsers, stats.lockedUsers,
                formatBytes(stats.totalQuotaBytes), formatBytes(stats.totalUsedBytes),
                stats.totalFiles, stats.totalOrders, stats.totalRevenue);
    }

    private String handleAdminAllOrders() {
        if (!session.isAdmin()) {
            return "550 Permission denied: Administrator role required";
        }

        List<Order> orders = orderDAO.getAllOrders();
        StringBuilder sb = new StringBuilder();
        sb.append("150 System orders list (").append(orders.size()).append(" orders):\n");
        sb.append(String.format("  %-8s %-26s %-10s %-16s %-12s %-20s%n", "ORDER_ID", "CODE", "PLAN", "AMOUNT", "STATUS", "DATE"));
        sb.append("  ------------------------------------------------------------------------------------------------\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Order o : orders) {
            String dateStr = (o.getCreatedAt() != null) ? sdf.format(o.getCreatedAt()) : "";
            sb.append(String.format("  %-8d %-26s %-10s %-16s %-12s %-20s%n",
                    o.getOrderId(), o.getTransactionCode(), o.getPlanName(),
                    o.getFormattedAmount(), o.getStatus(), dateStr));
        }
        sb.append("226 End of orders list.");
        return sb.toString();
    }

}

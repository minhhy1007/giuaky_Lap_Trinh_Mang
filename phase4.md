# PHASE 4 — FTP Session + Authentication
## FTP Project · Lập Trình Mạng

> **Mục tiêu Phase 4:**
> Client phải login bằng `USER` + `PASS` trước khi dùng lệnh khác.
> Mỗi Client có Session riêng — login của Client A không ảnh hưởng Client B.

---

## 📚 1. Lý Thuyết Bổ Sung

### 1.1 Authentication là gì?

Quá trình **kiểm tra danh tính** — xác nhận Client là ai và có quyền vào không.

```
FTP Authentication (RFC 959) — 2 bước:

Client → USER admin         Server → 331 Username accepted, enter password
Client → PASS 123           Server → 230 Login successful. Welcome, admin!
                            hoặc → 530 Login incorrect
```

---

### 1.2 Session là gì?

**Tập hợp thông tin** Server cần nhớ về một Client trong suốt thời gian kết nối.

```java
Session {
    state           : UNAUTHENTICATED | WAITING_PASSWORD | AUTHENTICATED
    pendingUsername : "admin"   // vừa gõ USER, chưa xác nhận
    loggedInUser    : "admin"   // sau khi login thành công
}
```

---

### 1.3 Vì sao mỗi ClientHandler phải có Session riêng?

```
❌ Session chung (sai):
   Thread-1: Client A login → session.authenticated = true
   Thread-2: Client B chưa login → session.authenticated = TRUE (bị nhiễm!)

✅ Session riêng (đúng):
   Thread-1: Session_A { authenticated = true  }  → chỉ Client A dùng
   Thread-2: Session_B { authenticated = false }  → chỉ Client B dùng
```

---

### 1.4 Authenticated state là gì?

Trạng thái "đã xác thực" — máy trạng thái (state machine) của một Session:

```
[UNAUTHENTICATED] ──USER──► [WAITING_PASSWORD] ──PASS đúng──► [AUTHENTICATED]
        ▲                           │                                  │
        │                     PASS sai                          LOGOUT / QUIT
        └───────────────────────────┘◄─────────────────────────────────┘
```

| State | Lệnh được phép |
|-------|---------------|
| `UNAUTHENTICATED` | `USER`, `QUIT` |
| `WAITING_PASSWORD` | `PASS`, `QUIT` |
| `AUTHENTICATED` | `PING`, `ECHO`, `LOGOUT`, `QUIT`, `USER` |

---

### 1.5 Tại sao Client A login không làm Client B login?

Vì **Session là object riêng trên heap**, thuộc về ClientHandler của Client đó:

```
Heap (bộ nhớ dùng chung):
  Session@0x001  ← của ClientHandler_A (Thread-1 dùng)
  Session@0x002  ← của ClientHandler_B (Thread-2 dùng)

Thread-1 chỉ trỏ đến Session@0x001
Thread-2 chỉ trỏ đến Session@0x002
→ Không bao giờ chạm nhau
```

---

### 1.6 Login state được lưu ở đâu?

```
TCPServer
  └── ClientHandler (1 per client, Thread riêng)
        └── CommandHandler
              ├── Session  ← LOGIN STATE LƯU Ở ĐÂY
              │     ├── state : AUTHENTICATED
              │     └── loggedInUser : "admin"
              └── UserDatabase (dùng chung, read-only)
```

---

## 🏗️ 2. Cấu Trúc Project Phase 4

```
FTPProject/
├── src/
│   ├── server/
│   │   ├── TCPServer.java        ← Thêm UserDatabase khởi tạo 1 lần
│   │   ├── ClientHandler.java    ← Tạo Session riêng, truyền vào Handler
│   │   ├── CommandHandler.java   ← Thêm USER, PASS, LOGOUT; chặn khi chưa login
│   │   ├── Session.java          ← MỚI: trạng thái auth của 1 kết nối
│   │   └── UserDatabase.java     ← MỚI: danh sách username/password
│   └── client/
│       └── TCPClient.java        ← Không đổi gì
├── phase1.md  phase2.md  phase3.md
└── phase4.md                     ← File này
```

---

## 💻 3. Class Mới

### 3.1 Session.java — State Machine

```java
public class Session {

    public enum State {
        UNAUTHENTICATED,   // chưa gõ USER
        WAITING_PASSWORD,  // đã gõ USER, đang chờ PASS
        AUTHENTICATED      // đã login thành công
    }

    private State state = State.UNAUTHENTICATED;
    private String pendingUsername = null;
    private String loggedInUser    = null;

    // Gọi khi CLIENT gõ USER <name>
    public void setPendingUsername(String username) {
        this.pendingUsername = username;
        this.state = State.WAITING_PASSWORD;
    }

    // Gọi khi PASS đúng
    public void loginSuccess() {
        this.loggedInUser    = this.pendingUsername;
        this.pendingUsername = null;
        this.state = State.AUTHENTICATED;
    }

    // Gọi khi PASS sai hoặc LOGOUT
    public void reset() {
        this.state           = State.UNAUTHENTICATED;
        this.pendingUsername = null;
        this.loggedInUser    = null;
    }

    public boolean isAuthenticated() {
        return state == State.AUTHENTICATED;
    }
}
```

---

### 3.2 UserDatabase.java — Tài khoản test

```java
public class UserDatabase {

    private final Map<String, String> users = new HashMap<>();

    public UserDatabase() {
        users.put("admin", "123");
        users.put("alice", "alice123");
        users.put("bob",   "bob456");
        users.put("guest", "guest");
    }

    public boolean validate(String username, String password) {
        String stored = users.get(username);
        return password.equals(stored);
    }
}
```

| Username | Password |
|----------|---------|
| `admin`  | `123` |
| `alice`  | `alice123` |
| `bob`    | `bob456` |
| `guest`  | `guest` |

---

## 📊 4. Luồng Authentication Chi Tiết

### Login thành công

```
Client                              Server
──────                              ──────
Nhập: USER admin
writer.println("USER admin")
         ──── "USER admin\n" ───────►
                                    parts = ["USER", "admin"]
                                    session.setPendingUsername("admin")
                                    state → WAITING_PASSWORD
                                    return "331 Username accepted..."
         ◄─── "331 Username..." ────
In: "331 Username accepted..."

Nhập: PASS 123
writer.println("PASS 123")
         ──── "PASS 123\n" ─────────►
                                    state == WAITING_PASSWORD ✓
                                    validate("admin","123") → true ✓
                                    session.loginSuccess()
                                    state → AUTHENTICATED
                                    loggedInUser = "admin"
                                    return "230 Login successful..."
         ◄─── "230 Login..." ────────
In: "230 Login successful. Welcome, admin!"
```

---

### Login sai password

```
Client                              Server
──────                              ──────
USER admin    ──────────────────────► 331 Username accepted
PASS wrongpw  ──────────────────────► validate("admin","wrongpw") → false
                                      session.reset()
                                      state → UNAUTHENTICATED
              ◄──────────────────────  530 Login incorrect
```

---

### Gõ lệnh khi chưa login

```
Client                              Server
──────                              ──────
PING          ──────────────────────► session.isAuthenticated() → false
              ◄──────────────────────  530 Not logged in

ECHO hello    ──────────────────────► session.isAuthenticated() → false
              ◄──────────────────────  530 Not logged in
```

---

### LOGOUT — đăng xuất nhưng vẫn kết nối

```
Client                              Server
──────                              ──────
(đã login là "admin")
LOGOUT        ──────────────────────► session.reset()
                                      state → UNAUTHENTICATED
              ◄──────────────────────  230 Goodbye, admin. Logged out...
                                      (kết nối VẪN MỞ)
USER alice    ──────────────────────► có thể login lại bằng user khác!
```

---

### 2 Client độc lập nhau

```
TERMINAL 2 (Client A)              TERMINAL 3 (Client B)
──────────────────────             ──────────────────────
USER admin                         (chưa gõ gì)
PASS 123
230 Login successful

PING                               PING
200 PONG                           530 Not logged in  ← Client B vẫn chưa login!

                                   USER bob
                                   PASS bob456
                                   230 Login successful
                                   PING
                                   200 PONG
```

---

## 🚀 5. Hướng Dẫn Chạy

### Compile

```powershell
javac -d out src\server\TCPServer.java src\server\ClientHandler.java src\server\CommandHandler.java src\server\Session.java src\server\UserDatabase.java src\client\TCPClient.java
```

### Chạy Server

```powershell
java -cp out server.TCPServer
```

```
=== FTP Project - Phase 4: Authentication Server ===
[Server] Đang khởi động...
[Server] UserDatabase khởi tạo: admin/123, alice, bob, guest
[Server] Đang lắng nghe trên port 6789...
```

### Test cơ bản

```
[Client] Nhập: PING
[Client] ← "530 Not logged in"       ← bị chặn!

[Client] Nhập: USER admin
[Client] ← "331 Username accepted, enter password"

[Client] Nhập: PASS 123
[Client] ← "230 Login successful. Welcome, admin!"

[Client] Nhập: PING
[Client] ← "200 PONG"                ← đã login, được phép!

[Client] Nhập: LOGOUT
[Client] ← "230 Goodbye, admin. Logged out..."

[Client] Nhập: PING
[Client] ← "530 Not logged in"       ← bị chặn lại sau logout!
```

---

## 🔑 6. Điểm Khác Biệt Phase 3 vs Phase 4

| | Phase 3 | Phase 4 |
|---|---------|---------|
| **Mọi lệnh** | Dùng được ngay | Phải login trước |
| **Session** | Không có | `Session.java` per client |
| **UserDatabase** | Không có | Shared, read-only |
| **USER / PASS** | Không có | Có, theo RFC 959 |
| **LOGOUT** | Không có | Có (kết nối vẫn mở) |
| **State machine** | Không có | 3 trạng thái rõ ràng |
| **Response 530** | Không có | Trả khi chưa authenticated |

---

## 📌 7. Tóm Tắt Nhanh

```
Authentication   = xác thực danh tính (USER + PASS)
Session          = trạng thái của 1 kết nối (per-client, độc lập)
State Machine    = UNAUTHENTICATED → WAITING_PASSWORD → AUTHENTICATED
UserDatabase     = lưu username/password (read-only, dùng chung)
setPendingUsername() = gọi khi USER gõ vào
loginSuccess()   = gọi khi PASS đúng → state = AUTHENTICATED
reset()          = gọi khi PASS sai hoặc LOGOUT
isAuthenticated()= kiểm tra nhanh state == AUTHENTICATED
331              = "Username OK, cần password"
230              = "Login OK" hoặc "Logged out"
530              = "Not logged in" hoặc "Login incorrect"
503              = "Bad sequence" (PASS trước USER)
```

---

## ❓ 8. Câu Hỏi Bảo Vệ

### 🟢 Câu 1
> Nếu Client gõ `PASS 123` mà chưa gõ `USER` trước, Server trả gì?
> Code nào trong `CommandHandler` xử lý trường hợp này?

### 🟢 Câu 2
> `UserDatabase` được tạo ở đâu? Tại sao chỉ tạo **một lần** mà không tạo mới cho mỗi client?

### 🟡 Câu 3
> Client A login thành công. Client B vẫn `UNAUTHENTICATED`.
> Giải thích tại sao bằng cách chỉ ra hai object `Session` khác nhau ở đâu trong bộ nhớ.

### 🟡 Câu 4
> `LOGOUT` và `QUIT` khác nhau thế nào về mã response và hành vi kết nối?

### 🔴 Câu 5
> `isQuitResponse()` trong Phase 4 kiểm tra `response.startsWith("221 Goodbye")`,
> không chỉ `"221"` như Phase 2.
> Tại sao phải thêm chữ "Goodbye"? Điều gì xảy ra nếu vẫn dùng `startsWith("221")`?

### 🔴 Câu 6
> Phase 4 lưu password dạng **plaintext** trong `UserDatabase`.
> Trong thực tế điều này nguy hiểm thế nào?
> Cần dùng kỹ thuật gì để bảo vệ (ít nhất là tên kỹ thuật)?

---

## 🗺️ 9. Roadmap

| Phase | Nội dung | Trạng thái |
|-------|----------|-----------|
| Phase 1 | TCP Socket cơ bản | ✅ |
| Phase 2 | Application Protocol | ✅ |
| Phase 3 | Multi-Client / Thread Pool | ✅ |
| **Phase 4** | **Session + Authentication** | ✅ **Hoàn thành** |
| Phase 5 | FTP Commands — LIST, RETR, STOR | ⏳ |
| Phase 6 | File Transfer thực sự | ⏳ |
| Phase 7 | GUI | ⏳ |

---

*Tài liệu được tạo ngày 20/09/2026 · FTP Project · Phase 4*

# ĐẶC TẢ HỆ THỐNG — GAME TRẮC NGHIỆM ĐỐI KHÁNG 1V1 (QUIZ ARENA)

> **Đề tài Nhóm 5 — Lập Trình Mạng**
> Học viện Công nghệ Bưu chính Viễn thông (PTIT)
> Java 17 · TCP Socket · Swing · PostgreSQL

---

## 1. TỔNG QUAN DỰ ÁN

### 1.1 Mô tả

Quiz Arena là ứng dụng game trắc nghiệm đối kháng thời gian thực 1 chọi 1 (1v1) trên mạng LAN/Internet, xây dựng hoàn toàn bằng Java 17. Hệ thống cho phép nhiều người chơi đồng thời đăng nhập vào một máy chủ trung tâm, thách đấu nhau theo chủ đề và độ khó, cùng làm 10 câu hỏi trắc nghiệm giới hạn thời gian, với việc chấm điểm và tính thời gian hoàn toàn tại phía server để đảm bảo công bằng tuyệt đối.

### 1.2 Mục tiêu kỹ thuật

| STT | Mục tiêu | Công nghệ áp dụng |
|-----|----------|--------------------|
| 1 | Lập trình mạng Socket TCP nhiều luồng | `java.net.ServerSocket`, `java.net.Socket`, `ExecutorService` |
| 2 | Giao tiếp nhị phân Client–Server thời gian thực | `DataInputStream` / `DataOutputStream` (giao thức tùy chỉnh `Wire`) |
| 3 | Giao diện đồ họa hiện đại | Java Swing + Bảng màu Kahoot-inspired |
| 4 | Bảo mật mật khẩu chuẩn mật mã học | SHA-256 với Salt 16 bytes ngẫu nhiên |
| 5 | Hệ quản trị CSDL quan hệ | PostgreSQL 16+ qua JDBC |
| 6 | Cơ chế dự phòng không gián đoạn | Fallback từ PostgreSQL sang file cục bộ (Fault Tolerance) |

### 1.3 Các bên tham gia (Actors)

```mermaid
graph LR
    A["👤 Người chơi A<br/>(Client Swing)"] -->|TCP Socket| S["🖥️ Server<br/>(Đa luồng)"]
    B["👤 Người chơi B<br/>(Client Swing)"] -->|TCP Socket| S
    C["👤 Người chơi N<br/>(Client Swing)"] -->|TCP Socket| S
    S -->|JDBC| DB["🐘 PostgreSQL"]
    S -.->|Fallback| F["📁 users.dat<br/>questions.txt"]
```

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1 Mô hình kiến trúc tổng quan

Hệ thống áp dụng mô hình **Client–Server 2 tầng (2-Tier)** với kiến trúc **Concurrent Multi-threaded Server**:

```mermaid
graph TB
    subgraph "Tầng Client (Presentation Layer)"
        C1["Client 1<br/>Swing GUI"]
        C2["Client 2<br/>Swing GUI"]
        CN["Client N<br/>Swing GUI"]
    end

    subgraph "Tầng Server (Business Logic + Data Access)"
        subgraph "Xử lý kết nối"
            SS["ServerSocket<br/>Lắng nghe cổng 5050"]
            TP["Thread Pool<br/>(CachedThreadPool)"]
        end
        subgraph "Nghiệp vụ chính"
            AUTH["Xác thực<br/>(Register/Login)"]
            LOBBY["Quản lý Sảnh chờ<br/>(Online/Broadcast)"]
            INV["Quản lý Lời mời<br/>(Invite/Accept/Reject)"]
            MATCH["Quản lý Trận đấu<br/>(Match Engine)"]
            RANK["Xếp hạng & Lịch sử<br/>(Leaderboard/History)"]
        end
        subgraph "Tầng dữ liệu"
            DB_LAYER["DB.java (DAO)"]
            STORE["Model.Store<br/>(Java Serialization)"]
        end
    end

    subgraph "Tầng lưu trữ"
        PG["PostgreSQL<br/>(Primary)"]
        FILE["File System<br/>(Fallback)"]
    end

    C1 & C2 & CN -->|"TCP Socket<br/>Giao thức Wire"| SS
    SS --> TP
    TP --> AUTH & LOBBY & INV & MATCH & RANK
    AUTH & MATCH & RANK --> DB_LAYER
    AUTH & MATCH & RANK --> STORE
    DB_LAYER --> PG
    STORE --> FILE
```

### 2.2 Phân lớp thành phần (Component Diagram)

#### Phía Server (Package `quiz`)

| Thành phần | File | Trách nhiệm |
|------------|------|-------------|
| **Server** | `Server.java` | Lõi nghiệp vụ: lắng nghe TCP, xử lý đa luồng, quản lý sảnh online, lời mời, trận đấu, chấm bài, tính điểm, xếp hạng |
| **Server.Peer** | (inner class) | Đại diện cho một kết nối client: đóng gói Socket + luồng I/O |
| **Server.Match** | (inner class) | Máy trạng thái (State Machine) cho một trận đấu 1v1: bốc đề → gửi đề → nhận bài → chấm điểm → xác định kết quả → tái đấu |
| **Server.Invite** | (record) | Dữ liệu lời mời thách đấu giữa hai người chơi |
| **Wire** | `Wire.java` | Giao thức truyền thông nhị phân tùy chỉnh: đóng gói và giải mã các gói tin qua DataStream |
| **Model** | `Model.java` | Lớp dữ liệu chung: `Question`, `SetData`, `User`, `Result`, `Store` |
| **Bank** | `Bank.java` | Ngân hàng câu hỏi: phân tích, xác thực và quản lý 240 câu hỏi (từ PostgreSQL hoặc file) |
| **Security** | `Security.java` | Băm mật khẩu SHA-256 + Salt, xác thực thời gian hằng số (Constant-time), tương thích ngược |
| **DB** | `DB.java` | Lớp truy cập dữ liệu (DAO): kết nối JDBC PostgreSQL, tự động tạo bảng (Auto-DDL), tự động nạp dữ liệu (Auto-Seed), các phương thức CRUD |

#### Phía Client (Package `quiz` + `quiz.ui`)

| Thành phần | File | Trách nhiệm |
|------------|------|-------------|
| **Client** | `Client.java` | Điều phối giao diện: kết nối Socket, nhận/gửi gói tin, điều hướng màn hình |
| **AuthScreen** | `AuthScreen.java` | Màn hình đăng nhập / đăng ký |
| **LobbyScreen** | `LobbyScreen.java` | Sảnh chờ: danh sách người chơi online, bộ lọc chủ đề/độ khó, thách đấu |
| **MatchScreen** | `MatchScreen.java` | Màn hình thi đấu: 10 câu hỏi, đồng hồ đếm ngược, 4 nút đáp án, emote |
| **ResultDialog** | `ResultDialog.java` | Hộp thoại kết quả: Thắng/Thua/Hòa, tái đấu |
| **ReviewAnswersDialog** | `ReviewAnswersDialog.java` | Xem chi tiết đáp án đúng/sai sau trận |
| **InviteDialog** | `InviteDialog.java` | Hộp thoại nhận lời mời thách đấu (đếm ngược 30s) |
| **LeaderboardDialog** | `LeaderboardDialog.java` | Bảng xếp hạng tổng |
| **HistoryDialog** | `HistoryDialog.java` | Lịch sử trận đấu cá nhân |
| **LeaderboardPanel** / **HistoryPanel** | `LeaderboardPanel.java`, `HistoryPanel.java` | Giao diện dạng panel cho Bảng xếp hạng và Lịch sử |
| **Navigation** | `NavigationBar.java`, `NavigationTab.java` | Thanh điều hướng chuyển đổi tab (Sảnh chờ, BXH, Lịch sử) |
| **Feedback UI** | `ToastManager.java`, `DisconnectOverlay.java` | Hiển thị thông báo (toast) và giao diện mất kết nối mạng |
| **Theme** | `Theme.java` | Hệ thống thiết kế: bảng màu, font chữ, bo tròn |
| **SoundManager** | `SoundManager.java` | Tổng hợp âm thanh PCM thời gian thực (không cần file ngoài) |

---

## 3. GIAO THỨC TRUYỀN THÔNG (WIRE PROTOCOL)

### 3.1 Định dạng gói tin

Giao thức sử dụng **Binary Framed Protocol** tùy chỉnh trên nền TCP, không dựa trên HTTP hay WebSocket:

```
┌──────────────────┬───────────────┬───────────────┬─────┬───────────────┐
│ Số lượng trường   │ Trường 1      │ Trường 2      │ ... │ Trường N      │
│ (int, 4 bytes)   │ (UTF string)  │ (UTF string)  │     │ (UTF string)  │
└──────────────────┴───────────────┴───────────────┴─────┴───────────────┘
```

- **Đọc**: `DataInputStream.readInt()` → lặp `readUTF()` theo số lượng trường.
- **Ghi**: `DataOutputStream.writeInt(n)` → lặp `writeUTF(field)` → `flush()`.
- **Đồng bộ**: Phương thức `send()` được `synchronized` trên luồng output để tránh xung đột khi nhiều thread cùng gửi.

### 3.2 Bảng các loại gói tin (Message Types)

#### Client → Server

| Gói tin | Trường | Mô tả |
|---------|--------|-------|
| `REGISTER` | `[username, password]` | Đăng ký tài khoản mới |
| `LOGIN` | `[username, password]` | Đăng nhập |
| `CHALLENGE` | `[target, topic, level]` | Gửi lời mời thách đấu |
| `ACCEPT` | `[from]` | Chấp nhận lời mời |
| `REJECT` | `[from]` | Từ chối lời mời |
| `SUBMIT` | `[matchId, a0, a1, ..., a9]` | Nộp bài (10 đáp án 0–3 hoặc -1) |
| `REMATCH` | `[YES/NO]` | Bỏ phiếu tái đấu |
| `LEAVE` | `[]` | Rời trận đấu |
| `EMOTE` | `[emoji]` | Gửi biểu cảm cho đối thủ |
| `LEADERBOARD` | `[]` | Yêu cầu bảng xếp hạng |
| `HISTORY` | `[]` | Yêu cầu lịch sử cá nhân |

#### Server → Client

| Gói tin | Trường | Mô tả |
|---------|--------|-------|
| `WELCOME` | `[username, topics]` | Đăng nhập thành công (kèm danh sách chủ đề phân cách `\|`) |
| `INFO` | `[message]` | Thông báo trung tính |
| `ERROR` | `[message]` | Thông báo lỗi |
| `LOBBY` | `[data]` | Cập nhật sảnh: `name;score;status\|name;score;status\|...` |
| `INVITE` | `[from, topic, level, questions, duration]` | Lời mời thách đấu (kèm thời gian trận) |
| `DECLINED` | `[reason]` | Lời mời bị từ chối / hết hạn |
| `MATCH` | `[matchId, left, right, topic, level, setId, duration, q1, c1a, c1b, c1c, c1d, ...]` | Bắt đầu trận (58 trường: 8 header + 10×5 câu hỏi) |
| `SUBMITTED` | `[message]` | Xác nhận server đã nhận bài |
| `RESULT` | `[winner, leftInfo, rightInfo, prompt, a0, a1, ..., a9]` | Kết quả trận (15 trường: 5 thông tin + 10 đáp án đúng) |
| `EMOTE` | `[from, emoji]` | Chuyển tiếp biểu cảm từ đối thủ |
| `CLOSED` | `[reason]` | Trận đấu kết thúc / phòng bị đóng |
| `LEADERBOARD` | `[data]` | Dữ liệu bảng xếp hạng |
| `HISTORY` | `[data]` | Dữ liệu lịch sử cá nhân |

---

## 4. THIẾT KẾ CƠ SỞ DỮ LIỆU

### 4.1 Mô hình quan hệ thực thể (ERD)

```mermaid
erDiagram
    USERS {
        varchar(50) username PK
        varchar(255) password
        int score
        timestamptz created_at
        timestamptz updated_at
    }

    QUESTION_SETS {
        varchar(50) id PK
        varchar(100) topic
        varchar(50) level
        int duration
        timestamptz created_at
    }

    QUESTIONS {
        serial id PK
        varchar(50) set_id FK
        int question_order
        text question_text
        text choice_a
        text choice_b
        text choice_c
        text choice_d
        int answer
    }

    MATCH_RESULTS {
        varchar(100) id PK
        varchar(50) left_player
        varchar(50) right_player
        varchar(100) topic
        varchar(50) level
        varchar(50) set_id
        int left_correct
        int right_correct
        bigint left_ms
        bigint right_ms
        int duration
        varchar(50) winner
        timestamptz created_at
    }

    QUESTION_SETS ||--o{ QUESTIONS : "chứa (1:N)"
    USERS ||--o{ MATCH_RESULTS : "tham gia (left_player)"
    USERS ||--o{ MATCH_RESULTS : "tham gia (right_player)"
    QUESTION_SETS ||--o{ MATCH_RESULTS : "sử dụng bộ đề"
```

### 4.2 Chiến lược lưu trữ kép (Dual Storage Strategy)

```mermaid
graph TD
    START["Server khởi động"] --> CHECK{"Kết nối PostgreSQL<br/>thành công?"}
    CHECK -->|Có| PG_MODE["Chế độ PostgreSQL"]
    CHECK -->|Không| FILE_MODE["Chế độ File dự phòng"]

    PG_MODE --> DDL["Tự động tạo bảng<br/>(Auto-DDL)"]
    DDL --> SEED{"Bảng câu hỏi<br/>trống?"}
    SEED -->|Có| IMPORT["Nạp 240 câu hỏi<br/>từ questions.txt"]
    SEED -->|Không| READY_PG["Sẵn sàng phục vụ<br/>(đọc/ghi PostgreSQL + file)"]
    IMPORT --> MIGRATE{"Bảng users<br/>trống?"}
    MIGRATE -->|Có| MIG_USERS["Chuyển đổi users.dat<br/>vào PostgreSQL"]
    MIGRATE -->|Không| READY_PG
    MIG_USERS --> READY_PG

    FILE_MODE --> LOAD_Q["Đọc questions.txt"]
    LOAD_Q --> LOAD_U["Đọc users.dat"]
    LOAD_U --> READY_F["Sẵn sàng phục vụ<br/>(đọc/ghi file cục bộ)"]
```

> **Nguyên tắc**: Khi PostgreSQL khả dụng, mỗi thao tác ghi (đăng ký, cập nhật điểm, lưu kết quả) được ghi đồng thời vào cả PostgreSQL **và** file `users.dat` để đảm bảo tính nhất quán.

---

## 5. LUỒNG HOẠT ĐỘNG (ACTIVITY FLOWS)

### 5.1 Luồng Đăng ký & Đăng nhập

```mermaid
sequenceDiagram
    participant C as Client (Swing)
    participant S as Server
    participant DB as PostgreSQL

    Note over C: Người dùng nhấn "Đăng ký"
    C->>S: REGISTER [username, password]
    S->>S: Kiểm tra định dạng tên (3-24 ký tự, chữ/số/_)
    S->>S: Kiểm tra mật khẩu >= 4 ký tự
    S->>S: Kiểm tra trùng tên trong Store
    alt Tên đã tồn tại
        S->>C: ERROR ["Tên đã tồn tại"]
    else Hợp lệ
        S->>S: Security.hashPassword(pass) → SHA-256 + Salt 16 bytes
        S->>DB: INSERT INTO users(username, password, score=0)
        S->>S: store.users.put(name, user)
        S->>S: store.save(users.dat)
        S->>C: INFO ["Đăng ký thành công"]
    end

    Note over C: Người dùng nhấn "Đăng nhập"
    C->>S: LOGIN [username, password]
    S->>S: Tìm user trong Store
    alt Không tìm thấy trong Store
        S->>DB: SELECT FROM users WHERE username = ?
    end
    S->>S: Security.verifyPassword(input, storedHash)
    alt Sai mật khẩu
        S->>C: ERROR ["Sai tài khoản hoặc mật khẩu"]
    else Đúng mật khẩu
        alt Mật khẩu dạng cũ (Legacy)
            S->>S: Tự động băm lại (re-hash) với Salt mới
            S->>DB: UPDATE users SET password = newHash
        end
        S->>S: Thêm vào danh sách online
        S->>C: WELCOME [username, "Chủ đề 1|Chủ đề 2|..."]
        S->>S: broadcast() → Gửi LOBBY cho tất cả client online
    end
```

### 5.2 Luồng Thách đấu & Thi đấu 1v1

```mermaid
sequenceDiagram
    participant A as Client A (Người thách)
    participant S as Server
    participant B as Client B (Người được thách)

    Note over A: Chọn đối thủ + chủ đề + độ khó
    A->>S: CHALLENGE [target=B, topic, level]
    S->>S: Kiểm tra: B online, cả hai không đang bận, chủ đề/độ khó hợp lệ
    S->>S: Kiểm tra tồn tại bộ đề phù hợp
    S->>S: Lưu Invite(A→B, topic, level)
    S->>B: INVITE [from=A, topic, level, duration]
    S->>A: INFO ["Đã gửi lời mời đến B"]
    S->>S: Đặt Timer 30 giây (hết hạn tự động)

    alt B chấp nhận (trong 30s)
        B->>S: ACCEPT [from=A]
        S->>S: Xóa Invite, tạo Match(A, B, topic, level)
        S->>S: bank.pick(topic, level, used) → Bốc ngẫu nhiên bộ đề
        S->>S: Ghi nhận thời điểm bắt đầu (System.nanoTime)
        S->>S: Đặt Timer = duration giây (hết giờ tự động)
        S->>A: MATCH [matchId, A, B, topic, level, setId, duration, q1, c1a..c1d, q2, ...]
        S->>B: MATCH [matchId, A, B, topic, level, setId, duration, q1, c1a..c1d, q2, ...]
        S->>S: broadcast() → Cập nhật trạng thái "Đang bận"

        par A và B làm bài đồng thời
            Note over A: A trả lời 10 câu và nộp bài
            A->>S: SUBMIT [matchId, a0, a1, ..., a9]
            S->>S: Chấm điểm A, ghi nhận thời gian A
            S->>A: SUBMITTED ["Đã nhận bài"]
        and
            Note over B: B trả lời 10 câu và nộp bài
            B->>S: SUBMIT [matchId, a0, a1, ..., a9]
            S->>S: Chấm điểm B, ghi nhận thời gian B
            S->>B: SUBMITTED ["Đã nhận bài"]
        end

        S->>S: Cả hai đã nộp → finish()
        S->>S: Xác định người thắng (điểm > hoặc thời gian < nếu hòa điểm)
        S->>S: Cập nhật score (+2 thắng, -1 thua, không < 0)
        S->>DB: UPDATE users, INSERT match_results
        S->>A: RESULT [winner, "A: 7 câu, 23000 ms", "B: 5 câu, 45000 ms", ..., 10 đáp án đúng]
        S->>B: RESULT [winner, "A: 7 câu, 23000 ms", "B: 5 câu, 45000 ms", ..., 10 đáp án đúng]

    else B từ chối hoặc hết hạn 30s
        S->>A: DECLINED ["B từ chối hoặc lời mời hết hạn"]
    end
```

### 5.3 Luồng Tái đấu (Rematch)

```mermaid
sequenceDiagram
    participant A as Client A
    participant S as Server
    participant B as Client B

    Note over A,B: Màn hình kết quả hiện "Chơi tiếp?"
    A->>S: REMATCH [YES]
    S->>A: INFO ["Đang chờ đối thủ đồng ý tái đấu"]
    B->>S: REMATCH [YES]
    S->>S: Cả hai đồng ý → Bốc bộ đề MỚI (không trùng bộ đã chơi)
    S->>A: MATCH [matchId, ..., bộ đề mới]
    S->>B: MATCH [matchId, ..., bộ đề mới]
    Note over A,B: Trận mới bắt đầu ngay lập tức
```

### 5.4 Luồng Xếp hạng (Leaderboard)

```mermaid
graph TD
    REQ["Client yêu cầu LEADERBOARD"] --> CALC["Server tính toán"]
    CALC --> S1["1️⃣ Sắp xếp giảm dần<br/>theo Điểm tích lũy (score)"]
    S1 --> S2["2️⃣ Nếu bằng điểm:<br/>Sắp giảm dần theo Điểm TB đối thủ đã gặp"]
    S2 --> S3["3️⃣ Nếu vẫn bằng:<br/>Sắp tăng dần theo Tỉ lệ thời gian khi thắng"]
    S3 --> S4["4️⃣ Nếu vẫn bằng:<br/>Sắp theo thứ tự chữ cái tên"]
    S4 --> SEND["Gửi LEADERBOARD cho Client"]
```

---

## 6. MÔ HÌNH LUỒNG XỬ LÝ ĐA LUỒNG (CONCURRENCY MODEL)

### 6.1 Sơ đồ luồng (Thread Model)

```mermaid
graph TB
    MAIN["Main Thread<br/>(khởi tạo ServerSocket)"] --> ACCEPT["Vòng lặp accept()<br/>(chờ kết nối mới)"]
    ACCEPT -->|"Mỗi client kết nối"| POOL["CachedThreadPool<br/>(không giới hạn luồng)"]
    POOL --> T1["Worker Thread 1<br/>serve(peer1)"]
    POOL --> T2["Worker Thread 2<br/>serve(peer2)"]
    POOL --> TN["Worker Thread N<br/>serve(peerN)"]

    TIMER["ScheduledThreadPool<br/>(2 luồng)"] --> TMR1["Timer: Hết hạn lời mời<br/>(30 giây)"]
    TIMER --> TMR2["Timer: Hết giờ trận đấu<br/>(duration giây)"]
```

### 6.2 Cơ chế đồng bộ hóa (Synchronization)

| Tài nguyên chia sẻ | Cơ chế bảo vệ | Lý do |
|--------------------|---------------|-------|
| `online` (Map người chơi) | `ConcurrentHashMap` | Nhiều worker thread đồng thời thêm/xóa người chơi |
| `matches` (Map trận đấu) | `ConcurrentHashMap` | Truy cập từ nhiều worker và timer thread |
| `invites` (Map lời mời) | `ConcurrentHashMap` | Thao tác từ cả hai phía người gửi và người nhận |
| `store` (dữ liệu users + results) | `synchronized(store)` | Đảm bảo tính nguyên tử khi đọc/ghi đồng thời |
| `Match` (trạng thái trận đấu) | `synchronized` trên từng phương thức | Hai người chơi nộp bài cùng lúc + timer hết giờ |
| `DataOutputStream` | `synchronized(out)` trong `Wire.send()` | Tránh xen kẽ byte khi gửi gói tin từ nhiều luồng |

---

## 7. BẢO MẬT

### 7.1 Mô hình băm mật khẩu

```mermaid
graph LR
    INPUT["Mật khẩu người dùng<br/>(plaintext)"] --> SALT["Tạo Salt<br/>16 bytes ngẫu nhiên<br/>(SecureRandom)"]
    SALT --> HASH["SHA-256<br/>(salt + password)"]
    HASH --> STORE["Lưu trữ<br/>salt_hex:hash_hex"]

    VERIFY["Xác thực"] --> SPLIT["Tách salt : hash<br/>từ chuỗi lưu trữ"]
    SPLIT --> REHASH["SHA-256<br/>(salt + input)"]
    REHASH --> COMPARE["So sánh Constant-Time<br/>MessageDigest.isEqual()"]
```

### 7.2 Các biện pháp bảo mật

| Biện pháp | Chi tiết |
|-----------|---------|
| **Chống tấn công từ điển (Dictionary Attack)** | Mỗi tài khoản có Salt 16 bytes ngẫu nhiên riêng, không thể dùng bảng Rainbow |
| **Chống tấn công thời gian (Timing Attack)** | Sử dụng `MessageDigest.isEqual()` — so sánh trong thời gian hằng số |
| **Tương thích ngược (Backward Compatibility)** | Mật khẩu cũ dạng plaintext được nhận diện qua `isLegacy()` và tự động nâng cấp (re-hash) ngay khi đăng nhập thành công |
| **Chống gian lận đáp án** | Đáp án đúng **KHÔNG** được gửi cho client trong gói `MATCH`. Chỉ gửi sau khi trận kết thúc trong gói `RESULT` |
| **Tính toán thời gian tại Server** | `System.nanoTime()` được gọi hoàn toàn tại server, client không thể giả mạo thời gian làm bài |

---

## 8. NGÂN HÀNG CÂU HỎI

### 8.1 Cấu trúc dữ liệu

| Thông số | Giá trị |
|----------|---------|
| Tổng số câu hỏi | 240 |
| Số chủ đề | 4 (Mạng máy tính, Lập trình & CTDL, Khoa học & Đời sống, Lịch sử & Địa lý) |
| Số cấp độ | 3 (Dễ: 100s, Trung bình: 150s, Khó: 200s) |
| Số bộ đề mỗi cấp | 2 bộ đề độc lập |
| Số câu mỗi bộ | 10 câu × 4 phương án |
| Tổng bộ đề | 4 × 3 × 2 = **24 bộ đề** |

### 8.2 Thuật toán bốc đề

```
1. Lọc tất cả bộ đề khớp (chủ đề, cấp độ)
2. Loại bỏ các bộ đã dùng trong phòng đấu hiện tại
3. Nếu không còn bộ mới → Reset danh sách đã dùng
4. Chọn ngẫu nhiên 1 bộ từ danh sách còn lại
```

> **Ý nghĩa**: Khi hai người chơi tái đấu liên tục trong cùng phòng, họ sẽ KHÔNG gặp lại bộ đề đã làm cho đến khi đã làm hết tất cả các bộ.

---

## 9. QUY TẮC TÍNH ĐIỂM & XẾP HẠNG

### 9.1 Tính điểm trận đấu

| Tình huống | Kết quả |
|-----------|---------|
| Đúng nhiều câu hơn | **Thắng** |
| Bằng số câu đúng + chênh lệch thời gian > 1 giây | Nộp sớm hơn → **Thắng** |
| Bằng số câu đúng + chênh lệch thời gian ≤ 1 giây | **Hòa** |
| Hết giờ chưa nộp bài | Tính 0 câu đúng, thời gian = duration × 1000 ms |
| Ngắt kết nối giữa trận | Tính 0 câu đúng, thời gian tối đa |

### 9.2 Cập nhật điểm xếp hạng

| Kết quả | Người thắng | Người thua |
|---------|-------------|------------|
| Thắng/Thua | +2 điểm | −1 điểm (không xuống dưới 0) |
| Hòa | Không đổi | Không đổi |

---

## 10. TỔNG KẾT CẤU TRÚC THƯ MỤC DỰ ÁN

```
QuizDoiKhang/
├── pom.xml                     # Cấu hình Maven + PostgreSQL JDBC dependency
├── schema_postgres.sql         # Script DDL tạo 4 bảng PostgreSQL
├── run-server.bat              # Script khởi chạy Server
├── run-client.bat              # Script khởi chạy Client
├── data/
│   ├── db.properties           # Cấu hình kết nối PostgreSQL
│   ├── questions.txt           # Ngân hàng 240 câu hỏi (nguồn dữ liệu gốc)
│   └── users.dat               # File lưu trữ dự phòng (Java Serialization)
├── lib/
│   └── postgresql-42.7.3.jar   # JDBC Driver cho PostgreSQL
├── src/quiz/
│   ├── Server.java             # Server TCP đa luồng (lõi nghiệp vụ)
│   ├── Client.java             # Client Swing (điều phối giao diện)
│   ├── Wire.java               # Giao thức truyền thông nhị phân
│   ├── Model.java              # Các lớp dữ liệu chung
│   ├── Bank.java               # Ngân hàng câu hỏi
│   ├── Security.java           # Bảo mật mật khẩu SHA-256 + Salt
│   ├── DB.java                 # Lớp truy cập dữ liệu PostgreSQL (DAO)
│   └── ui/                     # 31 file giao diện Swing hiện đại
│       ├── Theme.java          #   Hệ thống thiết kế (màu sắc, font)
│       ├── AuthScreen.java     #   Màn hình xác thực
│       ├── LobbyScreen.java    #   Sảnh chờ online
│       ├── MatchScreen.java    #   Màn hình thi đấu
│       ├── NavigationBar.java  #   Thanh điều hướng
│       ├── ToastManager.java   #   Hệ thống thông báo nhanh
│       ├── DisconnectOverlay.java # Giao diện khi mất mạng
│       ├── ResultDialog.java   #   Kết quả & tái đấu
│       ├── SoundManager.java   #   Âm thanh tổng hợp PCM
│       └── ...                 #   (các component UI khác)
└── tests/
    ├── TestPostgreSQL.java     # Kiểm thử kết nối PostgreSQL
    ├── TestE2E.java            # Kiểm thử tích hợp toàn diện
    ├── TestPhase7.java         # Kiểm thử ngân hàng câu hỏi
    └── ...                     # (các bộ test khác)
```

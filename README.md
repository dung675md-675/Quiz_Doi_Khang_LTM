# Game trắc nghiệm đối kháng 1v1

## Giao diện navy và lime

Giao diện được chuyển từ bản thiết kế Quiz Arena trước đó lên bản mã nguồn mới nhất của nhóm: nền navy tối, menu dọc, nút xanh lime, banner đấu trường và bộ chọn chủ đề hai hàng. Phần đăng nhập có nền minh họa và form tối; các màn hình bảng xếp hạng, lịch sử và quản trị dùng cùng bảng màu.

Các thao tác và dữ liệu của bản `duymanh (2)` được giữ lại, gồm kỹ năng chiến thuật, đồng hồ theo loại câu hỏi, tái đấu, bảng xếp hạng, lịch sử mới nhất lên đầu, chi tiết trận và chức năng quản trị. Bổ sung kết bạn, chat riêng giữa bạn bè và chat trong trận. Không bổ sung phòng đấu bằng mã hay báo lỗi câu hỏi.

Ảnh nằm trong `assets/backgrounds`; thông tin nguồn ảnh ở [ARTWORK.md](assets/backgrounds/ARTWORK.md). Maven đóng gói thư mục này vào classpath. Nút **Hiệu ứng: Bật/Tắt** điều khiển chuyển động nền; có thể đặt `-Dquiz.ui.reduceMotion=true` khi chạy client. Hiệu ứng dừng khi màn hình bị ẩn hoặc cửa sổ thu nhỏ.

Biên dịch bằng Git Bash trên Windows, tại thư mục có `src`, `lib` và `assets`:

```bash
javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src/quiz/*.java src/quiz/ui/*.java
```

Sau đó mở hai terminal tại thư mục dự án và chạy:

```bash
java -cp "out;lib/*" quiz.Server 5050 data
```

```bash
java -cp "out;lib/*" quiz.Client 127.0.0.1 5050
```

## Kết bạn và trò chuyện

- Mở **Bạn Bè** ở menu bên trái để tìm tài khoản, gửi lời mời, chấp nhận/từ chối, hủy lời mời hoặc hủy kết bạn. Có thể gửi lời mời cho tài khoản đang ngoại tuyến.
- Sau khi được chấp nhận, chọn **Nhắn tin** trên thẻ bạn bè. Gửi bằng nút **Gửi tin** hoặc **Ctrl+Enter**; lịch sử chia theo từng người, có thông báo chưa đọc và nút tải tin cũ hơn. Chỉ bạn bè đã chấp nhận mới gửi được tin nhắn riêng.
- Trong trận 1v1, bấm **Chat** cạnh nút **Thoát**. Cửa sổ chat không chặn thao tác trả lời; chỉ hai người trong cùng trận nhận được tin. Nút hiển thị số tin mới khi cửa sổ đóng. Chat không bật trong luyện tập với bot.
- Tin nhắn riêng được lưu nguyên tử trong `data/chat.dat`, gồm lịch sử và trạng thái đã đọc. Tin nhắn trận chỉ giữ tối đa 100 tin trong bộ nhớ của trận và không được lưu vĩnh viễn.
- Quan hệ bạn bè lưu cùng `users.dat`, tương thích dữ liệu tài khoản cũ; khi PostgreSQL được bật với tự tạo bảng, server bổ sung bảng `friendships`. Việc xóa tài khoản đồng thời dọn quan hệ và tin nhắn riêng để tài khoản tạo lại không nhận lịch sử cũ.
- Cần biên dịch và khởi động lại cả server lẫn client để dùng các lệnh mới.

Kiểm thử kết bạn/chat bằng dữ liệu tạm, không sửa dữ liệu chơi thật:

```bash
javac -encoding UTF-8 -cp "out;lib/*" -d out tests/*.java
java -ea -cp "out;lib/*" quiz.TestFriends
java -ea -cp "out;lib/*" quiz.TestFriendsProtocol
java -ea -cp "out;lib/*" quiz.TestSocialChat
java -Djava.awt.headless=true -ea -cp "out;lib/*" quiz.TestSocialPanels
java -Djava.awt.headless=true -ea -cp "out;lib/*" quiz.TestSocialLayout
```

Project Java 17 độc lập theo đề tài Nhóm 5: TCP Socket, server nhiều luồng, client Swing, tài khoản, sảnh online, lời mời theo chủ đề và mức độ, cùng bộ đề 10 câu cho hai người, tính điểm và thời gian tại server, tái đấu, lịch sử và bảng xếp hạng.

## Chạy trên Windows 11 và NetBeans

1. Cài JDK 17 trở lên; kiểm tra `java -version` và `javac -version` trong Command Prompt.
2. Giải nén ZIP, chạy `run-server.bat` trước. Server nghe cổng 5050.
3. Mở `run-client.bat` ở hai cửa sổ để thử trên cùng máy. Đăng ký hai tài khoản khác nhau, đăng nhập, chọn người đang rỗi, chủ đề và độ khó rồi thách đấu.
4. Máy khác cùng mạng chạy `run-client.bat 192.168.x.x`, thay bằng IP máy server; cho phép cổng TCP 5050 trên tường lửa máy server.
5. Trong NetBeans: File → Open Project, chọn thư mục có `pom.xml`. Đặt main class `quiz.Server` (arguments: `5050 data`) để chạy server, và `quiz.Client` (arguments: `127.0.0.1 5050`) để chạy client. Working directory cần là thư mục project vì đường dẫn `data` là tương đối.

Có thể dùng Command Prompt:

```bat
javac -encoding UTF-8 -d out src\quiz\*.java
java -cp out quiz.Server 5050 data
java -cp out quiz.Client 127.0.0.1 5050
```

Mở client lần hai ở cửa sổ Command Prompt khác.

## Phân chia mã nguồn

| Người phụ trách | Phần tương ứng |
|---|---|
| Duy | `Server.java` (socket, xử lý nhiều client), `Wire.java` (giao thức) |
| Dũng | `Model.java` (user, dữ liệu), `Bank.java` và `data/questions.txt` (ngân hàng) |
| Chiến | `Server.java`: `Invite`, `Match`, chọn đề, chấm, thời gian, tái đấu |
| Khánh | `Server.java`: `leaderboard`, `history`, lưu kết quả |
| Mạnh | `Client.java`: sảnh, lời mời, câu hỏi, đồng hồ, kết quả |

## Dữ liệu và quy tắc

- `data/questions.txt`: 4 chủ đề × 3 mức × 2 bộ × 10 câu = 240 dòng câu hỏi. Mỗi dòng `setId|topic|level|seconds|correctIndex|question|A;B;C;D`. Thêm chủ đề bằng cách thêm bộ câu hỏi đúng định dạng rồi khởi động lại server. File hiện là **dữ liệu minh họa**; hai bộ mỗi mức có câu hỏi tương tự nhau, nhóm cần biên soạn câu hỏi khác nhau và tăng độ khó thật trước khi nộp.
- `data/users.dat`: được tạo khi đăng ký và ghi lịch sử khi kết thúc trận. Lưu bằng Java serialization; **chưa dùng hệ quản trị CSDL**. Nếu giảng viên yêu cầu SQL, thay `Model.Store` bằng JDBC/MySQL hoặc SQLite, bổ sung bảng Users, Topics, QuestionSets, Questions, MatchHistory, MatchDetails.
- Server tính `submission_time` bằng `System.nanoTime()`, dùng một đồng hồ cho trận. Mỗi câu đúng 1 điểm; bằng điểm và lệch tối đa 1000 ms thì hòa; hết giờ tính 0 điểm nếu chưa nộp. Thắng +2 điểm xếp hạng, thua −1 nhưng không xuống dưới 0. Lời mời hết hạn sau 30 giây.
- Xếp hạng: điểm xếp hạng giảm dần, điểm trung bình hiện tại của đối thủ đã gặp giảm dần, tỉ lệ thời gian trung bình khi thắng tăng dần. Không có trận thắng thì tỉ lệ xếp sau người có thống kê khi các tiêu chí trước bằng nhau.
- Giao thức nhị phân đơn giản: số lượng trường `int`, rồi từng trường `writeUTF/readUTF`. Gói `SUBMIT` gồm mã trận và 10 số đáp án `0..3` hoặc `-1`.

## Kiến trúc và Lộ trình hoàn thiện (7 Phases)

Dự án đã được nâng cấp toàn diện lên kiến trúc **Quiz Arena** hiện đại, áp dụng chuẩn giao diện Kahoot-inspired palette và tối ưu trải nghiệm người dùng (UX):

1. **Phase 1 — Core Design System & Components (`quiz.ui.*`)**:
   - `Theme.java`: Bảng màu chuẩn Kahoot (`RED`, `BLUE`, `YELLOW`, `GREEN`, `PURPLE`, `DEEP_NAVY`), typography Segoe UI, Graphics2D Anti-aliasing.
   - Các UI Component: `ModernButton`, `ModernCard`, `ModernTextField`, `ModernPasswordField`, `ModernBadge`, `ToastNotification`, `ToastManager`.
   - `UIPreview.java`: Màn hình showcase kiểm thử toàn bộ UI.
2. **Phase 2 — Modern Authentication Screen (`AuthScreen.java`)**:
   - Tab chuyển đổi Đăng nhập / Đăng ký mượt mà, xác thực dữ liệu đầu vào thời gian thực, banner thông báo lỗi và thành công rõ ràng (`TestAuth.java`).
3. **Phase 3 — Modern Lobby Screen & Dialogs (`LobbyScreen.java`, `LeaderboardDialog.java`, `HistoryDialog.java`)**:
   - Thanh hồ sơ người chơi với Avatar tròn, cấp độ, bộ lọc chủ đề/độ khó, danh sách người chơi trực quan với thẻ `PlayerCard.java` (`TestLobby.java`).
4. **Phase 4 — Challenge & Invite Experience (`InviteDialog.java`, `WaitingChallengeDialog.java`)**:
   - Hộp thoại thách đấu với đồng hồ đếm ngược 30 giây, thanh tiến trình màu sắc và khả năng hủy lời mời (`TestChallenge.java`).
5. **Phase 5 — In-Game Battle Screen & Result / Rematch Dialog (`MatchScreen.java`, `ResultDialog.java`, `MatchQuestion.java`)**:
   - Màn hình thi đấu Dark Game HUD (Deep Navy `#1F2A44`), HUD Versus 1v1, đồng hồ đếm ngược đổi màu động, thanh điều hướng 10 câu hỏi, 4 nút đáp án Kahoot A/B/C/D, nộp bài sớm, và hộp thoại kết quả vinh danh Thắng/Thua/Hòa chi tiết (`TestMatch.java`).
6. **Phase 6 — Security, Audio Engine, In-Game Reactions & E2E Test Suite**:
   - **Bảo mật (`Security.java`)**: Băm mật khẩu bằng thuật toán **SHA-256 với Salt 16 bytes ngẫu nhiên**, chống tấn công từ điển & timing attack, tương thích ngược và tự động nâng cấp mật khẩu cũ.
   - **Âm thanh (`SoundManager.java`)**: Tổng hợp sóng âm PCM trực tiếp (Java Sound API) không phụ thuộc file ngoài, cung cấp âm thanh click haptic, tích tắc đếm ngược $\le 10$s, chuông thách đấu, fanfare chiến thắng, nhạc thất bại và nút bật/tắt âm thanh toàn cục.
   - **Reaction Emotes thời gian thực**: Cho phép 2 đấu thủ gửi biểu cảm (`🔥`, `⚡`, `😎`, `👏`, `😱`, `💪`) tương tác trực tiếp trong trận đấu.
   - **Kiểm thử tự động toàn diện (`TestE2E.java`)**: Kiểm tra trọn vẹn vòng đời ứng dụng từ băm mật khẩu, đăng nhập, thách đấu, thi đấu, emote, nộp bài, tính điểm, xếp hạng đến tái đấu.
7. **Phase 7 — Ngân hàng câu hỏi 240 câu chuẩn học thuật, Xem chi tiết đáp án & Chế độ Luyện tập đơn (Solo Mode)**:
   - **240 câu hỏi chuẩn hóa (`data/questions.txt`)**: 4 chủ đề lớn (*Mạng máy tính*, *Lập trình & Cấu trúc dữ liệu*, *Khoa học & Đời sống*, *Lịch sử & Địa lý*), 3 cấp độ (Dễ 100s, Trung bình 150s, Khó 200s), mỗi cấp độ 2 bộ đề độc lập (24 bộ đề x 10 câu = 240 câu hỏi 100% không trùng lặp, đầy đủ 4 phương án).
   - **Mở rộng giao thức `RESULT` (15 trường)**: Server gửi kèm 10 key đáp án chính xác của bộ đề (`a[5..14]`), đảm bảo an toàn tuyệt đối và chống gian lận trong lúc làm bài nhưng hiển thị đáp án minh bạch sau khi kết thúc.
   - **Xem chi tiết đáp án (`ReviewAnswersDialog.java`)**: Hộp thoại cuộn mượt mà hiển thị 10 câu hỏi, tô màu xanh lá câu bạn chọn đúng, tô màu đỏ câu chọn sai, gắn cờ ngôi sao vàng cho đáp án chính xác, gắn badge thống kê tỷ lệ % đạt được; tích hợp trực tiếp qua nút `"📝 Xem Đáp Án"` trên `ResultDialog`.
   - **Chế độ Luyện tập đơn (`Solo Practice Mode`)**: Nút `"🎯 Luyện Tập Đơn"` tại Sảnh chờ cho phép người chơi vào ngay phòng luyện tập 10 câu hỏi với Bot AI theo chủ đề và cấp độ đã chọn, không cần chờ đối thủ online. Bot có khả năng phản hồi emote và tính điểm đối chiếu trực tiếp.
   - **Bộ test tự động Phase 7 (`TestPhase7.java`)**: Kiểm tra tính toàn vẹn 240 câu hỏi, giao thức gói RESULT 15 trường và liên kết giao diện review.

8. **Phase 8 — Tích hợp Hệ quản trị CSDL Quan hệ PostgreSQL (`schema_postgres.sql`, `DB.java`, `db.properties`)**:
   - **Mô hình CSDL quan hệ chuẩn hóa**: 4 bảng nghiệp vụ gồm `users` (tài khoản, mật khẩu băm SHA-256 + Salt, điểm), `question_sets` (bộ đề, chủ đề, thời gian), `questions` (240 câu hỏi, 4 đáp án A/B/C/D, foreign key CASCADE), và `match_results` (lịch sử đối kháng 1v1).
   - **Tự động khởi tạo & Seed dữ liệu (Auto-DDL & Auto-Migration)**: Khi server khởi động và kết nối PostgreSQL, hệ thống tự động chạy DDL tạo bảng nếu chưa có, tự động nạp toàn bộ 240 câu hỏi từ `questions.txt` và chuyển đổi tài khoản từ `users.dat` vào PostgreSQL.
   - **Cơ chế Fallback an toàn (Fault Tolerance)**: Khi chưa bật PostgreSQL service hoặc cấu hình sai, Server tự động ghi log cảnh báo và chuyển mượt mà về chế độ lưu trữ file cục bộ (`users.dat`, `questions.txt`), đảm bảo hệ thống không bao giờ bị dừng đột ngột.
   - **Tương thích toàn diện**: Hỗ trợ đầy đủ JDBC Driver `postgresql-42.7.3.jar` trong cả Maven (`pom.xml`) và file thực thi nhanh `run-server.bat`.

## Lệnh kiểm thử tự động

```bat
javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src\quiz\*.java src\quiz\ui\*.java
javac -encoding UTF-8 -cp "out;src;lib/*" -d out tests\*.java
java -cp "out;lib/*" TestPostgreSQL
java -cp "out;lib/*" TestPhase7
java -cp "out;lib/*" TestE2E
java -cp "out;lib/*" TestMatch
java -cp "out;lib/*" quiz.ui.UIPreview
```

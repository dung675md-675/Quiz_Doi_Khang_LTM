package quiz;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public final class DB implements AutoCloseable {
    private final String url;
    private final String user;
    private final String password;
    private final boolean enabled;
    private final boolean autoCreateTables;
    private final boolean autoSeed;
    private volatile boolean connected = false;

    public DB(Path folder) {
        Properties prop = new Properties();
        Path propFile = folder.resolve("db.properties");
        if (!Files.exists(propFile)) {
            propFile = Path.of("db.properties");
        }
        if (Files.exists(propFile)) {
            try (InputStream in = Files.newInputStream(propFile)) {
                prop.load(in);
            } catch (IOException e) {
                System.err.println("[DATABASE] Không đọc được db.properties: " + e.getMessage());
            }
        }

        this.enabled = Boolean.parseBoolean(prop.getProperty("db.enabled", "true"));
        this.url = prop.getProperty("db.url", "jdbc:postgresql://localhost:5432/quiz_db");
        this.user = prop.getProperty("db.user", "postgres");
        this.password = prop.getProperty("db.password", "postgres");
        this.autoCreateTables = Boolean.parseBoolean(prop.getProperty("db.auto_create_tables", "true"));
        this.autoSeed = Boolean.parseBoolean(prop.getProperty("db.auto_seed", "true"));

        if (!enabled) {
            System.out.println("[DATABASE] Đang tắt chế độ CSDL trong cấu hình (db.enabled=false). Sử dụng file cục bộ.");
            return;
        }

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = getConnection()) {
                connected = true;
                System.out.println("[DATABASE] Kết nối PostgreSQL thành công: " + url);
                if (autoCreateTables) {
                    initTables(conn);
                }
                if (autoSeed) {
                    seedData(conn, folder);
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DATABASE] Chưa tìm thấy PostgreSQL JDBC Driver (postgresql.jar). Chuyển về chế độ file dự phòng.");
            connected = false;
        } catch (SQLException e) {
            System.err.println("[DATABASE] Không thể kết nối PostgreSQL tại " + url + " (" + e.getMessage() + ").");
            System.err.println("[DATABASE] -> Tự động chuyển sang chế độ lưu trữ file dự phòng (users.dat, questions.txt).");
            connected = false;
        }
    }

    public boolean isAvailable() {
        return connected;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void initTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY,
                    password VARCHAR(255) NOT NULL,
                    score INT NOT NULL DEFAULT 0 CHECK (score >= 0),
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS question_sets (
                    id VARCHAR(50) PRIMARY KEY,
                    topic VARCHAR(100) NOT NULL,
                    level VARCHAR(50) NOT NULL,
                    duration INT NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id SERIAL PRIMARY KEY,
                    set_id VARCHAR(50) NOT NULL REFERENCES question_sets(id) ON DELETE CASCADE,
                    question_order INT NOT NULL,
                    question_text TEXT NOT NULL,
                    choice_a TEXT NOT NULL,
                    choice_b TEXT NOT NULL,
                    choice_c TEXT NOT NULL,
                    choice_d TEXT NOT NULL,
                    answer INT NOT NULL CHECK (answer >= 0 AND answer <= 3)
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS match_results (
                    id VARCHAR(100) PRIMARY KEY,
                    left_player VARCHAR(50) NOT NULL,
                    right_player VARCHAR(50) NOT NULL,
                    topic VARCHAR(100) NOT NULL,
                    level VARCHAR(50) NOT NULL,
                    set_id VARCHAR(50),
                    left_correct INT NOT NULL DEFAULT 0,
                    right_correct INT NOT NULL DEFAULT 0,
                    left_ms BIGINT NOT NULL DEFAULT 0,
                    right_ms BIGINT NOT NULL DEFAULT 0,
                    duration INT NOT NULL,
                    winner VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
            """);
        }
    }

    private void seedData(Connection conn, Path folder) {
        try {
            // 1. Seed Question Sets & Questions from questions.txt if table is empty
            boolean setsEmpty = false;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM question_sets")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    setsEmpty = true;
                }
            }

            Path qFile = folder.resolve("questions.txt");
            if (setsEmpty && Files.exists(qFile)) {
                System.out.println("[DATABASE] Đang tự động nạp 240 câu hỏi từ questions.txt vào PostgreSQL...");
                List<String> lines = Files.readAllLines(qFile, StandardCharsets.UTF_8);

                Map<String, String[]> metadata = new LinkedHashMap<>();
                Map<String, List<Model.Question>> questionsMap = new LinkedHashMap<>();

                for (String line : lines) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] a = line.split("\\|", 7);
                    if (a.length != 7) continue;
                    String setId = a[0], topic = a[1], level = a[2];
                    int duration = Integer.parseInt(a[3]);
                    int answer = Integer.parseInt(a[4]);
                    String[] choices = a[6].split(";", -1);
                    metadata.putIfAbsent(setId, new String[]{topic, level, String.valueOf(duration)});
                    questionsMap.computeIfAbsent(setId, k -> new ArrayList<>())
                                .add(new Model.Question(a[5], choices, answer));
                }

                conn.setAutoCommit(false);
                String insertSetSql = "INSERT INTO question_sets(id, topic, level, duration) VALUES(?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
                try (PreparedStatement psSet = conn.prepareStatement(insertSetSql)) {
                    for (var entry : metadata.entrySet()) {
                        String[] m = entry.getValue();
                        psSet.setString(1, entry.getKey());
                        psSet.setString(2, m[0]);
                        psSet.setString(3, m[1]);
                        psSet.setInt(4, Integer.parseInt(m[2]));
                        psSet.addBatch();
                    }
                    psSet.executeBatch();
                }

                String insertQuestionSql = "INSERT INTO questions(set_id, question_order, question_text, choice_a, choice_b, choice_c, choice_d, answer) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psQ = conn.prepareStatement(insertQuestionSql)) {
                    for (var entry : questionsMap.entrySet()) {
                        String setId = entry.getKey();
                        List<Model.Question> qList = entry.getValue();
                        for (int i = 0; i < qList.size(); i++) {
                            Model.Question q = qList.get(i);
                            psQ.setString(1, setId);
                            psQ.setInt(2, i + 1);
                            psQ.setString(3, q.text());
                            psQ.setString(4, q.choices()[0]);
                            psQ.setString(5, q.choices()[1]);
                            psQ.setString(6, q.choices()[2]);
                            psQ.setString(7, q.choices()[3]);
                            psQ.setInt(8, q.answer());
                            psQ.addBatch();
                        }
                    }
                    psQ.executeBatch();
                }
                conn.commit();
                conn.setAutoCommit(true);
                System.out.println("[DATABASE] Đã nạp thành công " + metadata.size() + " bộ đề và " + (metadata.size() * 10) + " câu hỏi vào PostgreSQL.");
            }

            // 2. Migrate legacy users & results from users.dat if users table is empty
            boolean usersEmpty = false;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    usersEmpty = true;
                }
            }

            Path uFile = folder.resolve("users.dat");
            if (usersEmpty && Files.exists(uFile)) {
                try {
                    System.out.println("[DATABASE] Đang chuyển đổi dữ liệu người dùng cũ từ users.dat vào PostgreSQL...");
                    Model.Store store = Model.Store.read(uFile.toFile());
                    conn.setAutoCommit(false);
                    String insertUserSql = "INSERT INTO users(username, password, score) VALUES(?, ?, ?) ON CONFLICT (username) DO NOTHING";
                    try (PreparedStatement psUser = conn.prepareStatement(insertUserSql)) {
                        for (Model.User u : store.users.values()) {
                            psUser.setString(1, u.name);
                            psUser.setString(2, u.password);
                            psUser.setInt(3, u.score);
                            psUser.addBatch();
                        }
                        psUser.executeBatch();
                    }

                    String insertMatchSql = "INSERT INTO match_results(id, left_player, right_player, topic, level, set_id, left_correct, right_correct, left_ms, right_ms, duration, winner) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
                    try (PreparedStatement psMatch = conn.prepareStatement(insertMatchSql)) {
                        for (Model.Result r : store.results) {
                            psMatch.setString(1, r.match());
                            psMatch.setString(2, r.left());
                            psMatch.setString(3, r.right());
                            psMatch.setString(4, r.topic());
                            psMatch.setString(5, r.level());
                            psMatch.setString(6, r.setId());
                            psMatch.setInt(7, r.leftCorrect());
                            psMatch.setInt(8, r.rightCorrect());
                            psMatch.setLong(9, r.leftMs());
                            psMatch.setLong(10, r.rightMs());
                            psMatch.setInt(11, r.duration());
                            psMatch.setString(12, r.winner());
                            psMatch.addBatch();
                        }
                        psMatch.executeBatch();
                    }
                    conn.commit();
                    conn.setAutoCommit(true);
                    System.out.println("[DATABASE] Đã nạp thành công " + store.users.size() + " người dùng từ users.dat vào PostgreSQL.");
                } catch (Exception ex) {
                    try { conn.rollback(); conn.setAutoCommit(true); } catch (Exception ignored) {}
                    System.out.println("[DATABASE] Bỏ qua chuyển đổi users.dat cũ: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
            System.err.println("[DATABASE] Lỗi nạp dữ liệu ban đầu: " + e.getMessage());
        }
    }

    // --- USER DAO METHODS ---

    public boolean registerUser(String username, String passwordHash) {
        if (!connected) return false;
        String sql = "INSERT INTO users(username, password, score) VALUES(?, ?, 0)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public Model.User getUser(String username) {
        if (!connected) return null;
        String sql = "SELECT username, password, score FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Model.User u = new Model.User(rs.getString("username"), rs.getString("password"));
                    u.score = rs.getInt("score");
                    return u;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi lấy thông tin user " + username + ": " + e.getMessage());
        }
        return null;
    }

    public void updatePassword(String username, String newPasswordHash) {
        if (!connected) return;
        String sql = "UPDATE users SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi cập nhật mật khẩu cho " + username + ": " + e.getMessage());
        }
    }

    public void updateMatchScores(String winner, String loser) {
        if (!connected) return;
        String winSql = "UPDATE users SET score = score + 2, updated_at = CURRENT_TIMESTAMP WHERE username = ?";
        String loseSql = "UPDATE users SET score = GREATEST(0, score - 1), updated_at = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psWin = conn.prepareStatement(winSql);
                 PreparedStatement psLose = conn.prepareStatement(loseSql)) {
                psWin.setString(1, winner);
                psWin.executeUpdate();

                psLose.setString(1, loser);
                psLose.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi cập nhật điểm sau trận đấu: " + e.getMessage());
        }
    }

    public Map<String, Model.User> getAllUsers() {
        Map<String, Model.User> map = new HashMap<>();
        if (!connected) return map;
        String sql = "SELECT username, password, score FROM users";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Model.User u = new Model.User(rs.getString("username"), rs.getString("password"));
                u.score = rs.getInt("score");
                map.put(u.name, u);
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi nạp danh sách users: " + e.getMessage());
        }
        return map;
    }

    // --- MATCH RESULT DAO METHODS ---

    public void saveResult(Model.Result r) {
        if (!connected) return;
        String sql = "INSERT INTO match_results(id, left_player, right_player, topic, level, set_id, left_correct, right_correct, left_ms, right_ms, duration, winner) " +
                     "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.match());
            ps.setString(2, r.left());
            ps.setString(3, r.right());
            ps.setString(4, r.topic());
            ps.setString(5, r.level());
            ps.setString(6, r.setId());
            ps.setInt(7, r.leftCorrect());
            ps.setInt(8, r.rightCorrect());
            ps.setLong(9, r.leftMs());
            ps.setLong(10, r.rightMs());
            ps.setInt(11, r.duration());
            ps.setString(12, r.winner());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi lưu kết quả trận đấu: " + e.getMessage());
        }
    }

    public List<Model.Result> getAllResults() {
        List<Model.Result> list = new ArrayList<>();
        if (!connected) return list;
        String sql = "SELECT id, left_player, right_player, topic, level, set_id, left_correct, right_correct, left_ms, right_ms, duration, winner FROM match_results ORDER BY created_at ASC";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Model.Result(
                    rs.getString("id"),
                    rs.getString("left_player"),
                    rs.getString("right_player"),
                    rs.getString("topic"),
                    rs.getString("level"),
                    rs.getString("set_id"),
                    rs.getInt("left_correct"),
                    rs.getInt("right_correct"),
                    rs.getLong("left_ms"),
                    rs.getLong("right_ms"),
                    rs.getInt("duration"),
                    rs.getString("winner")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi nạp danh sách kết quả: " + e.getMessage());
        }
        return list;
    }

    public List<Model.Result> getUserHistory(String username) {
        List<Model.Result> list = new ArrayList<>();
        if (!connected) return list;
        String sql = "SELECT id, left_player, right_player, topic, level, set_id, left_correct, right_correct, left_ms, right_ms, duration, winner " +
                     "FROM match_results WHERE left_player = ? OR right_player = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Model.Result(
                        rs.getString("id"),
                        rs.getString("left_player"),
                        rs.getString("right_player"),
                        rs.getString("topic"),
                        rs.getString("level"),
                        rs.getString("set_id"),
                        rs.getInt("left_correct"),
                        rs.getInt("right_correct"),
                        rs.getLong("left_ms"),
                        rs.getLong("right_ms"),
                        rs.getInt("duration"),
                        rs.getString("winner")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi nạp lịch sử trận đấu của " + username + ": " + e.getMessage());
        }
        return list;
    }

    // --- QUESTION SETS & QUESTIONS DAO METHODS ---

    public List<Model.SetData> loadQuestionSets() {
        List<Model.SetData> sets = new ArrayList<>();
        if (!connected) return sets;
        String sqlSets = "SELECT id, topic, level, duration FROM question_sets ORDER BY id";
        String sqlQuestions = "SELECT question_text, choice_a, choice_b, choice_c, choice_d, answer FROM questions WHERE set_id = ? ORDER BY question_order";

        try (Connection conn = getConnection();
             Statement stSets = conn.createStatement();
             ResultSet rsSets = stSets.executeQuery(sqlSets);
             PreparedStatement psQ = conn.prepareStatement(sqlQuestions)) {

            while (rsSets.next()) {
                String setId = rsSets.getString("id");
                String topic = rsSets.getString("topic");
                String level = rsSets.getString("level");
                int duration = rsSets.getInt("duration");

                psQ.setString(1, setId);
                List<Model.Question> questions = new ArrayList<>();
                try (ResultSet rsQ = psQ.executeQuery()) {
                    while (rsQ.next()) {
                        String qText = rsQ.getString("question_text");
                        String[] choices = new String[]{
                            rsQ.getString("choice_a"),
                            rsQ.getString("choice_b"),
                            rsQ.getString("choice_c"),
                            rsQ.getString("choice_d")
                        };
                        int answer = rsQ.getInt("answer");
                        questions.add(new Model.Question(qText, choices, answer));
                    }
                }
                if (!questions.isEmpty()) {
                    sets.add(new Model.SetData(setId, topic, level, duration, List.copyOf(questions)));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi đọc ngân hàng câu hỏi từ PostgreSQL: " + e.getMessage());
        }
        return sets;
    }

    @Override
    public void close() {
        connected = false;
    }
}

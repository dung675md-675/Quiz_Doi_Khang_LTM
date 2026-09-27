-- ============================================================================
-- QUIZ ĐỐI KHÁNG 1V1 - POSTGRESQL DATABASE SCHEMA
-- Hệ cơ sở dữ liệu quan hệ cho đồ án Lập Trình Mạng (Nhóm 5)
-- ============================================================================

-- 1. BẢNG NGƯỜI DÙNG (USERS)
-- Lưu trữ thông tin tài khoản, mật khẩu (SHA-256 + Salt 16 bytes) và điểm số tích lũy
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    score INT NOT NULL DEFAULT 0 CHECK (score >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_score ON users(score DESC);

-- 2. BẢNG BỘ ĐỀ THI (QUESTION_SETS)
-- Mỗi bộ gồm 10 câu hỏi thuộc một chủ đề và độ khó nhất định
CREATE TABLE IF NOT EXISTS question_sets (
    id VARCHAR(50) PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    level VARCHAR(50) NOT NULL,
    duration INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sets_topic_level ON question_sets(topic, level);

-- 3. BẢNG CÂU HỎI TRẮC NGHIỆM (QUESTIONS)
-- Lưu từng câu hỏi, 4 phương án lựa chọn và đáp án đúng (0 = A, 1 = B, 2 = C, 3 = D)
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

CREATE INDEX IF NOT EXISTS idx_questions_set_id ON questions(set_id);

-- 4. BẢNG LỊCH SỬ TRẬN ĐẤU (MATCH_RESULTS)
-- Lưu chi tiết kết quả mỗi trận đấu 1v1
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

CREATE INDEX IF NOT EXISTS idx_matches_left_player ON match_results(left_player);
CREATE INDEX IF NOT EXISTS idx_matches_right_player ON match_results(right_player);
CREATE INDEX IF NOT EXISTS idx_matches_created_at ON match_results(created_at DESC);

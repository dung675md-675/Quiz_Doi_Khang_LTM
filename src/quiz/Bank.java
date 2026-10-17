package quiz;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public final class Bank {
    public static final List<String> STANDARD_TOPICS = List.of(
        "Kỳ quan thế giới",
        "Địa danh và khám phá thế giới",
        "Những con người làm nên lịch sử",
        "Dòng thời gian và những cột mốc",
        "Sự thật thú vị về thế giới",
        "Vũ trụ và khám phá không gian",
        "Văn hóa và di sản nhân loại",
        "Những điều lớn nhất, nhỏ nhất và đầu tiên"
    );

    final List<Model.Question> pool = new ArrayList<>();
    final List<Model.SetData> sets = new ArrayList<>();
    private final Path filePath;

    public Bank(Path path) throws IOException {
        this(path, null);
    }

    public Bank(Path path, DB db) throws IOException {
        this.filePath = path;
        loadFromFile(path);
        if (pool.isEmpty() && db != null && db.isAvailable()) {
            List<Model.SetData> dbSets = db.loadQuestionSets();
            if (!dbSets.isEmpty()) {
                sets.addAll(dbSets);
                for (Model.SetData s : dbSets) {
                    pool.addAll(s.questions());
                }
                System.out.println("[BANK] Đã nạp " + pool.size() + " câu hỏi dự phòng từ PostgreSQL.");
            }
        }
    }

    private void loadFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            System.err.println("[BANK] File không tồn tại: " + path);
            return;
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Map<String, List<Model.Question>> setMap = new LinkedHashMap<>();
        Map<String, String[]> metadata = new LinkedHashMap<>();

        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] a = line.split("\\|", -1);
            try {
                if (a.length >= 8) {
                    // Định dạng: id|topic|level|type|question|choices|answer|explanation[|blanks|fillMap]
                    Model.Question q = parseFields(a);
                    String id = q.id();
                    pool.add(q);
                    setMap.computeIfAbsent(id, k -> new ArrayList<>()).add(q);
                    metadata.putIfAbsent(id, new String[]{q.topic(), q.level(), "100"});
                } else if (a.length == 7) {
                    // Định dạng cũ: setId|topic|level|duration|answer|question|choices
                    String id = a[0].trim();
                    String topic = a[1].trim();
                    String level = a[2].trim();
                    int duration = Integer.parseInt(a[3].trim());
                    int answer = Integer.parseInt(a[4].trim());
                    String qText = a[5].trim();
                    String[] choices = a[6].split(";", -1);

                    if (choices.length != 4) {
                        choices = normalizeChoices(choices);
                    }
                    if (answer < 0 || answer >= choices.length) answer = 0;

                    Model.Question q = new Model.Question(id + "-Q" + pool.size(), topic, level, Model.QuestionType.MULTIPLE_CHOICE, qText, choices, answer, "");
                    pool.add(q);
                    setMap.computeIfAbsent(id, k -> new ArrayList<>()).add(q);
                    metadata.putIfAbsent(id, new String[]{topic, level, String.valueOf(duration)});
                }
            } catch (Exception e) {
                System.err.println("[BANK] Bỏ qua dòng lỗi #" + lineNo + ": " + e.getMessage());
            }
        }

        // Tạo SetData cho các bộ đề cố định nếu có đủ câu
        for (var e : setMap.entrySet()) {
            String[] m = metadata.get(e.getKey());
            if (m != null && !e.getValue().isEmpty()) {
                int dur = 120;
                try { dur = Integer.parseInt(m[2]); } catch (Exception ignored) {}
                sets.add(new Model.SetData(e.getKey(), m[0], m[1], dur, List.copyOf(e.getValue())));
            }
        }

        System.out.println("[BANK] Đã nạp tổng cộng " + pool.size() + " câu hỏi (" + sets.size() + " bộ có sẵn) từ " + path.getFileName());
    }

    private static String[] normalizeChoices(String[] in) {
        String[] out = new String[4];
        Arrays.fill(out, "—");
        for (int i = 0; i < Math.min(4, in.length); i++) {
            out[i] = in[i].trim().isEmpty() ? "—" : in[i].trim();
        }
        return out;
    }

    /** Parse các trường id|topic|level|type|question|choices|answer|explanation[|blanks|fillMap]. */
    public static Model.Question parseFields(String[] a) {
        String id = a[0].trim();
        String topic = a[1].trim();
        String level = a[2].trim();
        Model.QuestionType type = Model.QuestionType.fromString(a[3].trim());
        String qText = a[4].trim();
        String[] choices = a[5].split(";", -1);
        int answer = 0;
        if (type == Model.QuestionType.MULTIPLE_SELECT) {
            int bitmask = 0;
            for (String part : a[6].split("[,;]")) {
                try {
                    int idx = Integer.parseInt(part.trim());
                    if (idx >= 0 && idx < 4) bitmask |= (1 << idx);
                } catch (Exception ignored) {}
            }
            answer = bitmask;
        } else if (type == Model.QuestionType.FILL_IN) {
            answer = 1;
        } else {
            try {
                answer = Integer.parseInt(a[6].trim());
            } catch (Exception ignored) {
                answer = 0;
            }
        }
        String explanation = a.length > 7 ? a[7].trim() : "";

        String[] blanks = null;
        java.util.Map<String,String> fillMap = null;
        if (a.length >= 10) {
            String blanksRaw = a[8].trim();
            String fillMapRaw = a[9].trim();
            if (!blanksRaw.isEmpty()) {
                blanks = blanksRaw.split(";", -1);
                for (int i = 0; i < blanks.length; i++) blanks[i] = blanks[i].trim();
            }
            if (!fillMapRaw.isEmpty()) {
                fillMap = new java.util.LinkedHashMap<>();
                for (String pair : fillMapRaw.split(";", -1)) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) fillMap.put(kv[0].trim(), kv[1].trim());
                }
            }
        }

        if (type == Model.QuestionType.TRUE_FALSE) {
            if (choices.length >= 2) {
                choices = new String[]{choices[0].trim(), choices[1].trim()};
            } else {
                choices = new String[]{"Đúng", "Sai"};
            }
        } else if (type == Model.QuestionType.FILL_IN) {
            List<String> validChoices = new ArrayList<>();
            for (String c : choices) {
                String trimmed = c.trim();
                if (!trimmed.isEmpty()) validChoices.add(trimmed);
            }
            if (validChoices.isEmpty()) validChoices.add("—");
            choices = validChoices.toArray(String[]::new);
        } else if (type != Model.QuestionType.DRAG_AND_DROP && choices.length != 4) {
            choices = normalizeChoices(choices);
        }
        if (type != Model.QuestionType.MULTIPLE_SELECT && type != Model.QuestionType.FILL_IN && type != Model.QuestionType.DRAG_AND_DROP) {
            if (answer < 0 || answer >= choices.length) answer = 0;
        }
        return new Model.Question(id, topic, level, type, qText, choices, answer, explanation, blanks, fillMap);
    }

    /** Trả về {choicesRaw, answerRaw, blanksRaw, fillMapRaw} theo định dạng file/Admin. */
    public static String[] rawFields(Model.Question q) {
        String choicesStr = q.choices() != null ? String.join(";", q.choices()) : "";
        String ansStr;
        if (q.type() == Model.QuestionType.MULTIPLE_SELECT) {
            List<String> idxs = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                if ((q.answer() & (1 << i)) != 0) idxs.add(String.valueOf(i));
            }
            ansStr = idxs.isEmpty() ? "0" : String.join(",", idxs);
        } else {
            ansStr = String.valueOf(q.answer());
        }
        String blanksStr = (q.blanks() != null && q.blanks().length > 0) ? String.join(";", q.blanks()) : "";
        String fmStr = "";
        if (q.fillMap() != null && !q.fillMap().isEmpty()) {
            List<String> pairs = new ArrayList<>();
            for (var e : q.fillMap().entrySet()) pairs.add(e.getKey() + "=" + e.getValue());
            fmStr = String.join(";", pairs);
        }
        return new String[]{choicesStr, ansStr, blanksStr, fmStr};
    }

    /** Kiểm tra chặt chẽ dữ liệu câu hỏi do Admin nhập. Trả về null nếu hợp lệ, ngược lại là thông báo lỗi. */
    public static String validateForAdmin(Model.Question q) {
        if (q == null) return "Dữ liệu câu hỏi không hợp lệ";
        if (q.topic() == null || q.topic().isBlank()) return "Chủ đề không được để trống";
        if (q.text() == null || q.text().isBlank()) return "Nội dung câu hỏi không được để trống";
        if (!Arrays.asList(Model.LEVELS).contains(q.level())) return "Mức độ phải là Dễ / Trung bình / Khó";
        String[] ch = q.choices();
        switch (q.type()) {
            case TRUE_FALSE -> {
                if (ch == null || ch.length != 2) return "Câu Đúng/Sai cần đúng 2 lựa chọn";
                if (q.answer() < 0 || q.answer() > 1) return "Đáp án Đúng/Sai phải là 0 hoặc 1";
            }
            case FILL_IN -> {
                boolean hasMap = q.fillMap() != null && !q.fillMap().isEmpty();
                boolean hasChoice = ch != null && ch.length > 0 && !"—".equals(ch[0]);
                if (!hasMap && !hasChoice) return "Câu điền từ cần ít nhất 1 đáp án (Lựa chọn hoặc FillMap)";
            }
            case DRAG_AND_DROP -> {
                if (ch == null || ch.length < 2) return "Câu kéo thả cần ít nhất 2 thẻ lựa chọn";
                if (q.blanks() == null || q.blanks().length == 0) return "Câu kéo thả cần nhập Blanks";
            }
            case MULTIPLE_SELECT -> {
                if (ch == null || ch.length != 4 || Arrays.stream(ch).anyMatch(c -> c.isBlank() || "—".equals(c)))
                    return "Câu chọn nhiều cần đủ 4 lựa chọn";
                if (q.answer() <= 0 || q.answer() > 15) return "Đáp án chọn nhiều phải dạng 0,2 (chỉ số 0-3)";
            }
            default -> {
                if (ch == null || ch.length != 4 || Arrays.stream(ch).anyMatch(c -> c.isBlank() || "—".equals(c)))
                    return "Câu trắc nghiệm cần đủ 4 lựa chọn A;B;C;D";
                if (q.answer() < 0 || q.answer() > 3) return "Đáp án đúng phải từ 0 đến 3";
            }
        }
        return null;
    }

    public synchronized List<String> topics() {
        Set<String> set = new LinkedHashSet<>(STANDARD_TOPICS);
        for (Model.Question q : pool) {
            if (q.topic() != null && !q.topic().isBlank()) {
                set.add(q.topic());
            }
        }
        return new ArrayList<>(set);
    }

    public synchronized List<Model.Question> getPool() {
        return new ArrayList<>(pool);
    }

    public static boolean isValidQuestion(Model.Question q) {
        if (q == null || q.text() == null || q.text().isBlank() || q.topic() == null || q.topic().isBlank()) {
            return false;
        }
        if (q.choices() == null || q.choices().length == 0) {
            return false;
        }
        return true;
    }

    /**
     * Đếm số câu hỏi hợp lệ theo chủ đề trước khi bắt đầu trận đấu.
     */
    public synchronized int countValidQuestions(String topic, String level) {
        boolean isAllTopics = (topic == null || topic.isBlank()
                || topic.equalsIgnoreCase("Tổng hợp")
                || topic.equalsIgnoreCase("Ngẫu nhiên"));
        int count = 0;
        for (Model.Question q : pool) {
            if (!isValidQuestion(q)) continue;
            if (isAllTopics || q.topic().equalsIgnoreCase(topic)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sinh bộ đề thi đấu gồm 10 câu hỏi chuẩn xác.
     * Nếu số câu hỏi hợp lệ của chủ đề < 10 thì trả về null (không tự mượn chủ đề khác).
     */
    public synchronized Model.SetData generateMatchSet(String topic, String level, Set<String> usedQuestions, Model.ExamConfig config) {
        if (config == null) config = Model.ExamConfig.DEFAULT;
        if (pool.isEmpty()) {
            for (Model.SetData s : sets) pool.addAll(s.questions());
        }
        if (pool.isEmpty()) return null;

        boolean isAllTopics = (topic == null || topic.isBlank() || topic.equalsIgnoreCase("Tổng hợp") || topic.equalsIgnoreCase("Ngẫu nhiên"));
        List<Model.Question> topicPool = isAllTopics
            ? pool.stream().filter(Bank::isValidQuestion).collect(Collectors.toList())
            : pool.stream().filter(q -> isValidQuestion(q) && q.topic().equalsIgnoreCase(topic)).collect(Collectors.toList());

        if (topicPool.size() < config.totalQuestions()) {
            return null;
        }

        List<Model.Question> easyPool = topicPool.stream().filter(q -> q.level().equalsIgnoreCase("Dễ")).collect(Collectors.toList());
        List<Model.Question> medPool = topicPool.stream().filter(q -> q.level().equalsIgnoreCase("Trung bình")).collect(Collectors.toList());
        List<Model.Question> hardPool = topicPool.stream().filter(q -> q.level().equalsIgnoreCase("Khó")).collect(Collectors.toList());

        List<Model.Question> selected = new ArrayList<>();
        Set<String> usedInThisMatch = new HashSet<>();

        // Nếu người chơi chọn mức độ cụ thể, ưu tiên lấy tối đa câu ở mức độ đó trước
        if (level != null && (level.equalsIgnoreCase("Dễ") || level.equalsIgnoreCase("Trung bình") || level.equalsIgnoreCase("Khó"))) {
            List<Model.Question> primaryPool = level.equalsIgnoreCase("Dễ") ? easyPool
                    : (level.equalsIgnoreCase("Khó") ? hardPool : medPool);
            pickDiversified(primaryPool, Math.min(6, primaryPool.size()), usedQuestions, usedInThisMatch, selected, topicPool);
        }

        int remainNeeded = config.totalQuestions() - selected.size();
        if (remainNeeded > 0) {
            pickDiversified(easyPool, Math.min(config.easyCount(), remainNeeded), usedQuestions, usedInThisMatch, selected, topicPool);
            remainNeeded = config.totalQuestions() - selected.size();
        }
        if (remainNeeded > 0) {
            pickDiversified(medPool, Math.min(config.mediumCount(), remainNeeded), usedQuestions, usedInThisMatch, selected, topicPool);
            remainNeeded = config.totalQuestions() - selected.size();
        }
        if (remainNeeded > 0) {
            pickDiversified(hardPool, Math.min(config.hardCount(), remainNeeded), usedQuestions, usedInThisMatch, selected, topicPool);
        }

        // Bổ sung trong cùng chủ đề nếu vẫn chưa đủ 10 câu
        while (selected.size() < config.totalQuestions() && selected.size() < topicPool.size()) {
            List<Model.Question> remaining = topicPool.stream()
                .filter(q -> !usedInThisMatch.contains(q.id()) && !selected.contains(q))
                .collect(Collectors.toList());
            if (remaining.isEmpty()) break;
            Model.Question extra = remaining.get(new Random().nextInt(remaining.size()));
            selected.add(extra);
            usedInThisMatch.add(extra.id());
        }

        if (selected.size() < config.totalQuestions()) {
            return null;
        }

        // Đánh dấu các câu đã dùng để tránh trùng lặp ở ván sau
        if (usedQuestions != null) {
            for (Model.Question q : selected) {
                usedQuestions.add(q.id());
            }
        }

        // Xáo trộn thứ tự 10 câu hỏi để trải nghiệm sinh động
        Collections.shuffle(selected, new Random());

        // Đảo vị trí đáp án trắc nghiệm ngẫu nhiên trên server để cả 2 client cùng nhận
        List<Model.Question> finalQuestions = new ArrayList<>();
        for (Model.Question q : selected) {
            finalQuestions.add(shuffleChoices(q));
        }

        String examId = "EXAM-" + System.currentTimeMillis() % 1000000;
        String examTopic = (topic != null && !topic.isBlank()) ? topic : "Tổng hợp";
        String examLevel = (level != null && !level.isBlank()) ? level : "Tiêu chuẩn (3-4-3)";
        return new Model.SetData(examId, examTopic, examLevel, config.durationSeconds(), List.copyOf(finalQuestions));
    }

    private void pickDiversified(List<Model.Question> subPool, int count, Set<String> globalUsed, Set<String> matchUsed, List<Model.Question> dest, List<Model.Question> fallbackPool) {
        if (count <= 0) return;
        List<Model.Question> available = subPool.stream()
            .filter(q -> !matchUsed.contains(q.id()))
            .collect(Collectors.toList());

        // Ưu tiên câu chưa dùng ở các ván trước
        List<Model.Question> fresh = available.stream()
            .filter(q -> globalUsed == null || !globalUsed.contains(q.id()))
            .collect(Collectors.toList());

        List<Model.Question> source = !fresh.isEmpty() ? fresh : available;
        if (source.isEmpty()) {
            // Mượn từ fallback pool
            source = fallbackPool.stream()
                .filter(q -> !matchUsed.contains(q.id()) && !dest.contains(q))
                .collect(Collectors.toList());
        }

        // Đa dạng hóa QuestionType: sắp xếp ngẫu nhiên và chọn sao cho không trùng type nếu có thể
        Collections.shuffle(source, new Random());
        Set<Model.QuestionType> typesChosen = dest.stream().map(Model.Question::type).collect(Collectors.toSet());

        // Vòng 1: chọn type chưa có
        Iterator<Model.Question> it = source.iterator();
        while (it.hasNext() && count > 0) {
            Model.Question q = it.next();
            if (!typesChosen.contains(q.type()) || source.size() <= count) {
                dest.add(q);
                matchUsed.add(q.id());
                typesChosen.add(q.type());
                it.remove();
                count--;
            }
        }
        // Vòng 2: chọn tiếp nếu chưa đủ
        for (Model.Question q : source) {
            if (count <= 0) break;
            dest.add(q);
            matchUsed.add(q.id());
            count--;
        }
    }

    private Model.Question shuffleChoices(Model.Question q) {
        // Nếu là Đúng/Sai, Kéo thả, Điền từ hoặc Sắp xếp thứ tự thì giữ nguyên thứ tự
        if (q.type() == Model.QuestionType.TRUE_FALSE
                || q.type() == Model.QuestionType.DRAG_AND_DROP
                || q.type() == Model.QuestionType.FILL_IN
                || q.type() == Model.QuestionType.ORDERING) {
            return q;
        }
        String[] orig = q.choices();
        if (orig == null || orig.length != 4) return q;
        int origAns = q.answer();

        if (q.type() == Model.QuestionType.MULTIPLE_SELECT) {
            List<Integer> indices = new ArrayList<>(List.of(0, 1, 2, 3));
            Collections.shuffle(indices, new Random());
            String[] shuffled = new String[4];
            int newMask = 0;
            for (int newIdx = 0; newIdx < 4; newIdx++) {
                int oldIdx = indices.get(newIdx);
                shuffled[newIdx] = orig[oldIdx];
                if ((origAns & (1 << oldIdx)) != 0) {
                    newMask |= (1 << newIdx);
                }
            }
            return new Model.Question(
                q.id(), q.topic(), q.level(), q.type(),
                q.text(), shuffled, newMask, q.explanation(), q.blanks(), q.fillMap()
            );
        }

        if (origAns < 0 || origAns >= orig.length) origAns = 0;
        String correctText = orig[origAns];

        List<String> list = new ArrayList<>(Arrays.asList(orig));
        Collections.shuffle(list, new Random());

        int newAns = list.indexOf(correctText);
        if (newAns < 0) newAns = 0;

        return new Model.Question(
            q.id(), q.topic(), q.level(), q.type(),
            q.text(), list.toArray(new String[0]), newAns, q.explanation(), q.blanks(), q.fillMap()
        );
    }

    /**
     * Lấy bộ đề cho topic và level. Nếu không đủ 10 câu hỏi hợp lệ thì trả về null.
     */
    public synchronized Model.SetData pick(String topic, String level, Set<String> used) {
        if (countValidQuestions(topic, level) < 10) {
            return null;
        }
        Model.SetData generated = generateMatchSet(topic, level, used, Model.ExamConfig.DEFAULT);
        if (generated != null && generated.questions().size() == 10) {
            return generated;
        }
        return null;
    }

    // =====================================================
    // ADMIN QUESTION BANK CRUD & FILE PERSISTENCE
    // =====================================================
    public synchronized String nextQuestionId(String topic) {
        int maxNum = pool.size() + 1;
        while (true) {
            String candidate = String.format("Q-%03d", maxNum);
            boolean exists = false;
            for (Model.Question q : pool) {
                if (q.id().equalsIgnoreCase(candidate)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) return candidate;
            maxNum++;
        }
    }

    public synchronized boolean addQuestion(Model.Question q) {
        if (!isValidQuestion(q)) return false;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.get(i).id().equalsIgnoreCase(q.id())) {
                return false;
            }
        }
        pool.add(q);
        saveToFile();
        return true;
    }

    public synchronized boolean updateQuestion(String id, Model.Question updated) {
        if (id == null || !isValidQuestion(updated)) return false;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.get(i).id().equalsIgnoreCase(id)) {
                pool.set(i, updated);
                saveToFile();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean deleteQuestion(String id) {
        if (id == null || id.isBlank()) return false;
        boolean removed = pool.removeIf(q -> q.id().equalsIgnoreCase(id));
        if (removed) {
            saveToFile();
        }
        return removed;
    }

    public synchronized void saveToFile() {
        if (filePath == null) return;
        try {
            List<String> lines = new ArrayList<>();
            lines.add("# id|topic|level|type|question|choices|answer|explanation|blanks|fillMap");
            for (Model.Question q : pool) {
                String typeStr = q.type() != null ? q.type().name() : "MULTIPLE_CHOICE";
                String choicesStr = q.choices() != null ? String.join(";", q.choices()) : "";
                String ansStr;
                if (q.type() == Model.QuestionType.MULTIPLE_SELECT) {
                    List<String> idxs = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        if ((q.answer() & (1 << i)) != 0) idxs.add(String.valueOf(i));
                    }
                    ansStr = idxs.isEmpty() ? "0" : String.join(",", idxs);
                } else {
                    ansStr = String.valueOf(q.answer());
                }
                String expStr = q.explanation() != null ? q.explanation().replace("|", " ") : "";
                String blanksStr = (q.blanks() != null && q.blanks().length > 0) ? String.join(";", q.blanks()) : "";
                String fmStr = "";
                if (q.fillMap() != null && !q.fillMap().isEmpty()) {
                    List<String> pairs = new ArrayList<>();
                    for (var e : q.fillMap().entrySet()) {
                        pairs.add(e.getKey() + "=" + e.getValue());
                    }
                    fmStr = String.join(";", pairs);
                }
                String row = String.join("|",
                        q.id().replace("|", ""),
                        q.topic().replace("|", ""),
                        q.level().replace("|", ""),
                        typeStr,
                        q.text().replace("|", " ").replace("\n", " "),
                        choicesStr.replace("|", " "),
                        ansStr,
                        expStr,
                        blanksStr,
                        fmStr);
                lines.add(row);
            }
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[BANK] Lỗi lưu file câu hỏi: " + e.getMessage());
        }
    }
}


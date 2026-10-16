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

    public Bank(Path path) throws IOException {
        this(path, null);
    }

    public Bank(Path path, DB db) throws IOException {
        if (db != null && db.isAvailable()) {
            List<Model.SetData> dbSets = db.loadQuestionSets();
            if (!dbSets.isEmpty()) {
                sets.addAll(dbSets);
                for (Model.SetData s : dbSets) {
                    pool.addAll(s.questions());
                }
                System.out.println("[BANK] Đã nạp " + pool.size() + " câu hỏi từ PostgreSQL.");
                return;
            }
        }
        loadFromFile(path);
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
                    String explanation = a[7].trim();

                    // Parse blanks (field 9) and fillMap (field 10) nếu có
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

                    Model.Question q = new Model.Question(id, topic, level, type, qText, choices, answer, explanation, blanks, fillMap);
                    pool.add(q);
                    setMap.computeIfAbsent(id, k -> new ArrayList<>()).add(q);
                    metadata.putIfAbsent(id, new String[]{topic, level, "100"});
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

    private String[] normalizeChoices(String[] in) {
        String[] out = new String[4];
        Arrays.fill(out, "—");
        for (int i = 0; i < Math.min(4, in.length); i++) {
            out[i] = in[i].trim().isEmpty() ? "—" : in[i].trim();
        }
        return out;
    }

    public List<String> topics() {
        Set<String> set = new LinkedHashSet<>(STANDARD_TOPICS);
        for (Model.Question q : pool) {
            if (q.topic() != null && !q.topic().isBlank()) {
                set.add(q.topic());
            }
        }
        return new ArrayList<>(set);
    }

    public List<Model.Question> getPool() {
        return Collections.unmodifiableList(pool);
    }

    /**
     * Sinh bộ đề thi đấu gồm 10 câu hỏi chuẩn xác (3 Dễ, 4 Trung bình, 3 Khó).
     * Đảm bảo:
     * - Không trùng lặp câu hỏi trong trận.
     * - Đa dạng các dạng câu hỏi.
     * - Trộn ngẫu nhiên câu hỏi và đảo vị trí đáp án đồng bộ.
     * - Tự động dự phòng nếu thiếu câu ở mức độ/chủ đề cụ thể để không bao giờ bị lỗi.
     */
    public Model.SetData generateMatchSet(String topic, String level, Set<String> usedQuestions, Model.ExamConfig config) {
        if (config == null) config = Model.ExamConfig.DEFAULT;
        if (pool.isEmpty()) {
            for (Model.SetData s : sets) pool.addAll(s.questions());
        }
        if (pool.isEmpty()) return null;

        boolean isAllTopics = (topic == null || topic.isBlank() || topic.equalsIgnoreCase("Tổng hợp") || topic.equalsIgnoreCase("Ngẫu nhiên"));
        List<Model.Question> topicPool = isAllTopics
            ? new ArrayList<>(pool)
            : pool.stream().filter(q -> q.topic().equalsIgnoreCase(topic)).collect(Collectors.toList());

        if (topicPool.size() < config.totalQuestions()) {
            // Dự phòng: mượn thêm câu hỏi từ các chủ đề khác
            for (Model.Question q : pool) {
                if (!topicPool.contains(q)) {
                    topicPool.add(q);
                }
            }
        }

        List<Model.Question> easyPool = topicPool.stream().filter(q -> q.level().equalsIgnoreCase("Dễ")).collect(Collectors.toList());
        List<Model.Question> medPool = topicPool.stream().filter(q -> q.level().equalsIgnoreCase("Trung bình")).collect(Collectors.toList());
        List<Model.Question> hardPool = topicPool.stream().filter(q -> q.level().equalsIgnoreCase("Khó")).collect(Collectors.toList());

        List<Model.Question> selected = new ArrayList<>();
        Set<String> usedInThisMatch = new HashSet<>();

        pickDiversified(easyPool, config.easyCount(), usedQuestions, usedInThisMatch, selected, topicPool);
        pickDiversified(medPool, config.mediumCount(), usedQuestions, usedInThisMatch, selected, topicPool);
        pickDiversified(hardPool, config.hardCount(), usedQuestions, usedInThisMatch, selected, topicPool);

        // Bổ sung nếu vẫn chưa đủ 10 câu
        while (selected.size() < config.totalQuestions() && selected.size() < topicPool.size()) {
            List<Model.Question> remaining = topicPool.stream()
                .filter(q -> !usedInThisMatch.contains(q.id()) && !selected.contains(q))
                .collect(Collectors.toList());
            if (remaining.isEmpty()) break;
            Model.Question extra = remaining.get(new Random().nextInt(remaining.size()));
            selected.add(extra);
            usedInThisMatch.add(extra.id());
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
        // Nếu là Đúng/Sai hoặc Kéo thả thì giữ nguyên thứ tự
        if (q.type() == Model.QuestionType.TRUE_FALSE || q.type() == Model.QuestionType.DRAG_AND_DROP) {
            return q;
        }
        String[] orig = q.choices();
        if (orig == null || orig.length != 4) return q;
        int origAns = q.answer();
        if (origAns < 0 || origAns >= orig.length) origAns = 0;
        String correctText = orig[origAns];

        List<String> list = new ArrayList<>(Arrays.asList(orig));
        Collections.shuffle(list, new Random());

        int newAns = list.indexOf(correctText);
        if (newAns < 0) newAns = 0;

        return new Model.Question(
            q.id(), q.topic(), q.level(), q.type(),
            q.text(), list.toArray(new String[0]), newAns, q.explanation()
        );
    }

    /**
     * Tương thích ngược: Lấy bộ đề cho topic và level.
     * Mặc định sinh đề mới từ ngân hàng câu hỏi (3 Dễ, 4 Trung bình, 3 Khó).
     */
    public Model.SetData pick(String topic, String level, Set<String> used) {
        Model.SetData generated = generateMatchSet(topic, level, used, Model.ExamConfig.DEFAULT);
        if (generated != null && generated.questions().size() == 10) {
            return generated;
        }
        // Fallback sang bộ đề cố định cũ nếu có
        List<Model.SetData> eligible = sets.stream()
            .filter(s -> s.topic().equalsIgnoreCase(topic) && s.level().equalsIgnoreCase(level))
            .toList();
        if (eligible.isEmpty()) eligible = sets;
        if (eligible.isEmpty()) return null;
        List<Model.SetData> fresh = eligible.stream().filter(s -> !used.contains(s.id())).toList();
        if (fresh.isEmpty()) {
            used.clear();
            fresh = eligible;
        }
        return fresh.get(new Random().nextInt(fresh.size()));
    }
}


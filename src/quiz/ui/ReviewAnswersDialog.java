package quiz.ui;

import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * ReviewAnswersDialog - Modal tra cứu và đối chiếu kết quả làm bài chi tiết:
 * - Hiển thị toàn bộ 10 câu hỏi của trận đấu kèm 4 lựa chọn A/B/C/D.
 * - Đánh dấu trực quan câu đúng (màu xanh lá), câu sai (màu đỏ) và đáp án chính
 * xác của hệ thống.
 * - Thiết kế phong cách Kahoot-inspired hiện đại với ModernCard và cuộn mượt
 * mà.
 */
public class ReviewAnswersDialog extends JDialog {

    public ReviewAnswersDialog(
            Window owner,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers) {
        this(owner, questions, userAnswers, correctAnswers, null);
    }

    public ReviewAnswersDialog(
            Window owner,
            List<MatchQuestion> questions,
            int[] userAnswers,
            int[] correctAnswers,
            String[] userFillInAnswers) {

        super(owner, "Chi Tiết Đáp Án — Quiz Arena", ModalityType.APPLICATION_MODAL);

        setSize(700, 740);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.OFF_WHITE);

        int total = (questions != null) ? questions.size() : 0;
        int correctCount = 0;
        for (int i = 0; i < total; i++) {
            MatchQuestion q = (questions != null && i < questions.size()) ? questions.get(i) : null;
            int uAns = (userAnswers != null && i < userAnswers.length) ? userAnswers[i] : -1;
            int cAns = (correctAnswers != null && i < correctAnswers.length && correctAnswers[i] != -1)
                    ? correctAnswers[i]
                    : ((q != null) ? q.correctAnswer() : -1);
            boolean isCorrect;
            if (q != null && (q.isFillIn() || q.isDragAndDrop())) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
            } else {
                isCorrect = (uAns != -1 && uAns == cAns);
            }
            if (isCorrect) {
                correctCount++;
            }
        }

        // ==========================================
        // 1. TOP HEADER BANNER
        // ==========================================
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Theme.DEEP_NAVY);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(18, 24, 18, 24));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("BẢNG TRA CỨU & XEM LẠI ĐÁP ÁN");
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.WHITE);
        titleRow.add(lblTitle, BorderLayout.WEST);

        int scorePercent = total > 0 ? (correctCount * 100 / total) : 0;
        ModernBadge.Type badgeType = (scorePercent >= 80) ? ModernBadge.Type.READY
                : (scorePercent >= 50 ? ModernBadge.Type.RANK : ModernBadge.Type.BUSY);
        ModernBadge scoreBadge = new ModernBadge("Đúng " + correctCount + "/" + total + " câu (" + scorePercent + "%)",
                badgeType);
        titleRow.add(scoreBadge, BorderLayout.EAST);

        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(6));

        JLabel lblSubtitle = new JLabel("Đối chiếu các câu trả lời bạn đã chọn với đáp án chính xác của hệ thống.");
        lblSubtitle.setFont(Theme.SMALL);
        lblSubtitle.setForeground(new Color(200, 210, 230));
        headerPanel.add(lblSubtitle);

        add(headerPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. SCROLLABLE QUESTIONS LIST
        // ==========================================
        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.OFF_WHITE);
        listContainer.setBorder(new EmptyBorder(16, 20, 16, 20));

        String[] prefixes = { "A", "B", "C", "D" };

        for (int i = 0; i < total; i++) {
            MatchQuestion q = questions.get(i);
            int uAns = (userAnswers != null && i < userAnswers.length) ? userAnswers[i] : -1;
            int cAns = (correctAnswers != null && i < correctAnswers.length && correctAnswers[i] != -1)
                    ? correctAnswers[i]
                    : q.correctAnswer();
            boolean isCorrect;
            boolean isSkipped;
            if (q.isFillIn()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                String typed = (userFillInAnswers != null && i < userFillInAnswers.length) ? userFillInAnswers[i] : null;
                isSkipped = (uAns == -1) && (typed == null || typed.trim().isEmpty());
            } else if (q.isDragAndDrop()) {
                isCorrect = (uAns == 1 || (uAns != -1 && uAns == cAns));
                isSkipped = (uAns == -1);
            } else {
                isCorrect = (uAns != -1 && uAns == cAns);
                isSkipped = (uAns == -1);
            }

            ModernCard card = new ModernCard(new BorderLayout(0, 10));
            card.setCardPadding(14, 16, 14, 16);

            // Card Header: Question number + Status Badge
            JPanel cardHeader = new JPanel(new BorderLayout());
            cardHeader.setOpaque(false);

            String typeBadge = "";
            if (q.type() != null && !q.type().isBlank()) {
                typeBadge = " - " + quiz.Model.QuestionType.fromString(q.type()).getDisplayName();
            }
            JLabel lblQNum = new JLabel("CÂU HỎI " + (i + 1) + " / " + total + typeBadge);
            lblQNum.setFont(Theme.SMALL_BOLD);
            lblQNum.setForeground(Theme.MUTED_GRAY);
            cardHeader.add(lblQNum, BorderLayout.WEST);

            ModernBadge cardBadge;
            if (isSkipped) {
                cardBadge = new ModernBadge("- Chưa chọn", ModernBadge.Type.WARNING);
            } else if (isCorrect) {
                cardBadge = new ModernBadge("Đúng (+1)", ModernBadge.Type.READY);
            } else {
                cardBadge = new ModernBadge("Sai", ModernBadge.Type.BUSY);
            }
            cardHeader.add(cardBadge, BorderLayout.EAST);
            card.add(cardHeader, BorderLayout.NORTH);

            // Question Content Panel
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setOpaque(false);

            JLabel lblQuestionText = new JLabel("<html><body style='width: 500px;'>" + q.text() + "</body></html>");
            lblQuestionText.setFont(Theme.BODY_BOLD);
            lblQuestionText.setForeground(Theme.DARK_TEXT);
            contentPanel.add(lblQuestionText);
            contentPanel.add(Box.createVerticalStrut(10));

            if (q.isDragAndDrop() && q.fillMap() != null) {
                JPanel dndReviewBox = new JPanel(new GridLayout(0, 1, 0, 6));
                dndReviewBox.setOpaque(false);
                int slotIndex = 1;
                for (Map.Entry<String, String> entry : q.fillMap().entrySet()) {
                    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                    row.setOpaque(true);
                    row.setBackground(new Color(0xEC, 0xFD, 0xF5));
                    row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x6E, 0xE7, 0xB7), 1, true),
                        new EmptyBorder(4, 8, 4, 8)
                    ));
                    JLabel lblSlot = new JLabel("Chỗ trống (" + slotIndex++ + ") -> Đáp án đúng: ");
                    lblSlot.setFont(Theme.SMALL_BOLD);
                    lblSlot.setForeground(new Color(0x06, 0x5F, 0x46));
                    JLabel lblAns = new JLabel(entry.getValue());
                    lblAns.setFont(Theme.BODY_BOLD);
                    lblAns.setForeground(new Color(0x04, 0x78, 0x57));
                    row.add(lblSlot);
                    row.add(lblAns);
                    dndReviewBox.add(row);
                }
                contentPanel.add(dndReviewBox);
            } else if (q.isFillIn()) {
                JPanel fillInReviewBox = new JPanel(new GridLayout(0, 1, 0, 6));
                fillInReviewBox.setOpaque(false);

                String typedText = (userFillInAnswers != null && i < userFillInAnswers.length && userFillInAnswers[i] != null && !userFillInAnswers[i].isBlank())
                        ? userFillInAnswers[i]
                        : (isSkipped ? "(Chưa nhập)" : (isCorrect ? ((q.choices() != null && q.choices().length > 0) ? q.choices()[0] : "Đúng") : "(Đã trả lời sai)"));

                JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                userRow.setOpaque(true);
                userRow.setBackground(isCorrect ? new Color(0xE8, 0xF5, 0xE9) : new Color(0xFF, 0xEB, 0xEE));
                userRow.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isCorrect ? Theme.GREEN : Theme.RED, 1, true),
                    new EmptyBorder(6, 12, 6, 12)
                ));
                JLabel lblUserTitle = new JLabel("Câu trả lời của bạn: ");
                lblUserTitle.setFont(Theme.SMALL_BOLD);
                lblUserTitle.setForeground(isCorrect ? new Color(0x1B, 0x5E, 0x20) : new Color(0xB7, 0x1C, 0x1C));
                JLabel lblUserVal = new JLabel(typedText);
                lblUserVal.setFont(Theme.BODY_BOLD);
                lblUserVal.setForeground(isCorrect ? new Color(0x1B, 0x5E, 0x20) : new Color(0xB7, 0x1C, 0x1C));
                userRow.add(lblUserTitle);
                userRow.add(lblUserVal);
                fillInReviewBox.add(userRow);

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                row.setOpaque(true);
                row.setBackground(new Color(0xEC, 0xFD, 0xF5));
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x6E, 0xE7, 0xB7), 1, true),
                    new EmptyBorder(6, 12, 6, 12)
                ));
                JLabel lblTarget = new JLabel("Đáp án chính xác: ");
                lblTarget.setFont(Theme.SMALL_BOLD);
                lblTarget.setForeground(new Color(0x06, 0x5F, 0x46));
                String targetDisplay = (q.choices() != null && q.choices().length > 0) ? q.choices()[0] : "";
                JLabel lblTargetVal = new JLabel(targetDisplay);
                lblTargetVal.setFont(Theme.BODY_BOLD);
                lblTargetVal.setForeground(new Color(0x04, 0x78, 0x57));

                row.add(lblTarget);
                row.add(lblTargetVal);
                fillInReviewBox.add(row);
                contentPanel.add(fillInReviewBox);
            } else {
                // Options List (Multiple Choice / Multiple Select / True False)
                String[] choices = q.choices();
                int maxC = (q.isTrueFalse() || (choices != null && choices.length == 2)) ? 2 : 4;
                JPanel choicesPanel = new JPanel(new GridLayout(maxC, 1, 0, 6));
                choicesPanel.setOpaque(false);

                for (int c = 0; c < maxC; c++) {
                    String choiceText = (choices != null && c < choices.length) ? choices[c] : "";
                    boolean isUserChoice;
                    boolean isCorrectChoice;
                    if (q.isMultipleSelect()) {
                        isUserChoice = (uAns != -1) && ((uAns & (1 << c)) != 0);
                        isCorrectChoice = ((cAns & (1 << c)) != 0);
                    } else {
                        isUserChoice = (uAns == c);
                        isCorrectChoice = (cAns == c);
                    }

                    choicesPanel.add(createChoiceRow(prefixes[c], choiceText, isUserChoice, isCorrectChoice));
                }

                contentPanel.add(choicesPanel);
            }

            // Explanation Box
            if (q.explanation() != null && !q.explanation().isBlank()) {
                JPanel expBox = new JPanel(new BorderLayout(8, 4));
                expBox.setOpaque(true);
                expBox.setBackground(new Color(0xF8, 0xFA, 0xFC));
                expBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0), 1, true),
                    new EmptyBorder(8, 12, 8, 12)));

                JLabel lblExpTitle = new JLabel("Giải thích: ");
                lblExpTitle.setFont(Theme.SMALL_BOLD);
                lblExpTitle.setForeground(Theme.BLUE);

                JLabel lblExpText = new JLabel("<html><body style='width: 440px;'>" + q.explanation() + "</body></html>");
                lblExpText.setFont(Theme.SMALL);
                lblExpText.setForeground(Theme.DARK_TEXT);

                expBox.add(lblExpTitle, BorderLayout.WEST);
                expBox.add(lblExpText, BorderLayout.CENTER);

                contentPanel.add(Box.createVerticalStrut(10));
                contentPanel.add(expBox);
            }

            card.add(contentPanel, BorderLayout.CENTER);

            listContainer.add(card);
            listContainer.add(Box.createVerticalStrut(12));
        }

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM ACTIONS BAR
        // ==========================================
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        bottomBar.setBackground(Theme.WHITE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT));

        ModernButton btnClose = new ModernButton("Đóng", ModernButton.Style.SECONDARY);
        btnClose.setPreferredSize(new Dimension(120, 38));
        btnClose.addActionListener(e -> {
            SoundManager.playClick();
            dispose();
        });
        bottomBar.add(btnClose);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createChoiceRow(String prefix, String text, boolean isUserChoice, boolean isCorrectChoice) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(true);
        row.setBorder(new EmptyBorder(6, 10, 6, 10));

        Color bg = Theme.WHITE;
        Color border = Theme.BORDER_LIGHT;
        Color fg = Theme.DARK_TEXT;
        String statusNote = "";

        if (isUserChoice && isCorrectChoice) {
            bg = new Color(0xE8, 0xF5, 0xE9);
            border = Theme.GREEN;
            fg = new Color(0x1B, 0x5E, 0x20);
            statusNote = "Lựa chọn của bạn (Chính xác)";
        } else if (isUserChoice && !isCorrectChoice) {
            bg = new Color(0xFF, 0xEB, 0xEE);
            border = Theme.RED;
            fg = new Color(0xB7, 0x1C, 0x1C);
            statusNote = "Lựa chọn của bạn";
        } else if (!isUserChoice && isCorrectChoice) {
            bg = new Color(0xED, 0xF7, 0xED);
            border = Theme.GREEN;
            fg = new Color(0x1B, 0x5E, 0x20);
            statusNote = "Đáp án chính xác";
        }

        row.setBackground(bg);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(5, 8, 5, 8)));

        JLabel lblLeft = new JLabel(prefix + ": " + text);
        lblLeft.setFont(Theme.BODY);
        lblLeft.setForeground(fg);
        row.add(lblLeft, BorderLayout.CENTER);

        if (!statusNote.isEmpty()) {
            JLabel lblNote = new JLabel(statusNote);
            lblNote.setFont(Theme.SMALL_BOLD);
            lblNote.setForeground(fg);
            row.add(lblNote, BorderLayout.EAST);
        }

        return row;
    }
}

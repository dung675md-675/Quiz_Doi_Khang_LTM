package quiz.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** The arena's main action, with the existing matchmaking controls kept live. */
public final class ArenaHeroPanel extends ArenaBackgroundPanel {
    private final JButton motionButton;

    public ArenaHeroPanel(JComponent actions, JComponent matchmakingStatus) {
        super(new BorderLayout(12, 0), Scene.LOBBY);
        setOpaque(false);
        setPreferredSize(new Dimension(980, 260));
        setMinimumSize(new Dimension(460, 260));
        setBorder(new EmptyBorder(24, 28, 24, 24));

        JPanel copy = new JPanel(new GridBagLayout());
        copy.setOpaque(false);
        copy.setPreferredSize(new Dimension(366, 212));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel eyebrow = label("ĐỐI KHÁNG TRỰC TUYẾN  /  1v1", 11, Font.BOLD, ArenaPalette.CYAN);
        eyebrow.setIcon(new ArenaIcon(ArenaIcon.Type.BOLT, 14, ArenaPalette.CYAN));
        eyebrow.setIconTextGap(7);
        copy.add(eyebrow, c);
        c.gridy++;
        c.insets = new Insets(12, 0, 0, 0);
        copy.add(label("Sẵn sàng", 37, Font.BOLD, ArenaPalette.TEXT), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        copy.add(label("để tỏa sáng?", 37, Font.BOLD, ArenaPalette.ACCENT), c);
        c.gridy++;
        c.insets = new Insets(8, 0, 0, 0);
        copy.add(label("10 câu hỏi. Một đối thủ. Chinh phục thứ hạng.",
                12, Font.PLAIN, new Color(194, 211, 238)), c);

        JPanel actionRow = new JPanel(new BorderLayout(12, 0));
        actionRow.setOpaque(false);
        actionRow.add(actions, BorderLayout.WEST);
        actionRow.add(matchmakingStatus, BorderLayout.CENTER);
        c.gridy++;
        c.insets = new Insets(17, 0, 0, 0);
        copy.add(actionRow, c);
        add(copy, BorderLayout.WEST);

        JPanel illustration = new JPanel(new BorderLayout());
        illustration.setOpaque(false);
        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        toggleRow.setOpaque(false);
        motionButton = new JButton() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                Theme.setupAntiAliasing(g);
                g.setColor(new Color(8, 16, 37, getModel().isRollover() ? 235 : 190));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g.setColor(hasFocus() ? ArenaPalette.ACCENT : new Color(117, 149, 194, 85));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 13, 13);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        motionButton.setFont(ArenaPalette.font(11, Font.BOLD));
        motionButton.setForeground(new Color(221, 233, 251));
        motionButton.setIcon(new ArenaIcon(ArenaIcon.Type.SETTINGS, 13, new Color(188, 211, 242)));
        motionButton.setIconTextGap(6);
        motionButton.setContentAreaFilled(false);
        motionButton.setOpaque(false);
        motionButton.setFocusPainted(false);
        motionButton.setBorder(new EmptyBorder(7, 10, 7, 10));
        motionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        motionButton.setToolTipText("Bật hoặc tắt chuyển động nền");
        motionButton.getAccessibleContext().setAccessibleName("Hiệu ứng nền sảnh");
        motionButton.addActionListener(e -> {
            setMotionEnabled(!isMotionEnabled());
            updateMotionLabel();
        });
        updateMotionLabel();
        toggleRow.add(motionButton);
        illustration.add(toggleRow, BorderLayout.NORTH);
        add(motionButton, BorderLayout.CENTER);
    }

    @Override public void doLayout() {
        super.doLayout();
        Dimension size = motionButton.getPreferredSize();
        Insets insets = getInsets();
        motionButton.setBounds(getWidth() - insets.right - size.width, insets.top,
                size.width, size.height);
    }

    private void updateMotionLabel() {
        motionButton.setText(isMotionEnabled() ? "Hiệu ứng: Bật" : "Hiệu ứng: Tắt");
    }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setFont(ArenaPalette.font(size, style));
        label.setForeground(color);
        return label;
    }

    @Override public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.setupAntiAliasing(g);
        RoundRectangle2D outline = new RoundRectangle2D.Float(
                0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 28, 28);
        g.clip(outline);
        super.paint(g);
        g.setColor(new Color(125, 164, 222, 95));
        g.setStroke(new BasicStroke(1f));
        g.draw(outline);
        g.dispose();
    }
}

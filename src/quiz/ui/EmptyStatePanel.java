package quiz.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class EmptyStatePanel extends JPanel {

    public EmptyStatePanel(String icon, String title, String subtitle) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE);
        lblTitle.setForeground(Theme.DARK_TEXT);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(subtitle, SwingConstants.CENTER);
        lblSubtitle.setFont(Theme.BODY);
        lblSubtitle.setForeground(Theme.MUTED_GRAY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        if (icon != null && !icon.isBlank()) {
            JLabel lblIcon = new JLabel(icon, SwingConstants.CENTER);
            lblIcon.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));
            lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(lblIcon);
            add(Box.createVerticalStrut(16));
        }
        add(lblTitle);
        add(Box.createVerticalStrut(8));
        add(lblSubtitle);
        add(Box.createVerticalGlue());
    }
}

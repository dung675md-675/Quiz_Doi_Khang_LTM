package quiz.ui;

import java.awt.*;
import javax.swing.*;

public class LevelSelector extends JPanel {
    private String selectedLevel = "Dễ";
    private final ModernButton btnEasy;
    private final ModernButton btnMedium;
    private final ModernButton btnHard;

    public LevelSelector() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setOpaque(false);

        btnEasy = new ModernButton("Dễ", ModernButton.Style.SUCCESS);
        btnMedium = new ModernButton("Trung bình", ModernButton.Style.OUTLINE_GRAY);
        btnHard = new ModernButton("Khó", ModernButton.Style.OUTLINE_GRAY);

        btnEasy.setPreferredSize(new Dimension(80, 36));
        btnMedium.setPreferredSize(new Dimension(110, 36));
        btnHard.setPreferredSize(new Dimension(80, 36));
        
        btnEasy.addActionListener(e -> setLevel("Dễ"));
        btnMedium.addActionListener(e -> setLevel("Trung bình"));
        btnHard.addActionListener(e -> setLevel("Khó"));

        add(btnEasy);
        add(btnMedium);
        add(btnHard);
    }
    
    public void setLevel(String level) {
        selectedLevel = level;
        btnEasy.setStyle(level.equals("Dễ") ? ModernButton.Style.SUCCESS : ModernButton.Style.OUTLINE_GRAY);
        btnMedium.setStyle(level.equals("Trung bình") ? ModernButton.Style.ANSWER_C : ModernButton.Style.OUTLINE_GRAY);
        btnHard.setStyle(level.equals("Khó") ? ModernButton.Style.DANGER : ModernButton.Style.OUTLINE_GRAY);
        
        btnEasy.setSelectedState(level.equals("Dễ"));
        btnMedium.setSelectedState(level.equals("Trung bình"));
        btnHard.setSelectedState(level.equals("Khó"));
    }
    
    public String getSelectedLevel() {
        return selectedLevel;
    }
}

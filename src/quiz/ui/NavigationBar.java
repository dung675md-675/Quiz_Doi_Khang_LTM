package quiz.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

public class NavigationBar extends JPanel {

    private final List<NavigationTab> tabs = new ArrayList<>();
    private Consumer<String> onTabChanged;

    public NavigationBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 24, 0));
        setOpaque(false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT));
    }

    public void setOnTabChanged(Consumer<String> callback) {
        this.onTabChanged = callback;
    }

    public void addTab(String name) {
        NavigationTab tab = new NavigationTab(name);
        tab.setPreferredSize(new Dimension(120, 48));
        
        tab.addActionListener(e -> selectTab(name));
        
        tabs.add(tab);
        add(tab);
        
        if (tabs.size() == 1) {
            tab.setActive(true);
        }
    }

    public void selectTab(String name) {
        for (NavigationTab tab : tabs) {
            if (!tab.isVisible()) continue;
            if (tab.getText().equals(name) && !tab.isActive()) {
                SoundManager.playClick();
                tab.setActive(true);
                if (onTabChanged != null) {
                    onTabChanged.accept(name);
                }
            } else if (!tab.getText().equals(name) && tab.isActive()) {
                tab.setActive(false);
            }
        }
    }

    public void setTabVisible(String name, boolean visible) {
        boolean activeHidden = false;
        for (NavigationTab tab : tabs) {
            if (tab.getText().equals(name)) {
                tab.setVisible(visible);
                if (!visible && tab.isActive()) {
                    tab.setActive(false);
                    activeHidden = true;
                }
            }
        }
        if (activeHidden && !tabs.isEmpty()) {
            for (NavigationTab tab : tabs) {
                if (tab.isVisible()) {
                    selectTab(tab.getText());
                    break;
                }
            }
        }
        revalidate();
        repaint();
    }

    public String getActiveTab() {
        for (NavigationTab tab : tabs) {
            if (tab.isActive()) return tab.getText();
        }
        return tabs.isEmpty() ? "" : tabs.get(0).getText();
    }
}

package quiz.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * NavigationBar — Thanh điều hướng hiện đại với vạch chỉ báo (sliding indicator)
 * trượt mượt mà (smooth animated glide) giữa các tab, hỗ trợ ẩn/hiện tab động và phản hồi tức thì.
 */
public class NavigationBar extends JPanel {

    private final List<NavigationTab> tabs = new ArrayList<>();
    private Consumer<String> onTabChanged;

    // Toạ độ & kích thước của vạch indicator chuyển động
    private float animIndicatorX = -1.0f;
    private float animIndicatorWidth = 0.0f;
    private float targetIndicatorX = 0.0f;
    private float targetIndicatorWidth = 0.0f;

    private Timer slideTimer;

    public NavigationBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 16, 0));
        setOpaque(false);
    }

    public void setOnTabChanged(Consumer<String> callback) {
        this.onTabChanged = callback;
    }

    public void addTab(String name) {
        NavigationTab tab = new NavigationTab(name);

        // Tính toán kích thước tab linh hoạt theo độ dài chữ để không bao giờ bị cắt
        FontMetrics fm = getFontMetrics(Theme.BODY_BOLD);
        int tabW = Math.max(115, fm.stringWidth(name) + 36);
        tab.setPreferredSize(new Dimension(tabW, 48));

        tab.addActionListener(e -> selectTab(name));

        tabs.add(tab);
        add(tab);

        if (tabs.size() == 1) {
            tab.setActive(true);
        }
    }

    public void selectTab(String name) {
        NavigationTab targetTab = null;
        for (NavigationTab tab : tabs) {
            if (!tab.isVisible()) continue;
            if (tab.getText().equals(name)) {
                targetTab = tab;
                if (!tab.isActive()) {
                    SoundManager.playClick();
                    tab.setActive(true);
                    if (onTabChanged != null) {
                        onTabChanged.accept(name);
                    }
                }
            } else if (tab.isActive()) {
                tab.setActive(false);
            }
        }

        if (targetTab != null) {
            animateToTab(targetTab);
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

    private NavigationTab getActiveTabComponent() {
        for (NavigationTab tab : tabs) {
            if (tab.isActive() && tab.isVisible()) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Kích hoạt animation trượt vạch indicator sang tab mục tiêu.
     */
    private void animateToTab(NavigationTab targetTab) {
        if (targetTab == null || targetTab.getWidth() <= 0) {
            return;
        }

        int pad = 12;
        float newTargetX = targetTab.getX() + pad;
        float newTargetW = targetTab.getWidth() - pad * 2;

        targetIndicatorX = newTargetX;
        targetIndicatorWidth = newTargetW;

        if (animIndicatorX < 0) {
            // Lần đầu tiên: đặt ngay toạ độ không cần trượt
            animIndicatorX = targetIndicatorX;
            animIndicatorWidth = targetIndicatorWidth;
            repaint();
            return;
        }

        if (slideTimer != null && slideTimer.isRunning()) {
            slideTimer.stop();
        }

        // Tần số 60 FPS (16ms) mượt mà, nội suy Ease-Out
        slideTimer = new Timer(16, e -> {
            float factor = 0.24f; // Tốc độ trượt êm ái
            float dx = targetIndicatorX - animIndicatorX;
            float dw = targetIndicatorWidth - animIndicatorWidth;

            if (Math.abs(dx) < 0.6f && Math.abs(dw) < 0.6f) {
                animIndicatorX = targetIndicatorX;
                animIndicatorWidth = targetIndicatorWidth;
                ((Timer) e.getSource()).stop();
            } else {
                animIndicatorX += dx * factor;
                animIndicatorWidth += dw * factor;
            }
            repaint();
        });
        slideTimer.start();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        NavigationTab activeTab = getActiveTabComponent();
        if (activeTab != null && activeTab.getWidth() > 0) {
            int pad = 12;
            float newTargetX = activeTab.getX() + pad;
            float newTargetW = activeTab.getWidth() - pad * 2;

            if (animIndicatorX < 0 || (slideTimer == null || !slideTimer.isRunning())) {
                targetIndicatorX = newTargetX;
                targetIndicatorWidth = newTargetW;
                animIndicatorX = newTargetX;
                animIndicatorWidth = newTargetW;
            } else {
                targetIndicatorX = newTargetX;
                targetIndicatorWidth = newTargetW;
            }
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        // 1. Vẽ các tab con trước
        super.paintChildren(g);

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.setupAntiAliasing(g2);

        int w = getWidth();
        int h = getHeight();

        // 2. Đường kẻ ngang phân cách mờ ở đáy thanh tab
        g2.setColor(Theme.BORDER_LIGHT);
        g2.drawLine(0, h - 1, w, h - 1);

        // 3. VẼ VẠCH INDICATOR TRƯỢT MƯỢT MÀ
        if (animIndicatorWidth > 0 && animIndicatorX >= 0) {
            float barH = 3.5f;
            float barY = h - barH - 0.5f;

            // Hào quang tỏa sáng mềm mại phía dưới indicator
            g2.setColor(new Color(0x25, 0x63, 0xEB, 40));
            g2.fill(new RoundRectangle2D.Float(
                animIndicatorX - 1.5f,
                barY - 1.5f,
                animIndicatorWidth + 3.0f,
                barH + 3.0f,
                4,
                4
            ));

            // Vạch chỉ báo chính gradient màu xanh hoàng gia (Royal Blue -> Sky Blue)
            GradientPaint indicatorGrad = new GradientPaint(
                animIndicatorX, barY, new Color(0x1D, 0x4E, 0xD8),
                animIndicatorX + animIndicatorWidth, barY, new Color(0x3B, 0x82, 0xF6)
            );
            g2.setPaint(indicatorGrad);
            g2.fill(new RoundRectangle2D.Float(animIndicatorX, barY, animIndicatorWidth, barH, 3, 3));
        }

        g2.dispose();
    }
}

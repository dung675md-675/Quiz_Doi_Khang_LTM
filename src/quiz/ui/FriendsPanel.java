package quiz.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** Friends, pending invitations and public account search for the current session. */
public final class FriendsPanel extends JPanel {
    private record Person(
            String username,
            String fullName,
            String avatarId,
            int score,
            String rankLabel,
            String presence,
            String relation) {
        Person withRelation(String value) {
            return new Person(username, fullName, avatarId, score, rankLabel, presence, value);
        }

        Person withLobby(String[] fields) {
            int updatedScore = score;
            try {
                updatedScore = Integer.parseInt(fields[1]);
            } catch (NumberFormatException ignored) {
            }
            String state = switch (fields[2]) {
                case "Đang rỗi" -> "READY";
                case "Đang tìm" -> "QUEUED";
                default -> "BUSY";
            };
            return new Person(
                    username,
                    fullName,
                    fields.length > 3 ? fields[3] : avatarId,
                    updatedScore,
                    fields.length > 4 ? fields[4] : rankLabel,
                    state,
                    relation);
        }

        Person offline() {
            return new Person(username, fullName, avatarId, score, rankLabel, "OFFLINE", relation);
        }
    }

    private final ModernTextField searchField = new ModernTextField("Tên tài khoản cần tìm", 22);
    private final JLabel description = text("Kết nối với bạn bè và thách đấu khi cả hai sẵn sàng.", ArenaPalette.MUTED);
    private final JTabbedPane tabs = new ArenaTabbedPane();
    private final JPanel friendsList = createList();
    private final JPanel incomingList = createList();
    private final JPanel outgoingList = createList();
    private final JPanel searchList = createList();
    private final ChatPanel chatPanel = new ChatPanel();
    private final Map<String, Person> friends = new LinkedHashMap<>();
    private final Map<String, Person> requests = new LinkedHashMap<>();
    private final Map<String, Person> searchResults = new LinkedHashMap<>();
    private final Map<String, String[]> lobby = new LinkedHashMap<>();

    private boolean hasPresenceSnapshot;
    private boolean searchSubmitted;
    private BiConsumer<String, String> onAction;
    private Consumer<String> onSearch;
    private Runnable onRefresh;
    private Consumer<String> onChallenge;
    private Runnable onRelationsChanged;
    private Consumer<String> onChat;

    public FriendsPanel() {
        super(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(new EmptyBorder(24, 20, 24, 20));
        searchField.setArenaStyle(true);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(new JLabel(new ArenaIcon(ArenaIcon.Type.FRIENDS, 36, ArenaPalette.ACCENT)), BorderLayout.WEST);
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        JLabel title = text("Cùng bạn bè bứt phá", ArenaPalette.TEXT);
        title.setFont(ArenaPalette.font(25, java.awt.Font.BOLD));
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(description);
        header.add(heading, BorderLayout.CENTER);

        ModernButton refresh = new ModernButton("Làm mới", ModernButton.Style.OUTLINE_GRAY);
        refresh.setArenaStyle(true);
        refresh.setPreferredSize(new Dimension(100, 42));
        refresh.addActionListener(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
            if (searchSubmitted && !searchField.getText().isBlank()) {
                runSearch();
            }
        });
        header.add(refresh, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setOpaque(false);
        searchBar.add(searchField, BorderLayout.CENTER);
        ModernButton search = new ModernButton("Tìm bạn", ModernButton.Style.PRIMARY);
        search.setArenaStyle(true);
        search.setIcon(new ArenaIcon(ArenaIcon.Type.SEARCH, 16, null));
        search.setPreferredSize(new Dimension(116, 40));
        search.addActionListener(e -> runSearch());
        searchField.addActionListener(e -> runSearch());
        searchBar.add(search, BorderLayout.EAST);
        top.add(searchBar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        tabs.setFont(ArenaPalette.font(13, java.awt.Font.BOLD));
        tabs.setForeground(ArenaPalette.TEXT);
        tabs.setBackground(ArenaPalette.BG);
        tabs.addTab("Bạn bè (0)", scroll(friendsList));
        tabs.addTab("Lời mời nhận (0)", scroll(incomingList));
        tabs.addTab("Đã gửi (0)", scroll(outgoingList));
        tabs.addTab("Tìm bạn", scroll(searchList));
        tabs.addTab("Tin nhắn", chatPanel);
        chatPanel.setOnUnreadChanged(count -> tabs.setTitleAt(4,
                count > 0 ? "Tin nhắn (" + count + ")" : "Tin nhắn"));
        add(tabs, BorderLayout.CENTER);
        renderAll();
        ArenaPalette.styleTree(this);
    }

    public void setOnAction(BiConsumer<String, String> callback) {
        onAction = callback;
    }

    public void setOnSearch(Consumer<String> callback) {
        onSearch = callback;
    }

    public void setOnRefresh(Runnable callback) {
        onRefresh = callback;
    }

    public void setOnChallenge(Consumer<String> callback) {
        onChallenge = callback;
    }

    public ChatPanel getChatPanel() { return chatPanel; }

    public void setOnChat(Consumer<String> callback) { onChat = callback; }

    public void openChat(String username) {
        onEdt(() -> {
            tabs.setSelectedIndex(4);
            chatPanel.openConversation(username);
            if (onChat != null) onChat.accept(username);
        });
    }

    public void setOnRelationsChanged(Runnable callback) {
        onRelationsChanged = callback;
    }

    public String getRelationState(String username) {
        if (friends.containsKey(username)) {
            return "FRIEND";
        }
        Person request = requests.get(username);
        if (request != null) {
            return request.relation();
        }
        Person result = searchResults.get(username);
        return result != null ? result.relation() : "NONE";
    }

    public void showIncomingRequests() {
        onEdt(() -> tabs.setSelectedIndex(1));
    }

    public void refreshSearch() {
        onEdt(() -> {
            String query = searchField.getText().trim();
            if (query.length() >= 2 && onSearch != null) {
                onSearch.accept(query);
            } else {
                searchResults.clear();
                searchSubmitted = false;
                renderAll();
            }
        });
    }

    public void updateFriends(String[] rows) {
        String[] snapshot = rows == null ? new String[0] : rows.clone();
        onEdt(() -> {
            friends.clear();
            for (Person person : parse(snapshot)) {
                friends.put(person.username(), person.withRelation("FRIEND"));
            }
            chatPanel.updateContacts(snapshot);
            reconcileSearchRelations();
            renderAll();
            relationsChanged();
        });
    }

    public void updateRequests(String[] rows) {
        String[] snapshot = rows == null ? new String[0] : rows.clone();
        onEdt(() -> {
            requests.clear();
            for (Person person : parse(snapshot)) {
                if ("INCOMING".equals(person.relation()) || "OUTGOING".equals(person.relation())) {
                    requests.put(person.username(), person);
                }
            }
            reconcileSearchRelations();
            renderAll();
            relationsChanged();
        });
    }

    public void updateSearchResults(String[] rows) {
        String[] snapshot = rows == null ? new String[0] : rows.clone();
        onEdt(() -> {
            searchSubmitted = true;
            searchResults.clear();
            for (Person person : parse(snapshot)) {
                searchResults.put(person.username(), person);
            }
            renderAll();
            relationsChanged();
        });
    }

    public void updatePresence(String lobbyData) {
        onEdt(() -> {
            lobby.clear();
            if (lobbyData != null && !lobbyData.isBlank()) {
                for (String row : lobbyData.split("\\|")) {
                    String[] fields = row.split(";", -1);
                    if (fields.length >= 3 && !fields[0].isBlank()) {
                        lobby.put(fields[0], fields);
                    }
                }
            }
            hasPresenceSnapshot = true;
            renderAll();
        });
    }

    public void clearSession() {
        onEdt(() -> {
            friends.clear();
            requests.clear();
            searchResults.clear();
            lobby.clear();
            hasPresenceSnapshot = false;
            searchSubmitted = false;
            searchField.setText("");
            searchField.setError(false);
            chatPanel.clearSession();
            tabs.setSelectedIndex(0);
            renderAll();
            relationsChanged();
        });
    }

    private void runSearch() {
        String query = searchField.getText().trim();
        searchField.setError(query.length() < 2);
        if (query.length() < 2) {
            description.setText("Nhập ít nhất 2 ký tự trong tên tài khoản để tìm bạn.");
            searchField.requestFocusInWindow();
            return;
        }
        if (onSearch != null) {
            searchSubmitted = true;
            tabs.setSelectedIndex(3);
            onSearch.accept(query);
        }
    }

    private void reconcileSearchRelations() {
        searchResults.replaceAll((username, person) -> {
            if (friends.containsKey(username)) {
                return person.withRelation("FRIEND");
            }
            Person request = requests.get(username);
            return person.withRelation(request != null ? request.relation() : "NONE");
        });
    }

    private void relationsChanged() {
        if (onRelationsChanged != null) {
            onRelationsChanged.run();
        }
    }

    private void renderAll() {
        List<Person> friendRows = sorted(friends.values().stream().toList());
        List<Person> incoming = sorted(requests.values().stream()
                .filter(person -> "INCOMING".equals(person.relation())).toList());
        List<Person> outgoing = sorted(requests.values().stream()
                .filter(person -> "OUTGOING".equals(person.relation())).toList());
        List<Person> results = sorted(searchResults.values().stream().toList());

        tabs.setTitleAt(0, "Bạn bè (" + friendRows.size() + ")");
        tabs.setTitleAt(1, "Lời mời nhận (" + incoming.size() + ")");
        tabs.setTitleAt(2, "Đã gửi (" + outgoing.size() + ")");
        int online = (int) friendRows.stream().filter(person -> !"OFFLINE".equals(person.presence())).count();
        description.setText(friendRows.isEmpty()
                ? "Tìm người chơi để gửi lời mời và cùng nhau thách đấu."
                : online + " / " + friendRows.size() + " bạn đang trực tuyến");

        renderList(friendsList, friendRows, "Bạn chưa có bạn bè", "Tìm tên tài khoản hoặc kết bạn từ sảnh người chơi.");
        renderList(incomingList, incoming, "Chưa có lời mời mới", "Lời mời vẫn được lưu khi bạn ngoại tuyến.");
        renderList(outgoingList, outgoing, "Chưa gửi lời mời nào", "Những lời mời đang chờ xác nhận sẽ xuất hiện ở đây.");
        renderList(searchList, results,
                searchSubmitted ? "Không tìm thấy người chơi" : "Tìm bạn cùng chơi",
                searchSubmitted ? "Hãy thử một tên tài khoản khác." : "Nhập tên tài khoản ở phía trên rồi bấm Tìm bạn.");
    }

    private List<Person> sorted(List<Person> people) {
        List<Person> result = new ArrayList<>();
        for (Person person : people) {
            Person visible = person;
            if (hasPresenceSnapshot) {
                String[] fields = lobby.get(person.username());
                visible = fields != null ? person.withLobby(fields) : person.offline();
            }
            result.add(visible.withRelation(getRelationState(person.username())));
        }
        result.sort(Comparator.comparingInt((Person person) -> presenceOrder(person.presence()))
                .thenComparing(Person::username, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static int presenceOrder(String presence) {
        return switch (presence) {
            case "READY" -> 0;
            case "QUEUED" -> 1;
            case "BUSY" -> 2;
            default -> 3;
        };
    }

    private void renderList(JPanel list, List<Person> people, String emptyTitle, String emptyText) {
        list.removeAll();
        if (people.isEmpty()) {
            EmptyStatePanel empty = new EmptyStatePanel("", emptyTitle, emptyText);
            empty.setArenaStyle(true);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
            list.add(empty);
        } else {
            for (Person person : people) {
                list.add(createPersonCard(person));
                list.add(Box.createVerticalStrut(8));
            }
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent createPersonCard(Person person) {
        ModernCard card = new ModernCard(new BorderLayout(12, 0));
        card.setCardBackground(ArenaPalette.SURFACE);
        card.setBorderColor(ArenaPalette.BORDER);
        card.setHasShadow(false);
        card.setCardPadding(14, 18, 14, 18);
        card.setCornerRadius(18);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.setPreferredSize(new Dimension(700, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(new AvatarIcon(person.avatarId()), BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel name = text(person.username(), ArenaPalette.TEXT);
        name.setFont(ArenaPalette.font(14, java.awt.Font.BOLD));
        name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        name.setToolTipText(person.username());
        details.add(name);
        if (!person.fullName().isBlank()) {
            JLabel fullName = text(person.fullName(), ArenaPalette.MUTED);
            fullName.setFont(Theme.SMALL);
            fullName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            fullName.setToolTipText(person.fullName());
            details.add(fullName);
        }
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        ModernBadge presence = new ModernBadge(presenceText(person.presence()), presenceBadge(person.presence()));
        presence.setArenaStyle(true);
        badges.add(presence);
        JLabel score = text(person.rankLabel() + " · " + person.score() + " điểm", ArenaPalette.MUTED);
        score.setFont(Theme.SMALL);
        badges.add(score);
        details.add(badges);
        card.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 0, 8, 0));
        actions.setOpaque(false);
        switch (person.relation()) {
            case "FRIEND" -> {
                actions.setLayout(new GridLayout(2, 2, 6, 6));
                actions.add(button("Nhắn tin", ModernButton.Style.PRIMARY,
                        () -> openChat(person.username())));
                ModernButton challenge = button("Thách đấu", ModernButton.Style.EMERALD, () -> {
                    if (onChallenge != null) {
                        onChallenge.accept(person.username());
                    }
                });
                challenge.setEnabled("READY".equals(person.presence()));
                challenge.setToolTipText("READY".equals(person.presence())
                        ? "Mời bạn vào trận đấu"
                        : "Bạn này cần trực tuyến và sẵn sàng để thách đấu");
                actions.add(challenge);
                actions.add(button("Hủy kết bạn", ModernButton.Style.OUTLINE_GRAY, () -> {
                    int answer = JOptionPane.showConfirmDialog(
                            this,
                            "Hủy kết bạn với " + person.username() + "?",
                            "Hủy kết bạn",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (answer == JOptionPane.YES_OPTION) {
                        dispatch("FRIEND_REMOVE", person.username());
                    }
                }));
            }
            case "INCOMING" -> {
                actions.add(button("Chấp nhận", ModernButton.Style.SUCCESS,
                        () -> dispatch("FRIEND_ACCEPT", person.username())));
                actions.add(button("Từ chối", ModernButton.Style.OUTLINE_GRAY,
                        () -> dispatch("FRIEND_REJECT", person.username())));
            }
            case "OUTGOING" -> actions.add(button("Hủy lời mời", ModernButton.Style.OUTLINE_GRAY,
                    () -> dispatch("FRIEND_CANCEL", person.username())));
            default -> actions.add(button("Kết bạn", ModernButton.Style.PRIMARY,
                    () -> dispatch("FRIEND_REQUEST", person.username())));
        }
        if ("FRIEND".equals(person.relation())) {
            for (Component child : actions.getComponents()) child.setPreferredSize(new Dimension(112, 32));
        }
        JPanel actionWrapper = new JPanel(new GridBagLayout());
        actionWrapper.setOpaque(false);
        actionWrapper.add(actions);
        card.add(actionWrapper, BorderLayout.EAST);
        return card;
    }

    private void dispatch(String command, String target) {
        if (onAction != null) {
            SoundManager.playClick();
            onAction.accept(command, target);
        }
    }

    private static ModernButton button(String label, ModernButton.Style style, Runnable action) {
        ModernButton button = new ModernButton(label, style);
        button.setArenaStyle(true);
        button.setPreferredSize(new Dimension(112, 38));
        button.setFont(Theme.BODY_BOLD);
        button.addActionListener(e -> action.run());
        return button;
    }

    private static String presenceText(String presence) {
        return switch (presence) {
            case "READY" -> "Sẵn sàng";
            case "QUEUED" -> "Đang tìm trận";
            case "BUSY" -> "Đang đấu";
            default -> "Ngoại tuyến";
        };
    }

    private static ModernBadge.Type presenceBadge(String presence) {
        return switch (presence) {
            case "READY" -> ModernBadge.Type.READY;
            case "QUEUED" -> ModernBadge.Type.WARNING;
            case "BUSY" -> ModernBadge.Type.BUSY;
            default -> ModernBadge.Type.INFO;
        };
    }

    private static List<Person> parse(String[] rows) {
        List<Person> people = new ArrayList<>();
        for (String row : rows) {
            if (row == null || row.isBlank()) {
                continue;
            }
            String[] fields = row.split("\t", -1);
            if (fields.length < 7 || fields[0].isBlank()) {
                continue;
            }
            int score = 0;
            try {
                score = Integer.parseInt(fields[3]);
            } catch (NumberFormatException ignored) {
            }
            people.add(new Person(fields[0], fields[1], fields[2], score, fields[4], fields[5], fields[6]));
        }
        return people;
    }

    private static JPanel createList() {
        JPanel list = new FriendList();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setBorder(new EmptyBorder(14, 12, 12, 12));
        return list;
    }

    private static final class FriendList extends JPanel implements javax.swing.Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return Math.max(18, visible.height - 30);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static JScrollPane scroll(JPanel list) {
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setBackground(ArenaPalette.BG);
        scroll.getViewport().setBackground(ArenaPalette.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getHorizontalScrollBar().setUnitIncrement(18);
        ArenaPalette.styleScrollBar(scroll.getVerticalScrollBar());
        ArenaPalette.styleScrollBar(scroll.getHorizontalScrollBar());
        return scroll;
    }

    private static JLabel text(String value, Color color) {
        JLabel label = new JLabel(value);
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setFont(Theme.BODY);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static final class AvatarIcon extends JComponent {
        private final String avatarId;

        AvatarIcon(String avatarId) {
            this.avatarId = avatarId;
            setPreferredSize(new Dimension(44, 44));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            Theme.setupAntiAliasing(copy);
            int size = Math.min(getWidth(), getHeight());
            AvatarPicker.paintAvatar(copy, avatarId, 0, (getHeight() - size) / 2, size);
            copy.dispose();
        }
    }
}

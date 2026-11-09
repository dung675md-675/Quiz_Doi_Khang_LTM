package quiz;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import quiz.ui.FriendsPanel;
import quiz.ui.PlayerCard;
import quiz.ui.SoundManager;

/** Headless UI behavior checks; no windows, audio, network or real user data. */
public final class TestFriendsPanel {
    private static int checks;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.prefs.PreferencesFactory", MemoryPreferencesFactory.class.getName());
        SoundManager.setMuted(true);
        try {
            SwingUtilities.invokeAndWait(() -> {
                testActionsAndPresence();
                testSearchAndSessionIsolation();
                testLobbyCardFriendStates();
            });
            System.out.println("PASS TestFriendsPanel: " + checks + " checks");
        } finally {
            SoundManager.shutdown();
        }
    }

    private static void testActionsAndPresence() {
        FriendsPanel panel = new FriendsPanel();
        List<String> actions = new ArrayList<>();
        List<String> challenges = new ArrayList<>();
        panel.setOnAction((command, target) -> actions.add(command + ":" + target));
        panel.setOnChallenge(challenges::add);
        panel.updateFriends(new String[] {row("bob", "READY", "FRIEND")});
        panel.updateRequests(new String[] {
            row("carol", "OFFLINE", "INCOMING"),
            row("dave", "OFFLINE", "OUTGOING")
        });

        JTabbedPane tabs = component(panel, JTabbedPane.class);
        check("Bạn bè (1)".equals(tabs.getTitleAt(0)), "Friend tab count");
        check("Lời mời nhận (1)".equals(tabs.getTitleAt(1)), "Incoming tab count");
        check("Đã gửi (1)".equals(tabs.getTitleAt(2)), "Outgoing tab count");
        check("INCOMING".equals(panel.getRelationState("carol")), "Incoming relation visible");
        check("OUTGOING".equals(panel.getRelationState("dave")), "Outgoing relation visible");

        button(panel, "Chấp nhận").doClick(0);
        button(panel, "Từ chối").doClick(0);
        button(panel, "Hủy lời mời").doClick(0);
        check(actions.equals(List.of("FRIEND_ACCEPT:carol", "FRIEND_REJECT:carol", "FRIEND_CANCEL:dave")), "Invitation buttons emit authenticated protocol actions");
        button(panel, "Thách đấu").doClick(0);
        check(challenges.equals(List.of("bob")), "Ready friend challenge callback");

        panel.updatePresence("bob;10;Đang bận;avatar_01;Sắt|");
        check(!button(panel, "Thách đấu").isEnabled(), "Busy friend cannot be challenged");
        panel.updatePresence("");
        check(!button(panel, "Thách đấu").isEnabled(), "Offline friend cannot be challenged");
        panel.updatePresence("bob;10;Đang rỗi;avatar_01;Sắt|");
        check(button(panel, "Thách đấu").isEnabled(), "Lobby presence makes a friend available again");

        panel.updateRequests(new String[0]);
        check("NONE".equals(panel.getRelationState("carol")), "Empty invitation snapshot clears old incoming request");
        check("NONE".equals(panel.getRelationState("dave")), "Empty invitation snapshot clears old outgoing request");
        check("Lời mời nhận (0)".equals(tabs.getTitleAt(1)), "Incoming count reset");
        panel.updateFriends(new String[0]);
        check("Bạn bè (0)".equals(tabs.getTitleAt(0)), "Empty friendship snapshot clears old rows");
    }

    private static void testSearchAndSessionIsolation() {
        FriendsPanel panel = new FriendsPanel();
        List<String> queries = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        int[] refreshes = {0};
        panel.setOnSearch(queries::add);
        panel.setOnAction((command, target) -> actions.add(command + ":" + target));
        panel.setOnRefresh(() -> refreshes[0]++);
        JTextField input = component(panel, JTextField.class);
        input.setText("b");
        button(panel, "Tìm bạn").doClick(0);
        check(queries.isEmpty(), "Too-short search is not sent");
        input.setText("  bob  ");
        button(panel, "Tìm bạn").doClick(0);
        check(queries.equals(List.of("bob")), "Search trims the account query");

        String displayName = "<html><b>Not markup</b></html>";
        panel.updateSearchResults(new String[] {
            "bob\t" + displayName + "\tavatar_01\t10\tSắt\tOFFLINE\tNONE"
        });
        JLabel label = descendants(panel).stream()
                .filter(component -> component instanceof JLabel)
                .map(component -> (JLabel) component)
                .filter(candidate -> displayName.equals(candidate.getText()))
                .findFirst().orElseThrow(() -> new AssertionError("Search display name not shown"));
        check(Boolean.TRUE.equals(label.getClientProperty("html.disable")), "User display name rendered as plain text");
        button(panel, "Kết bạn").doClick(0);
        check(actions.equals(List.of("FRIEND_REQUEST:bob")), "Search request button identifies the target account");

        panel.updateRequests(new String[] {row("bob", "OFFLINE", "OUTGOING")});
        check("OUTGOING".equals(panel.getRelationState("bob")), "Search relation updated after an invitation");
        panel.updateFriends(new String[] {row("bob", "READY", "FRIEND")});
        panel.updateRequests(new String[0]);
        check("FRIEND".equals(panel.getRelationState("bob")), "Search relation updated after acceptance");
        button(panel, "Làm mới").doClick(0);
        check(refreshes[0] == 1 && queries.size() == 2, "Refresh reloads account lists and current search");

        panel.clearSession();
        check(input.getText().isEmpty(), "Logout clears previous account search");
        check("NONE".equals(panel.getRelationState("bob")), "Logout clears previous account friendship data");
        JTabbedPane tabs = component(panel, JTabbedPane.class);
        check("Bạn bè (0)".equals(tabs.getTitleAt(0))
                && "Lời mời nhận (0)".equals(tabs.getTitleAt(1))
                && "Đã gửi (0)".equals(tabs.getTitleAt(2)), "Logout clears all relation counts");
        panel.updateFriends(new String[] {row("carol", "READY", "FRIEND")});
        check(button(panel, "Thách đấu").isEnabled(), "New session does not inherit previous presence snapshot");
    }

    private static void testLobbyCardFriendStates() {
        PlayerCard card = new PlayerCard("bob", 10, "Sắt", "avatar_01", true, false);
        List<String> requests = new ArrayList<>();
        card.setOnFriendRequest(requests::add);
        card.setFriendState("NONE");
        button(card, "Kết bạn").doClick(0);
        check(requests.equals(List.of("bob")), "Lobby card sends friend target");
        card.setFriendState("OUTGOING");
        check(!button(card, "Đã gửi lời mời").isEnabled(), "Lobby card prevents duplicate outgoing requests");
        card.setFriendState("FRIEND");
        check(!button(card, "Bạn bè").isEnabled(), "Lobby card identifies an existing friend");
        card.setFriendState("INCOMING");
        check(button(card, "Xem lời mời").isEnabled(), "Lobby card can open received invitations");
    }

    private static String row(String user, String presence, String relation) {
        return user + "\t" + user + "\tavatar_01\t10\tSắt\t" + presence + "\t" + relation;
    }

    private static JButton button(Container root, String text) {
        return descendants(root).stream()
                .filter(component -> component instanceof JButton)
                .map(component -> (JButton) component)
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst().orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private static <T> T component(Container root, Class<T> type) {
        return descendants(root).stream().filter(type::isInstance).map(type::cast)
                .findFirst().orElseThrow(() -> new AssertionError("Component not found: " + type));
    }

    private static List<Component> descendants(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            result.add(child);
            if (child instanceof Container container) {
                result.addAll(descendants(container));
            }
        }
        return result;
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    /** Prevent SoundManager's test mute setting from modifying the user's preferences. */
    public static final class MemoryPreferencesFactory implements PreferencesFactory {
        private final Preferences root = new MemoryPreferences(null, "");

        public MemoryPreferencesFactory() {}

        @Override
        public Preferences systemRoot() {
            return root;
        }

        @Override
        public Preferences userRoot() {
            return root;
        }
    }

    private static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> children = new HashMap<>();

        MemoryPreferences(AbstractPreferences parent, String name) {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            values.clear();
            children.clear();
        }

        @Override
        protected String[] keysSpi() {
            return values.keySet().toArray(String[]::new);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return children.keySet().toArray(String[]::new);
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return children.computeIfAbsent(name, key -> new MemoryPreferences(this, key));
        }

        @Override
        protected void syncSpi() {}

        @Override
        protected void flushSpi() {}
    }
}

package quiz;

import java.io.Serializable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Friend-only private messages, participant-scoped history and durable read cursors. */
public final class ChatService {
    public static final int MAX_TEXT_CODEPOINTS = 2000;
    public static final int PAGE_SIZE = 50;
    private final Model.Store users;
    private final ChatStore community;
    private final Clock clock;

    public record Message(long id, String sender, String recipient, String text, long timestamp,
            String nonce) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
    public record SendResult(Message message, boolean created) {}
    public record HistoryPage(List<Message> messages, boolean hasMore) {
        public HistoryPage { messages = List.copyOf(messages); }
    }
    public record Conversation(String username, int unread, long lastId, long lastTimestamp, String preview) {}

    public ChatService(Model.Store users, ChatStore community) {
        this(users, community, Clock.systemUTC());
    }

    public ChatService(Model.Store users, ChatStore community, Clock clock) {
        this.users = Objects.requireNonNull(users);
        this.community = Objects.requireNonNull(community);
        this.clock = Objects.requireNonNull(clock);
    }

    public SendResult send(String actor, String target, String rawText, String nonce) {
        synchronized (users) {
            requireActor(actor);
            requireTarget(actor, target);
            if (nonce == null || !nonce.matches("[A-Za-z0-9_-]{8,96}"))
                throw new IllegalArgumentException("Mã gửi tin nhắn không hợp lệ.");
            String text = validateText(rawText);
            Model.Friendship friendship = users.friendships.get(Model.Friendship.key(actor, target));
            if (friendship == null || friendship.status() != Model.FriendshipStatus.ACCEPTED)
                throw new IllegalArgumentException("Chỉ có thể nhắn tin với người đã kết bạn.");
            synchronized (community) {
                ChatStore.State state = community.state();
                Long duplicateId = state.nonceMessages.get(new ChatStore.NonceKey(actor, nonce));
                if (duplicateId != null) {
                    Message existing = state.messages.get(duplicateId);
                    if (!existing.recipient().equals(target) || !existing.text().equals(text))
                        throw new IllegalArgumentException("Mã gửi đã được dùng cho một tin nhắn khác.");
                    return new SendResult(existing, false);
                }
                long now = clock.millis();
                Long previous = state.lastChatSent.get(actor);
                if (previous != null && now - previous < 300)
                    throw new IllegalArgumentException("Bạn đang gửi quá nhanh. Hãy chờ một chút.");
                if (state.nextMessageId == Long.MAX_VALUE) throw new IllegalStateException("Đã hết mã tin nhắn.");
                Message message = new Message(state.nextMessageId, actor, target, text, now, nonce);
                community.transact(next -> {
                    next.nextMessageId++;
                    next.messages.put(message.id(), message);
                    next.nonceMessages.put(new ChatStore.NonceKey(actor, nonce), message.id());
                    next.lastChatSent.put(actor, now);
                });
                return new SendResult(message, true);
            }
        }
    }

    public HistoryPage history(String actor, String target, long beforeId, int limit) {
        synchronized (users) {
            requireActor(actor);
            requireTarget(actor, target);
            if (beforeId < 0) throw new IllegalArgumentException("Mốc lịch sử không hợp lệ.");
            int size = pageSize(limit);
            synchronized (community) {
                List<Message> eligible = community.state().messages.values().stream()
                        .filter(message -> conversation(message, actor, target))
                        .filter(message -> beforeId == 0 || message.id() < beforeId).toList();
                int first = Math.max(0, eligible.size() - size);
                return new HistoryPage(eligible.subList(first, eligible.size()), first > 0);
            }
        }
    }

    /** Read only through an actual message in this conversation; later arrivals remain unread. */
    public long markRead(String actor, String target, long throughId) {
        synchronized (users) {
            requireActor(actor);
            requireTarget(actor, target);
            if (throughId < 0) throw new IllegalArgumentException("Mốc đọc tin không hợp lệ.");
            synchronized (community) {
                ChatStore.State state = community.state();
                ChatStore.ReadCursor key = new ChatStore.ReadCursor(actor, target);
                long previous = state.readThrough.getOrDefault(key, 0L);
                if (throughId == 0) return previous;
                Message through = state.messages.get(throughId);
                if (through == null || !conversation(through, actor, target))
                    throw new IllegalArgumentException("Mốc đọc không thuộc cuộc trò chuyện này.");
                if (throughId <= previous) return previous;
                community.transact(next -> next.readThrough.put(key, throughId));
                return throughId;
            }
        }
    }

    public List<Conversation> summaries(String actor) {
        synchronized (users) {
            requireActor(actor);
            synchronized (community) {
                ChatStore.State state = community.state();
                Map<String, Message> latest = new HashMap<>();
                Map<String, Integer> unread = new HashMap<>();
                for (Message message : state.messages.values()) {
                    String other = other(message, actor);
                    if (other == null || !users.users.containsKey(other)) continue;
                    latest.put(other, message);
                    if (message.recipient().equals(actor)
                            && message.id() > state.readThrough.getOrDefault(new ChatStore.ReadCursor(actor, other), 0L))
                        unread.merge(other, 1, Integer::sum);
                }
                List<Conversation> result = new ArrayList<>();
                for (var entry : latest.entrySet()) {
                    Message last = entry.getValue();
                    String preview = last.text().replace('\n', ' ').replace('\t', ' ');
                    int end = preview.offsetByCodePoints(0, Math.min(100, preview.codePointCount(0, preview.length())));
                    result.add(new Conversation(entry.getKey(), unread.getOrDefault(entry.getKey(), 0),
                            last.id(), last.timestamp(), preview.substring(0, end)));
                }
                result.sort(Comparator.comparingLong(Conversation::lastId).reversed());
                return List.copyOf(result.subList(0, Math.min(100, result.size())));
            }
        }
    }

    public int totalUnread(String actor) {
        synchronized (users) {
            requireActor(actor);
            synchronized (community) {
                ChatStore.State state = community.state();
                int total = 0;
                for (Message message : state.messages.values()) {
                    if (message.recipient().equals(actor) && users.users.containsKey(message.sender())
                            && message.id() > state.readThrough.getOrDefault(
                                    new ChatStore.ReadCursor(actor, message.sender()), 0L)) total++;
                }
                return total;
            }
        }
    }

    private void requireActor(String actor) {
        if (actor == null || !users.users.containsKey(actor))
            throw new IllegalArgumentException("Bạn cần đăng nhập để sử dụng tin nhắn.");
    }

    private void requireTarget(String actor, String target) {
        if (target == null || target.equals(actor) || !users.users.containsKey(target))
            throw new IllegalArgumentException("Người nhận không hợp lệ.");
    }

    private int pageSize(int limit) { return limit <= 0 ? PAGE_SIZE : Math.min(PAGE_SIZE, limit); }

    private static boolean conversation(Message message, String actor, String target) {
        return message.sender().equals(actor) && message.recipient().equals(target)
                || message.sender().equals(target) && message.recipient().equals(actor);
    }

    private static String other(Message message, String actor) {
        return message.sender().equals(actor) ? message.recipient()
                : message.recipient().equals(actor) ? message.sender() : null;
    }

    public static String validateText(String raw) {
        if (raw == null) throw new IllegalArgumentException("Tin nhắn không được để trống.");
        String text = raw.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (text.isBlank() || text.codePointCount(0, text.length()) > MAX_TEXT_CODEPOINTS)
            throw new IllegalArgumentException("Tin nhắn cần từ 1 đến 2000 ký tự.");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(++i)))
                    throw new IllegalArgumentException("Tin nhắn chứa ký tự không hợp lệ.");
            } else if (Character.isLowSurrogate(c) || Character.isISOControl(c) && c != '\n' && c != '\t') {
                throw new IllegalArgumentException("Tin nhắn chứa ký tự điều khiển không hợp lệ.");
            }
        }
        return text;
    }
}

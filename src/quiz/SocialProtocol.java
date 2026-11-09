package quiz;

import java.util.ArrayList;
import java.util.List;

/** Authenticated friendship chat and match-scoped messages; no scoring or timer changes. */
final class SocialProtocol {
    private final Server server;

    SocialProtocol(Server server) { this.server = server; }

    void handle(Server.Peer peer, String[] packet) {
        String command = Wire.field(packet, 0);
        try {
            synchronized (server.store) {
                Model.User account = server.store.users.get(peer.name == null ? "" : peer.name);
                if (account == null || server.online.get(peer.name) != peer
                        || "LOCKED".equalsIgnoreCase(account.role))
                    throw new IllegalArgumentException("Bạn cần đăng nhập để gửi tin nhắn.");
            }
            switch (command) {
                case "CHAT_SEND" -> {
                    ChatService.SendResult result = server.chat.send(peer.name,
                            Wire.field(packet, 1), Wire.field(packet, 2), Wire.field(packet, 3));
                    ChatService.Message message = result.message();
                    peer.send(messagePacket(message));
                    if (result.created()) server.send(message.recipient(), messagePacket(message));
                    sendSummary(peer);
                    Server.Peer other = server.online.get(message.recipient());
                    if (other != null) sendSummary(other);
                }
                case "CHAT_HISTORY" -> {
                    String target = Wire.field(packet, 1);
                    long before = number(Wire.field(packet, 2));
                    ChatService.HistoryPage page = server.chat.history(peer.name, target, before, 50);
                    List<String> response = new ArrayList<>(List.of("CHAT_HISTORY", target,
                            String.valueOf(before), String.valueOf(page.hasMore())));
                    for (ChatService.Message message : page.messages()) {
                        response.addAll(List.of(String.valueOf(message.id()), message.sender(),
                                message.recipient(), message.text(), String.valueOf(message.timestamp())));
                    }
                    peer.send(response.toArray(String[]::new));
                }
                case "CHAT_READ" -> {
                    String target = Wire.field(packet, 1);
                    long through = server.chat.markRead(peer.name, target, number(Wire.field(packet, 2)));
                    peer.send("CHAT_READ_OK", target, String.valueOf(through));
                    sendSummary(peer);
                }
                case "CHAT_SUMMARY" -> sendSummary(peer);
                case "MATCH_CHAT_SEND", "MATCH_CHAT_HISTORY" -> matchChat(peer, packet);
                default -> throw new IllegalArgumentException("Lệnh tin nhắn không hợp lệ.");
            }
        } catch (IllegalArgumentException failure) {
            error(peer, packet, failure.getMessage());
        } catch (RuntimeException failure) {
            System.err.println("[CHAT] " + command + ": " + failure.getMessage());
            error(peer, packet, "Không thể lưu hoặc tải tin nhắn. Vui lòng thử lại.");
        }
    }

    void sendSummary(Server.Peer peer) {
        if (peer == null || peer.name == null) return;
        List<String> fields = new ArrayList<>(List.of("CHAT_SUMMARY"));
        for (ChatService.Conversation summary : server.chat.summaries(peer.name)) {
            fields.addAll(List.of(summary.username(), String.valueOf(summary.unread()),
                    String.valueOf(summary.lastId()), String.valueOf(summary.lastTimestamp()), summary.preview()));
        }
        peer.send(fields.toArray(String[]::new));
        peer.send("CHAT_UNREAD", String.valueOf(server.chat.totalUnread(peer.name)));
    }

    private void matchChat(Server.Peer peer, String[] packet) {
        Server.Match match = server.matches.get(peer.name);
        if (match == null) throw new IllegalArgumentException("Bạn chưa tham gia trận đấu.");
        synchronized (match) {
            if (!match.id.equals(Wire.field(packet, 1)) || match.index(peer.name) < 0
                    || match.isSolo || match.done || server.matches.get(match.left) != match
                    || server.matches.get(match.right) != match)
                throw new IllegalArgumentException("Trò chuyện chỉ dành cho hai người trong trận hiện tại.");
            if ("MATCH_CHAT_HISTORY".equals(packet[0])) {
                for (String[] message : match.chatMessages) peer.send(message);
                return;
            }
            String text = ChatService.validateText(Wire.field(packet, 2));
            if (text.codePointCount(0, text.length()) > 500)
                throw new IllegalArgumentException("Tin nhắn trong trận tối đa 500 ký tự.");
            long now = System.nanoTime();
            Long previous = match.lastChatSent.get(peer.name);
            if (previous != null && now - previous < 300_000_000L)
                throw new IllegalArgumentException("Bạn đang gửi quá nhanh. Hãy chờ một chút.");
            match.lastChatSent.put(peer.name, now);
            String[] message = {"MATCH_CHAT_MESSAGE", match.id, String.valueOf(++match.chatSequence),
                    peer.name, text, String.valueOf(System.currentTimeMillis())};
            match.chatMessages.addLast(message);
            while (match.chatMessages.size() > 100) match.chatMessages.removeFirst();
            server.send(match.left, message);
            server.send(match.right, message);
        }
    }

    private static String[] messagePacket(ChatService.Message message) {
        return new String[]{"CHAT_MESSAGE", String.valueOf(message.id()), message.sender(),
                message.recipient(), message.text(), String.valueOf(message.timestamp()), message.nonce()};
    }

    private static long number(String raw) {
        try {
            long value = Long.parseLong(raw);
            if (value >= 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Mốc tin nhắn không hợp lệ.");
    }

    private static void error(Server.Peer peer, String[] packet, String message) {
        if (packet[0].startsWith("MATCH_CHAT_")) {
            peer.send("MATCH_CHAT_ERROR", Wire.field(packet, 1), message);
        } else {
            peer.send("CHAT_ERROR", packet[0], Wire.field(packet, 1), message,
                    "CHAT_SEND".equals(packet[0]) ? Wire.field(packet, 3) : "");
        }
    }
}

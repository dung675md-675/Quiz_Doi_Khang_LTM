package quiz;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Durable private-message rules and real TCP delivery/access checks with isolated data. */
public final class TestSocialChat {
    private static int checks;
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    private static void denied(Runnable operation) {
        try { operation.run(); throw new AssertionError("Operation must be denied"); }
        catch (IllegalArgumentException expected) { checks++; }
    }

    public static void main(String[] args) throws Exception {
        services();
        protocol();
        System.out.println("PASS TestSocialChat: " + checks + " service and TCP checks");
    }

    private static final class TestClock extends Clock {
        long time = 1_000_000;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(time); }
        public long millis() { return time; }
        void tick() { time += 350; }
    }

    private static void services() throws Exception {
        Path folder = Files.createTempDirectory("quiz-chat-services-");
        Model.Store users = new Model.Store();
        for (String name : List.of("alice", "bob", "carol")) users.users.put(name, new Model.User(name, "hash"));
        FriendService friends = new FriendService(users, folder.resolve("users.dat").toFile(), null);
        ChatStore store = new ChatStore(folder);
        TestClock clock = new TestClock();
        ChatService chat = new ChatService(users, store, clock);
        denied(() -> chat.send("alice", "bob", "hello", "message_0001"));
        denied(() -> chat.send("unknown", "bob", "hello", "message_0001"));
        friends.request("alice", "bob"); friends.accept("bob", "alice");
        ChatService.SendResult first = chat.send("alice", "bob", "Chào bạn 👋\n<html>plain</html>", "message_0001");
        check(first.created(), "First message is durable");
        check(first.message().sender().equals("alice") && first.message().recipient().equals("bob"), "Server assigns participants");
        check(chat.totalUnread("bob") == 1 && chat.totalUnread("alice") == 0, "Unread is recipient scoped");
        check(!chat.send("alice", "bob", first.message().text(), "message_0001").created(), "Retry does not duplicate message");
        denied(() -> chat.send("alice", "bob", "changed", "message_0001"));
        denied(() -> chat.send("alice", "bob", "too fast", "message_0002"));
        denied(() -> chat.send("alice", "bob", " ", "message_0003"));
        denied(() -> chat.send("alice", "bob", "x".repeat(2001), "message_0004"));
        denied(() -> chat.send("alice", "bob", "\uD800", "message_0005"));
        denied(() -> chat.send("alice", "bob", "\u0000", "message_0006"));
        check(chat.history("carol", "alice", 0, 50).messages().isEmpty(), "Third party cannot read conversation");
        denied(() -> chat.markRead("carol", "alice", first.message().id()));
        check(chat.markRead("bob", "alice", first.message().id()) == first.message().id(), "Valid read cursor accepted");
        check(chat.totalUnread("bob") == 0, "Read acknowledgement clears unread");
        for (int i = 2; i <= 56; i++) {
            clock.tick(); chat.send("alice", "bob", "Message " + i, "message_" + String.format("%04d", i));
        }
        ChatService.HistoryPage recent = chat.history("bob", "alice", 0, 50);
        check(recent.messages().size() == 50 && recent.hasMore(), "History is paginated");
        ChatService.HistoryPage older = chat.history("bob", "alice", recent.messages().get(0).id(), 50);
        check(older.messages().size() == 6 && !older.hasMore(), "Older page excludes current boundary");
        check(chat.summaries("bob").get(0).unread() == 55, "Summary counts unread across pages");
        ChatService reopened = new ChatService(users, new ChatStore(folder), clock);
        check(reopened.history("alice", "bob", 0, 50).messages().size() == 50, "Messages survive restart");
        check(reopened.totalUnread("bob") == 55, "Read cursor survives restart");
        friends.remove("alice", "bob"); clock.tick();
        denied(() -> reopened.send("alice", "bob", "not friends", "message_0100"));
        check(reopened.history("alice", "bob", 0, 50).messages().size() == 50, "Participants retain their previous history");
        ChatStore purge = new ChatStore(folder);
        purge.purgeUserData("bob", () -> users.users.remove("bob"));
        users.users.put("bob", new Model.User("bob", "different account"));
        check(new ChatService(users, new ChatStore(folder), clock).history("bob", "alice", 0, 50).messages().isEmpty(), "Recreated account inherits no private history");

        // Failed disk writes must not publish state or consume an idempotency key.
        Path failedFolder = Files.createTempDirectory("quiz-chat-failure-");
        friends.request("alice", "carol"); friends.accept("carol", "alice");
        ChatStore failedStore = new ChatStore(failedFolder) {
            protected void writeSnapshot(State next) throws IOException { throw new IOException("test disk failure"); }
        };
        ChatService failed = new ChatService(users, failedStore, clock);
        try { failed.send("alice", "carol", "not persisted", "message_0200"); throw new AssertionError("Must fail"); }
        catch (IllegalStateException expected) { checks++; }
        check(failed.history("alice", "carol", 0, 50).messages().isEmpty(), "Failed save leaves no in-memory message");
        Path corruptFolder = Files.createTempDirectory("quiz-chat-corrupt-");
        byte[] corrupt = {1, 2, 3, 4}; Files.write(corruptFolder.resolve("chat.dat"), corrupt);
        try { new ChatStore(corruptFolder); throw new AssertionError("Corrupt file must not be overwritten"); }
        catch (IOException expected) { checks++; }
        check(Arrays.equals(corrupt, Files.readAllBytes(corruptFolder.resolve("chat.dat"))), "Corrupt original is preserved");
    }

    private static void protocol() throws Exception {
        try (Harness h = new Harness(); Peer alice = h.connect(); Peer bob = h.connect();
                Peer carol = h.connect(); Peer anonymous = h.connect()) {
            anonymous.send("CHAT_SUMMARY"); anonymous.until("CHAT_ERROR"); checks++;
            for (Peer peer : List.of(alice, bob, carol)) { peer.register(); peer.login(); }
            alice.send("CHAT_SEND", "bob", "not yet", "socket_0001"); alice.until("CHAT_ERROR"); checks++;
            alice.send("FRIEND_REQUEST", "bob"); alice.until("FRIEND_OK");
            bob.send("FRIEND_ACCEPT", "alice"); bob.until("FRIEND_OK");
            alice.send("CHAT_SEND", "bob", "Chào Bob 👋", "socket_0002");
            String[] sent = alice.until("CHAT_MESSAGE"), delivered = bob.until("CHAT_MESSAGE");
            check(sent[1].equals(delivered[1]) && delivered[2].equals("alice") && delivered[3].equals("bob"), "Private message delivered to both correct endpoints");
            alice.send("CHAT_SEND", "bob", "Chào Bob 👋", "socket_0002");
            check(alice.until("CHAT_MESSAGE")[1].equals(sent[1]), "TCP retry reuses original message id");
            carol.send("CHAT_HISTORY", "alice", "0"); check(carol.until("CHAT_HISTORY").length == 4, "TCP third-party history is empty");
            carol.send("CHAT_READ", "alice", sent[1]); carol.until("CHAT_ERROR"); checks++;
            bob.send("CHAT_HISTORY", "alice", "0"); check(bob.until("CHAT_HISTORY")[7].equals("Chào Bob 👋"), "History preserves Vietnamese and emoji");
            bob.send("CHAT_READ", "alice", sent[1]); bob.until("CHAT_READ_OK");
            check(bob.read(a -> a[0].equals("CHAT_UNREAD") && a[1].equals("0"))[1].equals("0"), "Read cursor synchronized over TCP");
            bob.send("CHAT_SEND", "alice", "Trả lời", "socket_0003");
            bob.until("CHAT_MESSAGE"); check(alice.until("CHAT_MESSAGE")[2].equals("bob"), "Private reply arrives");

            Server.Match match = h.app.new Match("alice", "bob", "topic", "level");
            match.set = new Model.SetData("test-set", "topic", "level", 120,
                    java.util.stream.IntStream.range(0,10).mapToObj(i -> new Model.Question("Q"+i,new String[]{"A","B","C","D"},0)).toList());
            h.app.matches.put("alice", match); h.app.matches.put("bob", match);
            carol.send("MATCH_CHAT_HISTORY", match.id); carol.until("MATCH_CHAT_ERROR"); checks++;
            alice.send("MATCH_CHAT_SEND", "wrong-id", "outsider"); alice.until("MATCH_CHAT_ERROR"); checks++;
            alice.send("MATCH_CHAT_SEND", match.id, "Chúc may mắn!");
            String[] a = alice.until("MATCH_CHAT_MESSAGE"), b = bob.until("MATCH_CHAT_MESSAGE");
            check(a[1].equals(match.id) && b[3].equals("alice") && b[4].equals("Chúc may mắn!"), "Match chat reaches opponent with server sender");
            check(match.points[0] == 0 && match.points[1] == 0 && match.currentQuestionIndex == 0, "Chat does not change scores or question index");
            alice.send("MATCH_CHAT_SEND", match.id, "spam"); alice.until("MATCH_CHAT_ERROR"); checks++;
            bob.send("MATCH_CHAT_SEND", match.id, "x".repeat(501)); bob.until("MATCH_CHAT_ERROR"); checks++;
            bob.send("MATCH_CHAT_SEND", match.id, "Cảm ơn!"); bob.until("MATCH_CHAT_MESSAGE"); alice.until("MATCH_CHAT_MESSAGE");
            check(match.chatMessages.size() == 2, "Only accepted match messages are retained");
            bob.send("MATCH_CHAT_HISTORY", match.id); check(bob.until("MATCH_CHAT_MESSAGE")[2].equals(a[2]), "Match history is available for reconnect");
            bob.until("MATCH_CHAT_MESSAGE");
            try (Peer reconnected = h.connect("bob")) {
                reconnected.send("LOGIN", "bob", "test1234", "RECONNECT"); reconnected.until("WELCOME");
                reconnected.send("MATCH_CHAT_HISTORY", match.id);
                check(reconnected.until("MATCH_CHAT_MESSAGE")[2].equals(a[2]), "Replacement connection can reload match chat");
                reconnected.until("MATCH_CHAT_MESSAGE");
                synchronized(match) { match.done = true; }
                reconnected.send("MATCH_CHAT_SEND", match.id, "after finish"); reconnected.until("MATCH_CHAT_ERROR"); checks++;
            }
            h.app.matches.clear();
            alice.send("FRIEND_REMOVE", "bob"); alice.until("FRIEND_OK");
            alice.send("CHAT_SEND", "bob", "after removal", "socket_0004"); alice.until("CHAT_ERROR"); checks++;
            check(new ChatService(h.app.store, new ChatStore(h.folder)).history("alice", "bob", 0, 50).messages().size() == 2, "TCP private messages survive reopening the store");
        }
    }

    private static final class Peer implements AutoCloseable {
        final Socket socket; final DataInputStream in; final DataOutputStream out; final String username;
        Peer(Socket socket, String username) throws IOException {
            this.socket=socket; this.username=username; socket.setSoTimeout(4000);
            in=new DataInputStream(socket.getInputStream()); out=new DataOutputStream(socket.getOutputStream());
        }
        void send(String... fields) throws IOException { Wire.send(out, fields); }
        String[] until(String command) throws Exception { return read(a -> a[0].equals(command)); }
        String[] read(Predicate<String[]> predicate) throws Exception {
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
            while(System.nanoTime()<end) { String[] a=Wire.read(in); if(predicate.test(a))return a; }
            throw new AssertionError("Expected packet was not received");
        }
        void register() throws Exception { send("REGISTER",username,"test1234"); until("INFO"); }
        void login() throws Exception { send("LOGIN",username,"test1234"); until("WELCOME"); }
        public void close() throws IOException { socket.close(); }
    }
    private static final class Harness implements AutoCloseable {
        final Path folder=Files.createTempDirectory("quiz-chat-tcp-");
        final Server app; final ServerSocket socket=new ServerSocket(0,8,InetAddress.getLoopbackAddress());
        final List<Socket> accepted=new CopyOnWriteArrayList<>();
        int next;
        Harness() throws Exception {
            Files.writeString(folder.resolve("db.properties"),"db.enabled=false\n");
            app=new Server(folder);
            Thread thread=new Thread(() -> {
                try { while(!socket.isClosed()) { Socket client=socket.accept();accepted.add(client);app.workers.submit(() -> {
                    try { app.serve(new Server.Peer(client)); } catch(IOException ignored) { }
                }); } } catch(IOException ignored) { }
            }); thread.setDaemon(true);thread.start();
        }
        Peer connect() throws IOException { return connect(List.of("alice","bob","carol","anonymous").get(next++)); }
        Peer connect(String name) throws IOException { return new Peer(new Socket(InetAddress.getLoopbackAddress(),socket.getLocalPort()),name); }
        public void close() throws Exception {
            socket.close(); for(Socket s:accepted)try{s.close();}catch(IOException ignored){}
            app.timer.shutdownNow();app.workers.shutdownNow();app.workers.awaitTermination(3,TimeUnit.SECONDS);app.db.close();
        }
    }
}

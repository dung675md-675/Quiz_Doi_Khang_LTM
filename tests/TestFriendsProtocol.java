package quiz;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** End-to-end friendship commands over real loopback TCP, using only temporary data. */
public final class TestFriendsProtocol {
    private static final String ALICE = "friend_alice";
    private static final String BOB = "friend_bob";
    private static final String CAROL = "friend_carol";
    private static final String PASSWORD = "FriendsTest123";
    private static int checks;

    public static void main(String[] args) throws Exception {
        try (Harness h = new Harness();
                TestPeer anonymous = h.connect();
                TestPeer alice = h.connect();
                TestPeer carol = h.connect()) {
            anonymous.send("FRIEND_LIST");
            anonymous.readUntil(a -> a[0].equals("ERROR") || a[0].equals("FRIEND_ERROR"), "anonymous access denied");
            check(anonymous.seen.stream().noneMatch(a -> a[0].equals("FRIEND_LIST")), "Anonymous client receives no friendship list");

            alice.register(ALICE);
            alice.login(ALICE);
            carol.register(CAROL);
            carol.login(CAROL);
            try (TestPeer registrationOnly = h.connect()) {
                registrationOnly.register(BOB);
            }
            synchronized (h.app.store) {
                Model.User bob = h.app.store.users.get(BOB);
                bob.fullName = "Friend Bob";
                bob.email = "private-friend@example.invalid";
                bob.bio = "PRIVATE_BIO_MUST_NOT_LEAK";
                h.app.store.save(h.app.storeFile);
            }

            alice.send("FRIEND_SEARCH", "friend_");
            String[] search = alice.readUntil(a -> a[0].equals("FRIEND_SEARCH_RESULTS"), "search result");
            check(row(search, ALICE) == null, "Search excludes the requesting account");
            String[] bobSearch = row(search, BOB);
            check(bobSearch != null && bobSearch.length == 7, "Search returns the public seven-field user row");
            check("OFFLINE".equals(bobSearch[5]) && "NONE".equals(bobSearch[6]), "Search shows offline account without friendship");
            String payload = String.join("|", search);
            check(!payload.contains(PASSWORD) && !payload.contains("private-friend@example.invalid")
                    && !payload.contains("PRIVATE_BIO_MUST_NOT_LEAK"), "Search does not expose credentials or private profile fields");
            check(!payload.contains(h.app.store.users.get(BOB).password), "Password hash is never returned");

            testOversizedTargetKeepsConnection(alice);
            testSanitizedPublicProfile(h, alice);

            alice.ok("FRIEND_REQUEST", BOB);
            alice.send("FRIEND_REQUESTS");
            alice.readUntil(a -> a[0].equals("FRIEND_REQUESTS") && hasRow(a, BOB, null, "OUTGOING"), "outgoing offline invitation");
            check(!h.app.online.containsKey(BOB), "Invitations can be sent while recipient is offline");
            alice.error("FRIEND_REQUEST", BOB);
            carol.error("FRIEND_ACCEPT", ALICE);

            try (TestPeer bob = h.connect()) {
                bob.login(BOB);
                bob.readUntil(a -> a[0].equals("FRIEND_REQUESTS") && hasRow(a, ALICE, null, "INCOMING"), "invitation delivered on recipient login");
                bob.error("FRIEND_REQUEST", ALICE);
                alice.error("FRIEND_ACCEPT", BOB);
                bob.ok("FRIEND_ACCEPT", ALICE);

                alice.send("FRIEND_LIST");
                alice.readUntil(a -> a[0].equals("FRIEND_LIST") && hasRow(a, BOB, "READY", "FRIEND"), "sender friend list updated");
                bob.send("FRIEND_LIST");
                bob.readUntil(a -> a[0].equals("FRIEND_LIST") && hasRow(a, ALICE, "READY", "FRIEND"), "recipient friend list updated");
                check("FRIEND".equals(h.app.friends.relationState(ALICE, BOB)), "Server stores the shared accepted relationship");
            }

            await(() -> !h.app.online.containsKey(BOB), "Disconnected friend removed from online map");
            alice.send("FRIEND_LIST");
            alice.readUntil(a -> a[0].equals("FRIEND_LIST") && hasRow(a, BOB, "OFFLINE", "FRIEND"), "offline presence refresh");
            check("FRIEND".equals(h.app.friends.relationState(ALICE, BOB)), "Disconnect keeps the friendship");

            try (TestPeer bob = h.connect()) {
                bob.login(BOB);
                bob.send("FRIEND_LIST");
                bob.readUntil(a -> a[0].equals("FRIEND_LIST") && hasRow(a, ALICE, "READY", "FRIEND"), "friendship restored on login");
                alice.ok("FRIEND_REMOVE", BOB);
                alice.send("FRIEND_LIST");
                alice.readUntil(a -> a[0].equals("FRIEND_LIST") && a.length == 1, "sender list empty after removal");
                bob.send("FRIEND_LIST");
                bob.readUntil(a -> a[0].equals("FRIEND_LIST") && a.length == 1, "recipient list empty after removal");

                alice.ok("FRIEND_REQUEST", BOB);
                bob.ok("FRIEND_REJECT", ALICE);
                alice.send("FRIEND_REQUESTS");
                alice.readUntil(a -> a[0].equals("FRIEND_REQUESTS") && a.length == 1, "reject clears pending invitations");
                alice.ok("FRIEND_REQUEST", BOB);
                bob.error("FRIEND_CANCEL", ALICE);
                alice.ok("FRIEND_CANCEL", BOB);
                bob.send("FRIEND_REQUESTS");
                bob.readUntil(a -> a[0].equals("FRIEND_REQUESTS") && a.length == 1, "cancel clears recipient invitation");
            }

            carol.ok("FRIEND_REQUEST", ALICE);
            alice.ok("FRIEND_ACCEPT", CAROL);
            try (TestPeer admin = h.connect()) {
                admin.login("admin", "admin123");
                admin.send("DELETE_USER", CAROL);
                admin.readUntil(a -> a[0].equals("USER_SAVED"), "administrator account deletion");
                carol.readUntil(a -> a[0].equals("ACCOUNT_DELETED"), "deleted user notified");
                synchronized (h.app.store) {
                    check(!h.app.store.users.containsKey(CAROL), "Account removed");
                    check(h.app.store.friendships.values().stream().noneMatch(r -> r.involves(CAROL)), "Account deletion purges friendship records");
                }
                alice.send("FRIEND_LIST");
                alice.readUntil(a -> a[0].equals("FRIEND_LIST") && a.length == 1, "friend list refreshed after account deletion");
            }
        }
        testChunkedFriendList();
        System.out.println("PASS TestFriendsProtocol: " + checks + " checks and TCP request assertions");
    }

    private static void testOversizedTargetKeepsConnection(TestPeer alice) throws Exception {
        // This fits writeUTF's inbound byte limit; reflecting it inside a longer message would not.
        String hugeTarget = "x".repeat(65520);
        alice.send("FRIEND_REQUEST", hugeTarget);
        String[] error = alice.readUntil(a -> a[0].equals("FRIEND_ERROR")
                && a.length == 4 && a[1].equals("FRIEND_REQUEST"), "oversized target rejection");
        check(error[2].codePointCount(0, error[2].length()) <= 64, "Reflected error target is bounded");
        check(!error[3].isBlank(), "Oversized target has a readable error message");
        alice.send("PING", "after-oversized-target");
        alice.readUntil(a -> a[0].equals("PONG") && a.length > 1
                && a[1].equals("after-oversized-target"), "socket remains usable after malformed target");
    }

    private static void testSanitizedPublicProfile(Harness h, TestPeer alice) throws Exception {
        Model.User bob;
        String originalName;
        String originalAvatar;
        synchronized (h.app.store) {
            bob = h.app.store.users.get(BOB);
            originalName = bob.fullName;
            originalAvatar = bob.avatarId;
            bob.fullName = "Friend\tBob\r\n;|" + "\uD83D\uDE80".repeat(30000);
            bob.avatarId = "avatar_01\tforged\n" + "\uD83C\uDFAD".repeat(15000);
            h.app.store.save(h.app.storeFile);
        }
        try {
            alice.send("FRIEND_SEARCH", BOB);
            String[] search = alice.readUntil(a -> a[0].equals("FRIEND_SEARCH_RESULTS"), "large Unicode public profile search");
            String[] fields = row(search, BOB);
            check(fields != null && fields.length == 7, "Delimiter-rich profile cannot inject extra row fields");
            check(fields[1].codePointCount(0, fields[1].length()) <= 100, "Public display name is bounded by Unicode code points");
            check(fields[2].codePointCount(0, fields[2].length()) <= 40, "Public avatar identifier is bounded");
            check(!fields[1].contains("\r") && !fields[1].contains("\n")
                    && !fields[2].contains("\r") && !fields[2].contains("\n"), "Profile row strips line delimiters");
            String record = String.join("\t", fields);
            try (ByteArrayOutputStream encoded = new ByteArrayOutputStream();
                    DataOutputStream wireString = new DataOutputStream(encoded)) {
                wireString.writeUTF(record);
                check(encoded.size() < 4096, "Public profile stays safely within writeUTF's byte limit");
            }
            String payload = String.join("|", search);
            check(!payload.contains(bob.password) && !payload.contains(bob.email)
                    && !payload.contains(bob.bio), "Sanitized search still excludes private fields");
        } finally {
            synchronized (h.app.store) {
                bob.fullName = originalName;
                bob.avatarId = originalAvatar;
                h.app.store.save(h.app.storeFile);
            }
        }
    }

    private static void testChunkedFriendList() throws Exception {
        try (Harness h = new Harness(); TestPeer alice = h.connect()) {
            alice.register(ALICE);
            alice.login(ALICE);
            // Drain the login snapshots before introducing the large fixture.
            alice.send("PING", "before-friend-chunks");
            alice.readUntil(a -> a[0].equals("PONG") && a.length > 1
                    && a[1].equals("before-friend-chunks"), "login snapshot barrier");

            int total = 425;
            Set<String> expected = new HashSet<>();
            synchronized (h.app.store) {
                for (int i = 0; i < total; i++) {
                    String name = "chunk_friend_" + i;
                    expected.add(name);
                    h.app.store.users.put(name, new Model.User(name, "unused"));
                    Model.Friendship relation = new Model.Friendship(ALICE, name, ALICE,
                            Model.FriendshipStatus.ACCEPTED, System.currentTimeMillis());
                    h.app.store.friendships.put(Model.Friendship.key(ALICE, name), relation);
                }
                h.app.store.save(h.app.storeFile);
            }

            alice.send("FRIEND_LIST");
            String[] packet = alice.readUntil(a -> a[0].equals("FRIEND_LIST")
                    && a.length == 201, "first 200 friend rows");
            Set<String> received = new HashSet<>();
            int packets = 0;
            while (true) {
                packets++;
                check(packet.length <= 1000 && packet.length <= 201, "Friend chunk stays within Wire field limit");
                for (int i = 1; i < packet.length; i++) {
                    String[] fields = packet[i].split("\t", -1);
                    check(fields.length == 7 && expected.contains(fields[0])
                            && fields[5].equals("OFFLINE") && fields[6].equals("FRIEND"), "Friend chunk has a complete valid row");
                    check(received.add(fields[0]), "Friend chunks do not duplicate users");
                }
                if (received.size() >= total) {
                    break;
                }
                packet = alice.readUntil(a -> a[0].equals("FRIEND_LIST_MORE"), "friend-list continuation");
            }
            check(packets == 3 && packet.length == 26, "425 friends split into 200, 200 and 25 rows");
            check(received.equals(expected), "All friendships survive chunked transport");
            alice.send("PING", "after-friend-chunks");
            alice.readUntil(a -> a[0].equals("PONG") && a.length > 1
                    && a[1].equals("after-friend-chunks"), "socket remains framed after friend chunks");
        }
    }

    private static String[] row(String[] packet, String username) {
        for (int i = 1; i < packet.length; i++) {
            String[] fields = packet[i].split("\t", -1);
            if (fields.length > 0 && username.equals(fields[0])) {
                return fields;
            }
        }
        return null;
    }

    private static boolean hasRow(String[] packet, String user, String presence, String relation) {
        String[] fields = row(packet, user);
        return fields != null && fields.length == 7
                && (presence == null || presence.equals(fields[5]))
                && (relation == null || relation.equals(fields[6]));
    }

    private static void await(BooleanSupplier condition, String description) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        check(condition.getAsBoolean(), description);
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    private static final class TestPeer implements AutoCloseable {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final List<String[]> seen = new ArrayList<>();

        TestPeer(int port) throws Exception {
            socket = new Socket(InetAddress.getLoopbackAddress(), port);
            socket.setSoTimeout(5000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        }

        void send(String... fields) throws Exception {
            Wire.send(out, fields);
        }

        void register(String username) throws Exception {
            send("REGISTER", username, PASSWORD);
            readUntil(a -> a[0].equals("INFO"), "register " + username);
        }

        void login(String username) throws Exception {
            login(username, PASSWORD);
        }

        void login(String username, String password) throws Exception {
            send("LOGIN", username, password);
            readUntil(a -> a[0].equals("WELCOME") && a.length > 1 && a[1].equals(username), "login " + username);
        }

        void ok(String command, String target) throws Exception {
            send(command, target);
            readUntil(a -> a[0].equals("FRIEND_OK") && a.length >= 4
                    && a[1].equals(command) && a[2].equals(target), command + " success");
        }

        void error(String command, String target) throws Exception {
            send(command, target);
            readUntil(a -> a[0].equals("FRIEND_ERROR") && a.length >= 4
                    && a[1].equals(command) && a[2].equals(target), command + " rejected");
        }

        String[] readUntil(Predicate<String[]> wanted, String description) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(7);
            List<String> types = new ArrayList<>();
            try {
                while (System.nanoTime() < deadline) {
                    String[] packet = Wire.read(in);
                    seen.add(packet);
                    types.add(packet[0]);
                    if (wanted.test(packet)) {
                        checks++;
                        return packet;
                    }
                }
            } catch (SocketTimeoutException e) {
                throw new AssertionError("Timed out waiting for " + description + "; saw " + types, e);
            }
            throw new AssertionError("No matching response for " + description + "; saw " + types);
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }

    private static final class Harness implements AutoCloseable {
        final Path folder = Files.createTempDirectory("quiz-friends-protocol-");
        final Server app;
        final ServerSocket listener;
        final Thread acceptor;
        final Set<Socket> acceptedSockets = ConcurrentHashMap.newKeySet();
        final List<TestPeer> clients = new ArrayList<>();

        Harness() throws Exception {
            Files.writeString(folder.resolve("db.properties"), "db.enabled=false\n", StandardCharsets.UTF_8);
            Files.writeString(folder.resolve("questions.txt"), "", StandardCharsets.UTF_8);
            app = new Server(folder);
            listener = new ServerSocket(0, 16, InetAddress.getLoopbackAddress());
            acceptor = new Thread(() -> {
                while (!listener.isClosed()) {
                    try {
                        Socket socket = listener.accept();
                        acceptedSockets.add(socket);
                        Server.Peer peer = new Server.Peer(socket);
                        app.workers.submit(() -> app.serve(peer));
                    } catch (SocketException expectedDuringShutdown) {
                        if (!listener.isClosed()) {
                            throw new AssertionError(expectedDuringShutdown);
                        }
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }
            }, "Friends-test-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        TestPeer connect() throws Exception {
            TestPeer client = new TestPeer(listener.getLocalPort());
            clients.add(client);
            return client;
        }

        @Override
        public void close() throws Exception {
            for (TestPeer client : clients) {
                client.close();
            }
            listener.close();
            acceptor.join(3000);
            for (Socket socket : acceptedSockets) {
                socket.close();
            }
            app.timer.shutdownNow();
            app.workers.shutdownNow();
            check(app.workers.awaitTermination(5, TimeUnit.SECONDS), "Server worker threads terminated");
            check(app.timer.awaitTermination(3, TimeUnit.SECONDS), "Server timer threads terminated");
            app.db.close();
            TestFriends.removeTemporaryTree(folder);
        }
    }
}

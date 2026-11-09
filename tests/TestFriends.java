package quiz;

import java.io.File;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Isolated friendship state, authorization, persistence and concurrency checks. */
public final class TestFriends {
    private static final long STORE_UID = -2459928414739261786L;
    private static int checks;

    public static void main(String[] args) throws Exception {
        testRequestAuthorizationAndSymmetry();
        testRejectAndCancel();
        testPersistenceAndRestart();
        testSimultaneousRequests();
        testFailedSaveDoesNotApplyChanges();
        testAccountDeletionCleanup();
        testFailedAccountDeletionRollsBack();
        testStoreCompatibility(args.length == 0 ? null : Path.of(args[0]));
        System.out.println("PASS TestFriends: " + checks + " checks");
    }

    private static void testRequestAuthorizationAndSymmetry() throws Exception {
        try (Fixture f = new Fixture()) {
            check(f.service.relationships("alice").isEmpty(), "Initially no relationships");
            denied(() -> f.service.request("alice", "alice"), "Cannot request oneself");
            denied(() -> f.service.request("alice", "missing"), "Unknown target rejected");
            denied(() -> f.service.request("missing", "alice"), "Unknown actor rejected");

            f.service.request("alice", "bob");
            check("OUTGOING".equals(f.service.relationState("alice", "bob")), "Sender sees outgoing request");
            check("INCOMING".equals(f.service.relationState("bob", "alice")), "Recipient sees incoming request");
            check(f.store.friendships.size() == 1, "One canonical record per pair");
            check(Model.Friendship.key("alice", "bob").equals(Model.Friendship.key("bob", "alice")), "Pair key is symmetric");
            Model.Friendship pending = f.service.relationships("alice").get(0);
            check(pending.createdAt() > 0 && pending.status() == Model.FriendshipStatus.PENDING, "Pending request includes time");
            check(pending.involves("alice") && "bob".equals(pending.other("alice")), "Relationship participants preserved");

            denied(() -> f.service.request("alice", "bob"), "Duplicate request rejected");
            denied(() -> f.service.request("bob", "alice"), "Opposing request rejected");
            denied(() -> f.service.accept("alice", "bob"), "Sender cannot accept own request");
            denied(() -> f.service.reject("alice", "bob"), "Sender cannot reject as recipient");
            denied(() -> f.service.cancel("bob", "alice"), "Recipient cannot cancel as sender");
            denied(() -> f.service.accept("carol", "alice"), "Third party cannot accept request");

            f.service.accept("bob", "alice");
            check("FRIEND".equals(f.service.relationState("alice", "bob")), "Sender becomes friend");
            check("FRIEND".equals(f.service.relationState("bob", "alice")), "Recipient becomes friend");
            check(f.store.friendships.size() == 1, "Acceptance updates the shared relation");
            check(f.service.relationships("bob").get(0).status() == Model.FriendshipStatus.ACCEPTED, "Accepted status persisted");
            denied(() -> f.service.request("alice", "bob"), "Existing friends cannot request again");
            denied(() -> f.service.accept("bob", "alice"), "Already accepted request cannot be accepted again");
            denied(() -> f.service.reject("bob", "alice"), "Accepted friendship is not a pending request");

            f.service.remove("bob", "alice");
            check("NONE".equals(f.service.relationState("alice", "bob")), "Removing friendship clears sender view");
            check("NONE".equals(f.service.relationState("bob", "alice")), "Removing friendship clears recipient view");
            denied(() -> f.service.remove("alice", "bob"), "Cannot remove nonexistent friendship");
        }
    }

    private static void testRejectAndCancel() throws Exception {
        try (Fixture f = new Fixture()) {
            f.service.request("alice", "bob");
            denied(() -> f.service.remove("alice", "bob"), "Pending invitation uses cancel, not unfriend");
            f.service.reject("bob", "alice");
            check(f.store.friendships.isEmpty(), "Reject deletes pending record");
            f.service.request("alice", "bob");
            f.service.cancel("alice", "bob");
            check(f.store.friendships.isEmpty(), "Sender can cancel pending record");
            f.service.request("bob", "alice");
            f.service.accept("alice", "bob");
            check("FRIEND".equals(f.service.relationState("bob", "alice")), "A new invitation works after rejection/cancellation");
        }
    }

    private static void testPersistenceAndRestart() throws Exception {
        try (Fixture f = new Fixture()) {
            f.service.request("alice", "bob");
            Model.Store restored = Model.Store.read(f.file);
            FriendService restarted = new FriendService(restored, f.file, f.db);
            check("INCOMING".equals(restarted.relationState("bob", "alice")), "Offline invitation survives restart");
            check(restored.users.size() == 3, "User records preserved with friendship data");
            restarted.accept("bob", "alice");
            restored = Model.Store.read(f.file);
            restarted = new FriendService(restored, f.file, f.db);
            check("FRIEND".equals(restarted.relationState("alice", "bob")), "Accepted friendship survives restart");
            restarted.remove("alice", "bob");
            check(Model.Store.read(f.file).friendships.isEmpty(), "Removal survives restart");
        }
    }

    private static void testSimultaneousRequests() throws Exception {
        try (Fixture f = new Fixture()) {
            var workers = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger accepted = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            try {
                var first = workers.submit(() -> raceRequest(f.service, "alice", "bob", ready, start, accepted, rejected));
                var second = workers.submit(() -> raceRequest(f.service, "bob", "alice", ready, start, accepted, rejected));
                check(ready.await(3, TimeUnit.SECONDS), "Both request workers ready");
                start.countDown();
                first.get(5, TimeUnit.SECONDS);
                second.get(5, TimeUnit.SECONDS);
                check(accepted.get() == 1 && rejected.get() == 1, "Exactly one opposing request succeeds");
                check(f.store.friendships.size() == 1, "Concurrent requests leave one relation");
                check(f.service.relationships("alice").get(0).status() == Model.FriendshipStatus.PENDING, "Concurrent requests do not silently accept");
            } finally {
                start.countDown();
                workers.shutdownNow();
                check(workers.awaitTermination(3, TimeUnit.SECONDS), "Request workers stop");
            }
        }
    }

    private static void raceRequest(FriendService service, String actor, String target,
            CountDownLatch ready, CountDownLatch start, AtomicInteger accepted, AtomicInteger rejected) {
        ready.countDown();
        try {
            if (!start.await(3, TimeUnit.SECONDS)) {
                throw new AssertionError("Request race did not start");
            }
            service.request(actor, target);
            accepted.incrementAndGet();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            rejected.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void testFailedSaveDoesNotApplyChanges() throws Exception {
        try (Fixture f = new Fixture()) {
            Path blocker = Path.of(f.file.getPath() + ".tmp");
            Files.createDirectory(blocker);
            denied(() -> f.service.request("alice", "bob"), "Failed file save is reported");
            check(f.store.friendships.isEmpty(), "Failed save rolls back in-memory invitation");
            Model.Store saved = Model.Store.read(f.file);
            check(saved.friendships.isEmpty() && saved.users.size() == 3, "Failed save preserves original store");
            Files.delete(blocker);
            f.service.request("alice", "bob");
            check("OUTGOING".equals(f.service.relationState("alice", "bob")), "Retry works after storage is restored");
        }
    }

    private static void testAccountDeletionCleanup() throws Exception {
        try (Fixture f = new Fixture()) {
            f.service.request("alice", "bob");
            f.service.accept("bob", "alice");
            f.service.request("carol", "bob");
            synchronized (f.store) {
                f.store.users.remove("bob");
                f.service.removeUserRelationships("bob");
                f.store.save(f.file);
            }
            check(f.store.friendships.isEmpty(), "Account deletion removes accepted and pending relationships");
            check(Model.Store.read(f.file).friendships.isEmpty(), "Deletion cleanup can be persisted");
        }
    }

    private static void testStoreCompatibility(Path legacyFile) throws Exception {
        check(ObjectStreamClass.lookup(Model.Store.class).getSerialVersionUID() == STORE_UID, "Store serialVersionUID unchanged");
        if (legacyFile == null) {
            System.out.println("Legacy-file fixture not provided; pass its path to verify pre-feature users.dat compatibility.");
            return;
        }
        Model.Store legacy = Model.Store.read(legacyFile.toFile());
        check(!legacy.users.isEmpty(), "Legacy users.dat retains original users");
        check(legacy.friendships != null && legacy.friendships.isEmpty(), "Missing legacy friendship field initialized");
        try (Fixture f = new Fixture()) {
            String originalUser = legacy.users.keySet().iterator().next();
            legacy.users.put("new_friend", new Model.User("new_friend", "unused"));
            legacy.save(f.file);
            FriendService service = new FriendService(legacy, f.file, f.db);
            service.request(originalUser, "new_friend");
            check("OUTGOING".equals(service.relationState(originalUser, "new_friend")), "Legacy store accepts new friendship data");
        }
    }

    private static void testFailedAccountDeletionRollsBack() throws Exception {
        try (Fixture f = new Fixture()) {
            f.service.request("alice", "bob");
            f.service.accept("bob", "alice");
            Path blocker = Path.of(f.file.getPath() + ".tmp");
            Files.createDirectory(blocker);
            denied(() -> f.service.deleteUser("bob"), "Failed account-deletion save is reported");
            check(f.store.users.containsKey("bob"), "Failed account deletion restores user in memory");
            check("FRIEND".equals(f.service.relationState("alice", "bob")), "Failed account deletion restores friendship in memory");
            Model.Store saved = Model.Store.read(f.file);
            check(saved.users.containsKey("bob") && saved.friendships.size() == 1,
                    "Failed account deletion preserves user and friendship on disk");
            Files.delete(blocker);
            f.service.deleteUser("bob");
            check(!f.store.users.containsKey("bob") && f.store.friendships.isEmpty(),
                    "Successful account deletion purges user and relationships in memory");
            saved = Model.Store.read(f.file);
            check(!saved.users.containsKey("bob") && saved.friendships.isEmpty(),
                    "Successful account deletion persists user and relationship removal");
        }
    }

    private static void denied(CheckedAction action, String description) throws Exception {
        try {
            action.run();
            throw new AssertionError(description + ": unexpectedly succeeded");
        } catch (IllegalArgumentException | IllegalStateException expected) {
            checks++;
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static final class Fixture implements AutoCloseable {
        final Path folder = Files.createTempDirectory("quiz-friends-service-");
        final File file = folder.resolve("users.dat").toFile();
        final Model.Store store = new Model.Store();
        final DB db;
        final FriendService service;

        Fixture() throws Exception {
            Files.writeString(folder.resolve("db.properties"), "db.enabled=false\n", StandardCharsets.UTF_8);
            db = new DB(folder);
            for (String name : List.of("alice", "bob", "carol")) {
                store.users.put(name, new Model.User(name, "unused"));
            }
            store.save(file);
            service = new FriendService(store, file, db);
        }

        @Override
        public void close() throws Exception {
            db.close();
            removeTemporaryTree(folder);
        }
    }

    static void removeTemporaryTree(Path root) throws Exception {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        if (!absoluteRoot.getFileName().toString().startsWith("quiz-friends-")) {
            throw new IllegalArgumentException("Refusing cleanup outside the test temporary directory");
        }
        if (!Files.exists(absoluteRoot)) {
            return;
        }
        try (var paths = Files.walk(absoluteRoot)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.toAbsolutePath().normalize().startsWith(absoluteRoot)) {
                    throw new IllegalArgumentException("Temporary cleanup escaped the test directory");
                }
                Files.deleteIfExists(path);
            }
        }
    }
}

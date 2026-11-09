package quiz;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Durable, thread-safe friendship rules shared by all client connections. */
public final class FriendService {
    private final Model.Store store;
    private final File storeFile;
    private final DB db;

    public FriendService(Model.Store store, File storeFile, DB db) {
        this.store = Objects.requireNonNull(store, "store");
        this.storeFile = Objects.requireNonNull(storeFile, "storeFile");
        this.db = db;

        synchronized (store) {
            Map<String, Model.Friendship> previous = store.friendships;
            boolean initialized = store.friendshipsInitialized;
            Map<String, Model.Friendship> snapshot = new HashMap<>();
            if (previous != null) {
                for (Model.Friendship relation : previous.values()) {
                    if (relation == null
                            || !store.users.containsKey(relation.userA())
                            || !store.users.containsKey(relation.userB())) {
                        throw new IllegalStateException("Dữ liệu bạn bè chứa tài khoản không tồn tại.");
                    }
                    snapshot.put(Model.Friendship.key(relation.userA(), relation.userB()), relation);
                }
            }
            try {
                if (usesDatabase()) {
                    snapshot = db.initializeFriendships(snapshot, initialized, store.users);
                }
                // Once initialized, the local snapshot remains authoritative across DB outages.
                store.friendships = new HashMap<>(snapshot);
                store.friendshipsInitialized = true;
                store.save(storeFile);
            } catch (RuntimeException e) {
                store.friendships = previous == null ? new HashMap<>() : previous;
                store.friendshipsInitialized = initialized;
                throw new IllegalStateException("Không thể khởi tạo dữ liệu bạn bè.", e);
            }
        }
    }

    public String request(String actor, String target) {
        synchronized (store) {
            requireActor(actor);
            target = requireTarget(actor, target);
            String key = Model.Friendship.key(actor, target);
            Model.Friendship existing = store.friendships.get(key);
            if (existing != null) {
                if (existing.status() == Model.FriendshipStatus.ACCEPTED) {
                    throw new IllegalArgumentException("Hai bạn đã là bạn bè.");
                }
                if (existing.requestedBy().equals(actor)) {
                    throw new IllegalArgumentException("Bạn đã gửi lời mời kết bạn cho người này.");
                }
                throw new IllegalArgumentException(
                        "Người này đã gửi lời mời cho bạn. Hãy chấp nhận trong mục Lời mời nhận.");
            }
            Model.Friendship pending = new Model.Friendship(
                    actor, target, actor, Model.FriendshipStatus.PENDING, System.currentTimeMillis());
            change(key, null, pending);
            return "Đã gửi lời mời kết bạn tới " + target + ".";
        }
    }

    public String accept(String actor, String sender) {
        synchronized (store) {
            Model.Friendship pending = requireIncoming(actor, sender);
            Model.Friendship accepted = new Model.Friendship(
                    pending.userA(), pending.userB(), pending.requestedBy(),
                    Model.FriendshipStatus.ACCEPTED, pending.createdAt());
            change(Model.Friendship.key(actor, pending.other(actor)), pending, accepted);
            return "Bạn và " + pending.other(actor) + " đã trở thành bạn bè.";
        }
    }

    public String reject(String actor, String sender) {
        synchronized (store) {
            Model.Friendship pending = requireIncoming(actor, sender);
            change(Model.Friendship.key(actor, pending.other(actor)), pending, null);
            return "Đã từ chối lời mời kết bạn của " + pending.other(actor) + ".";
        }
    }

    public String cancel(String actor, String target) {
        synchronized (store) {
            requireActor(actor);
            target = requireTarget(actor, target);
            String key = Model.Friendship.key(actor, target);
            Model.Friendship pending = store.friendships.get(key);
            if (pending == null || pending.status() != Model.FriendshipStatus.PENDING
                    || !pending.requestedBy().equals(actor)) {
                throw new IllegalArgumentException("Không có lời mời đã gửi tới tài khoản này.");
            }
            change(key, pending, null);
            return "Đã hủy lời mời kết bạn tới " + target + ".";
        }
    }

    public String remove(String actor, String target) {
        synchronized (store) {
            requireActor(actor);
            target = requireTarget(actor, target);
            String key = Model.Friendship.key(actor, target);
            Model.Friendship existing = store.friendships.get(key);
            if (existing == null || existing.status() != Model.FriendshipStatus.ACCEPTED) {
                throw new IllegalArgumentException("Tài khoản này chưa có trong danh sách bạn bè.");
            }
            change(key, existing, null);
            return "Đã hủy kết bạn với " + target + ".";
        }
    }

    public List<Model.Friendship> relationships(String actor) {
        synchronized (store) {
            requireActor(actor);
            List<Model.Friendship> result = new ArrayList<>();
            for (Model.Friendship relation : store.friendships.values()) {
                if (relation.involves(actor)) {
                    result.add(relation);
                }
            }
            result.sort(Comparator.comparing(relation -> relation.other(actor)));
            return List.copyOf(result);
        }
    }

    public String relationState(String actor, String target) {
        synchronized (store) {
            requireActor(actor);
            if (target == null || actor.equals(target)) {
                return "NONE";
            }
            Model.Friendship relation = store.friendships.get(Model.Friendship.key(actor, target));
            if (relation == null) {
                return "NONE";
            }
            if (relation.status() == Model.FriendshipStatus.ACCEPTED) {
                return "FRIEND";
            }
            return relation.requestedBy().equals(actor) ? "OUTGOING" : "INCOMING";
        }
    }

    /** Caller handles the enclosing user deletion, SQL cascade, and Store.save atomically. */
    public void removeUserRelationships(String username) {
        synchronized (store) {
            store.friendships.entrySet().removeIf(entry -> entry.getValue().involves(username));
        }
    }

    /** Delete an account and its relationships as one durable application operation. */
    public void deleteUser(String username) {
        synchronized (store) {
            String target = username == null ? "" : username.trim();
            Model.User previousUser = store.users.get(target);
            if (previousUser == null) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản '" + target + "'.");
            }
            Map<String, Model.Friendship> previousRelationships = new HashMap<>(store.friendships);
            boolean[] snapshotAttempted = {false};
            Runnable save = () -> {
                store.users.remove(target);
                removeUserRelationships(target);
                snapshotAttempted[0] = true;
                store.save(storeFile);
            };

            try {
                if (usesDatabase()) {
                    db.deleteUserWithSnapshot(target, save);
                } else {
                    save.run();
                }
            } catch (RuntimeException failure) {
                store.users.put(target, previousUser);
                store.friendships.clear();
                store.friendships.putAll(previousRelationships);
                if (snapshotAttempted[0]) {
                    try {
                        store.save(storeFile);
                    } catch (RuntimeException rollbackFailure) {
                        failure.addSuppressed(rollbackFailure);
                    }
                }
                throw new IllegalStateException("Không thể lưu thao tác xóa tài khoản. Vui lòng thử lại.", failure);
            }
        }
    }

    private Model.Friendship requireIncoming(String actor, String sender) {
        requireActor(actor);
        sender = requireTarget(actor, sender);
        Model.Friendship pending = store.friendships.get(Model.Friendship.key(actor, sender));
        if (pending == null || pending.status() != Model.FriendshipStatus.PENDING
                || !pending.requestedBy().equals(sender)) {
            throw new IllegalArgumentException("Không có lời mời kết bạn từ tài khoản này.");
        }
        return pending;
    }

    private void requireActor(String actor) {
        if (actor == null || !store.users.containsKey(actor)) {
            throw new IllegalArgumentException("Bạn cần đăng nhập bằng tài khoản còn hoạt động.");
        }
    }

    private String requireTarget(String actor, String target) {
        target = target == null ? "" : target.trim();
        if (actor.equals(target)) {
            throw new IllegalArgumentException("Bạn không thể kết bạn với chính mình.");
        }
        if (!store.users.containsKey(target)) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản '" + target + "'.");
        }
        return target;
    }

    private boolean usesDatabase() {
        return db != null && db.isAvailable();
    }

    private void change(String key, Model.Friendship previous, Model.Friendship next) {
        boolean[] snapshotAttempted = {false};
        Runnable save = () -> {
            restore(key, next);
            snapshotAttempted[0] = true;
            store.save(storeFile);
        };
        try {
            if (usesDatabase()) {
                db.changeFriendship(previous, next, save);
            } else {
                save.run();
            }
        } catch (RuntimeException failure) {
            restore(key, previous);
            if (snapshotAttempted[0]) {
                try {
                    store.save(storeFile);
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw new IllegalStateException("Không thể lưu thay đổi kết bạn. Vui lòng thử lại.", failure);
        }
    }

    private void restore(String key, Model.Friendship relation) {
        if (relation == null) {
            store.friendships.remove(key);
        } else {
            store.friendships.put(key, relation);
        }
    }
}

package quiz;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Atomic file snapshots for private messages, independent of database availability.
 * Callers needing account authorization acquire Model.Store before this store's monitor.
 */
public class ChatStore {
    private final Path file;
    private State state;

    static final class State implements Serializable {
        private static final long serialVersionUID = 1L;
        long nextMessageId = 1;
        final LinkedHashMap<Long, ChatService.Message> messages = new LinkedHashMap<>();
        final Map<ReadCursor, Long> readThrough = new HashMap<>();
        final Map<NonceKey, Long> nonceMessages = new HashMap<>();
        final Map<String, Long> lastChatSent = new HashMap<>();

        State() {}

        State(State previous) {
            nextMessageId = previous.nextMessageId;
            messages.putAll(previous.messages);
            readThrough.putAll(previous.readThrough);
            nonceMessages.putAll(previous.nonceMessages);
            lastChatSent.putAll(previous.lastChatSent);
        }
    }

    record ReadCursor(String reader, String other) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    record NonceKey(String sender, String nonce) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public ChatStore(Path folder) throws IOException {
        Objects.requireNonNull(folder, "folder");
        Files.createDirectories(folder);
        file = folder.resolve("chat.dat").toAbsolutePath();
        state = readSnapshot();
    }

    public Path file() { return file; }

    synchronized State state() { return state; }

    /** Save first, publish second: a failed write never changes visible application state. */
    synchronized void transact(Consumer<State> mutation) {
        State next = new State(state);
        mutation.accept(next);
        save(next);
        state = next;
    }

    /**
     * Purges private conversations before removing an account, so a recreated username cannot
     * inherit them. The caller holds Model.Store while invoking its account deletion callback.
     */
    public synchronized void purgeUserData(String username, Runnable deleteAccount) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(deleteAccount, "deleteAccount");
        State previous = state;
        State next = new State(previous);
        next.messages.values().removeIf(m -> m.sender().equals(username) || m.recipient().equals(username));
        next.nonceMessages.entrySet().removeIf(e -> !next.messages.containsKey(e.getValue()));
        next.readThrough.keySet().removeIf(k -> k.reader().equals(username) || k.other().equals(username));
        next.lastChatSent.remove(username);
        save(next);
        state = next;
        try {
            deleteAccount.run();
        } catch (RuntimeException | Error failure) {
            state = previous;
            try { save(previous); }
            catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            throw failure;
        }
    }

    private void save(State snapshot) {
        try { writeSnapshot(snapshot); }
        catch (IOException failure) {
            throw new IllegalStateException("Không thể lưu dữ liệu tin nhắn. Vui lòng thử lại.", failure);
        }
    }

    /** Overridable only to inject disk failures in isolated tests. */
    protected void writeSnapshot(State snapshot) throws IOException {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        boolean published = false;
        try {
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(snapshot);
                output.flush();
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            published = true;
        } finally {
            if (!published) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) { /* Preserve the original write failure. */ }
            }
        }
    }

    private State readSnapshot() throws IOException {
        if (!Files.exists(file)) return new State();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            input.setObjectInputFilter(info -> {
                if (info.depth() > 32) return ObjectInputFilter.Status.REJECTED;
                Class<?> type = info.serialClass();
                if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                while (type.isArray()) type = type.getComponentType();
                String name = type.getName();
                return type.isPrimitive() || name.equals("java.lang.String")
                        || name.equals("java.lang.Long") || name.equals("java.lang.Number")
                        || name.equals("java.util.HashMap") || name.equals("java.util.LinkedHashMap")
                        || name.equals("java.util.Map$Entry")
                        || name.startsWith("quiz.ChatStore$")
                        || name.startsWith("quiz.ChatService$")
                        ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            Object value = input.readObject();
            if (!(value instanceof State loaded)) throw new IOException("Kiểu dữ liệu chat.dat không hợp lệ.");
            validate(loaded);
            return loaded;
        } catch (ClassNotFoundException | RuntimeException failure) {
            throw new IOException("Không đọc được chat.dat; dữ liệu gốc được giữ nguyên.", failure);
        } catch (IOException failure) {
            throw new IOException("Không đọc được chat.dat; dữ liệu gốc được giữ nguyên.", failure);
        }
    }

    private void validate(State loaded) throws IOException {
        if (loaded.messages == null || loaded.readThrough == null || loaded.nonceMessages == null || loaded.lastChatSent == null)
            throw new IOException("Thiếu trường dữ liệu chat.dat.");
        long maxMessage = 0;
        for (var entry : loaded.messages.entrySet()) {
            ChatService.Message message = entry.getValue();
            if (message == null || entry.getKey() != message.id() || message.id() <= maxMessage
                    || message.sender() == null || message.recipient() == null || message.text() == null
                    || message.nonce() == null || message.sender().equals(message.recipient()))
                throw new IOException("Tin nhắn lưu trữ không hợp lệ.");
            maxMessage = message.id();
            if (!Objects.equals(loaded.nonceMessages.get(new NonceKey(message.sender(), message.nonce())), message.id()))
                throw new IOException("Chỉ mục tin nhắn không hợp lệ.");
        }
        for (var entry : loaded.nonceMessages.entrySet()) {
            ChatService.Message message = loaded.messages.get(entry.getValue());
            if (message == null || !entry.getKey().equals(new NonceKey(message.sender(), message.nonce())))
                throw new IOException("Chỉ mục tin nhắn không khớp.");
        }
        if (loaded.nextMessageId <= maxMessage)
            throw new IOException("Mã tin nhắn lưu trữ không hợp lệ.");
        for (var entry : loaded.readThrough.entrySet()) {
            ReadCursor key = entry.getKey();
            if (key == null || key.reader() == null || key.other() == null
                    || key.reader().equals(key.other()) || entry.getValue() == null || entry.getValue() < 0)
                throw new IOException("Trạng thái đọc tin không hợp lệ.");
            if (entry.getValue() > 0) {
                ChatService.Message message = loaded.messages.get(entry.getValue());
                if (message == null || !(message.sender().equals(key.reader()) && message.recipient().equals(key.other())
                        || message.sender().equals(key.other()) && message.recipient().equals(key.reader())))
                    throw new IOException("Mốc đọc không thuộc cuộc trò chuyện lưu trữ.");
            }
        }
            for (var entry : loaded.lastChatSent.entrySet())
                if (entry.getKey() == null || entry.getValue() == null)
                    throw new IOException("Trạng thái gửi không hợp lệ.");
    }
}

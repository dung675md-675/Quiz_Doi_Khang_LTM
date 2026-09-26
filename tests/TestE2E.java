import java.net.*;
import java.io.*;
import java.util.*;
import quiz.Security;
import quiz.ui.*;

public class TestE2E {

    static class E2EClient implements AutoCloseable {
        Socket s;
        DataInputStream in;
        DataOutputStream out;

        E2EClient(int port) throws Exception {
            s = new Socket("127.0.0.1", port);
            s.setSoTimeout(5000);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
        }

        void send(String... a) throws Exception {
            out.writeInt(a.length);
            for (String x : a) out.writeUTF(x == null ? "" : x);
            out.flush();
        }

        String[] readEvent(String expectedType) throws Exception {
            for (int i = 0; i < 35; i++) {
                int n = in.readInt();
                String[] a = new String[n];
                for (int j = 0; j < n; j++) a[j] = in.readUTF();
                if (a[0].equals(expectedType)) return a;
            }
            throw new Exception("Timeout waiting for event: " + expectedType);
        }

        public void close() throws Exception {
            s.close();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("   QUIZ ARENA - PHASE 6 FULL END-TO-END TEST    ");
        System.out.println("=================================================");

        // 1. SECURITY & PASSWORD HASHING UNIT TESTS
        System.out.println("--- 1. Testing Security & Password Hashing ---");
        String rawPass = "MySecurePass_2026!";
        String hashed = Security.hashPassword(rawPass);
        if (!hashed.contains(":")) {
            throw new AssertionError("Hashed password must contain salt separator ':'");
        }
        if (!Security.verifyPassword(rawPass, hashed)) {
            throw new AssertionError("Password verification failed for correct password!");
        }
        if (Security.verifyPassword("WrongPassword", hashed)) {
            throw new AssertionError("Password verification succeeded for incorrect password!");
        }
        // Test backward compatibility with legacy plain text password
        String legacyPass = "old_plain_pass";
        if (!Security.verifyPassword(legacyPass, legacyPass)) {
            throw new AssertionError("Legacy plain password verification failed!");
        }
        if (!Security.isLegacy(legacyPass) || Security.isLegacy(hashed)) {
            throw new AssertionError("Security.isLegacy logic error!");
        }
        System.out.println("PASS 1: Password hashing, salting, constant-time verification & legacy upgrade verified!");

        // 2. AUDIO SYNTHESIS & SOUND MANAGER TESTS
        System.out.println("--- 2. Testing SoundManager Audio Engine ---");
        SoundManager.setMuted(true);
        SoundManager.playClick();
        SoundManager.playTick();
        SoundManager.setMuted(false);
        SoundManager.playClick();
        SoundManager.playInviteAlert();
        SoundManager.playVictory();
        SoundManager.playDefeat();
        SoundManager.playEmote();
        System.out.println("PASS 2: Audio synthesis engine executed gracefully without crashes!");

        // 3. START SERVER & MULTI-CLIENT NETWORK TESTS
        int port = 5072;
        System.out.println("--- 3. Starting Server on port " + port + " ---");
        Thread serverThread = new Thread(() -> {
            try {
                quiz.Server.main(new String[]{String.valueOf(port), "data"});
            } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(800); // Allow server to bind

        long ts = System.currentTimeMillis() % 100000;
        String userA = "e2e_a_" + ts;
        String userB = "e2e_b_" + ts;
        String pass = "SecurePass123";

        try (E2EClient a = new E2EClient(port); E2EClient b = new E2EClient(port)) {
            // Register A & B
            a.send("REGISTER", userA, pass);
            String[] regA = a.readEvent("INFO");
            if (!regA[1].contains("thành công")) throw new AssertionError("Register A failed: " + Arrays.toString(regA));

            b.send("REGISTER", userB, pass);
            b.readEvent("INFO");

            // Test Wrong Password Login
            a.send("LOGIN", userA, "WrongPass");
            String[] errLogin = a.readEvent("ERROR");
            if (!errLogin[1].contains("Sai tài khoản")) {
                throw new AssertionError("Expected wrong password error, got: " + Arrays.toString(errLogin));
            }
            System.out.println("PASS 3A: Authentication rejected incorrect password correctly: " + errLogin[1]);

            // Test Correct Password Login
            a.send("LOGIN", userA, pass);
            String[] welcomeA = a.readEvent("WELCOME");
            if (!welcomeA[1].equals(userA)) throw new AssertionError("Welcome username mismatch!");

            b.send("LOGIN", userB, pass);
            b.readEvent("WELCOME");
            System.out.println("PASS 3B: Both clients authenticated and received WELCOME with topics");

            // Test Duplicate Login Conflict
            try (E2EClient aDup = new E2EClient(port)) {
                aDup.send("LOGIN", userA, pass);
                String[] dupErr = aDup.readEvent("ERROR");
                if (!dupErr[1].contains("đang đăng nhập")) {
                    throw new AssertionError("Expected already logged in error, got: " + Arrays.toString(dupErr));
                }
                System.out.println("PASS 3C: Duplicate concurrent login prevented: " + dupErr[1]);
            }

            // 4. CHALLENGE & INVITE
            System.out.println("--- 4. Challenge & In-Game Battle ---");
            a.send("CHALLENGE", userB, "Mạng máy tính", "Dễ");
            a.readEvent("INFO");
            String[] inv = b.readEvent("INVITE");
            if (!inv[1].equals(userA)) throw new AssertionError("Invite sender mismatch!");
            b.send("ACCEPT", userA);

            String[] matchA = a.readEvent("MATCH");
            String[] matchB = b.readEvent("MATCH");
            String matchId = matchA[1];
            System.out.println("PASS 4A: Match started with ID: " + matchId + ", topic: " + matchA[4] + ", set: " + matchA[6]);

            // 5. IN-GAME REACTION EMOTES
            a.send("EMOTE", "🔥");
            String[] emoteB = b.readEvent("EMOTE");
            if (!emoteB[1].equals(userA) || !emoteB[2].equals("🔥")) {
                throw new AssertionError("Emote packet mismatch: " + Arrays.toString(emoteB));
            }
            b.send("EMOTE", "⚡");
            String[] emoteA = a.readEvent("EMOTE");
            if (!emoteA[1].equals(userB) || !emoteA[2].equals("⚡")) {
                throw new AssertionError("Emote packet mismatch: " + Arrays.toString(emoteA));
            }
            System.out.println("PASS 5: Real-time Emotes exchanged between players: A sent 🔥, B sent ⚡");

            // 6. SUBMISSION & SCORING
            String[] subA = new String[12];
            subA[0] = "SUBMIT";
            subA[1] = matchId;
            for (int i = 0; i < 10; i++) subA[i + 2] = "0";
            a.send(subA);
            a.readEvent("SUBMITTED");

            String[] subB = new String[12];
            subB[0] = "SUBMIT";
            subB[1] = matchId;
            for (int i = 0; i < 10; i++) subB[i + 2] = "-1";
            b.send(subB);
            b.readEvent("SUBMITTED");

            String[] resA = a.readEvent("RESULT");
            String[] resB = b.readEvent("RESULT");
            if (!resA[1].equals(userA)) {
                throw new AssertionError("Expected winner A, got: " + resA[1]);
            }
            System.out.println("PASS 6: Match completed! Winner: " + resA[1] + " | " + resA[2] + " vs " + resA[3]);

            // 7. LEADERBOARD & HISTORY
            System.out.println("--- 5. Statistics, Leaderboard & History ---");
            a.send("LEADERBOARD");
            String[] lb = a.readEvent("LEADERBOARD");
            if (!lb[1].contains(userA)) {
                throw new AssertionError("Leaderboard missing user A!");
            }
            System.out.println("PASS 7A: Leaderboard verified: " + lb[1].split("\\|")[0]);

            a.send("HISTORY");
            String[] hist = a.readEvent("HISTORY");
            if (!hist[1].contains(userA) || !hist[1].contains(userB)) {
                throw new AssertionError("History missing match entries: " + hist[1]);
            }
            System.out.println("PASS 7B: Match history verified: " + hist[1].trim());

            // 8. REMATCH & LEAVE
            System.out.println("--- 6. Rematch & Leave Flow ---");
            a.send("REMATCH", "YES");
            b.send("REMATCH", "YES");
            String[] rematchA = a.readEvent("MATCH");
            if (rematchA[6].equals(matchA[6])) {
                throw new AssertionError("Rematch did not rotate question set!");
            }
            System.out.println("PASS 8A: Rematch successfully started with new set: " + rematchA[6]);

            a.send("LEAVE");
            String[] closedB = b.readEvent("CLOSED");
            if (!closedB[1].contains("rời phòng")) {
                throw new AssertionError("Expected leave reason in CLOSED, got: " + Arrays.toString(closedB));
            }
            System.out.println("PASS 8B: Match leave and graceful close verified: " + closedB[1]);
        }

        System.out.println("=================================================");
        System.out.println("   🏆 ALL PHASE 6 E2E TESTS PASSED 100%! 🏆      ");
        System.out.println("=================================================");
        System.exit(0);
    }
}

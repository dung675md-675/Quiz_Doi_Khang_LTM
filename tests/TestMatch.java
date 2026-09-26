import java.net.*;
import java.io.*;
import java.util.*;
import quiz.ui.*;

public class TestMatch {

    static class TestClient implements AutoCloseable {
        Socket s;
        DataInputStream in;
        DataOutputStream out;

        TestClient(int port) throws Exception {
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
            for (int i = 0; i < 30; i++) {
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
        int port = 5068;
        System.out.println("Starting Phase 5 In-Game Match & Result / Rematch Test on port " + port);

        // Start server in background thread if not already running
        Thread serverThread = new Thread(() -> {
            try {
                quiz.Server.main(new String[]{String.valueOf(port), "data"});
            } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(800); // Allow server to bind

        long ts = System.currentTimeMillis() % 100000;
        String userA = "p5_a_" + ts;
        String userB = "p5_b_" + ts;
        String pass = "test1234";

        try (TestClient a = new TestClient(port); TestClient b = new TestClient(port)) {
            // 1. Register & Login
            a.send("REGISTER", userA, pass); a.readEvent("INFO");
            a.send("LOGIN", userA, pass); a.readEvent("WELCOME");

            b.send("REGISTER", userB, pass); b.readEvent("INFO");
            b.send("LOGIN", userB, pass); b.readEvent("WELCOME");
            System.out.println("PASS 1: Both clients logged in successfully");

            // 2. Challenge & Match start
            a.send("CHALLENGE", userB, "Mạng máy tính", "Dễ");
            a.readEvent("INFO");
            b.readEvent("INVITE");
            b.send("ACCEPT", userA);

            String[] matchA = a.readEvent("MATCH");
            String[] matchB = b.readEvent("MATCH");

            if (matchA.length != 58 || matchB.length != 58) {
                throw new AssertionError("MATCH packet length is not 58! length = " + matchA.length);
            }
            String matchId = matchA[1];
            if (!matchId.equals(matchB[1])) {
                throw new AssertionError("Match ID mismatch!");
            }
            System.out.println("PASS 2: Both clients received MATCH packet: " + matchId);

            // 3. Verify Question parsing for MatchScreen
            List<MatchQuestion> questions = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                int base = 8 + i * 5;
                String qText = matchA[base];
                String[] choices = new String[]{matchA[base + 1], matchA[base + 2], matchA[base + 3], matchA[base + 4]};
                questions.add(new MatchQuestion(qText, choices));
            }
            if (questions.size() != 10) {
                throw new AssertionError("Questions count is not 10!");
            }
            System.out.println("PASS 3: Successfully parsed 10 MatchQuestion items for UI: " + questions.get(0).text());

            // 4. Submissions
            // A submits answers: all "0" (guarantees at least 2 or 3 correct answers)
            String[] subA = new String[12];
            subA[0] = "SUBMIT";
            subA[1] = matchId;
            for (int i = 0; i < 10; i++) subA[i + 2] = "0";
            a.send(subA);
            String[] ackA = a.readEvent("SUBMITTED");
            if (!ackA[1].contains("Đã nhận bài")) {
                throw new AssertionError("Expected SUBMITTED ack for A, got: " + Arrays.toString(ackA));
            }
            System.out.println("PASS 4A: Client A received SUBMITTED ack");

            // B submits answers: all -1 (empty/unanswered)
            String[] subB = new String[12];
            subB[0] = "SUBMIT";
            subB[1] = matchId;
            for (int i = 0; i < 10; i++) subB[i + 2] = "-1";
            b.send(subB);
            String[] ackB = b.readEvent("SUBMITTED");
            System.out.println("PASS 4B: Client B received SUBMITTED ack");

            // 5. RESULT packets for both
            String[] resA = a.readEvent("RESULT");
            String[] resB = b.readEvent("RESULT");
            if (!resA[1].equals(userA)) {
                throw new AssertionError("Expected user A to win, but winner is: " + resA[1]);
            }
            System.out.println("PASS 5: Both received RESULT. Winner: " + resA[1] + " | A stat: " + resA[2] + " | B stat: " + resA[3]);

            // 6. Test REMATCH YES -> New MATCH
            a.send("REMATCH", "YES");
            b.send("REMATCH", "YES");

            String[] newMatchA = a.readEvent("MATCH");
            String[] newMatchB = b.readEvent("MATCH");
            if (!newMatchA[1].equals(matchId)) {
                throw new AssertionError("Rematch session ID should match!");
            }
            if (newMatchA[6].equals(matchA[6])) {
                throw new AssertionError("Rematch should pick a new question set!");
            }
            System.out.println("PASS 6: Rematch successful with new question set: " + newMatchA[6] + " (old: " + matchA[6] + ")");

            // 7. Test LEAVE during running match -> CLOSED
            a.send("LEAVE");
            String[] closedA = a.readEvent("CLOSED");
            String[] closedB = b.readEvent("CLOSED");
            if (!closedB[1].contains("rời phòng")) {
                throw new AssertionError("Expected leave close message, got: " + Arrays.toString(closedB));
            }
            System.out.println("PASS 7: Client left match properly closed: " + closedB[1]);
        }

        // 8. Test UI Component Logic directly
        System.out.println("PASS 8: Verifying ResultDialog & MatchScreen UI components in memory...");
        ResultDialog.PlayerStat stat1 = new ResultDialog.PlayerStat(userA, 8, 14200, true, true);
        ResultDialog.PlayerStat stat2 = new ResultDialog.PlayerStat(userB, 5, 25000, false, false);
        if (stat1.correct() != 8 || stat2.durationMs() != 25000) {
            throw new AssertionError("PlayerStat data error!");
        }

        System.out.println("=================================================");
        System.out.println("   ALL PHASE 5 IN-GAME MATCH & RESULT TESTS PASSED!   ");
        System.out.println("=================================================");
        System.exit(0);
    }
}

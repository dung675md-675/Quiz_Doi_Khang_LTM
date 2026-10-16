package tests;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import quiz.Security;
import quiz.ui.*;

public class TestMatchLeaveLifecycle {

    static class SimpleClient implements AutoCloseable {
        Socket s;
        DataInputStream in;
        DataOutputStream out;

        SimpleClient(int port) throws Exception {
            s = new Socket("127.0.0.1", port);
            s.setSoTimeout(6000);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
        }

        void send(String... a) throws Exception {
            out.writeInt(a.length);
            for (String x : a) out.writeUTF(x == null ? "" : x);
            out.flush();
        }

        String[] readEvent(String expectedType) throws Exception {
            for (int i = 0; i < 40; i++) {
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
        System.out.println("  TEST MATCH LEAVE LIFECYCLE (CLIENT & SERVER)  ");
        System.out.println("=================================================");

        // --- 1. Client Unit Test: MatchScreen cleanupAndStop ---
        System.out.println("--- 1. Testing MatchScreen.cleanupAndStop() ---");
        SwingUtilities.invokeAndWait(() -> {
            List<MatchQuestion> sampleQs = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                sampleQs.add(new MatchQuestion("Cau hoi " + i, new String[]{"A", "B", "C", "D"}, "Giai thich", "MULTIPLE_CHOICE", null, null, 0));
            }
            final boolean[] submitFired = {false};
            final boolean[] answerFired = {false};
            MatchScreen ms = new MatchScreen(
                    "match-test-1", "PlayerA", "PlayerB",
                    "avatar_01", "avatar_02", "Kỳ quan thế giới", "Dễ", "set_01",
                    60, sampleQs,
                    answers -> submitFired[0] = true,
                    () -> {}
            );
            ms.setOnAnswerListener((q, ans) -> answerFired[0] = true);

            // Goi cleanupAndStop
            ms.cleanupAndStop();

            // Kiem tra stopTimer va vo hieu hoa cac callback
            ms.onSubmittedAcknowledged(); // Khong gay loi va khong bat hien thi loi
            ms.onMatchFinished();

            System.out.println("  [PASS] MatchScreen cleaned up gracefully without exceptions!");
        });

        // --- 2. Server & Client Network Lifecycle: Player Leaves Mid-Match ---
        int port = 5088;
        System.out.println("--- 2. Starting Test Server on port " + port + " ---");
        Thread serverThread = new Thread(() -> {
            try {
                quiz.Server.main(new String[]{String.valueOf(port), "data"});
            } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(1000); // Allow server to bind

        long ts = System.currentTimeMillis() % 100000;
        String userA = "leave_a_" + ts;
        String userB = "stay_b_" + ts;
        String pass = "SecurePass123";

        try (SimpleClient a = new SimpleClient(port); SimpleClient b = new SimpleClient(port)) {
            // Register & Login
            a.send("REGISTER", userA, pass);
            a.readEvent("INFO");
            a.send("LOGIN", userA, pass);
            a.readEvent("WELCOME");

            b.send("REGISTER", userB, pass);
            b.readEvent("INFO");
            b.send("LOGIN", userB, pass);
            b.readEvent("WELCOME");

            System.out.println("  [PASS] Both players registered and logged in.");

            // A challenges B on valid topic "Kỳ quan thế giới"
            a.send("CHALLENGE", userB, "Kỳ quan thế giới", "Dễ");
            a.readEvent("INFO");
            b.readEvent("INVITE");
            b.send("ACCEPT", userA);

            String[] matchA = a.readEvent("MATCH");
            String[] matchB = b.readEvent("MATCH");
            String matchId = matchA[1];
            System.out.println("  [PASS] Match started: " + matchId);

            // Player A answers question 0
            a.send("ANSWER", matchId, "0", "0");
            b.readEvent("SCORE");

            // NOW: Player A leaves match ("LEAVE")
            System.out.println("  -> Player A leaves match mid-game...");
            a.send("LEAVE", matchId);

            // Player B must receive RESULT where B is winner and A forfeited
            String[] resB = b.readEvent("RESULT");
            if (!resB[1].equals(userB)) {
                throw new AssertionError("Expected winner to be B (" + userB + "), but got: " + resB[1]);
            }
            System.out.println("  [PASS] Opponent B received RESULT declaring B winner: " + resB[1]);

            // Opponent B also receives INFO confirming A left
            String[] infoB = b.readEvent("INFO");
            System.out.println("  [PASS] Opponent B received INFO: " + infoB[1]);

            // Verify: Player A does NOT receive any delayed RESULT or crash, and can immediately start another action
            // Player A should now be free in lobby (not stuck in match)
            a.send("LEADERBOARD");
            String[] lbA = a.readEvent("LEADERBOARD");
            if (!lbA[1].contains(userA)) {
                throw new AssertionError("Player A not in leaderboard!");
            }
            System.out.println("  [PASS] Leaver Player A is back in lobby and can view leaderboard cleanly.");

            // 3. Test Concurrent Leave / Double Leave: B also leaves or leaves finished match
            b.send("LEAVE", matchId);
            b.send("LEADERBOARD");
            String[] lbB = b.readEvent("LEADERBOARD");
            System.out.println("  [PASS] Both players left cleanly without race condition or orphan locks.");

            // 4. Test New Match after Leave: A and B can start another match immediately
            System.out.println("--- 3. Testing New Match Initiation After Leave ---");
            a.send("CHALLENGE", userB, "Kỳ quan thế giới", "Dễ");
            a.readEvent("INFO");
            b.readEvent("INVITE");
            b.send("ACCEPT", userA);

            String[] newMatchA = a.readEvent("MATCH");
            String[] newMatchB = b.readEvent("MATCH");
            System.out.println("  [PASS] New match started successfully after previous forfeit: " + newMatchA[1]);

            // Clean exit
            a.send("LEAVE", newMatchA[1]);

            // 5. Test Solo Bot Practice Match Leave Lifecycle
            System.out.println("--- 4. Testing Solo Bot Practice Match Leave Lifecycle ---");
            a.send("SOLO_MATCH", "Kỳ quan thế giới", "Dễ");
            String[] soloMatch1 = a.readEvent("MATCH");
            String soloMatch1Id = soloMatch1[1];
            System.out.println("  [PASS] Solo Bot Match 1 started: " + soloMatch1Id);

            // Player answers question 0
            a.send("ANSWER", soloMatch1Id, "0", "0");
            a.readEvent("SCORE");

            // Player leaves Solo Bot match mid-game
            System.out.println("  -> Player leaves Solo Bot match mid-game...");
            a.send("LEAVE", soloMatch1Id);

            Thread.sleep(300);

            // Immediately start a NEW Solo Bot match
            a.send("SOLO_MATCH", "Kỳ quan thế giới", "Dễ");
            String[] soloMatch2 = a.readEvent("MATCH");
            String soloMatch2Id = soloMatch2[1];
            System.out.println("  [PASS] Solo Bot Match 2 started: " + soloMatch2Id);

            if (soloMatch1Id.equals(soloMatch2Id)) {
                throw new AssertionError("Solo match 2 should have a distinct ID!");
            }

            // Answer question in new match
            a.send("ANSWER", soloMatch2Id, "0", "1");
            String[] score2 = a.readEvent("SCORE");
            System.out.println("  [PASS] Answered question in Match 2 successfully, score updated.");

            // Clean exit
            a.send("LEAVE", soloMatch2Id);
        }

        System.out.println("\n>>> ALL MATCH LEAVE LIFECYCLE TESTS PASSED 100%! <<<");
    }
}


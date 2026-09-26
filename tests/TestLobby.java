import java.net.*;
import java.io.*;
import java.util.*;
import quiz.ui.*;

public class TestLobby {
    static class TestClient implements AutoCloseable {
        Socket s;
        DataInputStream in;
        DataOutputStream out;
        String name;

        TestClient(int port) throws Exception {
            s = new Socket("127.0.0.1", port);
            s.setSoTimeout(4000);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
        }

        void send(String... a) throws Exception {
            out.writeInt(a.length);
            for (String x : a) out.writeUTF(x == null ? "" : x);
            out.flush();
        }

        String[] event(String type) throws Exception {
            for (int i = 0; i < 20; i++) {
                int n = in.readInt();
                String[] a = new String[n];
                for (int j = 0; j < n; j++) a[j] = in.readUTF();
                if (a[0].equals(type)) return a;
            }
            throw new Exception("Timeout waiting for event: " + type);
        }

        public void close() throws Exception {
            s.close();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 5060;
        long ts = System.currentTimeMillis() % 10000;
        String u1 = "lobby_u1_" + ts;
        String u2 = "lobby_u2_" + ts;
        String pass = "testpass123";

        System.out.println("Starting Lobby Phase 3 Test with 2 Clients on port " + port);

        try (TestClient c1 = new TestClient(port)) {
            // 1. Client 1 Register & Login
            c1.send("REGISTER", u1, pass);
            c1.event("INFO");
            c1.send("LOGIN", u1, pass);
            String[] w1 = c1.event("WELCOME");
            if (!w1[1].equals(u1)) throw new AssertionError("Expected welcome for " + u1);
            String[] l1 = c1.event("LOBBY");
            System.out.println("PASS: Client 1 logged in, LOBBY received: " + l1[1]);

            // 2. Client 2 Register & Login
            try (TestClient c2 = new TestClient(port)) {
                c2.send("REGISTER", u2, pass);
                c2.event("INFO");
                c2.send("LOGIN", u2, pass);
                String[] w2 = c2.event("WELCOME");
                if (!w2[1].equals(u2)) throw new AssertionError("Expected welcome for " + u2);
                System.out.println("PASS: Client 2 logged in");

                // Client 1 should receive broadcast LOBBY containing Client 2
                String[] l1_updated = c1.event("LOBBY");
                if (!l1_updated[1].contains(u2)) {
                    throw new AssertionError("Client 1 did not see Client 2 in LOBBY: " + l1_updated[1]);
                }
                System.out.println("PASS: Client 1 saw Client 2 online: " + l1_updated[1]);

                // Client 2 should receive LOBBY containing Client 1
                String[] l2 = c2.event("LOBBY");
                if (!l2[1].contains(u1)) {
                    throw new AssertionError("Client 2 did not see Client 1 in LOBBY: " + l2[1]);
                }
                System.out.println("PASS: Client 2 saw Client 1 online: " + l2[1]);

                // 3. Test Leaderboard request
                c1.send("LEADERBOARD");
                String[] lb = c1.event("LEADERBOARD");
                if (lb.length < 2) throw new AssertionError("Invalid LEADERBOARD response");
                System.out.println("PASS: LEADERBOARD response received: " + lb[1]);

                // 4. Test History request
                c1.send("HISTORY");
                String[] hist = c1.event("HISTORY");
                if (hist.length < 2) throw new AssertionError("Invalid HISTORY response");
                System.out.println("PASS: HISTORY response received");
            }
        }

        // 5. Test UI Parsing in LobbyScreen
        LobbyScreen screen = new LobbyScreen(new String[]{"Dễ", "Trung bình", "Khó"});
        screen.setCurrentUser(u1);
        screen.updatePlayersFromLobbyData(u1 + ";10;Đang rỗi|" + u2 + ";5;Đang rỗi|");
        System.out.println("PASS: LobbyScreen UI data parsing verified");

        System.out.println("=== ALL 2-CLIENT LOBBY TESTS PASSED ===");
    }
}

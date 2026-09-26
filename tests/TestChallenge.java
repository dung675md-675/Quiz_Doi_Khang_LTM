import java.net.*;
import java.io.*;
import java.util.*;

public class TestChallenge {
    static class PeerClient implements AutoCloseable {
        Socket s;
        DataInputStream in;
        DataOutputStream out;

        PeerClient(int port) throws Exception {
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
            for (int i = 0; i < 25; i++) {
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
        int port = 5061;
        long ts = System.currentTimeMillis() % 10000;
        String uA = "chal_a_" + ts;
        String uB = "chal_b_" + ts;
        String pass = "pass123";

        System.out.println("Starting Challenge & Invite Phase 4 Test on port " + port);

        // ==========================================
        // TEST 1: Challenge & Accept -> MATCH
        // ==========================================
        try (PeerClient a = new PeerClient(port); PeerClient b = new PeerClient(port)) {
            // Register & Login A
            a.send("REGISTER", uA, pass); a.readEvent("INFO");
            a.send("LOGIN", uA, pass); a.readEvent("WELCOME");

            // Register & Login B
            b.send("REGISTER", uB, pass); b.readEvent("INFO");
            b.send("LOGIN", uB, pass); b.readEvent("WELCOME");

            // A challenges B
            a.send("CHALLENGE", uB, "Mạng máy tính", "Dễ");
            a.readEvent("INFO"); // "Đã gửi lời mời đến..."

            // B receives INVITE
            String[] inv = b.readEvent("INVITE");
            if (!inv[1].equals(uA) || !inv[2].equals("Mạng máy tính") || !inv[3].equals("Dễ")) {
                throw new AssertionError("Invalid INVITE received: " + Arrays.toString(inv));
            }
            System.out.println("PASS 1A: B received INVITE from " + inv[1] + " for topic: " + inv[2] + ", time: " + inv[5] + "s");

            // B accepts
            b.send("ACCEPT", uA);

            // Both should receive MATCH packet with 58 fields
            String[] matchA = a.readEvent("MATCH");
            String[] matchB = b.readEvent("MATCH");
            if (matchA.length != 58 || matchB.length != 58) {
                throw new AssertionError("MATCH length is not 58! A: " + matchA.length + ", B: " + matchB.length);
            }
            if (!matchA[1].equals(matchB[1])) {
                throw new AssertionError("Different match IDs!");
            }
            System.out.println("PASS 1B: Both received MATCH with ID: " + matchA[1] + " and 10 questions");

            // Reset match with LEAVE
            a.send("LEAVE");
            b.send("LEAVE");
        }

        // ==========================================
        // TEST 2: Challenge & Reject -> DECLINED
        // ==========================================
        try (PeerClient a = new PeerClient(port); PeerClient b = new PeerClient(port)) {
            String uA2 = "rej_a_" + ts;
            String uB2 = "rej_b_" + ts;

            a.send("REGISTER", uA2, pass); a.readEvent("INFO");
            a.send("LOGIN", uA2, pass); a.readEvent("WELCOME");

            b.send("REGISTER", uB2, pass); b.readEvent("INFO");
            b.send("LOGIN", uB2, pass); b.readEvent("WELCOME");

            // A challenges B
            a.send("CHALLENGE", uB2, "Mạng máy tính", "Dễ");
            a.readEvent("INFO");

            // B receives INVITE
            b.readEvent("INVITE");

            // B rejects
            b.send("REJECT", uA2);

            // A receives DECLINED
            String[] dec = a.readEvent("DECLINED");
            if (!dec[1].contains("từ chối")) {
                throw new AssertionError("Expected decline message, got: " + Arrays.toString(dec));
            }
            System.out.println("PASS 2: A received DECLINED: " + dec[1]);
        }

        // ==========================================
        // TEST 3: Challenge & Disconnect -> DECLINED
        // ==========================================
        try (PeerClient a = new PeerClient(port)) {
            String uA3 = "disc_a_" + ts;
            String uB3 = "disc_b_" + ts;

            a.send("REGISTER", uA3, pass); a.readEvent("INFO");
            a.send("LOGIN", uA3, pass); a.readEvent("WELCOME");

            try (PeerClient b = new PeerClient(port)) {
                b.send("REGISTER", uB3, pass); b.readEvent("INFO");
                b.send("LOGIN", uB3, pass); b.readEvent("WELCOME");

                // A challenges B
                a.send("CHALLENGE", uB3, "Mạng máy tính", "Dễ");
                a.readEvent("INFO");
                b.readEvent("INVITE");

                // B closes socket suddenly (disconnects)
            }

            // A receives DECLINED because B disconnected
            String[] dec = a.readEvent("DECLINED");
            if (!dec[1].contains("đã thoát")) {
                throw new AssertionError("Expected opponent left message, got: " + Arrays.toString(dec));
            }
            System.out.println("PASS 3: A received DECLINED upon B disconnect: " + dec[1]);
        }

        System.out.println("=== ALL CHALLENGE & INVITE TESTS PASSED ===");
    }
}

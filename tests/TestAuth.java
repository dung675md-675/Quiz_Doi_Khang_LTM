import java.net.*;
import java.io.*;
import java.util.*;

public class TestAuth {
    static class TestPeer implements AutoCloseable {
        Socket s;
        DataInputStream in;
        DataOutputStream out;

        TestPeer(int port) throws Exception {
            s = new Socket("127.0.0.1", port);
            s.setSoTimeout(3000);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
        }

        void send(String... a) throws Exception {
            out.writeInt(a.length);
            for (String x : a) out.writeUTF(x == null ? "" : x);
            out.flush();
        }

        String[] read() throws Exception {
            int n = in.readInt();
            String[] a = new String[n];
            for (int i = 0; i < n; i++) a[i] = in.readUTF();
            return a;
        }

        public void close() throws Exception {
            s.close();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 5059;
        String uniqueId = "" + (System.currentTimeMillis() % 100000);
        String testUser = "user_" + uniqueId;
        String testPass = "pass1234";

        System.out.println("Starting Auth Test on port " + port + " with user " + testUser);

        // 1. Test Register invalid username
        try (TestPeer c = new TestPeer(port)) {
            c.send("REGISTER", "ab", "1234"); // too short (< 3)
            String[] res = c.read();
            if (!res[0].equals("ERROR")) throw new AssertionError("Expected ERROR for short username, got: " + Arrays.toString(res));
            System.out.println("PASS: Register short username rejected");
        }

        // 2. Test Register invalid password
        try (TestPeer c = new TestPeer(port)) {
            c.send("REGISTER", testUser, "12"); // too short (< 4)
            String[] res = c.read();
            if (!res[0].equals("ERROR")) throw new AssertionError("Expected ERROR for short password, got: " + Arrays.toString(res));
            System.out.println("PASS: Register short password rejected");
        }

        // 3. Test Register valid user
        try (TestPeer c = new TestPeer(port)) {
            c.send("REGISTER", testUser, testPass);
            String[] res = c.read();
            if (!res[0].equals("INFO") || !res[1].contains("thành công")) {
                throw new AssertionError("Expected INFO success, got: " + Arrays.toString(res));
            }
            System.out.println("PASS: Register valid user succeeded");
        }

        // 4. Test Register duplicate user
        try (TestPeer c = new TestPeer(port)) {
            c.send("REGISTER", testUser, testPass);
            String[] res = c.read();
            if (!res[0].equals("ERROR") || !res[1].contains("tồn tại")) {
                throw new AssertionError("Expected ERROR duplicate user, got: " + Arrays.toString(res));
            }
            System.out.println("PASS: Duplicate registration rejected");
        }

        // 5. Test Login wrong password
        try (TestPeer c = new TestPeer(port)) {
            c.send("LOGIN", testUser, "wrong_pass");
            String[] res = c.read();
            if (!res[0].equals("ERROR") || !res[1].contains("Sai tài khoản hoặc mật khẩu")) {
                throw new AssertionError("Expected ERROR wrong pass, got: " + Arrays.toString(res));
            }
            System.out.println("PASS: Login wrong password rejected");
        }

        // 6. Test Login non-existent user
        try (TestPeer c = new TestPeer(port)) {
            c.send("LOGIN", "non_existent_9999", "random_pass");
            String[] res = c.read();
            if (!res[0].equals("ERROR")) {
                throw new AssertionError("Expected ERROR non-existent user, got: " + Arrays.toString(res));
            }
            System.out.println("PASS: Login non-existent user rejected");
        }

        // 7. Test Login success
        try (TestPeer c1 = new TestPeer(port)) {
            c1.send("LOGIN", testUser, testPass);
            String[] res = c1.read();
            if (!res[0].equals("WELCOME") || !res[1].equals(testUser)) {
                throw new AssertionError("Expected WELCOME, got: " + Arrays.toString(res));
            }
            System.out.println("PASS: Login success received WELCOME with topics: " + res[2]);

            // 8. Test Duplicate Login while already online
            try (TestPeer c2 = new TestPeer(port)) {
                c2.send("LOGIN", testUser, testPass);
                String[] res2 = c2.read();
                if (!res2[0].equals("ERROR") || !res2[1].contains("đang đăng nhập")) {
                    throw new AssertionError("Expected ERROR already logged in, got: " + Arrays.toString(res2));
                }
                System.out.println("PASS: Concurrent login with same user rejected");
            }
        }

        System.out.println("=== ALL AUTH TESTS PASSED ===");
    }
}

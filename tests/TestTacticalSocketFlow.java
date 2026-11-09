package tests;

import quiz.Model;
import quiz.Server;
import quiz.Wire;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestTacticalSocketFlow {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BAT DAU KIEM TRA SOCKET FLOW & VALIDATION KY NANG CHIEN THUAT ===");

        testSkillTurnLimitsAndCancel();
        testPointThresholdValidation();
        testAlreadyAnsweredValidation();

        System.out.println(">>> TAT CA CAC TEST CHO SOCKET FLOW & VALIDATION DEU PASS 100%! <<<");
    }

    private static void testSkillTurnLimitsAndCancel() throws Exception {
        System.out.println("1. Kiem tra so luot (1 Atk, 1 Def, 1 Star) va CANCEL khong mat luot...");

        // Setup Match thong qua reflection hoac Server
        Path tmpFolder = Files.createTempDirectory("quiz_tactical_test");
        Server server = new Server(tmpFolder);

        // Tao Match giua Alice va Bob
        Object match = createMockMatch(server, "Alice", "Bob");

        // Alice dung DEFEND
        invokeMethod(match, "handleTacticalAction", "Alice", "DEFEND");
        assertFieldArrayValue(match, "currentTactical", 0, "DEFEND");

        // Alice CANCEL truoc khi nop bai
        invokeMethod(match, "handleTacticalAction", "Alice", "CANCEL");
        assertFieldArrayValue(match, "currentTactical", 0, "NONE");

        // Kiem tra so luot cua Alice van con nguyen 1 Defend vi da CANCEL
        assertFieldArrayInt(match, "defendRemaining", 0, 1);

        System.out.println("   [PASS] CANCEL thanh cong va khong bi tru luot!");
    }

    private static void testPointThresholdValidation() throws Exception {
        System.out.println("2. Kiem tra Server tu choi ATTACK / STAR khi diem < 50...");

        Path tmpFolder = Files.createTempDirectory("quiz_tactical_test2");
        Server server = new Server(tmpFolder);
        Object match = createMockMatch(server, "Alice", "Bob");

        // Set diem cua Alice = 30 (< 50)
        setFieldArrayInt(match, "points", 0, 30);

        // Alice thu dung ATTACK -> phai bi tu choi, currentTactical van la NONE
        invokeMethod(match, "handleTacticalAction", "Alice", "ATTACK");
        assertFieldArrayValue(match, "currentTactical", 0, "NONE");

        // Alice thu dung STAR -> phai bi tu choi
        invokeMethod(match, "handleTacticalAction", "Alice", "STAR");
        assertFieldArrayValue(match, "currentTactical", 0, "NONE");

        // Alice dung DEFEND -> phai duoc chap nhan vi DEFEND luon dung duoc neu con luot
        invokeMethod(match, "handleTacticalAction", "Alice", "DEFEND");
        assertFieldArrayValue(match, "currentTactical", 0, "DEFEND");

        System.out.println("   [PASS] Server chan dung ATTACK / STAR khi < 50 diem, cho phep DEFEND!");
    }

    private static void testAlreadyAnsweredValidation() throws Exception {
        System.out.println("3. Kiem tra Server chan thay doi ky nang sau khi da nop dap an...");

        Path tmpFolder = Files.createTempDirectory("quiz_tactical_test3");
        Server server = new Server(tmpFolder);
        Object match = createMockMatch(server, "Alice", "Bob");

        // Set diem Alice = 100
        setFieldArrayInt(match, "points", 0, 100);

        // Gia su Alice da chon dap an (liveAnswers[0][currentQ] != -1)
        int curQ = getFieldInt(match, "currentQuestionIndex");
        setField2DInt(match, "liveAnswers", 0, curQ, 1);

        // Alice co gang kich hoat ATTACK sau khi da chon dap an -> bi tu choi
        invokeMethod(match, "handleTacticalAction", "Alice", "ATTACK");
        assertFieldArrayValue(match, "currentTactical", 0, "NONE");

        // Alice co gang CANCEL sau khi da chon dap an -> bi tu choi
        invokeMethod(match, "handleTacticalAction", "Alice", "CANCEL");
        assertFieldArrayValue(match, "currentTactical", 0, "NONE");

        System.out.println("   [PASS] Server chan khong cho thay doi ky nang sau khi da nop dap an!");
    }

    private static Object createMockMatch(Server server, String left, String right) throws Exception {
        Class<?> matchClass = Class.forName("quiz.Server$Match");
        java.lang.reflect.Constructor<?> ctor = matchClass.getDeclaredConstructor(Server.class, String.class, String.class, String.class, String.class);
        ctor.setAccessible(true);
        Object m = ctor.newInstance(server, left, right, "Toan", "De");

        // Set Phase = ACTIVE
        Class<?> phaseEnum = Class.forName("quiz.Server$Match$MatchPhase");
        Object activePhase = Enum.valueOf((Class<Enum>) phaseEnum, "ACTIVE");
        Field phaseField = matchClass.getDeclaredField("currentPhase");
        phaseField.setAccessible(true);
        phaseField.set(m, activePhase);

        return m;
    }

    private static void invokeMethod(Object target, String methodName, Object... args) throws Exception {
        for (Method m : target.getClass().getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                m.invoke(target, args);
                return;
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static void assertFieldArrayValue(Object target, String fieldName, int idx, String expected) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        String[] arr = (String[]) f.get(target);
        assert expected.equals(arr[idx]) : "Field " + fieldName + "[" + idx + "] phai la '" + expected + "' nhung la '" + arr[idx] + "'";
    }

    private static void assertFieldArrayInt(Object target, String fieldName, int idx, int expected) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        int[] arr = (int[]) f.get(target);
        assert arr[idx] == expected : "Field " + fieldName + "[" + idx + "] phai la " + expected + " nhung la " + arr[idx];
    }

    private static void setFieldArrayInt(Object target, String fieldName, int idx, int val) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        int[] arr = (int[]) f.get(target);
        arr[idx] = val;
    }

    private static void setField2DInt(Object target, String fieldName, int idx1, int idx2, int val) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        int[][] arr = (int[][]) f.get(target);
        arr[idx1][idx2] = val;
    }

    private static int getFieldInt(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.getInt(target);
    }
}


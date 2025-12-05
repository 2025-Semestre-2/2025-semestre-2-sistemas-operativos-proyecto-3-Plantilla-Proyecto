package tests;

import filesystem.User;
import java.util.Arrays;

/**
 *
 * @author gadyr
 */
public class UserTest {

    public static void main(String[] args) {

        User original = new User(
                42,
                "gadyr",
                "1234abcd",
                "Gadyr C",
                "/home/gadyr",
                1000
        );

        System.out.println("=== TEST: SERIALIZACIÓN / DESERIALIZACIÓN DE USER ===");

        byte[] serialized = original.toBytes();

        // Mostrar bytes (raw)
        printBytes(serialized);

        // Deserializar
        User restored = User.fromBytes(Arrays.copyOf(serialized, serialized.length));

        // Comparaciones campo por campo
        compare("userId", original.getUserId(), restored.getUserId());
        compare("username", original.getUsername(), restored.getUsername());
        compare("passwordHash", original.getPasswordHash(), restored.getPasswordHash());
        compare("fullName", original.getFullName(), restored.getFullName());
        compare("homeDirectory", original.getHomeDirectory(), restored.getHomeDirectory());
        compare("groupId", original.getGroupId(), restored.getGroupId());

        System.out.println("\n=== TEST DE CONTRASEÑA ===");
        System.out.println("Check correcto: " + restored.checkPassword("1234abcd"));
        System.out.println("Check incorrecto: " + restored.checkPassword("otra"));

        System.out.println("\n=== TEST CAMBIO DE CONTRASEÑA ===");
        restored.setPassword("nuevaClave123");
        System.out.println("Check nueva contraseña: " + restored.checkPassword("nuevaClave123"));

        System.out.println("\n=== FIN ===");
    }

    private static void compare(String field, Object expected, Object actual) {
        System.out.println("\n--- " + field + " ---");
        System.out.println(" esperado : " + expected);
        System.out.println(" obtenido : " + actual);

        if ((expected == null && actual == null)
                || (expected != null && expected.equals(actual))) {
            System.out.println(" RESULTADO: OK");
        } else {
            System.out.println(" RESULTADO: ERROR");
        }
    }

    private static void printBytes(byte[] data) {
        System.out.println("\n=== BYTES (RAW) ===");
        for (int i = 0; i < data.length; i++) {
            System.out.print((data[i] & 0xFF) + " ");
        }
        System.out.println("\n=== FIN RAW ===\n");
    }
}

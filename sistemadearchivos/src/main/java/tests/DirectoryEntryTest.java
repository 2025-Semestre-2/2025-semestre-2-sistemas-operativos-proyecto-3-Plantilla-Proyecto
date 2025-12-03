package tests;

import filesystem.DirectoryEntry;
import filesystem.FSConstants;
import java.util.Arrays;

public class DirectoryEntryTest {

    public static void main(String[] args) {

        DirectoryEntry original = new DirectoryEntry(
                42, // inodeNumber
                FSConstants.TYPE_FILE, // entryType
                "hello.txt" // name
        );

        System.out.println("=== DirectoryEntry: Serialización / Deserialización ===");

        // Serializar
        byte[] serialized = original.toBytes();

        // Deserializar
        DirectoryEntry deserialized
                = DirectoryEntry.fromBytes(Arrays.copyOf(serialized, serialized.length));

        // Comparaciones campo a campo
        compare("inodeNumber", original.getInodeNumber(), deserialized.getInodeNumber());
        compare("entryType", original.getEntryType(), deserialized.getEntryType());
        compare("nameLength", original.getNameLength(), deserialized.getNameLength());
        compare("name", original.getName(), deserialized.getName());

        // Verificación de isFree()
        System.out.println("\nisFree() original: " + original.isFree());
        System.out.println("isFree() deserializado: " + deserialized.isFree());

        System.out.println("\n=== Fin ===");
    }

    /* ---------------- UTILIDADES DE DEPURACIÓN ---------------- */
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
}

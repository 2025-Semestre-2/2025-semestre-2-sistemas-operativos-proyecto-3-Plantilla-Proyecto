package tests;

import filesystem.Bitmap;

public class BitmapTest {

    public static void main(String[] args) {

        System.out.println("=== Prueba de Bitmap ===");

        Bitmap bitmap = new Bitmap(16); // 16 bits = 2 bytes

        // Operaciones
        bitmap.allocate(0);
        bitmap.allocate(3);
        bitmap.allocate(7);
        bitmap.free(3);
        bitmap.allocate(10);

        System.out.println("\n--- Estado inicial ---");
        printBits(bitmap);

        // Serializar
        byte[] bytes = bitmap.toBytes();

        // Deserializar
        Bitmap loaded = Bitmap.fromBytes(bytes, 16);

        System.out.println("\n--- Comparación bit a bit ---");
        for (int i = 0; i < bitmap.getSize(); i++) {
            compare("bit[" + i + "]",
                    bitmap.isAllocated(i),
                    loaded.isAllocated(i));
        }

        System.out.println("\n--- Pruebas de búsqueda y conteo ---");
        compare("findFirstFree()", bitmap.findFirstFree(), loaded.findFirstFree());
        compare("countFree()", bitmap.countFree(), loaded.countFree());

        System.out.println("\n=== Fin de pruebas ===");
    }

    private static void printBits(Bitmap b) {
        System.out.print("Bits (1=free, 0=allocated): ");
        for (int i = 0; i < b.getSize(); i++) {
            System.out.print(b.isAllocated(i) ? "0" : "1");
        }
        System.out.println();
    }  
    
    private static void compare(String field, Object expected, Object actual) {
        System.out.println("\n" + field);
        System.out.println(" esperado : " + expected);
        System.out.println(" obtenido : " + actual);
        if ((expected == null && actual == null) ||
                (expected != null && expected.equals(actual))) {
            System.out.println(" RESULTADO: OK");
        } else {
            System.out.println(" RESULTADO: ERROR");
        }
    }
}

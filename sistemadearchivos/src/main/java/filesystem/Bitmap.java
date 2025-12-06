package filesystem;

import java.util.BitSet;

public class Bitmap {
    private BitSet bits;
    private int size;
    
    public Bitmap(int size) {
        this.size = size;
        this.bits = new BitSet(size);
    }
    
    /**
     * Marca un bit como usado (1)
     */
    public void allocate(int index) {
        if (index >= 0 && index < size) {
            bits.set(index);
        }
    }
    
    /**
     * Marca un bit como libre (0)
     */
    public void free(int index) {
        if (index >= 0 && index < size) {
            bits.clear(index);
        }
    }
    
    /**
     * Verifica si un bit está ocupado
     */
    public boolean isAllocated(int index) {
        if (index >= 0 && index < size) {
            return bits.get(index);
        }
        return false;
    }
    
    /**
     * Encuentra el primer bit libre
     */
    public int findFirstFree() {
        for (int i = 0; i < size; i++) {
            if (!bits.get(i)) {
                return i;
            }
        }
        return -1; // No hay espacio libre
    }
    
    /**
     * Cuenta cuántos bits están libres
     */
    public int countFree() {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (!bits.get(i)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Serializa el bitmap a bytes
     */
    public byte[] toBytes() {
        byte[] bytes = bits.toByteArray();
        // Asegurarse de que tenga el tamaño correcto
        int neededSize = (size + 7) / 8; // Redondear hacia arriba
        if (bytes.length < neededSize) {
            byte[] result = new byte[neededSize];
            System.arraycopy(bytes, 0, result, 0, bytes.length);
            return result;
        }
        return bytes;
    }
    
    /**
     * Deserializa un bitmap desde bytes
     */
    public static Bitmap fromBytes(byte[] data, int size) {
        Bitmap bitmap = new Bitmap(size);
        bitmap.bits = BitSet.valueOf(data);
        return bitmap;
    }
    
    public int getSize() {
        return size;
    }
}

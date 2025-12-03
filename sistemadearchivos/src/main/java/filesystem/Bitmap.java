package filesystem;

import java.util.BitSet;

/**
 *
 * @author dylan
 */
public class Bitmap {
    private BitSet bits;
    private int size;
    
    public Bitmap(int size) {
        this.size = size; 
        this.bits = new BitSet(size);
    }
    
    // Getters
    public int getSize() {
        return size;
    }
    
    /**
     * Marcar un bit como usado (1)
     * @param index
     */
    public void allocate(int index) {
        if (index >= 0 && index < size) {
            bits.clear(index);
        }
    }
    
    /** 
     * Marcar un bit como libre (0)
     * @param index
     */
    public void free(int index) {
        if (index >= 0 && index < size) {
            bits.set(index);
        }
    }
    
    /**
     * Verificar si un bit está ocupado
     * @param index
     * @return 
     */
    public boolean isAllocated(int index) {
        if (index >= 0 && index < size) {
            return !bits.get(index);
        }
        return false;
    }
    
    /**
     * Encontrar el primer bit libre
     * @return 
     */
    public int findFirstFree() {
        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                return i;
            }
        }
        return -1; // No hay espacio libre       
    }

    /**
     * Contar cuántos bits están libres
     * @return 
     */
    public int countFree() {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                count++;
            }
        }
        return count;
    }  
    
    /** 
     * Serializa el bitmap a bytes
     * @return 
     */
    public byte[] tobytes() {
        byte[] bytes = bits.toByteArray();
        
        int neededSize = (size + 7) / 8;
        if (bytes.length < neededSize) {
            byte[] result = new byte[neededSize];
            System.arraycopy(bytes, 0, result, 0, bytes.length);
            return result;
        }
        
        return bytes;
    }
    
    /**
     * Deserializa un bitmap desde bytes
     * @param data
     * @param size
     * @return 
     */
    public static Bitmap fromBytes(byte[] data, int size) {
        Bitmap bitmap = new Bitmap(size);
        bitmap.bits = BitSet.valueOf(data);
        return bitmap;
    }
}

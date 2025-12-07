package filesystem;

import java.nio.ByteBuffer;

/**
 *
 * @author dylan
 */
public class DirectoryEntry {

    private int inodeNumber;
    private int entryType;
    private int nameLength;
    private String name;

    public DirectoryEntry() {
        this.inodeNumber = -1;
        this.entryType = FSConstants.TYPE_FREE;
        this.nameLength = 0;
        this.name = "";
    }

    public DirectoryEntry(int inodeNumber, int entryType, String name) {
        this.inodeNumber = inodeNumber;
        this.entryType = entryType;
        this.name = name;
        this.nameLength = name.length();
    }

    // Getters y Setters
    public int getInodeNumber() {
        return inodeNumber;
    }

    public void setInodeNumber(int inodeNumber) {
        this.inodeNumber = inodeNumber;
    }

    public int getEntryType() {
        return entryType;
    }

    public void setEntryType(int entryType) {
        this.entryType = entryType;
    }

    public int getNameLength() {
        return nameLength;
    }

    public void setNameLength(int nameLength) {
        this.nameLength = nameLength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameLength = name.length();
    }

    /**
     * Verifica si la entrada está libre
     * 
     * @return
     */
    public boolean isFree() {
        return inodeNumber == -1 || entryType == FSConstants.TYPE_FREE;
    }

    /**
     * Serializa la entrada a bytes
     * 
     * @return
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(FSConstants.DIR_ENTRY_SIZE);

        buffer.putInt(inodeNumber);
        buffer.putInt(entryType);
        buffer.putInt(nameLength);

        // name (244 bytes)
        byte[] nameBytes = new byte[244];
        if (name != null && !name.isEmpty()) {
            byte[] actualName = name.getBytes();
            System.arraycopy(actualName, 0, nameBytes, 0,
                    Math.min(actualName.length, 244));
        }
        buffer.put(nameBytes);

        return buffer.array();
    }

    /**
     * Deserializa una entrada desde bytes
     * 
     * @param data
     * @return
     */
    public static DirectoryEntry fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        DirectoryEntry entry = new DirectoryEntry();
        entry.inodeNumber = buffer.getInt();
        entry.entryType = buffer.getInt();
        entry.nameLength = buffer.getInt();

        byte[] nameBytes = new byte[244];
        buffer.get(nameBytes);

        if (entry.nameLength > 0 && entry.nameLength <= 244) {
            entry.name = new String(nameBytes, 0, entry.nameLength);
        } else {
            entry.name = "";
        }

        return entry;
    }
}

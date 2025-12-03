package filesystem;

import java.nio.ByteBuffer;

/**
 *
 * @author dylan
 */
public class Superblock {

    private int magicNumber;
    private String fsName;
    private int fsVersion;
    private int blockSize;
    private int totalBlocks;
    private int totalInodes;
    private int freeBlocks;
    private int freeInodes;
    private int rootInode;
    private int allocationStrategy;
    private int inodeBitmapStart;
    private int dataBitmapStart;
    private int inodeTableStart;
    private int dataBlocksStart;
    private long creationTime;
    private long lastMountTime;

    public Superblock() {
        this.magicNumber = FSConstants.MAGIC_NUMBER;
        this.fsName = "myFS";
        this.fsVersion = 1;
        this.blockSize = FSConstants.BLOCK_SIZE;
        this.rootInode = FSConstants.ROOT_INODE;
        this.creationTime = System.currentTimeMillis();
    }

    // Getters y Setters
    public int getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(int magicNumber) {
        this.magicNumber = magicNumber;
    }

    public String getFsName() {
        return fsName;
    }

    public void setFsName(String fsName) {
        this.fsName = fsName;
    }

    public int getFsVersion() {
        return fsVersion;
    }

    public void setFsVersion(int fsVersion) {
        this.fsVersion = fsVersion;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }

    public int getTotalInodes() {
        return totalInodes;
    }

    public void setTotalInodes(int totalInodes) {
        this.totalInodes = totalInodes;
    }

    public int getFreeBlocks() {
        return freeBlocks;
    }

    public void setFreeBlocks(int freeBlocks) {
        this.freeBlocks = freeBlocks;
    }

    public int getFreeInodes() {
        return freeInodes;
    }

    public void setFreeInodes(int freeInodes) {
        this.freeInodes = freeInodes;
    }

    public int getRootInode() {
        return rootInode;
    }

    public void setRootInode(int rootInode) {
        this.rootInode = rootInode;
    }

    public int getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(int allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public int getInodeBitmapStart() {
        return inodeBitmapStart;
    }

    public void setInodeBitmapStart(int inodeBitmapStart) {
        this.inodeBitmapStart = inodeBitmapStart;
    }

    public int getDataBitmapStart() {
        return dataBitmapStart;
    }

    public void setDataBitmapStart(int dataBitmapStart) {
        this.dataBitmapStart = dataBitmapStart;
    }

    public int getInodeTableStart() {
        return inodeTableStart;
    }

    public void setInodeTableStart(int inodeTableStart) {
        this.inodeTableStart = inodeTableStart;
    }

    public int getDataBlocksStart() {
        return dataBlocksStart;
    }

    public void setDataBlocksStart(int dataBlocksStart) {
        this.dataBlocksStart = dataBlocksStart;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getLastMountTime() {
        return lastMountTime;
    }

    public void setLastMountTime(long lastMountTime) {
        this.lastMountTime = lastMountTime;
    }

    /**
     * Serializa el Superblock a un array de bytes
     * @return 
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(FSConstants.BLOCK_SIZE);
        buffer.putInt(magicNumber);
        
        byte[] nameBytes = new byte[32];
        byte[] actualName = fsName.getBytes();
        System.arraycopy(actualName, 0, nameBytes, 0, Math.min(actualName.length, 32));
        buffer.put(nameBytes);
        
        buffer.putInt(fsVersion);
        buffer.putInt(blockSize);
        buffer.putInt(totalBlocks);
        buffer.putInt(totalInodes);
        buffer.putInt(freeBlocks);
        buffer.putInt(freeInodes);
        buffer.putInt(rootInode);
        buffer.putInt(allocationStrategy);
        buffer.putInt(inodeBitmapStart);
        buffer.putInt(dataBitmapStart);
        buffer.putInt(inodeTableStart);
        buffer.putInt(dataBlocksStart);
        buffer.putLong(creationTime);
        buffer.putLong(lastMountTime);       
        
        return buffer.array();
    }
    
    /**
     * Deserializa un Superblock desde un array de bytes    
     * @param data
     * @return 
     */
    public static Superblock fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        Superblock sb = new Superblock();
        
        sb.magicNumber = buffer.getInt();
        
        byte[] nameBytes = new byte[32];
        buffer.get(nameBytes);
        sb.fsName = new String(nameBytes).trim().replace("\0", "");
        
        sb.fsVersion = buffer.getInt();
        sb.blockSize = buffer.getInt();
        sb.totalBlocks = buffer.getInt();
        sb.totalInodes = buffer.getInt();
        sb.freeBlocks = buffer.getInt();
        sb.freeInodes = buffer.getInt();
        sb.rootInode = buffer.getInt();
        sb.allocationStrategy = buffer.getInt();
        sb.inodeBitmapStart = buffer.getInt();
        sb.dataBitmapStart = buffer.getInt();
        sb.inodeTableStart = buffer.getInt();
        sb.dataBlocksStart = buffer.getInt();
        sb.creationTime = buffer.getLong();
        sb.lastMountTime = buffer.getLong();
        
        return sb;
    }
    
    /**
     * Valida si el superblock es válido
     * @return 
     */
    public boolean isValid() {
        return magicNumber == FSConstants.MAGIC_NUMBER;
    }
}

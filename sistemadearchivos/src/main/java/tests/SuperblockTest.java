package tests;

import filesystem.Superblock;
import java.util.Arrays;

/**
 *
 * @author dylan
 */
public class SuperblockTest {

    public static void main(String[] args) {
        Superblock original = new Superblock();
        original.setFsName("TestFS");
        original.setFsVersion(3);
        original.setTotalBlocks(5000);
        original.setTotalInodes(1024);
        original.setFreeBlocks(4500);
        original.setFreeInodes(800);
        original.setAllocationStrategy(1);
        original.setInodeBitmapStart(2);
        original.setDataBitmapStart(3);
        original.setInodeTableStart(4);
        original.setDataBlocksStart(100);
        original.setCreationTime(1234567890L);
        original.setLastMountTime(9876543210L);

        System.out.println("=== Superblock: Serialización / Deserialización ===");

        byte[] serialized = original.toBytes();

        Superblock deserialized = Superblock.fromBytes(Arrays.copyOf(serialized, serialized.length));

        compare("magicNumber", original.getMagicNumber(), deserialized.getMagicNumber());
        compare("fsName", original.getFsName(), deserialized.getFsName());
        compare("fsVersion", original.getFsVersion(), deserialized.getFsVersion());
        compare("blockSize", original.getBlockSize(), deserialized.getBlockSize());
        compare("totalBlocks", original.getTotalBlocks(), deserialized.getTotalBlocks());
        compare("totalInodes", original.getTotalInodes(), deserialized.getTotalInodes());
        compare("freeBlocks", original.getFreeBlocks(), deserialized.getFreeBlocks());
        compare("freeInodes", original.getFreeInodes(), deserialized.getFreeInodes());
        compare("rootInode", original.getRootInode(), deserialized.getRootInode());
        compare("allocationStrategy", original.getAllocationStrategy(), deserialized.getAllocationStrategy());
        compare("inodeBitmapStart", original.getInodeBitmapStart(), deserialized.getInodeBitmapStart());
        compare("dataBitmapStart", original.getDataBitmapStart(), deserialized.getDataBitmapStart());
        compare("inodeTableStart", original.getInodeTableStart(), deserialized.getInodeTableStart());
        compare("dataBlocksStart", original.getDataBlocksStart(), deserialized.getDataBlocksStart());
        compare("creationTime", original.getCreationTime(), deserialized.getCreationTime());
        compare("lastMountTime", original.getLastMountTime(), deserialized.getLastMountTime());

        System.out.println("\nSuperblock válido: " + deserialized.isValid());
        System.out.println("=== Fin ===");
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
}

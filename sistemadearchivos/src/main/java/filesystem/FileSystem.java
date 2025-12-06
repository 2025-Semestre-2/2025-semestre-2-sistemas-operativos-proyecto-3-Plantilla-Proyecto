package filesystem;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dylan
 */
public class FileSystem {

    private String fsFilePath;
    private RandomAccessFile fsFile;
    private Superblock superblock;
    private Bitmap inodeBitmap;
    private Bitmap dataBlockBitmap;

    // Tablas en memoria
    private Map<Integer, User> userTable;               // userID -> User
    private Map<String, User> userByName;               // username -> User
    private Map<Integer, Group> groupTable;              // groupID -> Group
    private Map<String, Group> groupByName;              // groupname -> Group

    // Archivos abiertos
    private Map<String, Inode> openFileTable;           // path -> inode

    public FileSystem(String fsFilePath) {
        this.fsFilePath = fsFilePath;
        this.userTable = new HashMap<>();
        this.userByName = new HashMap<>();
        this.groupTable = new HashMap<>();
        this.groupByName = new HashMap<>();
        this.openFileTable = new HashMap<>();
    }

    /**
     * Calcula el offset de un bloque en el archivo
     */
    private long getBlockOffset(int blockNumber) {
        return (long) blockNumber * FSConstants.BLOCK_SIZE;
    }

    /**
     * Lee un bloque completo del disco
     */
    private byte[] readBlock(int blockNumber) throws IOException {
        byte[] block = new byte[FSConstants.BLOCK_SIZE];
        fsFile.seek(getBlockOffset(blockNumber));
        fsFile.readFully(block);

        return block;
    }

    /**
     * Escribe un bloque completo al disco
     */
    private void writeBlock(int blockNumber, byte[] data) throws IOException {
        if (data.length != FSConstants.BLOCK_SIZE) {
            throw new IllegalArgumentException("El bloque debe tener " + FSConstants.BLOCK_SIZE + " bytes");
        }
        fsFile.seek(getBlockOffset(blockNumber));
        fsFile.write(data);
    }

    /**
     * Lee un inode de la tabla
     */
    public Inode readInode(int inodeNumber) throws IOException {
        if (inodeNumber < 0 || inodeNumber >= superblock.getTotalInodes()) {
            throw new IllegalArgumentException("Número de inode inválido: " + inodeNumber);
        }

        // offset
        long inodeTableOffset = getBlockOffset(superblock.getInodeTableStart());
        long inodeOffset = inodeTableOffset + (inodeNumber * FSConstants.INODE_SIZE);

        byte[] inodeData = new byte[FSConstants.INODE_SIZE];
        fsFile.seek(inodeOffset);
        fsFile.readFully(inodeData);

        return Inode.fromBytes(inodeData);
    }

    /**
     * Escribe un inode en la tabla
     */
    public void writeInode(Inode inode) throws IOException {
        int inodeNumber = inode.getInodeNumber();
        if (inodeNumber < 0 || inodeNumber >= superblock.getTotalInodes()) {
            throw new IllegalArgumentException("Número de inode inválido: " + inodeNumber);
        }

        long inodeTableOffset = getBlockOffset(superblock.getInodeTableStart());
        long inodeOffset = inodeTableOffset + (inodeNumber * FSConstants.INODE_SIZE);

        fsFile.seek(inodeOffset);
        fsFile.write(inode.toBytes());
    }

    /**
     * Asigna un inode libre
     */
    public int allocateInode() throws IOException {
        int inodeNumber = inodeBitmap.findFirstFree();
        if (inodeNumber == -1) {
            throw new IOException("No hay inodes disponibles");
        }
        
        inodeBitmap.allocate(inodeNumber);
        superblock.setFreeInodes(superblock.getFreeInodes() - 1);
        writeSuperblock();
        writeInodeBitmap();
        
        return inodeNumber;
    }

    /**
     * Libera un inode
     */
    public void freeInode(int inodeNumber) throws IOException {
        if (inodeNumber < 0 || inodeNumber >= superblock.getTotalInodes()) {
            return;
        }

        inodeBitmap.free(inodeNumber);
        superblock.setFreeInodes(superblock.getFreeInodes() + 1);
        writeSuperblock();
        writeInodeBitmap();
    }

    /**
     * Asigna un bloque de datos libre
     */
    public int allocateDataBlock() throws IOException {
        int blockNumber = dataBlockBitmap.findFirstFree();
        if (blockNumber == -1) {
            throw new IOException("No hay bloques disponibles");
        }

        dataBlockBitmap.allocate(blockNumber);
        superblock.setFreeBlocks(superblock.getFreeBlocks() - 1);
        writeSuperblock();
        writeDataBlockBitmap();

        return superblock.getDataBlocksStart() + blockNumber;
    }

    /**
     * Libera un bloque de datos
     */
    public void freeDataBlock(int absoluteBlockNumber) throws IOException {
        int relativeBlock = absoluteBlockNumber - superblock.getDataBlocksStart();

        if (relativeBlock < 0 || relativeBlock >= dataBlockBitmap.getSize()) {
            return;
        }

        dataBlockBitmap.free(relativeBlock);
        superblock.setFreeBlocks(superblock.getFreeBlocks() + 1);
        writeSuperblock();
        writeDataBlockBitmap();
    }

    /**
     * Escribe el superblock al disco
     */
    private void writeSuperblock() throws IOException {
        writeBlock(0, superblock.toBytes());
    }

    /**
     * Escribe el inode bitmap al disco
     */
    private void writeInodeBitmap() throws IOException {
        byte[] bitmapBytes = inodeBitmap.toBytes();
        int blocksNeeded = (bitmapBytes.length + FSConstants.BLOCK_SIZE - 1)
                / FSConstants.BLOCK_SIZE;

        for (int i = 0; i < blocksNeeded; i++) {
            byte[] blockData = new byte[FSConstants.BLOCK_SIZE];
            int copyLength = Math.min(FSConstants.BLOCK_SIZE,
                    bitmapBytes.length - i * FSConstants.BLOCK_SIZE);
            System.arraycopy(bitmapBytes, i * FSConstants.BLOCK_SIZE,
                    blockData, 0, copyLength);
            writeBlock(superblock.getInodeBitmapStart() + i, blockData);
        }
    }

    /**
     * Escribe el data block bitmap al disco
     */
    private void writeDataBlockBitmap() throws IOException {
        byte[] bitmapBytes = dataBlockBitmap.toBytes();
        int blocksNeeded = (bitmapBytes.length + FSConstants.BLOCK_SIZE - 1)
                / FSConstants.BLOCK_SIZE;

        for (int i = 0; i < blocksNeeded; i++) {
            byte[] blockData = new byte[FSConstants.BLOCK_SIZE];
            int copyLength = Math.min(FSConstants.BLOCK_SIZE,
                    bitmapBytes.length - i * FSConstants.BLOCK_SIZE);
            System.arraycopy(bitmapBytes, i * FSConstants.BLOCK_SIZE,
                    blockData, 0, copyLength);
            writeBlock(superblock.getDataBitmapStart() + i, blockData);
        }
    }

    /**
     * Lee las entradas de un directorio
     */
    public List<DirectoryEntry> readDirectoryEntries(Inode dirInode) throws IOException {
        if (!dirInode.isDirectory()) {
            throw new IllegalArgumentException("El inode no es un directorio");
        }

        List<DirectoryEntry> entries = new ArrayList<>();

        // SOLO LEE DEL PRIMER BLOQUE DIRECTO 
        int blockNumber = dirInode.getDirectBlocks()[0];
        if (blockNumber == -1) {
            return entries; // Directorio vacío
        }

        byte[] blockData = readBlock(blockNumber);

        for (int i = 0; i < FSConstants.ENTRIES_PER_BLOCK; i++) {
            int offset = i * FSConstants.DIR_ENTRY_SIZE;
            byte[] entryData = new byte[FSConstants.DIR_ENTRY_SIZE];
            System.arraycopy(blockData, offset, entryData, 0, FSConstants.DIR_ENTRY_SIZE);

            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
            entries.add(entry);
        }

        return entries;
    }

    /**
     * Escribe las entradas de un directorio
     */
    public void writeDirectoryEntries(Inode dirInode, List<DirectoryEntry> entries)
            throws IOException {
        if (!dirInode.isDirectory()) {
            throw new IllegalArgumentException("El inode no es un directorio");
        }

        if (entries.size() > FSConstants.ENTRIES_PER_BLOCK) {
            throw new IOException("Demasiadas entradas para un solo bloque");
        }

        int blockNumber = dirInode.getDirectBlocks()[0];
        if (blockNumber == -1) {
            // Necesitamos asignar un bloque
            blockNumber = allocateDataBlock();
            dirInode.setDirectBlock(0, blockNumber);
            dirInode.setFileSize(FSConstants.BLOCK_SIZE);
            writeInode(dirInode);
        }

        byte[] blockData = new byte[FSConstants.BLOCK_SIZE];

        for (int i = 0; i < entries.size() && i < FSConstants.ENTRIES_PER_BLOCK; i++) {
            byte[] entryData = entries.get(i).toBytes();
            System.arraycopy(entryData, 0, blockData,
                    i * FSConstants.DIR_ENTRY_SIZE, FSConstants.DIR_ENTRY_SIZE);
        }

        writeBlock(blockNumber, blockData);
    }

    // Getters
    public Superblock getSuperblock() {
        return superblock;
    }

    public Bitmap getInodeBitmap() {
        return inodeBitmap;
    }

    public Bitmap getDataBlockBitmap() {
        return dataBlockBitmap;
    }

    public Map<Integer, User> getUserTable() {
        return userTable;
    }

    public Map<String, User> getUserByName() {
        return userByName;
    }

    public Map<Integer, Group> getGroupTable() {
        return groupTable;
    }

    public Map<String, Group> getGroupByName() {
        return groupByName;
    }

    /**
     * Formatea y crea el sistema de archivos
     *
     * @param sizeMB Tamaño del disco
     * @param allocationStrategy Estrategia de asignación (1=Contigua,
     * 2=Enlazada, 3=Indexada)
     * @param rootPassword Contraseña del usuario root
     */
    public void format(int sizeMB, int allocationStrategy, String rootPassword) throws IOException {
        System.out.println("iniciando formateo del sistema de archivos...");
        System.out.println("Tamaño: " + sizeMB + " MB");
        System.out.println("Estrategia: " + getStrategyName(allocationStrategy));

        // Paso 1: Calcular estructuras
        long totalBytes = (long) sizeMB * 1024 * 1024;
        int totalBlocks = (int) (totalBytes / FSConstants.BLOCK_SIZE);

        // Calcular total de inodes (1 inode por cada 16 KB)
        int totalInodes = (int) (totalBytes / (16 * 1024));

        // Calcular bloques necesarios para el inode bitmap
        int inodeBitmapBits = totalInodes;
        int inodeBitmapBytes = (inodeBitmapBits + 7) / 8;
        int inodeBitmapBlocks = (inodeBitmapBytes + FSConstants.BLOCK_SIZE - 1) / FSConstants.BLOCK_SIZE;

        // Calcular bloques para la tabla de inodes
        int inodeTableBytes = totalInodes * FSConstants.INODE_SIZE;
        int inodeTableBlocks = (inodeTableBytes + FSConstants.BLOCK_SIZE - 1) / FSConstants.BLOCK_SIZE;

        // Calcular bloques de datos provisionales
        int metadataBlocksWithoutDataBitmap = 1 + inodeBitmapBlocks + inodeTableBlocks;
        int provisionalDataBlocks = totalBlocks - metadataBlocksWithoutDataBitmap;

        // Calcular bloques para data bitmap
        int dataBitmapBits = provisionalDataBlocks;
        int dataBitmapBytes = (dataBitmapBits + 7) / 8;
        int dataBitmapBlocks = (dataBitmapBytes + FSConstants.BLOCK_SIZE - 1) / FSConstants.BLOCK_SIZE;

        // Calcular bloques de datos reales
        int actualDataBlocks = totalBlocks - 1 - inodeBitmapBlocks - dataBitmapBlocks - inodeTableBlocks;

        System.out.println("\nCálculos del sistema de archivos:");
        System.out.println("  Total de bloques: " + totalBlocks);
        System.out.println("  Total de inodes: " + totalInodes);
        System.out.println("  Bloques para inode bitmap: " + inodeBitmapBlocks);
        System.out.println("  Bloques para data bitmap: " + dataBitmapBlocks);
        System.out.println("  Bloques para tabla de inodes: " + inodeTableBlocks);
        System.out.println("  Bloques de datos: " + actualDataBlocks);

        // Paso 2: Crear el archivo
        File fsFileObj = new File(fsFilePath);
        if (fsFileObj.exists()) {
            System.out.println("\nAdvertencia: El archivo ya existe. Será sobreescrito.");
        }

        fsFile = new RandomAccessFile(fsFilePath, "rw");
        fsFile.setLength(totalBytes);

        // Paso 3: Crear y escribir el superblock
        System.out.println("\nCreando Superblock...");
        superblock = new Superblock();
        superblock.setFsName("myFS");
        superblock.setBlockSize(FSConstants.BLOCK_SIZE);
        superblock.setTotalBlocks(totalBlocks);
        superblock.setTotalInodes(totalInodes);
        superblock.setFreeBlocks(actualDataBlocks - 2); // -2 por "/" y "/root"
        superblock.setFreeInodes(totalInodes - 2); // -2 por "/" y "/root"
        superblock.setRootInode(FSConstants.ROOT_INODE);
        superblock.setAllocationStrategy(allocationStrategy);

        // Calcular posiciones de inicio
        superblock.setInodeBitmapStart(1);
        superblock.setDataBitmapStart(1 + inodeBitmapBlocks);
        superblock.setInodeTableStart(1 + inodeBitmapBlocks + dataBitmapBlocks);
        superblock.setDataBlocksStart(1 + inodeBitmapBlocks + dataBitmapBlocks + inodeTableBlocks);

        writeSuperblock();
        System.out.println(" Superblock escrito en el bloque 0");

        // Paso 4: Inicializar Inode Bitmap
        System.out.println("\nInicializando Inode Bitmap...");
        inodeBitmap = new Bitmap(totalInodes);
        // inodes 0 y 1 ocupados (root y /root)
        inodeBitmap.allocate(0);
        inodeBitmap.allocate(1);
        writeInodeBitmap();
        System.out.println(" Inode Bitmap escrito");

        // Paso 5: inicializar Data Block Bitmap
        System.out.println("\nInicializando Data Block Bitmap...");
        dataBlockBitmap = new Bitmap(actualDataBlocks);
        // 0 y 1 ocupados ("/" y "/root")
        dataBlockBitmap.allocate(0);
        dataBlockBitmap.allocate(1);
        writeDataBlockBitmap();
        System.out.println(" Data Block Bitmap escrito");

        // Paso 6: Crear inode del directorio raíz "/"
        System.out.println("\nCreando directorio raíz '/'...");
        Inode rootInode = new Inode(
                FSConstants.ROOT_INODE,
                FSConstants.TYPE_DIRECTORY,
                FSConstants.DEFAULT_DIR_PERMS,
                FSConstants.ROOT_UID,
                FSConstants.ROOT_GID
        );
        
        rootInode.setName("/");
        rootInode.setFileSize(FSConstants.BLOCK_SIZE);
        rootInode.setLinkCount(3); // ".", ".." y "root"
        rootInode.setDirectBlock(0, superblock.getDataBitmapStart());
        writeInode(rootInode);
        System.out.println(" Inode de '/' creado (inode 0)");
        
        // Paso 7: Crear contenido del directorio raíz
        System.out.println(" Creando entradas de directorio para '/'...");
        List<DirectoryEntry> rootEntries = new ArrayList<>();
        
        // Entrada "." (apunta a sí mismo)
        rootEntries.add(new DirectoryEntry(0, FSConstants.TYPE_DIRECTORY, "."));
        
        // Entrada ".." (apunta a sí mismo porque es la raíz)
        rootEntries.add(new DirectoryEntry(0, FSConstants.TYPE_DIRECTORY, ".."));

        // Entrada "root" (apunta al directorio home del usuario root)
        rootEntries.add(new DirectoryEntry(1, FSConstants.TYPE_DIRECTORY, "root"));
        
        // Rellenar con entradas vacías
        for (int i = 3; i < FSConstants.ENTRIES_PER_BLOCK; i++) {
            rootEntries.add(new DirectoryEntry());
        }
        
        writeDirectoryEntries(rootInode, rootEntries);
        System.out.println("  Entradas de directorio escritas");

        // Paso 8: Crear inode del directorio "/root"
        System.out.println("\nCreando directorio '/root'...");
        Inode rootHomeInode = new Inode(
            1,
            FSConstants.TYPE_DIRECTORY,
            FSConstants.DEFAULT_DIR_PERMS,
            FSConstants.ROOT_UID,
            FSConstants.ROOT_GID
        );
        rootHomeInode.setName("root");
        rootHomeInode.setFileSize(FSConstants.BLOCK_SIZE);
        rootHomeInode.setLinkCount(2); // "." y ".."
        rootHomeInode.setDirectBlock(0, superblock.getDataBlocksStart() + 1);
        writeInode(rootHomeInode);
        System.out.println("  Inode de '/root' creado (inode 1)");
        
        // Paso 9: Crear contenido del directorio "/root"
        System.out.println("  Creando entradas de directorio para '/root'...");
        List<DirectoryEntry> rootHomeEntries = new ArrayList<>();
        
        // Entrada "." (apunta a sí mismo)
        rootHomeEntries.add(new DirectoryEntry(1, FSConstants.TYPE_DIRECTORY, "."));
        
        // Entrada ".." (apunta al directorio padre "/")
        rootHomeEntries.add(new DirectoryEntry(0, FSConstants.TYPE_DIRECTORY, ".."));
        
        // Rellenar con entradas vacías
        for (int i = 2; i < FSConstants.ENTRIES_PER_BLOCK; i++) {
            rootHomeEntries.add(new DirectoryEntry());
        }
        
        writeDirectoryEntries(rootHomeInode, rootHomeEntries);
        System.out.println("  Entradas de directorio escritas");
        
        // Paso 10: Crear usuario root
        System.out.println("\nCreando usuario root...");
        User rootUser = new User(
                FSConstants.ROOT_UID,
                "root",
                rootPassword,
                "Root Admin",
                "/root",
                FSConstants.ROOT_GID
        );
        userTable.put(rootUser.getUserId(), rootUser);
        userByName.put(rootUser.getUsername(), rootUser);
        System.out.println("  Usuario root creado");
        
        // Paso 11: Crear grupo root
        System.out.println("\nCreando grupo root...");
        Group rootGroup = new Group(FSConstants.ROOT_GID, "root");
        rootGroup.addMember(FSConstants.ROOT_UID);
        groupTable.put(rootGroup.getGroupId(), rootGroup);
        groupByName.put(rootGroup.getGroupName(), rootGroup);
        System.out.println("  Grupo root creado");

        // Paso 12: Guardar usuarios y grupos en bloques especiales
        saveUsersAndGroups();
        
        // Paso 13: Sincronizar y cerrar
        fsFile.getFD().sync();
        System.out.println("\n¡Sistema de archivos formateado exitosamente!");
        System.out.println("Archivo: " + fsFilePath);
        System.out.println("Usuario root creado con directorio home: /root");        
    }
    
    /**
     * Guarda las tablas de usuarios y grupos en bloques especiales del FS
     * (Bloques reservados despúes de los metadatos)
     */
    private void saveUsersAndGroups() throws IOException {
        System.out.println("\nGuardando tablas de usuarios y grupos...");
        
        // Bloque especial para usuarios (después del último bloque de datos usado)
        int userBlockNumber = superblock.getDataBlocksStart() + 2;
        
        // Bloque especial para grupos
        int groupBlockNumber = superblock.getDataBlocksStart() + 3;
        
        // Marcar estos bloques como ocupados
        dataBlockBitmap.allocate(2);
        dataBlockBitmap.allocate(3);
        superblock.setFreeBlocks(superblock.getFreeBlocks() - 2);
        writeSuperblock();
        writeDataBlockBitmap();
        
        // Guardar usuarios
        byte[] userBlock = new byte[FSConstants.BLOCK_SIZE];
        int offset = 0;
        
        // Guardar cantidad de usuarios
        ByteBuffer userBuffer = ByteBuffer.wrap(userBlock);
        userBuffer.putInt(userTable.size());
        offset = 4;
        
        // Guardar cada usuario (512 bytes por usuario)
        for (User user : userTable.values()) {
            byte[] userData = user.toBytes();
            if (offset + userData.length <= FSConstants.BLOCK_SIZE) {
                System.arraycopy(userData, 0, userBlock, offset, userData.length);
                offset += userData.length;
            }
        }
        
        writeBlock(userBlockNumber, userBlock);
        System.out.println("  Usuarios guardados en bloque " + userBlockNumber);
        
        // Guardar grupos
        byte[] groupBlock = new byte[FSConstants.BLOCK_SIZE];
        offset = 0;
        
        ByteBuffer groupBuffer = ByteBuffer.wrap(groupBlock);
        groupBuffer.putInt(groupTable.size());
        offset = 4;
        
        for (Group group : groupTable.values()) {
            byte[] groupData = group.toBytes();
            if (offset + groupData.length <= FSConstants.BLOCK_SIZE) {
                System.arraycopy(groupData, 0, groupBlock, offset, groupData.length);
                offset += groupData.length;
            }
        }
        
        writeBlock(groupBlockNumber, groupBlock);
        System.out.println("  Grupos guardados en bloque " + groupBlockNumber);
    }
    
    
    /**
     * Monta un sistema de archivos existente
     */
    public void mount() throws IOException {
        System.out.println("Montando sistema de archivos: " + fsFilePath);
        
        File fsFileObj = new File(fsFilePath);
        if (!fsFileObj.exists()) {
            throw new IOException("El archivo del sistema de archivos no existe: " + fsFilePath);
        }
        
        fsFile = new RandomAccessFile(fsFilePath, "rw");
        
        // Leer Superblock
        System.out.println("Leyendo Superblock...");
        byte[] superblockData = readBlock(0);
        superblock = Superblock.fromBytes(superblockData);
        
        // Validar magic number
        if (!superblock.isValid()) {
            throw new IOException("Sistema de archivos inválido o corrupto (magic number incorrecto)");
        }
        
        System.out.println("  Sistema de archivos: " + superblock.getFsName());
        System.out.println("  Versión: " + superblock.getFsVersion());
        System.out.println("  Tamaño de bloque: " + superblock.getBlockSize());
        System.out.println("  Total de bloques: " + superblock.getTotalBlocks());
        System.out.println("  Bloques libres: " + superblock.getFreeBlocks());
        System.out.println("  Total de inodes: " + superblock.getTotalInodes());
        System.out.println("  Inodes libres: " + superblock.getFreeInodes());
        
        // Actualizar last mount time
        superblock.setLastMountTime(System.currentTimeMillis());
        writeSuperblock();
        
        // Leer Inode Bitmap
        System.out.println("\nCargando Inode Bitmap...");
        int inodeBitmapBytes = (superblock.getTotalInodes() + 7) / 8;
        int inodeBitmapBlocks = (inodeBitmapBytes + FSConstants.BLOCK_SIZE - 1) 
                               / FSConstants.BLOCK_SIZE;
        
        byte[] inodeBitmapData = new byte[inodeBitmapBytes];
        for (int i = 0; i < inodeBitmapBlocks; i++) {
            byte[] block = readBlock(superblock.getInodeBitmapStart() + i);
            int copyLength = Math.min(FSConstants.BLOCK_SIZE, 
                                     inodeBitmapBytes - i * FSConstants.BLOCK_SIZE);
            System.arraycopy(block, 0, inodeBitmapData, 
                           i * FSConstants.BLOCK_SIZE, copyLength);
        }
        inodeBitmap = Bitmap.fromBytes(inodeBitmapData, superblock.getTotalInodes());
        System.out.println("  Inode Bitmap cargado");
        
        // Leer Data Block Bitmap
        System.out.println("\nCargando Data Block Bitmap...");
        int dataBlocks = superblock.getTotalBlocks() - superblock.getDataBlocksStart();
        int dataBitmapBytes = (dataBlocks + 7) / 8;
        int dataBitmapBlocks = (dataBitmapBytes + FSConstants.BLOCK_SIZE - 1) 
                              / FSConstants.BLOCK_SIZE;
        
        byte[] dataBitmapData = new byte[dataBitmapBytes];
        for (int i = 0; i < dataBitmapBlocks; i++) {
            byte[] block = readBlock(superblock.getDataBitmapStart() + i);
            int copyLength = Math.min(FSConstants.BLOCK_SIZE, 
                                     dataBitmapBytes - i * FSConstants.BLOCK_SIZE);
            System.arraycopy(block, 0, dataBitmapData, 
                           i * FSConstants.BLOCK_SIZE, copyLength);
        }
        dataBlockBitmap = Bitmap.fromBytes(dataBitmapData, dataBlocks);
        System.out.println("  Data Block Bitmap cargado");
        
        // Cargar usuarios y grupos
        loadUsersAndGroups();
        
        System.out.println("\n¡Sistema de archivos montado exitosamente!");
    }
    
    /**
     * Carga las tablas de usuarios y grupos desde el disco
     */
    private void loadUsersAndGroups() throws IOException {
        System.out.println("\nCargando usuarios y grupos...");
        
        int userBlockNumber = superblock.getDataBlocksStart() + 2;
        int groupBlockNumber = superblock.getDataBlocksStart() + 3;
        
        // Cargar usuarios
        byte[] userBlock = readBlock(userBlockNumber);
        ByteBuffer userBuffer = ByteBuffer.wrap(userBlock);
        
        int userCount = userBuffer.getInt();
        System.out.println("  Cargando " + userCount + " usuarios...");
        
        userTable.clear();
        userByName.clear();
        
        for (int i = 0; i < userCount; i++) {
            byte[] userData = new byte[512];
            userBuffer.get(userData);
            User user = User.fromBytes(userData);
            
            if (user.getUserId() != -1) { // Usuario válido
                userTable.put(user.getUserId(), user);
                userByName.put(user.getUsername(), user);
                System.out.println("    - " + user.getUsername() + " (" + user.getFullName() + ")");
            }
        }
        
        // Cargar grupos
        byte[] groupBlock = readBlock(groupBlockNumber);
        ByteBuffer groupBuffer = ByteBuffer.wrap(groupBlock);
        
        int groupCount = groupBuffer.getInt();
        System.out.println("  Cargando " + groupCount + " grupos...");
        
        groupTable.clear();
        groupByName.clear();
        
        for (int i = 0; i < groupCount; i++) {
            byte[] groupData = new byte[512];
            groupBuffer.get(groupData);
            Group group = Group.fromBytes(groupData);
            
            if (group.getGroupId() != -1) { // Grupo válido
                groupTable.put(group.getGroupId(), group);
                groupByName.put(group.getGroupName(), group);
                System.out.println("    - " + group.getGroupName());
            }
        }
    }
    
    /**
     * Desmonta el sistema de archivos
     */
    public void unmount() throws IOException {
        if (fsFile != null) {
            System.out.println("Desmontando sistema de archivos...");
            
            // Guardar usuarios y grupos
            saveUsersAndGroups();
            
            // Sincronizar cambios
            fsFile.getFD().sync();
            
            // Cerrar archivo
            fsFile.close();
            fsFile = null;
            
            System.out.println("Sistema de archivos desmontado correctamente");
        }
    }
    
    /**
     * Retorna el nombre de la estrategia de asignación
     */
    private String getStrategyName(int strategy) {
        switch (strategy) {
            case FSConstants.ALLOC_CONTIGUOUS:
                return "Asignación Contigua";
            case FSConstants.ALLOC_LINKED:
                return "Asignación Enlazada";
            case FSConstants.ALLOC_INDEXED:
                return "Asignación Indexada";
            default:
                return "Desconocida";
        }
    }
    
    /**
     * Verifica si el sistema de archivos está montado
     */
    public boolean isMounted() {
        return fsFile != null;
    }
}    

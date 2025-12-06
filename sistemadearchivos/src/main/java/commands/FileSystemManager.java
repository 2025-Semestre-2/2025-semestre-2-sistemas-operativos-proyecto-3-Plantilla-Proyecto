package commands;

import filesystem.*;
import filesystem.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author dylan
 */
public class FileSystemManager {
    private FileSystem fs;
    private String fsFilePath;
    private User currentUser;
    private String currentDirectory;
    private boolean running;
    
    public FileSystemManager(String fsFilePath) {
        this.fsFilePath = fsFilePath;
        this.currentDirectory = "/";
        this.running = true;
    }
    
    /**
     * Formatea el sistema de archivos
     */
    public void format(int sizeMB) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n=== FORMATEO DEL SISTEMA DE ARCHIVOS ===\n");     
        
        int strategy = FSConstants.ALLOC_INDEXED;
        System.out.println("El sistema de archivos usa la estrategia de asignación indexada.");

        // Solicitar contraseña del usuario root
        System.out.print("\nEstablezca la contraseña para el usuario root: ");
        String password = scanner.nextLine();
        
        System.out.print("Confirme la contraseña: ");
        String confirmPassword = scanner.nextLine();
        
        if (!password.equals(confirmPassword)) {
            throw new IOException("Las contraseñas no coinciden");
        }
        
        if (password.trim().isEmpty()) {
            throw new IOException("La contraseña no puede estar vacía");
        }
        
        // Crear y formatear el sistema de archivos
        fs = new FileSystem(fsFilePath);
        fs.format(sizeMB, strategy, password);
        
        // Establecer usuario actual como root
        currentUser = fs.getUserByName().get("root");
        currentDirectory = "/root";
        
        System.out.println("\n¡Sistema de archivos formateado correctamente!");
        System.out.println("Usuario actual: root");
        System.out.println("Directorio actual: " + currentDirectory);
    }

    /**
     * Monta un sistema de archivos existente
     */
    public void mount() throws IOException {
        fs = new FileSystem(fsFilePath);
        fs.mount();
        
        // Por defecto, no hay usuario autenticado
        currentUser = null;
        currentDirectory = "/";
    }
    
    /**
     * Desmonta el sistema de archivos
     */
    public void unmount() throws IOException {
        if (fs != null) {
            fs.unmount();
            fs = null;
            currentUser = null;
            currentDirectory = "/";
        }
    }    

    
    /**
     * Crea un nuevo usuario
     */
    public void addUser(String username) throws IOException {
        if (!isRoot()) {
            throw new IOException("Permiso denegado: solo root puede crear usuarios");
        }
        
        // Verificar que el usuario no exista
        if (fs.getUserByName().containsKey(username)) {
            throw new IOException("El usuario '" + username + "' ya existe");
        }
        
        Scanner scanner = new Scanner(System.in);
        
        // Solicitar nombre completo
        System.out.print("Nombre completo: ");
        String fullName = scanner.nextLine();
        
        // Solicitar contraseña
        System.out.print("Contraseña: ");
        String password = scanner.nextLine();
        
        System.out.print("Confirme contraseña: ");
        String confirmPassword = scanner.nextLine();
        
        if (!password.equals(confirmPassword)) {
            throw new IOException("Las contraseñas no coinciden");
        }
        
        if (password.trim().isEmpty()) {
            throw new IOException("La contraseña no puede estar vacía");
        }
        
        // Asignar nuevo ID de usuario
        int newUserId = fs.getUserTable().size();
        
        // Crear directorio home
        String homeDir = "/home/" + username;
        
        // Crear usuario
        User newUser = new User(newUserId, username, password, fullName, homeDir, 1);
        fs.getUserTable().put(newUserId, newUser);
        fs.getUserByName().put(username, newUser);
        
        // Crear directorio /home si no existe
        createHomeStructure();
        
        // Crear directorio home del usuario
        createUserHomeDirectory(username, newUserId);
        
        // Guardar cambios
        fs.unmount();
        fs.mount();
        
        System.out.println("Usuario '" + username + "' creado exitosamente");
        System.out.println("Directorio home: " + homeDir);
    }
    
    /**
     * Crea la estructura /home si no existe
     */
    private void createHomeStructure() throws IOException {
        // Buscar si existe el directorio /home en la raíz
        Inode rootInode = fs.readInode(0);
        List<DirectoryEntry> rootEntries = fs.readDirectoryEntries(rootInode);
        
        boolean homeExists = false;
        for (DirectoryEntry entry : rootEntries) {
            if (!entry.isFree() && entry.getName().equals("home")) {
                homeExists = true;
                break;
            }
        }
        
        if (!homeExists) {
            // Crear el directorio /home
            int homeInodeNum = fs.allocateInode();
            
            Inode homeInode = new Inode(
                homeInodeNum,
                FSConstants.TYPE_DIRECTORY,
                FSConstants.DEFAULT_DIR_PERMS,
                FSConstants.ROOT_UID,
                FSConstants.ROOT_GID
            );
            homeInode.setName("home");
            homeInode.setFileSize(FSConstants.BLOCK_SIZE);
            homeInode.setLinkCount(2);
            
            // Asignar bloque de datos
            int dataBlock = fs.allocateDataBlock();
            homeInode.setDirectBlock(0, dataBlock);
            fs.writeInode(homeInode);
            
            // Crear entradas del directorio /home
            List<DirectoryEntry> homeEntries = new ArrayList<>();
            homeEntries.add(new DirectoryEntry(homeInodeNum, FSConstants.TYPE_DIRECTORY, "."));
            homeEntries.add(new DirectoryEntry(0, FSConstants.TYPE_DIRECTORY, ".."));
            
            for (int i = 2; i < FSConstants.ENTRIES_PER_BLOCK; i++) {
                homeEntries.add(new DirectoryEntry());
            }
            
            fs.writeDirectoryEntries(homeInode, homeEntries);
            
            // Agregar entrada en el directorio raíz
            boolean added = false;
            for (int i = 0; i < rootEntries.size(); i++) {
                if (rootEntries.get(i).isFree()) {
                    rootEntries.set(i, new DirectoryEntry(homeInodeNum, 
                                    FSConstants.TYPE_DIRECTORY, "home"));
                    added = true;
                    break;
                }
            }
            
            if (!added) {
                throw new IOException("No hay espacio en el directorio raíz");
            }
            
            fs.writeDirectoryEntries(rootInode, rootEntries);
            rootInode.setLinkCount(rootInode.getLinkCount() + 1);
            fs.writeInode(rootInode);
            
            System.out.println("Directorio /home creado");
        }
    }
    
    /**
     * Crea el directorio home de un usuario
     */
    private void createUserHomeDirectory(String username, int userId) throws IOException {
        // Leer el directorio /home
        Inode rootInode = fs.readInode(0);
        List<DirectoryEntry> rootEntries = fs.readDirectoryEntries(rootInode);
        
        int homeInodeNum = -1;
        for (DirectoryEntry entry : rootEntries) {
            if (!entry.isFree() && entry.getName().equals("home")) {
                homeInodeNum = entry.getInodeNumber();
                break;
            }
        }
        
        if (homeInodeNum == -1) {
            throw new IOException("Directorio /home no encontrado");
        }
        
        Inode homeInode = fs.readInode(homeInodeNum);
        List<DirectoryEntry> homeEntries = fs.readDirectoryEntries(homeInode);
        
        // Crear el directorio del usuario
        int userHomeInodeNum = fs.allocateInode();
        
        Inode userHomeInode = new Inode(
            userHomeInodeNum,
            FSConstants.TYPE_DIRECTORY,
            FSConstants.DEFAULT_DIR_PERMS,
            userId,
            1 // grupo users por defecto
        );
        userHomeInode.setName(username);
        userHomeInode.setFileSize(FSConstants.BLOCK_SIZE);
        userHomeInode.setLinkCount(2);
        
        // Asignar bloque de datos
        int dataBlock = fs.allocateDataBlock();
        userHomeInode.setDirectBlock(0, dataBlock);
        fs.writeInode(userHomeInode);
        
        // Crear entradas del directorio del usuario
        List<DirectoryEntry> userHomeEntries = new ArrayList<>();
        userHomeEntries.add(new DirectoryEntry(userHomeInodeNum, 
                           FSConstants.TYPE_DIRECTORY, "."));
        userHomeEntries.add(new DirectoryEntry(homeInodeNum, 
                           FSConstants.TYPE_DIRECTORY, ".."));
        
        for (int i = 2; i < FSConstants.ENTRIES_PER_BLOCK; i++) {
            userHomeEntries.add(new DirectoryEntry());
        }
        
        fs.writeDirectoryEntries(userHomeInode, userHomeEntries);
        
        // Agregar entrada en /home
        boolean added = false;
        for (int i = 0; i < homeEntries.size(); i++) {
            if (homeEntries.get(i).isFree()) {
                homeEntries.set(i, new DirectoryEntry(userHomeInodeNum, 
                               FSConstants.TYPE_DIRECTORY, username));
                added = true;
                break;
            }
        }
        
        if (!added) {
            throw new IOException("No hay espacio en el directorio /home");
        }
        
        fs.writeDirectoryEntries(homeInode, homeEntries);
        homeInode.setLinkCount(homeInode.getLinkCount() + 1);
        fs.writeInode(homeInode);
    }
    
    /**
     * Crea un nuevo grupo
     */
    public void addGroup(String groupName) throws IOException {
        if (!isRoot()) {
            throw new IOException("Permiso denegado: solo root puede crear grupos");
        }
        
        if (fs.getGroupByName().containsKey(groupName)) {
            throw new IOException("El grupo '" + groupName + "' ya existe");
        }
        
        int newGroupId = fs.getGroupTable().size();
        
        Group newGroup = new Group(newGroupId, groupName);
        fs.getGroupTable().put(newGroupId, newGroup);
        fs.getGroupByName().put(groupName, newGroup);
        
        // Guardar cambios
        fs.unmount();
        fs.mount();
        
        System.out.println("Grupo '" + groupName + "' creado exitosamente");
    }
    
    /**
     * Cambia la contraseña de un usuario
     */
    public void changePassword(String username) throws IOException {
        // Solo root puede cambiar contraseña de otros, o el mismo usuario su propia contraseña
        if (!isRoot() && !currentUser.getUsername().equals(username)) {
            throw new IOException("Permiso denegado");
        }
        
        User user = fs.getUserByName().get(username);
        if (user == null) {
            throw new IOException("Usuario '" + username + "' no encontrado");
        }
        
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Nueva contraseña: ");
        String password = scanner.nextLine();
        
        System.out.print("Confirme contraseña: ");
        String confirmPassword = scanner.nextLine();
        
        if (!password.equals(confirmPassword)) {
            throw new IOException("Las contraseñas no coinciden");
        }
        
        if (password.trim().isEmpty()) {
            throw new IOException("La contraseña no puede estar vacía");
        }
        
        user.setPassword(password);
        
        // Guardar cambios
        fs.unmount();
        fs.mount();
        
        System.out.println("Contraseña cambiada exitosamente");
    }
    
    /**
     * Cambia de usuario (su - switch user)
     */
    public void switchUser(String username) throws IOException {
        User user = fs.getUserByName().get(username);
        if (user == null) {
            throw new IOException("Usuario '" + username + "' no encontrado");
        }
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Contraseña: ");
        String password = scanner.nextLine();
        
        if (!user.checkPassword(password)) {
            throw new IOException("Contraseña incorrecta");
        }
        
        currentUser = user;
        currentDirectory = user.getHomeDirectory();
        
        System.out.println("Sesión iniciada como: " + username);
        System.out.println("Directorio actual: " + currentDirectory);
    }
    
    /**
     * Muestra información del usuario actual
     */
    public void whoami() {
        if (currentUser == null) {
            System.out.println("No hay usuario autenticado");
            return;
        }
        
        System.out.println("Usuario: " + currentUser.getUsername());
        System.out.println("Nombre completo: " + currentUser.getFullName());
        System.out.println("UID: " + currentUser.getUserId());
        System.out.println("GID: " + currentUser.getGroupId());
        System.out.println("Directorio home: " + currentUser.getHomeDirectory());
    }
    
    /**
     * Muestra el directorio actual
     */
    public void pwd() {
        System.out.println(currentDirectory);
    }
    
    /**
     * Muestra información del sistema de archivos
     */
    public void infoFS() throws IOException {
        if (fs == null || !fs.isMounted()) {
            throw new IOException("Sistema de archivos no montado");
        }
        
        Superblock sb = fs.getSuperblock();
        
        System.out.println("\n=== INFORMACIÓN DEL SISTEMA DE ARCHIVOS ===");
        System.out.println("Nombre: " + sb.getFsName());
        System.out.println("Versión: " + sb.getFsVersion());
        System.out.println("Archivo: " + fsFilePath);
        
        long totalSizeMB = ((long) sb.getTotalBlocks() * sb.getBlockSize()) / (1024 * 1024);
        long usedBlocks = sb.getTotalBlocks() - sb.getFreeBlocks();
        long usedSizeMB = (usedBlocks * sb.getBlockSize()) / (1024 * 1024);
        long freeSizeMB = ((long) sb.getFreeBlocks() * sb.getBlockSize()) / (1024 * 1024);
        
        System.out.println("\nTamaño total: " + totalSizeMB + " MB");
        System.out.println("Espacio utilizado: " + usedSizeMB + " MB");
        System.out.println("Espacio disponible: " + freeSizeMB + " MB");
        
        System.out.println("\nBloques:");
        System.out.println("  Tamaño de bloque: " + sb.getBlockSize() + " bytes");
        System.out.println("  Total de bloques: " + sb.getTotalBlocks());
        System.out.println("  Bloques libres: " + sb.getFreeBlocks());
        System.out.println("  Bloques usados: " + usedBlocks);
        
        System.out.println("\nInodes:");
        System.out.println("  Total de inodes: " + sb.getTotalInodes());
        System.out.println("  Inodes libres: " + sb.getFreeInodes());
        System.out.println("  Inodes usados: " + (sb.getTotalInodes() - sb.getFreeInodes()));
        
        String strategy = "";
        switch (sb.getAllocationStrategy()) {
            case FSConstants.ALLOC_CONTIGUOUS:
                strategy = "Asignación Contigua";
                break;
            case FSConstants.ALLOC_LINKED:
                strategy = "Asignación Enlazada";
                break;
            case FSConstants.ALLOC_INDEXED:
                strategy = "Asignación Indexada";
                break;
        }
        System.out.println("\nEstrategia de asignación: " + strategy);
        
        System.out.println("\nUsuarios registrados: " + fs.getUserTable().size());
        System.out.println("Grupos registrados: " + fs.getGroupTable().size());
    }
    
    /**
     * Verifica si el usuario actual es root
     */
    private boolean isRoot() {
        return currentUser != null && currentUser.getUserId() == FSConstants.ROOT_UID;
    }
    
    /**
     * Verifica si hay un usuario autenticado
     */
    private void requireAuth() throws IOException {
        if (currentUser == null) {
            throw new IOException("Debe autenticarse primero. Use el comando 'su'");
        }
    }
    
    /**
     * Cierra el gestor del sistema de archivos
     */
    public void shutdown() throws IOException {
        if (fs != null && fs.isMounted()) {
            unmount();
        }
        running = false;
    }
    
    /**
     * Obtiene el prompt del shell
     */
    public String getPrompt() {
        if (currentUser == null) {
            return "guest@myFS$ ";
        }
        return currentUser.getUsername() + "@myFS:" + currentDirectory + "$ ";
    }
    
    // Getters
    public FileSystem getFileSystem() { return fs; }
    public User getCurrentUser() { return currentUser; }
    public String getCurrentDirectory() { return currentDirectory; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
}
package filesystem;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 *
 * @author gadyr
 */
public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String homeDirectory;
    private int groupId;

    public User() {
        this.userId = -1;
        this.username = "";
        this.passwordHash = "";
        this.fullName = "";
        this.homeDirectory = "";
        this.groupId = -1;
    }

    public User(int userId, String username, String password, String fullName, String homeDirectory, int groupId) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = hashPassword(password);
        this.fullName = fullName;
        this.homeDirectory = homeDirectory;
        this.groupId = groupId;
    }

    // Getters y Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    /**
     * Verificar contraseña
     */
    public boolean checkPassword(String password) {
        return passwordHash.equals(hashPassword(password));
    }

    /**
     * Cambiar contraseña
     */
    public void setPassword(String newPassword) {
        this.passwordHash = hashPassword(newPassword);
    }

    /**
     * Hash contraseña usando SHA-256
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al hashear contraseña", e);
        }
    }

    /**
     * Serializar usuario a bytes (512 bytes)
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(512);

        buffer.putInt(userId);

        // username (64 bytes)
        byte[] usernameBytes = new byte[64];
        if (username != null && !username.isEmpty()) {
            byte[] actual = username.getBytes();
            System.arraycopy(actual, 0, usernameBytes, 0,
                    Math.min(actual.length, 64));
        }
        buffer.put(usernameBytes);

        // passwordHash (88 bytes - Base64 de SHA-256 es ~44 caracteres)
        byte[] passwordBytes = new byte[88];
        if (passwordHash != null && !passwordHash.isEmpty()) {
            byte[] actual = passwordHash.getBytes();
            System.arraycopy(actual, 0, passwordBytes, 0,
                    Math.min(actual.length, 88));
        }
        buffer.put(passwordBytes);

        // fullName (128 bytes)
        byte[] fullNameBytes = new byte[128];
        if (fullName != null && !fullName.isEmpty()) {
            byte[] actual = fullName.getBytes();
            System.arraycopy(actual, 0, fullNameBytes, 0,
                    Math.min(actual.length, 128));
        }
        buffer.put(fullNameBytes);

        // homeDirectory (128 bytes)
        byte[] homeBytes = new byte[128];
        if (homeDirectory != null && !homeDirectory.isEmpty()) {
            byte[] actual = homeDirectory.getBytes();
            System.arraycopy(actual, 0, homeBytes, 0,
                    Math.min(actual.length, 128));
        }
        buffer.put(homeBytes);

        buffer.putInt(groupId);

        return buffer.array();
    }

    /**
     * Deserializa un usuario desde bytes
     */
    public static User fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        User user = new User();
        user.userId = buffer.getInt();
        
        // username
        byte[] usernameBytes = new byte[64];
        buffer.get(usernameBytes);
        user.username = new String(usernameBytes).trim().replace("\0", "");
        
        // passwordHash
        byte[] passwordBytes = new byte[88];
        buffer.get(passwordBytes);
        user.passwordHash = new String(passwordBytes).trim().replace("\0", "");
        
        // fullName
        byte[] fullNameBytes = new byte[128];
        buffer.get(fullNameBytes);
        user.fullName = new String(fullNameBytes).trim().replace("\0", "");
        
        // homeDirectory
        byte[] homeBytes = new byte[128];
        buffer.get(homeBytes);
        user.homeDirectory = new String(homeBytes).trim().replace("\0", "");
        
        user.groupId = buffer.getInt();
        
        return user;
    }    
}

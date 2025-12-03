package comandos;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

/**
 * Shell de comandos que simula un conjunto de utilidades básicas de un
 * sistema de archivos.
 *
 * @author dylan
 */
@Command(name = "", subcommands = {CommandLine.HelpCommand.class})
public class ComandosShell implements Runnable {

    /**
     * Constructor por defecto.
     */    
    public ComandosShell() {

    }


    /**
     * Método ejecutado cuando no se especifica ningún comando.
     * Actualmente no realiza ninguna acción.
     */    
    @Override
    public void run() {
        
    }

    /**
     * Crea y formatea el sistema de archivos con el tamaño especificado.
     *
     * @param size Tamaño del disco en MB.
     */    
    @Command(name = "format", description = "Formatea y crea el sistema de archivos")
    public void format(
            @Parameters(index = "0", description = "Tamaño del disco en MB") int size) {
        // TODO
    }

    /**
     * Finaliza la ejecución del programa.
     * Imprime un mensaje de salida antes de terminar.
     */    
    @Command(name = "exit", description = "Sale del programa")
    public void exit() {
        System.out.println("Saliendo del sistema de archivos...");
        // TODO
    }

    /**
     * Crea un nuevo usuario dentro del sistema.
     *
     * @param username Nombre del nuevo usuario.
     */    
    @Command(name = "useradd", description = "Crea un nuevo usuario")
    public void useradd(
            @Parameters(index = "0", description = "Nombre de usuario") String username) {
        // TODO
    }

    /**
     * Crea un nuevo grupo.
     *
     * @param groupName Nombre del grupo.
     */    
    @Command(name = "groupadd", description = "Crea un nuevo grupo")
    public void groupadd(
            @Parameters(index = "0", description = "Nombre del grupo") String groupName) {
        // TODO
    }

    /**
     * Cambia la contraseña del usuario especificado.
     *
     * @param username Nombre del usuario.
     */    
    @Command(name = "passwd", description = "Cambia la contraseña de un usuario")
    public void passwd(
            @Parameters(index = "0", description = "Nombre de usuario") String username) {
        // TODO
    }

    /**
     * Cambia el usuario actual.
     *
     * @param username Nuevo usuario (por defecto: root).
     */    
    @Command(name = "su", description = "Cambia de usuario")
    public void su(
            @Parameters(index = "0", defaultValue = "root", description = "Nombre de usuario (default: root)") String username) {
        // TODO
    }

    /**
     * Muestra el usuario que está actualmente autenticado.
     */
    @Command(name = "whoami", description = "Muestra el usuario actual")
    public void whoami() {
        // TODO
    }    
        
    /**
     * Muestra el directorio actual dentro del sistema de archivos.
     */    
    @Command(name = "pwd", description = "Muestra el directorio actual")
    public void pwd() {
        // TODO
    }

    /**
     * Crea uno o varios directorios.
     *
     * @param dirNames Lista de nombres de directorios a crear.
     */    
    @Command(name = "mkdir", description = "Crea uno o más directorios")
    public void mkdir(
            @Parameters(description = "Nombres de los directorios") String[] dirNames) {
        // TODO
    }

    /**
     * Elimina archivos o directorios.
     *
     * @param recursive Si es true, elimina de forma recursiva.
     * @param paths Lista de archivos o directorios a eliminar.
     */    
    @Command(name = "rm", description = "Elimina archivos o directorios")
    public void rm(
            @Option(names = {"-R"}, description = "Eliminación recursiva") boolean recursive,
            @Parameters(description = "Archivos o directorios a eliminar") String[] paths) {
        // TODO
    }

    /**
     * Mueve o renombra un archivo o directorio.
     *
     * @param source Ruta de origen.
     * @param destination Ruta de destino.
     */    
    @Command(name = "mv", description = "Mueve o renombra archivos/directorios")
    public void mv(
            @Parameters(index = "0", description = "Origen") String source,
            @Parameters(index = "1", description = "Destino") String destination) {
        // TODO
    }
    
    /**
     * Lista el contenido de un directorio.
     *
     * @param recursive Si es true, realiza un listado recursivo.
     * @param path Ruta del directorio a listar (por defecto ".").
     */    
    @Command(name = "ls", description = "Lista el contenido del directorio")
    public void ls(
            @Option(names = {"-R"}, description = "Listado recursivo") boolean recursive,
            @Parameters(index = "0", defaultValue = ".", description = "Ruta del directorio") String path) {
        // TODO
    }

    /**
     * Limpia la pantalla utilizando secuencias ANSI.
     */    
    @Command(name = "clear", description = "Limpia la pantalla")
    public void clear() {
        // Secuencias de escape ANSI para limpiar la pantalla
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    /**
     * Cambia el directorio actual.
     *
     * @param path Ruta del directorio al que se desea acceder.
     */    
    @Command(name = "cd", description = "Cambia de directorio")
    public void cd(
            @Parameters(index = "0", defaultValue = ".", description = "Ruta del directorio") String path) {
        // TODO
    }

    /**
     * Busca un archivo dentro del sistema.
     *
     * @param filename Nombre del archivo.
     */    
    @Command(name = "whereis", description = "Busca un archivo en el sistema")
    public void whereis(
            @Parameters(index = "0", description = "Nombre del archivo") String filename) {
        // TODO
    }
    
    /**
     * Crea un enlace hacia un archivo fuente.
     *
     * @param source Archivo original.
     * @param linkPath Ruta del enlace.
     */    
    @Command(name = "ln", description = "Crea un enlace a un archivo")
    public void ln(
            @Parameters(index = "0", description = "Archivo fuente") String source,
            @Parameters(index = "1", description = "Ruta del enlace") String linkPath) {
        // TODO
    }    
    
    /**
     * Crea un archivo vacío con el nombre especificado.
     *
     * @param filename Nombre del archivo.
     */    
    @Command(name = "touch", description = "Crea un archivo vacío")
    public void touch(
            @Parameters(index = "0", description = "Nombre del archivo") String filename) {
        // TODO
    }

    /**
     * Muestra el contenido del archivo especificado.
     *
     * @param filename Nombre del archivo.
     */    
    @Command(name = "cat", description = "Muestra el contenido de un archivo")
    public void cat(
            @Parameters(index = "0", description = "Nombre del archivo") String filename) {
        // TODO
    }

    /**
     * Cambia el propietario de un archivo o directorio.
     *
     * @param recursive Si es true, aplica el cambio de manera recursiva.
     * @param owner Nuevo propietario.
     * @param path Archivo o directorio a modificar.
     */    
    @Command(name = "chown", description = "Cambia el propietario de un archivo/directorio")
    public void chown(
            @Option(names = {"-R"}, description = "Cambio recursivo") boolean recursive,
            @Parameters(index = "0", description = "Nuevo propietario") String owner,
            @Parameters(index = "1", description = "Archivo o directorio") String path) {
        // TODO
    }

    /**
     * Cambia el grupo de un archivo o directorio.
     *
     * @param recursive Si es true, aplica el cambio de manera recursiva.
     * @param group Nuevo grupo.
     * @param path Archivo o directorio a modificar.
     */    
    @Command(name = "chgrp", description = "Cambia el grupo de un archivo/directorio")
    public void chgrp(
            @Option(names = {"-R"}, description = "Cambio recursivo") boolean recursive,
            @Parameters(index = "0", description = "Nuevo grupo") String group,
            @Parameters(index = "1", description = "Archivo o directorio") String path) {
        // TODO
    }

    /**
     * Cambia los permisos de un archivo usando notación numérica.
     *
     * @param permissions Permisos en formato numérico (por ejemplo, 755).
     * @param filename Archivo afectado.
     */    
    @Command(name = "chmod", description = "Cambia los permisos de un archivo")
    public void chmod(
            @Parameters(index = "0", description = "Permisos (ej: 77)") String permissions,
            @Parameters(index = "1", description = "Archivo") String filename) {
        // TODO
    }

    /**
     * Abre un archivo para lectura o edición.
     *
     * @param filename Nombre del archivo.
     */    
    @Command(name = "openFile", description = "Abre un archivo")
    public void openFile(
            @Parameters(index = "0", description = "Nombre del archivo") String filename) {
        // TODO
    }

    /**
     * Cierra un archivo previamente abierto.
     *
     * @param filename Nombre del archivo.
     */    
    @Command(name = "closeFile", description = "Cierra un archivo")
    public void closeFile(
            @Parameters(index = "0", description = "Nombre del archivo") String filename) {
        // TODO
    }

    /**
     * Muestra el FCB (File Control Block) de un archivo.
     *
     * @param filename Nombre del archivo.
     */    
    @Command(name = "viewFCB", description = "Muestra el FCB de un archivo")
    public void viewFCB(
            @Parameters(index = "0", description = "Nombre del archivo") String filename) {
        // TODO
    }

    /**
     * Muestra información general del sistema de archivos.
     */    
    @Command(name = "infoFS", description = "Muestra información del sistema de archivos")
    public void infoFS() {
        // TODO
    }

    /**
     * Abre un editor de texto simple para el archivo especificado.
     *
     * @param filename Nombre del archivo.
     */    
    @Command(name = "note", description = "Editor de texto simple")
    public void note(
            @Parameters(index = "0", description = "Nombre del archivo") String filename) {
        // TODO
    }
}

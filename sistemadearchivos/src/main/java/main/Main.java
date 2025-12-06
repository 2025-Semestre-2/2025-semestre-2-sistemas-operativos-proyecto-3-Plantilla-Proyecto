package main;

import commands.ShellCommands;
import commands.FileSystemManager;
import filesystem.FSConstants;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author dylan
 */
public class Main {
    public static void main(String[] args) {
        String fsFilePath = FSConstants.DEFAULT_FS_FILE;
        
        if (args.length > 0) {
            fsFilePath = args[0];
        }
        
        System.out.println("=========== Sistema de archivos ===========");
        
        FileSystemManager fsManager = new FileSystemManager(fsFilePath);
        
        File fsFile = new File(fsFilePath);
        boolean needsFormat = !fsFile.exists();
        
        if (needsFormat) {
            System.out.println("No se encontró un sistema de archivos existente.");
            System.out.println("Archivo: " + fsFilePath);
            System.out.println();
            System.out.println("Debe formatear el disco virtual antes de usar el sistema.");
            System.out.println("Use el comando: format <tamaño_en_MB>");
            System.out.println("Ejemplo: format 100");
            System.out.println();
        } else {
            System.out.println("Sistema de archivos encontrado: " + fsFilePath);
            System.out.println("Montando sistema de archivos...");
            System.out.println();
            
            try {
                fsManager.mount();
                System.out.println("Sistema de archivos montado exitosamente.");
                System.out.println("Por favor autentíquese usando el comando: su <usuario>");
                System.out.println("Ejemplo: su root");
                System.out.println();
            } catch (IOException e) {
                System.err.println("Error al montar el sistema de archivos: " + e.getMessage());
                System.err.println("Puede que el archivo esté corrupto. Considere formatearlo nuevamente.");
                System.out.println();
            }
        }
        
        // Crear el shell de comandos
        ShellCommands shell = new ShellCommands(fsManager);
        CommandLine cmd = new CommandLine(shell);
        
        // Configurar el CommandLine
        cmd.setUsageHelpAutoWidth(true);
        cmd.setExecutionExceptionHandler(new CustomExceptionHandler());
        
        // Loop principal del shell
        Scanner scanner = new Scanner(System.in);
        
        
        while (fsManager.isRunning()) {
            try {
                // Mostrar prompt
                System.out.print(fsManager.getPrompt());
                
                // Leer comando
                String input = scanner.nextLine().trim();
                
                // Ignorar líneas vacías
                if (input.isEmpty()) {
                    continue;
                }
                
                // Parsear y ejecutar comando
                String[] cmdArgs = parseCommand(input);
                
                // Verificar comandos especiales que no requieren autenticación
                String mainCommand = cmdArgs[0].toLowerCase();
                
                boolean requiresAuth = !mainCommand.equals("format") 
                                    && !mainCommand.equals("su") 
                                    && !mainCommand.equals("exit")
                                    && !mainCommand.equals("help")
                                    && !mainCommand.equals("clear")
                                    && !mainCommand.equals("infofs");
                
                // Verificar si necesita autenticación y si hay sistema montado
                if (requiresAuth) {
                    if (fsManager.getFileSystem() == null || !fsManager.getFileSystem().isMounted()) {
                        System.err.println("Error: Sistema de archivos no montado. Use 'format' o monte un FS existente.");
                        continue;
                    }
                    
                    if (fsManager.getCurrentUser() == null) {
                        System.err.println("Error: Debe autenticarse primero. Use el comando 'su <usuario>'");
                        continue;
                    }
                }
                
                int exitCode = cmd.execute(cmdArgs);
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
        System.out.println("\n¡Hasta luego!");
    }
    
    /**
     * Parsea el comando ingresado por el usuario
     * Maneja correctamente argumentos entre comillas
     */
    private static String[] parseCommand(String input) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentToken = new StringBuilder();
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
            } else {
                currentToken.append(c);
            }
        }
        
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        
        return tokens.toArray(new String[0]);
    }    

    /**
     * Manejador personalizado de excepciones para CommandLine
     */
    static class CustomExceptionHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception ex, CommandLine commandLine, 
                                           CommandLine.ParseResult parseResult) {
            
            // Imprimir mensaje de error
            commandLine.getErr().println(commandLine.getColorScheme()
                                        .errorText("Error: " + ex.getMessage()));
            
            // Si es un error de parámetros, mostrar ayuda
            if (ex instanceof CommandLine.ParameterException) {
                commandLine.usage(commandLine.getOut());
            }
            
            return commandLine.getExitCodeExceptionMapper() != null
                    ? commandLine.getExitCodeExceptionMapper().getExitCode(ex)
                    : commandLine.getCommandSpec().exitCodeOnExecutionException();
        }
    }    
}

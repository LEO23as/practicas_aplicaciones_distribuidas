package ec.edu.uteq.distribuidas.tcp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Cliente TCP interactivo.
 * Envía comandos al servidor y muestra la respuesta.
 */
public class ClienteTCP {

    private static final String HOST = "localhost";
    private static final int PUERTO = 9000;

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(
                     socket.getOutputStream(), true);
             Scanner teclado = new Scanner(System.in)) {

            System.out.println("Conectado a " + HOST + ":" + PUERTO);
            System.out.println("Comandos disponibles: HORA | ECO <texto> | SALIR");
            System.out.println("--------------------------------------------------");

            String comando;
            while (true) {
                System.out.print("> ");
                comando = teclado.nextLine().trim();
                if (comando.isEmpty()) continue;

                salida.println(comando);
                String respuesta = entrada.readLine();
                System.out.println("Servidor: " + respuesta);

                if (comando.equalsIgnoreCase("SALIR")) break;
            }
        }
        System.out.println("Conexion cerrada.");
    }
}
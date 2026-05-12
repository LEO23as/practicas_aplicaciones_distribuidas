package ec.edu.uteq.distribuidas.udp;

import java.net.*;
import java.util.Scanner;

/**
 * Cliente UDP: envía un datagrama y espera respuesta con
 * timeout de 3 segundos.
 */
public class ClienteUDP {

    private static final String HOST = "localhost";
    private static final int PUERTO = 9001;
    private static final int TIMEOUT = 3000;
    private static final int TAM_BUF = 1024;

    public static void main(String[] args) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress servidor = InetAddress.getByName(HOST);
            Scanner sc = new Scanner(System.in);

            System.out.println("Cliente UDP. Escribe un mensaje (o 'salir'):");
            while (sc.hasNextLine()) {
                String linea = sc.nextLine().trim();
                if (linea.equalsIgnoreCase("salir")) break;

                byte[] datos = linea.getBytes();
                DatagramPacket pkt = new DatagramPacket(
                        datos, datos.length, servidor, PUERTO);
                socket.send(pkt);

                byte[] buf = new byte[TAM_BUF];
                DatagramPacket resp = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(resp);
                    System.out.println("Respuesta: "
                            + new String(resp.getData(), 0, resp.getLength()));
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout: no se recibio respuesta en "
                            + TIMEOUT + "ms");
                }
            }
        }
    }
}
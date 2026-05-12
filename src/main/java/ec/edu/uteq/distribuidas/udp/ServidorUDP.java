package ec.edu.uteq.distribuidas.udp;

import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servidor UDP sin conexion.
 * Recibe datagramas de hasta 1024 bytes y responde en el mismo socket.
 */
public class ServidorUDP {

    private static final int PUERTO = 9001;
    private static final int TAM_BUF = 1024;

    public static void main(String[] args) throws Exception {
        try (DatagramSocket socket = new DatagramSocket(PUERTO)) {
            System.out.println("Servidor UDP escuchando en puerto " + PUERTO);

            byte[] buffer = new byte[TAM_BUF];
            while (true) {
                DatagramPacket paquete =
                        new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);

                String mensaje = new String(paquete.getData(),
                        0, paquete.getLength());
                String hora = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));

                System.out.println("[" + hora + "] Recibido de "
                        + paquete.getAddress() + ":" + paquete.getPort()
                        + " -> " + mensaje);

                String respuesta = "ACK:" + mensaje + " @" + hora;
                byte[] resp = respuesta.getBytes();
                DatagramPacket respPkt = new DatagramPacket(
                        resp, resp.length,
                        paquete.getAddress(), paquete.getPort());
                socket.send(respPkt);
            }
        }
    }
}

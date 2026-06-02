package ec.edu.uteq.distribuidas.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.distribuidas.model.Message;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClienteRegistro {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TOKEN_VALIDO = "TOKEN-SECRETO-2025";
    private static final String TOKEN_INVALIDO = "TOKEN-FALSO";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Cliente RegistroDistribuido ===");
        System.out.println("Conectando a nodo (host:puerto)...");

        String host = "localhost";
        int port = 8081;

        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        System.out.println("Nodo destino: " + host + ":" + port);
        System.out.println("Comandos: op <texto> | op-bad <texto> | salir");

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.equals("salir")) break;

            String[] parts = line.split(" ", 2);
            String cmd = parts[0];
            String payload = parts.length > 1 ? parts[1] : "OP-" + System.currentTimeMillis();

            String token = cmd.equals("op-bad") ? TOKEN_INVALIDO : TOKEN_VALIDO;

            Message msg = new Message(Message.Type.OPERATION, 0, 0, payload, token);

            try {
                Message response = sendAndReceive(host, port, msg);
                System.out.println("[RESPUESTA] " + response.getPayload());
            } catch (Exception e) {
                System.err.println("[ERROR] No se pudo conectar con " + host + ":" + port + " — " + e.getMessage());
            }
        }

        System.out.println("Cliente cerrado.");
    }

    private static Message sendAndReceive(String host, int port, Message message) throws IOException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            byte[] data = mapper.writeValueAsBytes(message);
            out.writeInt(data.length);
            out.write(data);
            out.flush();

            int len = in.readInt();
            byte[] response = new byte[len];
            in.readFully(response);
            return mapper.readValue(response, Message.class);
        }
    }
}

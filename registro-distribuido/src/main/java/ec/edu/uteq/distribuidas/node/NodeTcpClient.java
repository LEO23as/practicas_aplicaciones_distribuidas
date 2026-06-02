package ec.edu.uteq.distribuidas.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.distribuidas.model.Message;

import java.io.*;
import java.net.Socket;

public class NodeTcpClient {

    private final String host;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();

    public NodeTcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public Message send(Message message) throws IOException {
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


    public void sendNoReply(Message message) {
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            socket.setSoTimeout(1000);
            byte[] data = mapper.writeValueAsBytes(message);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
        } catch (IOException ignored) {}
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}

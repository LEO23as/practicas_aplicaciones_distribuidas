package ec.edu.uteq.distribuidas.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.distribuidas.model.Message;

import java.io.*;
import java.net.Socket;

public class ConnectionHandler implements Runnable {

    private final Socket socket;
    private final NodeService nodeService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ConnectionHandler(Socket socket, NodeService nodeService) {
        this.socket = socket;
        this.nodeService = nodeService;
    }

    @Override
    public void run() {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {


            int len = in.readInt();
            byte[] data = new byte[len];
            in.readFully(data);

            Message incoming = mapper.readValue(data, Message.class);
            Message response = nodeService.handle(incoming);

            if (response != null) {
                byte[] respData = mapper.writeValueAsBytes(response);
                out.writeInt(respData.length);
                out.write(respData);
                out.flush();
            }

        } catch (IOException e) {

        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}

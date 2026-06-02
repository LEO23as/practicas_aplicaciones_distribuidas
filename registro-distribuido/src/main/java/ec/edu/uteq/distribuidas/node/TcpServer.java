package ec.edu.uteq.distribuidas.node;

import ec.edu.uteq.distribuidas.config.NodeConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TcpServer {

    private final NodeConfig config;
    private final NodeService nodeService;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public TcpServer(NodeConfig config, NodeService nodeService) {
        this.config = config;
        this.nodeService = nodeService;
    }

    @PostConstruct
    public void start() {
        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(config.getPort());
                System.out.println("[N" + config.getId() + "] TCP escuchando en puerto " + config.getPort());
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        pool.submit(new ConnectionHandler(client, nodeService));
                    } catch (IOException e) {
                        if (running) e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("[N" + config.getId() + "] Error al abrir puerto " + config.getPort() + ": " + e.getMessage());
            }
        }, "tcp-server-n" + config.getId());
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        pool.shutdownNow();
    }
}

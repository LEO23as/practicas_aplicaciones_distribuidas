package ec.edu.uteq.distribuidas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "node")
public class NodeConfig {

    private int id;
    private int port;
    private List<Map<String, Object>> peers;
    private String validToken;
    private int heartbeatIntervalMs = 2000;
    private int heartbeatTimeoutMs = 6000;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public List<Map<String, Object>> getPeers() { return peers; }
    public void setPeers(List<Map<String, Object>> peers) { this.peers = peers; }

    public String getValidToken() { return validToken; }
    public void setValidToken(String validToken) { this.validToken = validToken; }

    public int getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
    public void setHeartbeatIntervalMs(int heartbeatIntervalMs) { this.heartbeatIntervalMs = heartbeatIntervalMs; }

    public int getHeartbeatTimeoutMs() { return heartbeatTimeoutMs; }
    public void setHeartbeatTimeoutMs(int heartbeatTimeoutMs) { this.heartbeatTimeoutMs = heartbeatTimeoutMs; }

    public record PeerInfo(int id, String host, int port) {}


    public List<PeerInfo> getPeerList() {
        if (peers == null) return List.of();

        return peers.stream().map(m -> new PeerInfo(
                Integer.parseInt(m.get("id").toString()),
                m.get("host").toString(),
                Integer.parseInt(m.get("port").toString())
        )).toList();
    }

}

package ec.edu.uteq.distribuidas.node;

import ec.edu.uteq.distribuidas.config.NodeConfig;
import ec.edu.uteq.distribuidas.model.LogEvent;
import ec.edu.uteq.distribuidas.model.Message;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class NodeService {

    private final NodeConfig config;
    private final LamportClock clock;
    private final EventLog eventLog;

    private int coordinatorId = -1;
    private boolean electionInProgress = false;
    private final Map<Integer, Long> lastHeartbeat = new ConcurrentHashMap<>();
    private final Map<Integer, NodeTcpClient> peers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public NodeService(NodeConfig config, LamportClock clock, EventLog eventLog) {
        this.config = config;
        this.clock = clock;
        this.eventLog = eventLog;
    }

    @PostConstruct
    public void init() {

        for (NodeConfig.PeerInfo p : config.getPeerList()) {
            peers.put(p.id(), new NodeTcpClient(p.host(), p.port()));
            lastHeartbeat.put(p.id(), System.currentTimeMillis());
        }

        log("Nodo N" + config.getId() + " iniciado en puerto " + config.getPort());


        scheduler.scheduleAtFixedRate(this::sendHeartbeats,
                2000, config.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);


        scheduler.scheduleAtFixedRate(this::checkHeartbeats,
                3000, config.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);


        scheduler.schedule(this::startElection, 4000, TimeUnit.MILLISECONDS);
    }


    public Message handle(Message msg) {
        switch (msg.getType()) {
            case OPERATION -> { return handleOperation(msg); }
            case HEARTBEAT -> { return handleHeartbeat(msg); }
            case HEARTBEAT_ACK -> { handleHeartbeatAck(msg); return null; }
            case ELECTION -> { return handleElection(msg); }
            case ELECTION_OK -> { handleElectionOk(msg); return null; }
            case COORDINATOR -> { handleCoordinator(msg); return null; }
            default -> { return buildResponse(Message.Type.OPERATION, "UNKNOWN_TYPE"); }
        }
    }

    private Message handleOperation(Message msg) {

        if (!config.getValidToken().equals(msg.getToken())) {
            System.out.println("[N" + config.getId() + "] RECHAZADO — token inválido de sender=" + msg.getSenderId());
            return buildResponse(Message.Type.OPERATION, "ERROR:TOKEN_INVALIDO");
        }


        long newClock = clock.update(msg.getLamportClock());
        String desc = "OP recibida de cliente: " + msg.getPayload();
        LogEvent event = new LogEvent(newClock, config.getId(), desc);
        eventLog.add(event);

        System.out.println("[N" + config.getId() + "] " + event);
        eventLog.print(config.getId());


        replicateTopeers(msg, newClock);

        return buildResponse(Message.Type.OPERATION, "OK:LC=" + newClock);
    }

    private void replicateTopeers(Message original, long currentClock) {
        Message sync = new Message(
                Message.Type.SYNC_CLOCK,
                config.getId(),
                currentClock,
                original.getPayload(),
                config.getValidToken()
        );
        for (Map.Entry<Integer, NodeTcpClient> entry : peers.entrySet()) {
            if (isAlive(entry.getKey())) {
                try {
                    entry.getValue().sendNoReply(sync);
                } catch (Exception ignored) {}
            }
        }
    }


    private void sendHeartbeats() {
        Message hb = new Message(Message.Type.HEARTBEAT, config.getId(), clock.tick(), null, null);
        for (Map.Entry<Integer, NodeTcpClient> entry : peers.entrySet()) {
            try {
                Message ack = entry.getValue().send(hb);
                if (ack != null) {
                    clock.update(ack.getLamportClock());
                    lastHeartbeat.put(entry.getKey(), System.currentTimeMillis());
                }
            } catch (Exception e) {

            }
        }
    }

    private Message handleHeartbeat(Message msg) {
        clock.update(msg.getLamportClock());
        lastHeartbeat.put(msg.getSenderId(), System.currentTimeMillis());
        return buildResponse(Message.Type.HEARTBEAT_ACK, "ACK");
    }

    private void handleHeartbeatAck(Message msg) {
        clock.update(msg.getLamportClock());
        lastHeartbeat.put(msg.getSenderId(), System.currentTimeMillis());
    }

    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        long timeout = config.getHeartbeatTimeoutMs();
        for (Map.Entry<Integer, Long> entry : lastHeartbeat.entrySet()) {
            int peerId = entry.getKey();
            long last = entry.getValue();
            if (now - last > timeout) {
                System.out.println("[N" + config.getId() + "] FALLO DETECTADO: nodo N" + peerId + " no responde.");
                log("FALLO: N" + peerId + " marcado como caído");
                lastHeartbeat.put(peerId, now); // reset para no repetir el log
                if (peerId == coordinatorId && !electionInProgress) {
                    System.out.println("[N" + config.getId() + "] El coordinador N" + peerId + " cayó. Iniciando elección...");
                    startElection();
                }
            }
        }
    }

    private boolean isAlive(int peerId) {
        Long last = lastHeartbeat.get(peerId);
        if (last == null) return false;
        return System.currentTimeMillis() - last < config.getHeartbeatTimeoutMs();
    }


    public void startElection() {
        if (electionInProgress) return;
        electionInProgress = true;
        log("ELECCIÓN iniciada por N" + config.getId());
        System.out.println("[N" + config.getId() + "] Iniciando elección Bully...");

        boolean anyHigher = false;
        for (Map.Entry<Integer, NodeTcpClient> entry : peers.entrySet()) {
            int peerId = entry.getKey();
            if (peerId > config.getId() && isAlive(peerId)) {
                anyHigher = true;
                Message elMsg = new Message(Message.Type.ELECTION, config.getId(), clock.tick(), null, null);
                try {
                    Message ok = entry.getValue().send(elMsg);
                    if (ok != null && ok.getType() == Message.Type.ELECTION_OK) {
                        clock.update(ok.getLamportClock());
                        System.out.println("[N" + config.getId() + "] N" + peerId + " respondió OK a la elección.");
                    }
                } catch (Exception e) {
                    System.out.println("[N" + config.getId() + "] N" + peerId + " no respondió en elección.");
                    anyHigher = false;
                }
            }
        }

        if (!anyHigher) {

            becomeCoordinator();
        } else {

            scheduler.schedule(() -> {
                if (coordinatorId == -1 || !isAlive(coordinatorId)) {
                    electionInProgress = false;
                    startElection();
                } else {
                    electionInProgress = false;
                }
            }, config.getHeartbeatTimeoutMs(), TimeUnit.MILLISECONDS);
        }
    }

    private void becomeCoordinator() {
        coordinatorId = config.getId();
        electionInProgress = false;
        log("COORDINADOR: N" + config.getId() + " es el nuevo coordinador");
        System.out.println("[N" + config.getId() + "] SOY EL NUEVO COORDINADOR.");

        Message coordMsg = new Message(Message.Type.COORDINATOR, config.getId(), clock.tick(),
                String.valueOf(config.getId()), null);
        for (Map.Entry<Integer, NodeTcpClient> entry : peers.entrySet()) {
            if (isAlive(entry.getKey())) {
                entry.getValue().sendNoReply(coordMsg);
            }
        }
    }

    private Message handleElection(Message msg) {
        clock.update(msg.getLamportClock());

        if (msg.getSenderId() < config.getId()) {
            scheduler.submit(this::startElection);
            return buildResponse(Message.Type.ELECTION_OK, "OK");
        }
        return buildResponse(Message.Type.ELECTION_OK, "OK");
    }

    private void handleElectionOk(Message msg) {
        clock.update(msg.getLamportClock());
    }

    private void handleCoordinator(Message msg) {
        clock.update(msg.getLamportClock());
        coordinatorId = Integer.parseInt(msg.getPayload());
        electionInProgress = false;
        log("COORDINADOR reconocido: N" + coordinatorId);
        System.out.println("[N" + config.getId() + "] Nuevo coordinador reconocido: N" + coordinatorId);
    }


    private void log(String desc) {
        long lc = clock.tick();
        eventLog.add(new LogEvent(lc, config.getId(), desc));
    }

    private Message buildResponse(Message.Type type, String payload) {
        return new Message(type, config.getId(), clock.tick(), payload, null);
    }

    public int getCoordinatorId() { return coordinatorId; }
    public long getLamportClock() { return clock.get(); }
    public List<LogEvent> getLog() { return eventLog.getSorted(); }
}

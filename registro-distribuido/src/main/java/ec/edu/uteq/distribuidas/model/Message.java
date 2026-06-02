package ec.edu.uteq.distribuidas.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {

    public enum Type {
        OPERATION,
        HEARTBEAT,
        HEARTBEAT_ACK,
        ELECTION,
        ELECTION_OK,
        COORDINATOR,
        SYNC_CLOCK,
        AUTH
    }

    private final Type type;
    private final int senderId;
    private final long lamportClock;
    private final String payload;
    private final String token;

    @JsonCreator
    public Message(
            @JsonProperty("type") Type type,
            @JsonProperty("senderId") int senderId,
            @JsonProperty("lamportClock") long lamportClock,
            @JsonProperty("payload") String payload,
            @JsonProperty("token") String token) {
        this.type = type;
        this.senderId = senderId;
        this.lamportClock = lamportClock;
        this.payload = payload;
        this.token = token;
    }

    public Type getType() { return type; }
    public int getSenderId() { return senderId; }
    public long getLamportClock() { return lamportClock; }
    public String getPayload() { return payload; }
    public String getToken() { return token; }

    @Override
    public String toString() {
        return "Message{type=" + type + ", sender=" + senderId + ", clock=" + lamportClock + ", payload='" + payload + "'}";
    }
}

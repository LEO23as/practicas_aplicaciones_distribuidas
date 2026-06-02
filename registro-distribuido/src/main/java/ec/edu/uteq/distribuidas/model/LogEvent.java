package ec.edu.uteq.distribuidas.model;

public class LogEvent implements Comparable<LogEvent> {

    private final long lamportClock;
    private final int nodeId;
    private final String description;
    private final long wallTime;

    public LogEvent(long lamportClock, int nodeId, String description) {
        this.lamportClock = lamportClock;
        this.nodeId = nodeId;
        this.description = description;
        this.wallTime = System.currentTimeMillis();
    }


    @Override
    public int compareTo(LogEvent other) {
        if (this.lamportClock != other.lamportClock) {
            return Long.compare(this.lamportClock, other.lamportClock);
        }
        return Integer.compare(this.nodeId, other.nodeId);
    }

    public long getLamportClock() { return lamportClock; }
    public int getNodeId() { return nodeId; }
    public String getDescription() { return description; }
    public long getWallTime() { return wallTime; }

    @Override
    public String toString() {
        return String.format("[LC=%d | N%d | %s]", lamportClock, nodeId, description);
    }
}

package ec.edu.uteq.distribuidas.node;

import ec.edu.uteq.distribuidas.model.LogEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EventLog {

    private final CopyOnWriteArrayList<LogEvent> events = new CopyOnWriteArrayList<>();

    public void add(LogEvent event) {
        events.add(event);
    }

    public List<LogEvent> getSorted() {
        List<LogEvent> sorted = new ArrayList<>(events);
        Collections.sort(sorted);
        return sorted;
    }

    public void print(int nodeId) {
        System.out.println("\n=== LOG ORDENADO — Nodo " + nodeId + " ===");
        getSorted().forEach(System.out::println);
        System.out.println("===========================================\n");
    }
}

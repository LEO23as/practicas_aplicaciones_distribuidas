package ec.edu.uteq.distribuidas.node;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class LamportClock {

    private final AtomicLong clock = new AtomicLong(0);


    public long tick() {
        return clock.incrementAndGet();
    }


    public long update(long received) {
        long updated;
        long current;
        do {
            current = clock.get();
            updated = Math.max(current, received) + 1;
        } while (!clock.compareAndSet(current, updated));
        return updated;
    }

    public long get() {
        return clock.get();
    }
}

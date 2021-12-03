package we.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hongqiaowei
 */

public class LongIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(LongIdGenerator.class);

    private static final int  bound            = 131071;

    private static final int  timestampOffset  = 31;

    private static final int  serverIdOffset   = 17;

    private static final long serverId         = NetworkUtils.getServerId();

    private static final long serverIdSegment  = serverId << serverIdOffset;

    private AtomicInteger counter = new AtomicInteger(-1);

    private boolean reseting = false;

    private final ReentrantLock lck = new ReentrantLock(true);

    private final Condition condition = lck.newCondition();

    public AtomicInteger getCounter() {
        return counter;
    }

    public long next() {
        if (reseting) {
            try {
                condition.await();
            } catch (InterruptedException e) {
                log.warn("cond is interrupted, counter = " + counter.intValue());
            }
        }
        int c = counter.incrementAndGet();
        if (c < 0) {
            lck.lock();
            try {
                c = counter.incrementAndGet();
                if (c < 0) {
                    reseting = true;
                    c = 0;
                    counter.set(c);
                    reseting = false;
                    condition.signalAll();
                }
            } finally {
                lck.unlock();
            }
        }
        return ((System.currentTimeMillis() / 1000) << timestampOffset) | serverIdSegment | (c % bound);
    }
}

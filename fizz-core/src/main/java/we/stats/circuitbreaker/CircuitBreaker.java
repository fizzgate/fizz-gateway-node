package we.stats.circuitbreaker;

import org.springframework.web.server.ServerWebExchange;

import java.math.BigDecimal;

/**
 * inaccuracy is tolerable
 */
public class CircuitBreaker {

    // not use strategy pattern
    private enum BreakStrategy {
        total_errors, errors_ratio
    }

    private enum ResumeStrategy {
        immediate, gradual, detective
    }

    public enum State {
        CLOSED/* and monitoring*/, OPEN, RESUME_GRADUALLY, RESUME_DETECTIVELY
    }

    private BreakStrategy breakStrategy;

    private ResumeStrategy resumeStrategy;

    private int resumeDuration;

    private int originResumeTraffic;

    private BigDecimal errorsRatio;

    private int totalErrors;

    private static long currentTimeSlot() {
        return System.currentTimeMillis() / 1000 * 1000;
    }

    private long begin = currentTimeSlot();

    // AtomicInteger
    private int requestCount;


    private int errorCount;

    private int breakDuration;

    private int rejectCount;


    private State state = State.CLOSED;

    // 实例化时执行这个
    private void originResumeTraffic() {
        originResumeTraffic = 100 / resumeDuration;
    }


    // 需要视当前状态，重置 errorCount ？比如新的监控周期
    private void correctState() {

    }

    // FlowControlFilter记录请求失败的地方调下这个方法
    public void incrErrorCount() {
        correctState();
    }

    public void transitionTo(State s) {
        // 新状态的开始时间
    }

    private boolean isResumeTraffic() {
        return true;
    }

    public boolean permit(ServerWebExchange exchange, String service, String path) {
        correctState();
        requestCount++;
        if (state == State.CLOSED) { // 一种状态，拆为一个方法
            if (breakStrategy == BreakStrategy.total_errors && true /*错误数达到阀值，就此时此处算*/) {
                transitionTo(State.OPEN);
                rejectCount++;
                return false;
            }
            if (breakStrategy == BreakStrategy.errors_ratio && true /*错误比例达到阀值*/) {
                transitionTo(State.OPEN);
                rejectCount++;
                return false;
            }
            return true;
        }

        if (state == State.OPEN) {
            long currentTimeSlot = currentTimeSlot();
            if (currentTimeSlot - begin > breakDuration) {
                // 根据恢复策略处理
                if (resumeStrategy == ResumeStrategy.immediate) {
                    transitionTo(State.CLOSED);
                    return true;
                }
                if (resumeStrategy == ResumeStrategy.detective) {
                    // 抢到的请求，充当唯一的探针请求
                        // 它
                        transitionTo(State.RESUME_DETECTIVELY);
                        exchange.getAttributes().put("detectiveReq", this);
                            // FlowControlFilter记录请求成功失败的地方，发现是探针请求
                            // 如果探针请求成功
                                this.transitionTo(State.CLOSED);
                            // 如果探针请求失败，保持 open，但还是要执行，因为要重置新熔断周期的时间等
                                this.transitionTo(State.OPEN);
                        if (true) {
                            return true;
                        }
                    // 没抢到的请求，全拒绝
                    rejectCount++;
                    return false;
                }
                if (resumeStrategy == ResumeStrategy.gradual) {
                    // 抢到的请求（有请求r可能在t前，然后当前请求执行t，然后r不用再抢了，直接return !isResumeTraffic()）
                        // 它
                        transitionTo(State.RESUME_GRADUALLY); // t
                        // 判断自己是否在恢复流量内
                        if (true) {
                            return !isResumeTraffic();
                        }
                    // 其它请求，判断自己是否在恢复流量内
                    if (true) {
                        return !isResumeTraffic();
                    }
                }
            }
            rejectCount++; // ++rejectCount ?
            return false;
        }

        if (state == State.RESUME_DETECTIVELY) {
            rejectCount++;
            return false;
        }

        if (state == State.RESUME_GRADUALLY) {
            return !isResumeTraffic();
        }

        return true;
    }
}

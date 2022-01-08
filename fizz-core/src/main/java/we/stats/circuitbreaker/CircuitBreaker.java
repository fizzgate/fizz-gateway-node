/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.stats.circuitbreaker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.server.ServerWebExchange;
import we.util.JacksonUtils;
import we.util.ResourceIdUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * inaccuracy is acceptable
 *
 * @author hongqiaowei
 */

public class CircuitBreaker {

    private enum Type {
        service_default, service, path
    }

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

    private static class GradualResumeTimeWindowContext {
        private final AtomicLong timeWindow = new AtomicLong(0);
        private final int resumeTraffic;
        private final int rejectTraffic;
        private final AtomicInteger resumeCount = new AtomicInteger(0);
        private final AtomicInteger rejectCount = new AtomicInteger(0);

        public GradualResumeTimeWindowContext(int resumeTraffic) {
            this.resumeTraffic = resumeTraffic;
            rejectTraffic = 100 - this.resumeTraffic;
        }

        public boolean permit(long current) {
            long tw = timeWindow.get();
            if (tw == 0 || current - tw > 999) {
                timeWindow.compareAndSet(tw, timeWindow(current));
                if (tw != 0) {
                    resumeCount.set(1);
                    rejectCount.set(0);
                }
            }

            if (resumeCount.incrementAndGet() <= resumeTraffic) {
                return true;
            }
            if (rejectCount.incrementAndGet() <= rejectTraffic) {
                return false;
            }
            if (resumeCount.get() > resumeTraffic && rejectCount.get() > rejectTraffic) {
                resumeCount.set(1);
                rejectCount.set(0);
            }
            return true;
        }
    }

    public static final String DETECT_REQUEST = "detectReq@";


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isDeleted = false;

    public Type    type;

    public boolean enable = false;

    public long    id;

    public String  service;

    public String  path;

    public String  resource;

    public BreakStrategy breakStrategy;

    public float         errorRatioThreshold;

    public int           totalErrorThreshold;

    public int           monitorDuration;

    public int           minRequests;

    public int           breakDuration;


    public  ResumeStrategy                        resumeStrategy;

    public  int                                   resumeDuration;

    private List<GradualResumeTimeWindowContext>  gradualResumeTimeWindowContexts;

    public  int                                   initialResumeTraffic;


    public String responseContentType;

    public String responseContent;


    public final AtomicInteger          requestCount    = new AtomicInteger(0);

    public final AtomicInteger          errorCount      = new AtomicInteger(0);

    public final AtomicInteger          rejectCount     = new AtomicInteger(0);

    public final AtomicReference<State> stateRef        = new AtomicReference<>(State.CLOSED);

    public       long                   stateStartTime;


    @JsonCreator
    public CircuitBreaker(
            @JsonProperty("isDeleted")           int    isDeleted,
            @JsonProperty("type")                int    type,
            @JsonProperty("enable")              int    enable,
            @JsonProperty("id")                  long   id,
            @JsonProperty("service")             String service,
            @JsonProperty("path")                String path,
            @JsonProperty("strategy")            int    strategy,
            @JsonProperty("ratioThreshold")      float  ratioThreshold,
            @JsonProperty("exceptionCount")      int    exceptionCount,
            @JsonProperty("minRequestCount")     int    minRequestCount,
            @JsonProperty("timeWindow")          int    timeWindow,
            @JsonProperty("statInterval")        int    statInterval,
            @JsonProperty("recoveryStrategy")    int    recoveryStrategy,
            @JsonProperty("recoveryTimeWindow")  int    recoveryTimeWindow,
            @JsonProperty("responseContentType") String responseContentType,
            @JsonProperty("responseContent")     String responseContent) {

        if (isDeleted == 1) {
            this.isDeleted = true;
        }

        if (type == 1) {
            this.type = Type.service_default;
            this.service = ResourceIdUtils.SERVICE_DEFAULT;
            if (enable == 1) {
                this.enable = true;
            }
        } else if (type == 2) {
            this.type = Type.service;
        } else {
            this.type = Type.path;
        }

        this.id = id;
        if (this.type != Type.service_default) {
            this.service = service;
        }
        this.path = path;
        resource = ResourceIdUtils.buildResourceId(null, null, null, this.service, this.path);

        if (strategy == 1) {
            breakStrategy = BreakStrategy.errors_ratio;
            errorRatioThreshold = ratioThreshold;
        } else {
            breakStrategy = BreakStrategy.total_errors;
            totalErrorThreshold = exceptionCount;
        }
        minRequests = minRequestCount;
        breakDuration = timeWindow;
        monitorDuration = statInterval;
        if (recoveryStrategy == 1) {
            resumeStrategy = ResumeStrategy.detective;
        } else if (recoveryStrategy == 2) {
            resumeStrategy = ResumeStrategy.gradual;
            resumeDuration = recoveryTimeWindow;
            initGradualResumeTimeWindowContext();
        } else {
            resumeStrategy = ResumeStrategy.immediate;
        }

        this.responseContentType = responseContentType;
        this.responseContent = responseContent;

        stateStartTime = currentTimeWindow();
    }

    private static long currentTimeWindow() {
        return timeWindow(System.currentTimeMillis());
    }

    private static long timeWindow(long timeMills) {
        return timeMills / 1000 * 1000;
    }

    private void initGradualResumeTimeWindowContext() {
        BigDecimal totalTraffic = new BigDecimal(100);
        BigDecimal duration = new BigDecimal(resumeDuration);
        initialResumeTraffic = totalTraffic.divide(duration, 0, RoundingMode.HALF_UP).intValue();
        if (initialResumeTraffic == 0) {
            initialResumeTraffic = 1;
        }

        gradualResumeTimeWindowContexts = new ArrayList<>(resumeDuration);
        for (int i = 1; i <= resumeDuration; i++) {
            int resumeTraffic = initialResumeTraffic * i;
            GradualResumeTimeWindowContext ctx = new GradualResumeTimeWindowContext(resumeTraffic);
            gradualResumeTimeWindowContexts.add(ctx);
        }
    }

    private boolean isResumeTraffic(long current) {
        long nThSecond = (current - stateStartTime) / 1000 + 1;
        GradualResumeTimeWindowContext ctx = gradualResumeTimeWindowContexts.get((int) nThSecond);
        return ctx.permit(current);
    }

    private void correctState(long current) {
        State s = stateRef.get();
        long stateDuration = current - stateStartTime;

        if (s == State.CLOSED && stateDuration > monitorDuration) {
            transite(s, State.CLOSED);

        } else if (s == State.OPEN && stateDuration > breakDuration) {
            transite(s, State.CLOSED);

        } else if (s == State.RESUME_GRADUALLY && stateDuration > resumeDuration) {
            transite(s, State.CLOSED);
        }
    }

    // FlowControlFilter记录请求失败的地方调下这个方法
    public void incrErrorCount() {
        correctState(System.currentTimeMillis());
    }

    public boolean transite(State current, State target) {
        if (stateRef.compareAndSet(current, target)) {
            stateStartTime = currentTimeWindow();
            requestCount.set(0);
            errorCount  .set(0);
            rejectCount .set(0);
            return true;
        } else {
            return false;
        }
    }

    public boolean permit(ServerWebExchange exchange) {
        long current = System.currentTimeMillis();
        correctState(current);
        requestCount.getAndIncrement();
        if (stateRef.get() == State.CLOSED) {
            return permitCallInClosedState();
        }
        if (stateRef.get() == State.OPEN) {
            return permitCallInOpenState(exchange, current);
        }
        if (stateRef.get() == State.RESUME_DETECTIVELY) {
            rejectCount.getAndIncrement();
            return false;
        }
        if (stateRef.get() == State.RESUME_GRADUALLY) {
            return permitCallInResumeGraduallyState(current);
        }
        return true;
    }

    private boolean permitCallInClosedState() {
        if (breakStrategy == BreakStrategy.total_errors && requestCount.get() > minRequests && errorCount.get() > totalErrorThreshold) {
            transite(State.CLOSED, State.OPEN);
            rejectCount.getAndIncrement();
            return false;
        }
        if (breakStrategy == BreakStrategy.errors_ratio && requestCount.get() > minRequests) {
            BigDecimal errors   = new BigDecimal(errorCount.get());
            BigDecimal requests = new BigDecimal(requestCount.get());
            float p = errors.divide(requests, 2, RoundingMode.HALF_UP).floatValue();
            if (p - errorRatioThreshold > 0) {
                transite(State.CLOSED, State.OPEN);
                rejectCount.getAndIncrement();
                return false;
            }
        }
        return true;
    }

    private boolean permitCallInOpenState(ServerWebExchange exchange, long current) {
        if (current - stateStartTime > breakDuration) {
            if (resumeStrategy == ResumeStrategy.immediate) {
                transite(State.OPEN, State.CLOSED);
                return true;
            }
            if (resumeStrategy == ResumeStrategy.detective) {
                if (transite(State.OPEN, State.RESUME_DETECTIVELY)) {
                    exchange.getAttributes().put(DETECT_REQUEST, this);
                    return true;
                }
                rejectCount.getAndIncrement();
                return false;
            }
            if (resumeStrategy == ResumeStrategy.gradual) {
                if (transite(State.OPEN, State.RESUME_GRADUALLY)) {
                    return true;
                }
                return isResumeTraffic(current);
            }
        }

        rejectCount.getAndIncrement();
        return false;
    }

    private boolean permitCallInResumeGraduallyState(long current) {
        if (current - stateStartTime > resumeDuration) {
            transite(State.RESUME_GRADUALLY, State.CLOSED);
            return true;
        }
        return isResumeTraffic(current);
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;
import we.stats.FlowStat;
import we.stats.ResourceStat;
import we.stats.TimeSlot;
import we.stats.TimeWindowStat;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum Type {
        SERVICE_DEFAULT, SERVICE, PATH
    }

    // not use strategy pattern
    public enum BreakStrategy {
        TOTAL_ERRORS, ERRORS_RATIO
    }

    public enum ResumeStrategy {
        IMMEDIATE, GRADUAL, DETECTIVE
    }

    public enum State {
        CLOSED/* and monitoring*/, OPEN, RESUME_GRADUALLY, RESUME_DETECTIVE
    }

    private static class GradualResumeTimeWindowContext {
        private final int resumeTraffic;
        private final int rejectTraffic;

        public GradualResumeTimeWindowContext(int resumeTraffic) {
            this.resumeTraffic = resumeTraffic;
            rejectTraffic = 100 - this.resumeTraffic;
        }

        public boolean permit(ResourceStat resourceStat, long currentTimeWindow) {

            TimeSlot timeSlot = resourceStat.getTimeSlot(currentTimeWindow);
            AtomicLong resumeCount = timeSlot.getGradualResumeNum();
            AtomicInteger resumeTrafficFactor = timeSlot.getResumeTrafficFactor();
            long n = resumeTrafficFactor.get();
            if (resumeCount.incrementAndGet() <= resumeTraffic * n) {
                LOGGER.debug("{} current time window {}, resume traffic {}, resume traffic factor {}, resume count {}, resume current request",
                             resourceStat.getResourceId(), currentTimeWindow, resumeTraffic, n, resumeCount.get());
                return true;
            }
            AtomicLong rejectCount = timeSlot.getGradualRejectNum();
            if (rejectCount.incrementAndGet() <= rejectTraffic * n) {
                resumeCount.decrementAndGet();
                LOGGER.debug("{} current time window {}, reject traffic {}, resume traffic factor {}, reject count {}, reject current request",
                             resourceStat.getResourceId(), currentTimeWindow, rejectTraffic, n, rejectCount.get());
                return false;
            }
            rejectCount.decrementAndGet();
            resumeTrafficFactor.incrementAndGet();
            LOGGER.debug("{} current time window {}, resume traffic {}, reject traffic {}, resume traffic factor {}, resume count {}, reject count {}, resume current request",
                         resourceStat.getResourceId(), currentTimeWindow, resumeTraffic, rejectTraffic, n, resumeCount.get(), rejectCount.get());
            return true;
        }

        @Override
        public String toString() {
            return "GRTWC{resumeTraffic=" + resumeTraffic + ",rejectTraffic=" + rejectTraffic + '}';
        }
    }

    public static final String DETECT_REQUEST = "detectReq@";


    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isDeleted            = false;

    public Type    type;

    public boolean serviceDefaultEnable = false;

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

    public final AtomicReference<State> stateRef        = new AtomicReference<>(State.CLOSED);

    public       long                   stateStartTime;

    public CircuitBreaker() {
    }

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
            this.type = Type.SERVICE_DEFAULT;
            this.service = ResourceIdUtils.SERVICE_DEFAULT;
            if (enable == 1) {
                this.serviceDefaultEnable = true;
            }
        } else if (type == 2) {
            this.type = Type.SERVICE;
        } else {
            this.type = Type.PATH;
        }

        this.id = id;
        if (this.type != Type.SERVICE_DEFAULT) {
            this.service = service;
        }
        if (StringUtils.isNotBlank(path)) {
            this.path = path;
        }
        resource = ResourceIdUtils.buildResourceId(null, null, null, this.service, this.path);

        if (strategy == 1) {
            breakStrategy = BreakStrategy.ERRORS_RATIO;
            errorRatioThreshold = ratioThreshold;
        } else {
            breakStrategy = BreakStrategy.TOTAL_ERRORS;
            totalErrorThreshold = exceptionCount;
        }
        minRequests     = minRequestCount;
        monitorDuration = statInterval * 1000;
        breakDuration   = timeWindow * 1000;

        if (recoveryStrategy == 1) {
            resumeStrategy = ResumeStrategy.DETECTIVE;
        } else if (recoveryStrategy == 2) {
            resumeStrategy = ResumeStrategy.GRADUAL;
            resumeDuration = recoveryTimeWindow * 1000;
            initGradualResumeTimeWindowContext();
        } else {
            resumeStrategy = ResumeStrategy.IMMEDIATE;
        }

        this.responseContentType = responseContentType;
        this.responseContent     = responseContent;

        stateStartTime = currentTimeWindow();
    }

    private static long currentTimeWindow() {
        return timeWindow(System.currentTimeMillis());
    }

    private static long timeWindow(long timeMills) {
        return timeMills / 1000 * 1000;
    }

    public void initGradualResumeTimeWindowContext() {
        BigDecimal totalTraffic = new BigDecimal(100);
        int resumeDurationSecs = this.resumeDuration / 1000;
        BigDecimal duration = new BigDecimal(resumeDurationSecs);
        initialResumeTraffic = totalTraffic.divide(duration, 0, RoundingMode.HALF_UP).intValue();
        if (initialResumeTraffic == 0) {
            initialResumeTraffic = 1;
        }

        gradualResumeTimeWindowContexts = new ArrayList<>(resumeDurationSecs);
        for (int i = 1; i <= resumeDurationSecs; i++) {
            int resumeTraffic = initialResumeTraffic * i;
            GradualResumeTimeWindowContext ctx = new GradualResumeTimeWindowContext(resumeTraffic);
            gradualResumeTimeWindowContexts.add(ctx);
        }
        LOGGER.info("{} gradualResumeTimeWindowContexts: {}", resource, gradualResumeTimeWindowContexts);
    }

    private boolean isResumeTraffic(long currentTimeWindow, FlowStat flowStat) {
        long nThSecond = getStateDuration(currentTimeWindow);
        GradualResumeTimeWindowContext ctx = gradualResumeTimeWindowContexts.get((int) nThSecond);
        ResourceStat resourceStat = flowStat.getResourceStat(resource);
        return ctx.permit(resourceStat, currentTimeWindow);
    }

    private long getStateDuration(long currentTimeWindow) {
        return currentTimeWindow - stateStartTime;
    }

    public void correctState(long currentTimeWindow, FlowStat flowStat) {
        State s = stateRef.get();
        long stateDuration = getStateDuration(currentTimeWindow);

        if (s == State.CLOSED && stateDuration > monitorDuration) {
            LOGGER.debug("current time window {}, {} last {} second in {} large than monitor duration {}, correct to CLOSED state",
                         currentTimeWindow, resource, stateDuration, stateRef.get(), monitorDuration);
            transit(s, State.CLOSED, currentTimeWindow, flowStat);

        } else if (s == State.OPEN && stateDuration > breakDuration) {
            LOGGER.debug("current time window {}, {} last {} second in {} large than break duration {}, correct to CLOSED state",
                         currentTimeWindow, resource, stateDuration, stateRef.get(), breakDuration);
            transit(s, State.CLOSED, currentTimeWindow, flowStat);

        } else if (s == State.RESUME_GRADUALLY && stateDuration > resumeDuration) {
            LOGGER.debug("current time window {}, {} last {} second in {} large than resume duration {}, correct to CLOSED state",
                         currentTimeWindow, resource, stateDuration, stateRef.get(), resumeDuration);
            transit(s, State.CLOSED, currentTimeWindow, flowStat);
        }
    }

    public void correctCircuitBreakerStateAsError(long currentTimeWindow, FlowStat flowStat) {
        if (stateRef.get() == State.CLOSED) {
            long endTimeWindow = currentTimeWindow + 1000;
            // TimeWindowStat timeWindowStat = flowStat.getTimeWindowStat(resource, endTimeWindow - monitorDuration, endTimeWindow);
            TimeWindowStat timeWindowStat = flowStat.getTimeWindowStat(resource, stateStartTime, endTimeWindow);
            long reqCount = timeWindowStat.getCompReqs();
            long errCount = timeWindowStat.getErrors();

            if (breakStrategy == BreakStrategy.TOTAL_ERRORS && reqCount >= minRequests && errCount >= totalErrorThreshold) {
                LOGGER.debug("{} current time window {} request count {} >= min requests {} error count {} >= total error threshold {}, correct to OPEN state as error",
                             resource, currentTimeWindow, reqCount, minRequests, errCount, totalErrorThreshold);
                transit(State.CLOSED, State.OPEN, currentTimeWindow, flowStat);
            } else if (breakStrategy == BreakStrategy.ERRORS_RATIO && reqCount >= minRequests) {
                BigDecimal errors   = new BigDecimal(errCount);
                BigDecimal requests = new BigDecimal(reqCount);
                float p = errors.divide(requests, 2, RoundingMode.HALF_UP).floatValue();
                if (p - errorRatioThreshold >= 0) {
                    LOGGER.debug("{} current time window {} request count {} >= min requests {} error ratio {} >= error ratio threshold {}, correct to OPEN state as error",
                                 resource, currentTimeWindow, reqCount, minRequests, p, errorRatioThreshold);
                    transit(State.CLOSED, State.OPEN, currentTimeWindow, flowStat);
                }
            }
        }
    }

    public boolean transit(State current, State target, long currentTimeWindow, FlowStat flowStat) {
        if (stateRef.compareAndSet(current, target)) {
            stateStartTime = currentTimeWindow;
            ResourceStat resourceStat = flowStat.getResourceStat(resource);
            AtomicLong circuitBreakNum = resourceStat.getTimeSlot(currentTimeWindow).getCircuitBreakNum();
            circuitBreakNum.set(0);
            resourceStat.updateCircuitBreakState(currentTimeWindow, current, target);
            LOGGER.debug("transit {} current time window {} from {} which start at {} to {}", resource, currentTimeWindow, current, stateStartTime, target);
            return true;
        }
        return false;
    }

    public boolean permit(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat) {
        correctState(currentTimeWindow, flowStat);
        if (stateRef.get() == State.CLOSED) {
            return permitCallInClosedState(currentTimeWindow, flowStat);
        }
        if (stateRef.get() == State.OPEN) {
            return permitCallInOpenState(exchange, currentTimeWindow, flowStat);
        }
        if (stateRef.get() == State.RESUME_DETECTIVE) {
            flowStat.getResourceStat(resource).incrCircuitBreakNum(currentTimeWindow);
            return false;
        }
        if (stateRef.get() == State.RESUME_GRADUALLY) {
            return permitCallInResumeGraduallyState(currentTimeWindow, flowStat);
        }
        return true;
    }

    private boolean permitCallInClosedState(long currentTimeWindow, FlowStat flowStat) {

        long endTimeWindow = currentTimeWindow + 1000;
        // TimeWindowStat timeWindowStat = flowStat.getTimeWindowStat(resource, endTimeWindow - monitorDuration, endTimeWindow);
        TimeWindowStat timeWindowStat = flowStat.getTimeWindowStat(resource, stateStartTime, endTimeWindow);
        long reqCount = timeWindowStat.getCompReqs();
        long errCount = timeWindowStat.getErrors();

        if (breakStrategy == BreakStrategy.TOTAL_ERRORS && reqCount >= minRequests && errCount >= totalErrorThreshold) {
            LOGGER.debug("{} current time window {} request count {} >= min requests {} error count {} >= total error threshold {}",
                         resource, currentTimeWindow, reqCount, minRequests, errCount, totalErrorThreshold);
            transit(State.CLOSED, State.OPEN, currentTimeWindow, flowStat);
            flowStat.getResourceStat(resource).incrCircuitBreakNum(currentTimeWindow);
            return false;
        }
        if (breakStrategy == BreakStrategy.ERRORS_RATIO && reqCount >= minRequests) {
            BigDecimal errors   = new BigDecimal(errCount);
            BigDecimal requests = new BigDecimal(reqCount);
            float p = errors.divide(requests, 2, RoundingMode.HALF_UP).floatValue();
            if (p - errorRatioThreshold >= 0) {
                LOGGER.debug("{} current time window {} request count {} >= min requests {} error ratio {} >= error ratio threshold {}",
                             resource, currentTimeWindow, reqCount, minRequests, p, errorRatioThreshold);
                transit(State.CLOSED, State.OPEN, currentTimeWindow, flowStat);
                flowStat.getResourceStat(resource).incrCircuitBreakNum(currentTimeWindow);
                return false;
            }
        }

        LOGGER.debug("{} current time window {} in {} which start at {}, permit current request", resource, currentTimeWindow, stateRef.get(), stateStartTime);

        return true;
    }

    private boolean permitCallInOpenState(ServerWebExchange exchange, long currentTimeWindow, FlowStat flowStat) {
        long stateDuration = getStateDuration(currentTimeWindow);
        if (stateDuration > breakDuration) {
            if (resumeStrategy == ResumeStrategy.IMMEDIATE) {
                LOGGER.debug("current time window {}, {} last {} second in {} large than break duration {}, resume immediately",
                             currentTimeWindow, resource, stateDuration, stateRef.get(), breakDuration);
                transit(State.OPEN, State.CLOSED, currentTimeWindow, flowStat);
                return true;
            }
            if (resumeStrategy == ResumeStrategy.DETECTIVE) {
                LOGGER.debug("current time window {}, {} last {} second in {} large than break duration {}, resume detective",
                             currentTimeWindow, resource, stateDuration, stateRef.get(), breakDuration);
                if (transit(State.OPEN, State.RESUME_DETECTIVE, currentTimeWindow, flowStat)) {
                    exchange.getAttributes().put(DETECT_REQUEST, this);
                    return true;
                }
                flowStat.getResourceStat(resource).incrCircuitBreakNum(currentTimeWindow);
                return false;
            }
            if (resumeStrategy == ResumeStrategy.GRADUAL) {
                LOGGER.debug("current time window {}, {} last {} second in {} large than break duration {}, resume gradual",
                             currentTimeWindow, resource, stateDuration, stateRef.get(), breakDuration);
                transit(State.OPEN, State.RESUME_GRADUALLY, currentTimeWindow, flowStat);
                return isResumeTraffic(currentTimeWindow, flowStat);
            }
        }

        flowStat.getResourceStat(resource).incrCircuitBreakNum(currentTimeWindow);
        LOGGER.debug("{} current time window {} in {} which start at {}, reject current request", resource, currentTimeWindow, stateRef.get(), stateStartTime);
        return false;
    }

    private boolean permitCallInResumeGraduallyState(long currentTimeWindow, FlowStat flowStat) {
        long stateDuration = getStateDuration(currentTimeWindow);
        if (stateDuration > resumeDuration) {
            LOGGER.debug("current time window {}, {} last {} second in {} large than resume duration {}, resume immediately",
                         currentTimeWindow, resource, stateDuration, stateRef.get(), resumeDuration);
            transit(State.RESUME_GRADUALLY, State.CLOSED, currentTimeWindow, flowStat);
            return true;
        }
        return isResumeTraffic(currentTimeWindow, flowStat);
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}

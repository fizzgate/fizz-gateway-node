//package we.log;
//
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import ch.qos.logback.core.AppenderBase;
//import ch.qos.logback.core.Layout;
//import ch.qos.logback.core.LogbackException;
//import lombok.Getter;
//import lombok.Setter;
//import we.FizzAppContext;
//import we.flume.clients.log4j2appender.LogService;
//import we.util.NetworkUtils;
//
//import static we.log.LogSendAppender.*;
//
///**
// * log send appender with logback
// *
// * @author huahuang
// */
//public class LogSendAppenderWithLogback extends AppenderBase<ILoggingEvent> {
//
//    //负责将日志事件转换为字符串，需Getter和Setter方法
//    @Getter
//    @Setter
//    private Layout<ILoggingEvent> layout;
//
//    @Override
//    protected void append(ILoggingEvent event) {
//        try {
//            if (logEnabled != null && !logEnabled) {
//                return;
//            }
//
//            if (logEnabled == null && FizzAppContext.appContext == null && logSendService == null) {
//                // local cache
//                logSends[logSendIndex.getAndIncrement() % logSends.length] = new LogSend(
//                        this.getBizId(event.getArgumentArray()), NetworkUtils.getServerIp(), event.getLevel().levelInt,
//                        event.getTimeStamp(), this.getLayout().doLayout(event));
//                return;
//            }
//
//            if (logEnabled == null && logSendService == null) {
//                // no legal logSendService, discard the local cache
//                logEnabled = Boolean.FALSE;
//                logSends = null;
//                return;
//            }
//
//            if (logEnabled == null) {
//                logEnabled = Boolean.TRUE;
//
//                LogSend[] logSends;
//                synchronized (LogSendAppender.class) {
//                    logSends = LogSendAppender.logSends;
//                    LogSendAppender.logSends = null;
//                }
//
//                // logSendService is ready, send the local cache
//                if (logSends != null) {
//                    int size = Math.min(logSendIndex.get(), logSends.length);
//                    for (int i = 0; i < size; i++) {
//                        logSendService.send(logSends[i]);
//                    }
//                }
//            }
//
//            LogSend logSend = new LogSend(this.getBizId(event.getArgumentArray()), NetworkUtils.getServerIp(),
//                    event.getLevel().levelInt, event.getTimeStamp(), this.getLayout().doLayout(event));
//            logSendService.send(logSend);
//        } catch (Exception ex) {
//            throw new LogbackException(event.getFormattedMessage(), ex);
//        }
//    }
//
//    private String getBizId(Object[] parameters) {
//        Object bizId = LogService.getBizId();
//        if (parameters != null) {
//            for (int i = parameters.length - 1; i > -1; --i) {
//                Object p = parameters[i];
//                if (p == LogService.BIZ_ID) {
//                    if (i != parameters.length - 1) {
//                        bizId = parameters[i + 1];
//                    }
//                    break;
//                }
//            }
//        }
//        if (bizId == null) {
//            return "";
//        }
//        return bizId.toString();
//    }
//
//}

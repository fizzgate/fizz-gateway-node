package we.log;

import java.util.concurrent.atomic.AtomicInteger;

public class LogSendAppender {
    public static volatile LogSendService logSendService;
    public static volatile Boolean logEnabled;
    public static volatile LogSend[] logSends = new LogSend[1000];
    public static volatile AtomicInteger logSendIndex = new AtomicInteger(0);
}

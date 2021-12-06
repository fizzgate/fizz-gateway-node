package we.dedicatedline.proxy.codec;

import org.apache.yetus.audience.InterfaceStability;

import javax.annotation.Nullable;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzTcpTextMessage extends FizzSocketTextMessage {

    public static final int MAX_LENGTH             = 1024 * 1024;
    public static final int LENGTH_FIELD_LENGTH    = 4;
    public static final int LENGTH_FIELD_OFFSET    = 85;
    public static final int LENGTH_ADJUSTMENT      = 0;
    public static final int INITIAL_BYTES_TO_STRIP = 0;

    private int length;

    public FizzTcpTextMessage() {
    }

    public FizzTcpTextMessage(@Nullable Long id, @Nullable Integer orderNumber, Integer type, byte[] dedicatedLine, long timestamp, byte[] sign, int length, byte[] content) {
        super(id, orderNumber, type, dedicatedLine, timestamp, sign, content);
        this.length = length;
    }

    public FizzTcpTextMessage(@Nullable Long id, @Nullable Integer orderNumber, Integer type, String dedicatedLine, long timestamp, String sign, int length, String content) {
        this(id, orderNumber, type, dedicatedLine.getBytes(), timestamp, sign.getBytes(), length, content.getBytes());
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String toString() {
        return "id="             + getId() + ',' +
               "orderNumber="    + getOrderNumber() + ',' +
               "type="           + getType() + ',' +
               "dedicatedLine="  + getDedicatedLineStr() + ',' +
               "timestamp="      + getTimestamp() + ',' +
               "sign="           + getSignStr() + ',' +
               "length="         + length + ',' +
               "content="        + getContentStr();
    }
}

package we.dedicatedline.proxy.codec;

import org.apache.yetus.audience.InterfaceStability;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzTcpMessage extends FizzSocketMessage {

    public static final int MAX_LENGTH             = 1024 * 1024;
    public static final int LENGTH_FIELD_LENGTH    = 4;
    public static final int LENGTH_FIELD_OFFSET    = 73;
    public static final int LENGTH_ADJUSTMENT      = 0;
    public static final int INITIAL_BYTES_TO_STRIP = 0;

    private int length;

    public FizzTcpMessage() {
    }

    public FizzTcpMessage(int type, byte[] dedicatedLine, long timestamp, byte[] sign, int length, byte[] content) {
        super(type, dedicatedLine, timestamp, sign, content);
        this.length = length;
    }

    public FizzTcpMessage(int type, String dedicatedLine, long timestamp, String sign, int length, String content) {
        super(type, dedicatedLine, timestamp, sign, content);
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String toString() {
        return "type="           + getType() + ',' +
               "dedicatedLine="  + getDedicatedLineStr() + ',' +
               "timestamp="      + getTimestamp() + ',' +
               "sign="           + getSignStr() + ',' +
               "length="         + length + ',' +
               "content="        + getContentStr();
    }
}

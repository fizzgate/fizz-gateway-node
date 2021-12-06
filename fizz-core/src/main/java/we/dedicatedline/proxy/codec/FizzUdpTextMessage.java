package we.dedicatedline.proxy.codec;

import org.apache.yetus.audience.InterfaceStability;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzUdpTextMessage extends FizzSocketTextMessage {

    public static int MAX_LENGTH         = 4096;

    public static int CONTENT_MAX_LENGTH = MAX_LENGTH - METADATA_LENGTH;

    public FizzUdpTextMessage() {
    }

    public FizzUdpTextMessage(int type, byte[] dedicatedLine, long timestamp, byte[] sign, byte[] content) {
        super(0L, 0, type, dedicatedLine, timestamp, sign, content);
    }

    public FizzUdpTextMessage(int type, String dedicatedLine, long timestamp, String sign, String content) {
        this(type, dedicatedLine.getBytes(), timestamp, sign.getBytes(), content.getBytes());
    }
}

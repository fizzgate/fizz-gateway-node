package we.dedicatedline.proxy.codec;

import org.apache.yetus.audience.InterfaceStability;

/**
 * @author hongqiaowei
 */

@InterfaceStability.Unstable
public class FizzSocketMessage {

    public static int TYPE_LENGTH           = 1;
    public static int DEDICATED_LINE_LENGTH = 32;
    public static int TIMESTAMP_LENGTH      = 8;
    public static int SIGN_LENGTH           = 32;
    public static int METADATA_LENGTH       = TYPE_LENGTH + DEDICATED_LINE_LENGTH + TIMESTAMP_LENGTH + SIGN_LENGTH;

    private byte   type              = 1;

    private byte[] dedicatedLine;

    private String dedicatedLineStr;

    private long   timestamp;

    private byte[] sign;

    private String signStr;

    private byte[] content;

    private String contentStr;

    public FizzSocketMessage() {
    }

    public FizzSocketMessage(int type, byte[] dedicatedLine, long timestamp, byte[] sign, byte[] content) {
        this.type          = (byte) type;
        this.dedicatedLine = dedicatedLine;
        this.timestamp     = timestamp;
        this.sign          = sign;
        this.content       = content;
    }

    public FizzSocketMessage(int type, String dedicatedLine, long timestamp, String sign, String content) {
        this.type          = (byte) type;
        this.dedicatedLine = dedicatedLine.getBytes();
        this.timestamp     = timestamp;
        this.sign          = sign.getBytes();
        this.content       = content.getBytes();
    }

    public byte getType() {
        return type;
    }

    public void setType(int type) {
        this.type = (byte) type;
    }

    public byte[] getDedicatedLine() {
        return dedicatedLine;
    }

    public String getDedicatedLineStr() {
        if (dedicatedLineStr == null) {
            dedicatedLineStr = new String(dedicatedLine);
        }
        return dedicatedLineStr;
    }

    public void setDedicatedLine(byte[] dedicatedLine) {
        this.dedicatedLine = dedicatedLine;
        dedicatedLineStr = null;
    }

    public void setDedicatedLine(String dedicatedLine) {
        setDedicatedLine(dedicatedLine.getBytes());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getSign() {
        return sign;
    }

    public String getSignStr() {
        if (signStr == null) {
            signStr = new String(sign);
        }
        return signStr;
    }

    public void setSign(byte[] sign) {
        this.sign = sign;
        signStr = null;
    }

    public void setSign(String sign) {
        setSign(sign.getBytes());
    }

    public byte[] getContent() {
        return content;
    }

    public String getContentStr() {
        if (contentStr == null) {
            contentStr = new String(content);
        }
        return contentStr;
    }

    public void setContent(byte[] content) {
        this.content = content;
        contentStr = null;
    }

    public void setContent(String content) {
        setContent(content.getBytes());
    }

    public String toString() {
        return "type="          + type + ',' +
               "dedicatedLine=" + getDedicatedLineStr() + ',' +
               "timestamp="     + timestamp + ',' +
               "sign="          + getSignStr() + ',' +
               "content="       + getContentStr();
    }

    public static void inv(byte[] bytes) {
        inv(0, bytes);
    }

    public static void inv(int start, byte[] bytes) {
        for (int i = start; i < bytes.length; i = i + 2) {
            bytes[i] ^= (1);
        }
    }
}

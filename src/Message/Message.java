package Message;

public class Message {
    private final String sender;
    private final String[] receivers;
    private final String body;
    private final long length;

    private final MessageTypes type;

    /**
     * This class hold a message and it's parameters.
     *
     * @param sender
     * @param receivers
     * @param body
     * @param length
     * @param type
     */
    public Message(String sender, String[] receivers, String body, long length, MessageTypes type) {
        this.sender = sender;
        this.receivers = receivers;
        this.body = body;
        this.length = length;
        this.type = type;
    }

    /*
    list of getters
     */

    public String getSender() {
        return sender;
    }

    public String[] getReceivers() {
        return receivers;
    }

    public String getBody() {
        return body;
    }

    /**
     * It returns a substring of body based on the legth field.
     *
     * @return
     */
    public String getSafeBody() {
        return body.substring(0, (int) Math.min(length, body.length()));
    }

    public long getLength() {
        return length;
    }

    public MessageTypes getType() {
        return type;
    }

}

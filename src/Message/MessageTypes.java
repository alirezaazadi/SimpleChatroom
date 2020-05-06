package Message;

/**
 * List of Message Types
 */
public enum MessageTypes {
    PUBLIC, //Use in both side
    PRIVATE, //Use in both sides
    COMMAND, //Only in server side
    PUBLIC_DATA, // Till now, no where
    PRIVATE_DATA,
    NONE, // Only in server side
    RESPONSE,// Only in client side
    BROADCAST, //Only in client side
    DL,
}

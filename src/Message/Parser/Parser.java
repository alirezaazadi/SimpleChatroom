package Message.Parser;

import Message.Message;

public interface Parser {

    String BODY_SEPARATOR = "\n\r";
    String RECEIVERS_SEPARATOR = " to ";
    String SENDER_SEPARATOR = " from ";
    String RECEIVERS_LIST_SEPARATOR = ",";
    String USERNAMES_SEPARATOR = ",";
    String TYPE_SEPARATOR = ",";

    int CHAR_SIZE = 2;

    Message privateMessageParser(String message) throws Exception;

    Message publicMessageParser(String message) throws Exception;

    Message publicDataMessageParser(String message) throws Exception;

    Message privateDataMessageParser(String message) throws Exception;
}

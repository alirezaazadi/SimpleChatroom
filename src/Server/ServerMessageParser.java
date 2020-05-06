package Server;

import Message.Message;
import Message.MessageTypes;
import Message.Parser.Exceptions.*;
import Message.Parser.Parser;

import static Message.MessageTypes.*;
import static Message.StaticMessages.*;

public class ServerMessageParser implements Parser {

    private final String userName;

    /**
     * Constructor
     *
     * @param userName
     */
    public ServerMessageParser(String userName) {
        this.userName = userName;
    }

    public static String[] convertStringToList(String string) {
        String[] receiversList = string.split(USERNAMES_SEPARATOR);

        for (int i = 0; i < receiversList.length; i++) {
            receiversList[i] = receiversList[i].substring(receiversList[i].indexOf("<") + 1,
                    receiversList[i].lastIndexOf(">"));
        }
        return receiversList;
    }

    /**
     * In ServerWorker, we call this function and
     * each raw message will paa to this function for parsing based on the message.
     *
     * @param message
     * @return
     * @throws Exception
     */
    public Message parse(String message) throws MessageIsNotValid, MessageTypeIsNotValid,
            MessageLengthDoesNotExists,
            MessageIsTooLong, ReceiverDoesNotExists {

        if (message.equals(GET_USERS_LIST))
            return new Message(userName, new String[]{"Server"}, message, message.length(),
                    MessageTypes.COMMAND);

        if (message.equals(SIGN_OUT))
            return new Message(userName, new String[]{"Server"}, message, message.length(),
                    MessageTypes.COMMAND);

        else {
            String[] messageParts = message.split(TYPE_SEPARATOR, 2);

            if (messageParts.length != 2)
                throw new MessageIsNotValid(message);

            if (messageParts[0].equals(PUBLIC_MESSAGE))
                return publicMessageParser(messageParts[1]);

            if (messageParts[0].equals(PRIVATE_MESSAGE))
                return privateMessageParser(messageParts[1]);

            if (messageParts[0].equals(PRIVATE_FILE_MESSAGE))
                return privateDataMessageParser(messageParts[1]);

            if (messageParts[0].equals(PUBLIC_FILE_MESSAGE))
                return publicDataMessageParser(messageParts[1]);

            if (messageParts[0].equals(DOWNLOAD))
                return downloadRequestParser(messageParts[1]);

            throw new MessageTypeIsNotValid(message);
        }
    }

    /**
     * Parse download request messages . when the client want to
     * call the server to start it's  file upload service.
     *
     * @param message
     * @return
     */
    private Message downloadRequestParser(String message) {
        String fileName = message.substring(0, message.lastIndexOf(","));
        String from = message.substring(message.lastIndexOf("<") + 1, message.lastIndexOf(">"));
        return new Message(userName, new String[]{from}, fileName, fileName.length(), DL);
    }

    /**
     * Parse public upload request message (from client).
     *
     * @param message
     * @return
     * @throws MessageIsNotValid
     */
    @Override
    public Message publicDataMessageParser(String message) throws MessageIsNotValid {
        String[] parts = message.split(USERNAMES_SEPARATOR);

        if (parts.length != 2)
            throw new MessageIsNotValid(message);

        String fileName = parts[0].substring(parts[0].indexOf("<") + 1, parts[0].indexOf(">"));
        String length = parts[1].substring(parts[1].indexOf("<") + 1, parts[1].indexOf(">"));

        return new Message(userName, new String[]{"All"}, fileName, Long.parseLong(length), PUBLIC_DATA);
    }

    /**
     * Parse private upload request message (from client).
     *
     * @param message
     * @return
     * @throws MessageIsNotValid
     */
    @Override
    public Message privateDataMessageParser(String message) throws MessageIsNotValid {
        String[] parts = message.split(USERNAMES_SEPARATOR);

        String fileName = parts[0].substring(parts[0].indexOf("<") + 1, parts[0].indexOf(">"));
        String length = parts[1].substring(parts[1].indexOf("<") + 1, parts[1].indexOf(">"));
        String rec = parts[2];

        return new Message(userName, convertStringToList(rec), fileName, Long.parseLong(length), PRIVATE_DATA);
    }

    /**
     * Private messages from user pars in here.
     *
     * @param message
     * @return
     * @throws Exception
     */
    @Override
    public Message privateMessageParser(String message) throws MessageLengthDoesNotExists,
            ReceiverDoesNotExists, MessageIsTooLong {
        String length;
        int messageLength;

        //Take th length of message and convert it to int.
        try {
            length = message.substring(message.indexOf("<") + 1, message.indexOf(">"));
        } catch (IndexOutOfBoundsException e) {
            throw new MessageLengthDoesNotExists(message);
        }


        try {
            messageLength = Integer.parseInt(length);
        } catch (NumberFormatException e) {
            throw new MessageIsTooLong(length);
        }

        // Try to get the receivers of message from the raw message
        String[] parts = message.split(RECEIVERS_SEPARATOR, 2);
        if (parts.length != 2)
            throw new ReceiverDoesNotExists(message);

        String rec = parts[1].substring(0, parts[1].indexOf(BODY_SEPARATOR));


        // Try to get the body of message.
        String[] endSection = parts[1].split(BODY_SEPARATOR, 2);

        String body;

        if (endSection.length == 2) {

            try {
                body = endSection[1].substring(endSection[1].indexOf("<") + 1, endSection[1].lastIndexOf(">"));
            } catch (IndexOutOfBoundsException e) {
                return new Message(userName, new String[]{"None"}, "", 0, MessageTypes.NONE);
            }
            return new Message(userName, convertStringToList(rec), body, messageLength, PRIVATE);
        } else
            return new Message(userName, new String[]{"None"}, "", 0, MessageTypes.NONE);

    }

    /**
     * Public messages pars in here.
     *
     * @param message
     * @return
     * @throws Exception
     */
    @Override
    public Message publicMessageParser(String message) throws MessageLengthDoesNotExists, MessageIsTooLong {
        String length;
        int messageLength;

        try {
            length = message.substring(message.indexOf("<") + 1, message.indexOf(">"));
        } catch (IndexOutOfBoundsException e) {
            throw new MessageLengthDoesNotExists(message);
        }

        try {
            messageLength = Integer.parseInt(length);
        } catch (NumberFormatException e) {
            throw new MessageIsTooLong(length);
        }

        String[] endSection = message.split(BODY_SEPARATOR, 2);

        if (endSection.length == 2) {
            String body = "";
            try {
                body = endSection[1].substring(endSection[1].indexOf("<") + 1, endSection[1].lastIndexOf(">"));
            } catch (IndexOutOfBoundsException e) {
                return new Message(userName, new String[]{"None"}, "", 0, MessageTypes.NONE);
            }

            return new Message(userName, new String[]{"All"}, body, messageLength, MessageTypes.PUBLIC);
        } else
            return new Message(userName, new String[]{"None"}, "", 0, MessageTypes.NONE);
    }
}
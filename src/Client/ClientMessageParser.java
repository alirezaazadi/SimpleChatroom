package Client;

import Message.Message;
import Message.Parser.Exceptions.*;
import Message.Parser.Parser;

import static Message.MessageTypes.*;
import static Message.StaticMessages.*;
import static Message.Status.Status.LIST_PASSED;

/**
 * It's implements Parser interface and pars messages from server
 * and return a parsed message as a Message
 */
public class ClientMessageParser implements Parser {

    private final String userName;

    /**
     * Constructor
     *
     * @param userName
     */
    public ClientMessageParser(String userName) {
        this.userName = userName;
    }

    /**
     * In ClientMessageReader, we call this function and
     * each raw message will paa to this function for parsing based on the message.
     *
     * @param message
     * @return
     * @throws Exception
     */
    public Message parse(String message) throws Exception {
        String[] messageParts = message.split(TYPE_SEPARATOR, 2);
        if (messageParts[0].equals(PUBLIC_MESSAGE))
            return publicMessageParser(messageParts[1]);

        else if (messageParts[0].equals(PRIVATE_MESSAGE))
            return privateMessageParser(messageParts[1]);

        else if (messageParts[0].equals(SERVER_MESSAGE))
            return serverMessageParser(messageParts[1]);

        else if (messageParts[0].equals(PUBLIC_FILE_MESSAGE))
            return publicDataMessageParser(messageParts[1]);
        else if (messageParts[0].equals(PRIVATE_FILE_MESSAGE))
            return privateDataMessageParser(messageParts[1]);
        else
            return new Message("NONE", new String[]{userName}, "", 0, NONE);
    }

    /**
     * Parsing received file message (Private)
     *
     * @param message
     * @return
     * @throws MessageIsNotValid
     * @throws ReceiverDoesNotExists
     */
    @Override
    public Message privateDataMessageParser(String message) throws MessageIsNotValid, ReceiverDoesNotExists {

        String[] all = message.split(TYPE_SEPARATOR);

        if (all.length != 4)
            throw new MessageIsNotValid(message);

        String sender = all[0].substring(all[0].indexOf("<") + 1, all[0].lastIndexOf(">"));

        String fileName = all[1].substring(all[1].indexOf("<") + 1, all[1].lastIndexOf(">"));

        long fileSize = Long.parseLong(all[2].substring(all[2].indexOf("<") + 1, all[2].lastIndexOf(">")));

        String rec = all[3];

        return new Message(sender, stringToList(rec), fileName, fileSize, PRIVATE_DATA);
    }

    /**
     * Parsing received file message (Private)
     *
     * @param message
     * @return
     * @throws MessageIsNotValid
     */
    @Override
    public Message publicDataMessageParser(String message) throws MessageIsNotValid {
        String[] all = message.split(TYPE_SEPARATOR);

        if (all.length != 3)
            throw new MessageIsNotValid(message);

        String sender = all[0].substring(all[0].indexOf("<") + 1, all[0].lastIndexOf(">"));

        String fileName = all[1].substring(all[1].indexOf("<") + 1, all[1].lastIndexOf(">"));

        long fileSize = Long.parseLong(all[2].substring(all[2].indexOf("<") + 1, all[2].lastIndexOf(">")));

        return new Message(sender, new String[]{userName}, fileName, fileSize, PUBLIC_DATA);
    }


    /**
     * Server messages (Responses) parse in here.
     *
     * @param message
     * @return
     */
    private Message serverMessageParser(String message) {
        if (message.startsWith(LIST_PASSED)) {
            String receiversString = message.substring(message.indexOf("<") + 1, message.indexOf(">"));
            String[] receivers = receiversString.split(USERNAMES_SEPARATOR);
            return new Message("Server", receivers, message,
                    message.length(), RESPONSE);
        } else {
            return new Message("Server", new String[]{}, message, message.length(), RESPONSE);
        }
    }

    /**
     * Private messages from user pars in here.
     *
     * @param message
     * @return
     * @throws Exception
     */
    @Override
    public Message privateMessageParser(String message) throws Exception {
        String length, sender;

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
            throw new MessageIsTooLong("Message is to long!");
        }

        // Try to get the sender of message from the raw message
        String[] messageParts = message.split(SENDER_SEPARATOR, 2);

        if (messageParts.length != 2)
            throw new SenderDoesNotExists("Sender does not exists!");
        try {
            sender = messageParts[1].substring(messageParts[1].indexOf("<") + 1, messageParts[1].indexOf(">"));
        } catch (IndexOutOfBoundsException e) {
            throw new SenderDoesNotExists("Sender does not exists!");
        }


        // Try to get the body of message.
        String[] endSection = messageParts[1].split(BODY_SEPARATOR, 2);

        // Try to get the receivers of message from the raw message
        String rec = endSection[0].substring(endSection[0].indexOf(RECEIVERS_SEPARATOR) + RECEIVERS_SEPARATOR.length());

        String body = "";

        if (endSection.length == 2) {
            try {
                body = endSection[1].substring(endSection[1].indexOf("<") + 1, endSection[1].lastIndexOf(">"));
            } catch (IndexOutOfBoundsException e) {
                return new Message(sender, new String[]{userName}, "", 0, NONE);
            }
            return new Message(sender, stringToList(rec), body, messageLength, PRIVATE);
        } else
            return new Message(sender, new String[]{userName}, "", 0, NONE);
    }

    private String[] stringToList(String string) throws ReceiverDoesNotExists {
        String[] receiversNames = string.split(RECEIVERS_SEPARATOR);

        for (int i = 0; i < receiversNames.length; i++) {
            try {
                receiversNames[i] = receiversNames[i].substring(receiversNames[i].indexOf("<") + 1,
                        receiversNames[i].lastIndexOf(">"));
            } catch (IndexOutOfBoundsException e) {
                throw new ReceiverDoesNotExists(receiversNames[i]);
            }
        }
        return receiversNames;
    }

    /**
     * Public messages pars in here.
     *
     * @param message
     * @return
     * @throws Exception
     */
    @Override
    public Message publicMessageParser(String message) throws Exception {
        String length, sender;

        int messageLength;

        try {
            length = message.substring(message.indexOf("<") + 1, message.indexOf(">"));
        } catch (IndexOutOfBoundsException e) {
            throw new MessageLengthDoesNotExists(message);
        }

        try {
            messageLength = Integer.parseInt(length);
        } catch (NumberFormatException e) {
            throw new MessageIsTooLong("Message is to long!");
        }

        String[] messageParts = message.split(SENDER_SEPARATOR, 2);

        if (messageParts.length != 2)
            throw new SenderDoesNotExists("Sender does not exists!");
        try {
            sender = messageParts[1].substring(messageParts[1].indexOf("<") + 1, messageParts[1].indexOf(">"));
        } catch (IndexOutOfBoundsException e) {
            throw new SenderDoesNotExists("Sender does not exists!");
        }

        String[] endSection = messageParts[1].split(BODY_SEPARATOR, 2);

        if (endSection.length == 2) {
            String body = "";
            try {
                body = endSection[1].substring(endSection[1].indexOf("<") + 1, endSection[1].lastIndexOf(">"));
            } catch (IndexOutOfBoundsException e) {
                return new Message(sender, new String[]{"None"}, "", 0, NONE);
            }
            return new Message(sender, new String[]{userName}, body, messageLength, PUBLIC);
        } else
            return new Message(sender, new String[]{"None"}, "", 0, NONE);
    }
}

package Server;

import java.util.Set;

import static Message.Parser.Parser.*;
import static Message.StaticMessages.*;

/**
 * This class with it's static methods, responsible for building messages.
 */
public class ServerMessageBuilder {

    /**
     * Takes a username and build the welcome message. e.g:
     * "Hi Alireza, welcome to the chat room."
     *
     * @param userName
     * @return
     */
    public static String handshakeAcceptResponse(String userName) {
        return String.format("Hi %s, welcome to the chat room.", userName);
    }

    /**
     * Build user joining public message. e.g :
     * "Alireza join the chat room."
     *
     * @param userName
     * @return
     */
    public static String userJoiningInChatRoom(String userName) {
        return String.format("%s join the chat room.", userName);
    }

    /**
     * Build online users list message. e.g :
     * Here is the list of attendees:
     * <user_name1>,<user_name2>,<user_name3>,<user_name4>
     *
     * @param onlineUsersList
     * @return
     */
    public static String sendUsersList(Set<String> onlineUsersList) {
        StringBuilder list = new StringBuilder();
        for (String user : onlineUsersList)
            list.append(user).append(USERNAMES_SEPARATOR);
        return String.format("%s%s<%s>", GET_USER_LIST_RESPONSE, BODY_SEPARATOR,
                list.subSequence(0, list.length() - 1).toString());
    }

    /**
     * Build public message . e.g :
     * "Public message, length=<5> from <Alireza>\n\r<Hello>"
     *
     * @param body
     * @param length
     * @param sender
     * @return
     */
    public static String publicMessage(String body, long length, String sender) {
        return String.format("Public message, length=<%d> from <%s>%s" +
                "<%s>", length, sender, BODY_SEPARATOR, body);
    }

    /**
     * Build private message . e.g :
     * "Private message, length=<5> from <Alireza> to <A>,<B>\n\r<Hello>"
     *
     * @param safeBody
     * @param length
     * @param userName
     * @param receivers
     * @return
     */
    public static String privateMessage(String safeBody, long length, String userName, String[] receivers) {
        return String.format("Private message, length=<%d> from <%s> to %s%s<%s>",
                length, userName, convertListToString(receivers),
                BODY_SEPARATOR, safeBody);
    }

    /**
     * It's convert a list of string (users) to this format:
     * ["A","B"] -> "<A>,<B>"
     *
     * @param receivers
     * @return
     */
    public static String convertListToString(String[] receivers) {
        if (receivers.length == 0)
            return "<>";

        StringBuilder list = new StringBuilder();

        for (String receiver : receivers)
            list.append(String.format("<%s>%s", receiver.trim(), RECEIVERS_LIST_SEPARATOR));
        return list.substring(0, list.length() - RECEIVERS_LIST_SEPARATOR.length());
    }


    /**
     * Build public logout message. e.g:
     * "<Alireza> left the chat room."
     *
     * @param userName
     * @return
     */
    public static String logoutMessage(String userName) {
        return String.format("<%s> left the chat room.", userName);
    }

    /**
     * Build Server response message. e.g:
     * Server message,201,HANDSHAKE_ACCEPTED.
     *
     * @param status
     * @param response
     * @return
     */
    public static String responseBuilder(String status, String response) {
        return String.format("%s,%s,%s", SERVER_MESSAGE, status, response);
    }

    /**
     * Build file private upload message for client
     *
     * @param sender
     * @param fileName
     * @param fileSize
     * @param receivers
     * @return
     */
    public static String privateFileSendMessage(String sender, String fileName, long fileSize, String[] receivers) {
        return String.format("%s, from<%s>, name<%s>, length<%d>, %s", PRIVATE_FILE_MESSAGE, sender, fileName, fileSize,
                convertListToString(receivers));
    }

    /**
     * Build file public upload message for client
     *
     * @param sender
     * @param fileName
     * @param fileSize
     * @return
     */
    public static String publicFileSendMessage(String sender, String fileName, long fileSize) {
        return String.format("%s, from<%s>, name<%s>, length<%d>", PUBLIC_FILE_MESSAGE, sender, fileName, fileSize);
    }

}

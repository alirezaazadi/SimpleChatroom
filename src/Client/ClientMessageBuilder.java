package Client;

import static Message.Parser.Parser.*;
import static Message.StaticMessages.*;

/**
 * This class with it's static methods, responsible for building client side messages.
 */
public class ClientMessageBuilder {
    /**
     * Takes a message and length of it and make message with public message format. e.g:
     * "Public message, length=<5>\n\r<Hello>"
     *
     * @param body
     * @param length
     * @return
     */
    public static String publicMessage(String body, int length) {
        return String.format("Public message%s length=<%d>%s<%s>", TYPE_SEPARATOR, length, BODY_SEPARATOR, body);
    }

    /**
     * Takes a message , length of it and receivers list and try to make message with private message format. e.g:
     * "Private message, length=<5> to <Alireza>\n\r<Hello>"
     *
     * @param body
     * @param length
     * @return
     */
    public static String privateMessageBuilder(String body, int length, String... receivers) {
        return String.format("Private message%s length=<%d> to %s%s<%s>", TYPE_SEPARATOR, length,
                buildReceiversString(receivers), BODY_SEPARATOR, body);
    }

    /**
     * It builds the handshake message based on the username. e.g:
     * "Hello<Alireza>"
     *
     * @param userName
     * @return
     */
    public static String handshakeMessage(String userName) {
        return String.format("Hello<%s>", userName);
    }

    /**
     * It's convert a list of string (users) to this format:
     * ["A","B"] -> "<A>,<B>"
     *
     * @param receivers
     * @return
     */
    private static String buildReceiversString(String[] receivers) {
        if (receivers.length == 0)
            return "<>";
        StringBuilder list = new StringBuilder();

        for (String receiver : receivers)
            list.append(String.format("<%s>%s", receiver, RECEIVERS_LIST_SEPARATOR));
        return list.substring(0, list.length() - RECEIVERS_LIST_SEPARATOR.length());
    }

    /**
     * Build private file upload request message. e.g :
     * "PFile, name<Alireza>, length<4096>, <A>,<B>
     *
     * @param fileName
     * @param fileSize
     * @param receivers
     * @return
     */
    public static String privateFileSendMessage(String fileName, long fileSize, String[] receivers) {
        return String.format("%s, name<%s>, length<%d>, %s", PRIVATE_FILE_MESSAGE, fileName, fileSize,
                buildReceiversString(receivers));
    }

    /**
     * Build public file upload request message. e.g :
     * "PFile, name<Alireza>, length<4096>
     *
     * @param fileName
     * @param fileSize
     * @param receivers
     * @return
     */
    public static String publicFileSendMessage(String fileName, long fileSize) {
        return String.format("%s, name<%s>, length<%d>", PUBLIC_FILE_MESSAGE, fileName, fileSize);
    }

    /**
     * Build file download request message. e.g:
     * "Download,fileName,<source>"
     *
     * @param fileName
     * @return
     */
    public static String downloadRequest(String fileName, String source) {
        return String.format("%s,%s,<%s>", DOWNLOAD, fileName, source);
    }
}

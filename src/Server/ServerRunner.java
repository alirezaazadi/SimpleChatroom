package Server;

/**
 * Runs server :)
 */
public class ServerRunner {
    private static final int PORT = 15_000;
    private static final int CAPACITY = 100;

    public static void main(String[] args) {
        Server server;
        try {
            server = (args.length == 2) ? new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1])) :
                    new Server(PORT, CAPACITY);
        } catch (NumberFormatException e) {
            server = new Server(PORT, CAPACITY);
        }

        server.runServer();
    }
}

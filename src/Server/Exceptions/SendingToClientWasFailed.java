package Server.Exceptions;

public class SendingToClientWasFailed extends Exception {
    public SendingToClientWasFailed(String message) {
        super(message);
    }
}


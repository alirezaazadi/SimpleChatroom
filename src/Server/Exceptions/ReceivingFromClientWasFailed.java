package Server.Exceptions;

public class ReceivingFromClientWasFailed extends Exception {
    public ReceivingFromClientWasFailed(String message) {
        super(message);
    }
}

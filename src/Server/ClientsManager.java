package Server;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ClientsManager {
    private final ConcurrentHashMap<String, Client> userTable;

    /**
     * Constructor. It takes server capacity and create a
     * hashtable with this cap.
     *
     * @param userCount
     */
    ClientsManager(int userCount) {
        userTable = new ConcurrentHashMap<String, Client>(userCount);
    }

    /**
     * Validate username, it ony allows to have character in it's usrname.
     *
     * @param userName
     * @return
     */
    private static boolean validateUserName(String userName) {
        return (!userName.equals("") && (!userName.contains(",") && !userName.contains(":")
                && !userName.contains("<") && !userName.contains(">") && !userName.contains("=")
                && !userName.contains("\n") && !userName.contains("\r") && !userName.contains("\t")
                && !userName.contains(" "))) && userName.chars().allMatch(Character::isLetter)
                && !userName.equalsIgnoreCase("Server");
    }

    /**
     * Get a username and returns  it's client object.
     *
     * @param userName
     * @return
     */
    protected Client getUser(String userName) {
        Client client = userTable.get(userName);
        if (Optional.ofNullable(client).isPresent())
            return client;
        else
            return null;
    }

    /**
     * Takes a client object and it's username and map them.
     *
     * @param client
     * @param userName
     * @return
     */
    protected boolean addClient(Client client, String userName) {
        if (ClientsManager.validateUserName(userName)) {
            if (!userTable.containsKey(userName)) {
                client.setUserName(userName);
                userTable.put(userName, client);
                return true;
            } else
                return false;
        } else
            return false;
    }

    /**
     * Returns the set of online users/
     *
     * @return
     */
    protected Set<String> getUserNameList() {
        return userTable.keySet();
    }

    /**
     * It takes a username and remove it from the map.
     *
     * @param userName
     * @return
     */
    protected boolean removeClient(String userName) {
        return Optional.ofNullable(userTable.remove(userName)).isPresent();
    }
}

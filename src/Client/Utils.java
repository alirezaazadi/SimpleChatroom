package Client;

public class Utils {
    /**
     * Takes a path and based on the user's os,
     * change the path separators to os file separator.
     *
     * @param path
     * @return
     */
    public static String rebuildPath(String path) {
        if (path.contains("\\"))
            return path.replace("\\", java.nio.file.FileSystems.getDefault().getSeparator());
        else if (path.contains("/"))
            return path.replace("/", java.nio.file.FileSystems.getDefault().getSeparator());
        else
            return path;
    }

    /**
     * Convert a list to a String. e.g:
     * ["A","B"] -> "A,B"
     *
     * @param list
     * @return
     */
    public static String convertListToString(String... list) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            if (i == list.length - 1)
                stringBuilder.append(list[i]);
            else
                stringBuilder.append(list[i]).append(",");
        }
        return stringBuilder.toString();
    }
}

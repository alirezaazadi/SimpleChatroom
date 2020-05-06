package Server.FileSharing;

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

}

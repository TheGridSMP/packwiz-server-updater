package sleepy_evelyn.packwizsu.util;

public class PackTomlURLException extends Exception {
    private static final String EXCEPTION_START = "Failed to read the Packwiz pack.toml file. ";

    public PackTomlURLException(String message) {
        super(EXCEPTION_START + message);
    }
}
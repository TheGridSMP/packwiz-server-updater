package sleepy_evelyn.packwizsu.util;

import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public class JavaUtil {

    private static final String JAVA_BIN = "java" + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");

    @Nullable
    public static String getFullJavaPath() {
        Path path = Path.of(System.getProperty("java.home"), "bin", JAVA_BIN);

        if (Files.exists(path))
            return path.toAbsolutePath().toString();

        path = Path.of(System.getProperty("sun.boot.library.path") , JAVA_BIN);

        if (Files.exists(path))
            return path.toAbsolutePath().toString();

        path = Path.of(System.getProperty("java.library.path"), JAVA_BIN);

        if (Files.exists(path))
            return path.toAbsolutePath().toString();

        return null;
    }
}

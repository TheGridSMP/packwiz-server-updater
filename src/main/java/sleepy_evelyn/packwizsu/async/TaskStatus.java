package sleepy_evelyn.packwizsu.async;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TaskStatus {
    public static final TaskStatus UNKNOWN_FAIL = error("Command failed. Check the server console for errors.");
    public static final TaskStatus UPDATE_FINISHED = success("Packwiz has finished updating. Restart the server for changes to take effect.");
    public static final TaskStatus BOOTSTRAP_DOWNLOAD_FINISHED = neutral("Bootstrap downloaded successfully.");
    public static final TaskStatus PROCESS_INTERRUPTED = error("Process was interrupted. Check the server console for details.");
    public static final TaskStatus FILE_HANDLING_ERROR = error("Read/write process failed. Check the server console for details.");

    static TaskStatus error(String text) {
        return new TaskStatus(Text.literal(text).formatted(Formatting.RED));
    }

    public static TaskStatus success(String text) {
        return new TaskStatus(Text.literal(text).formatted(Formatting.GREEN));
    }

    public static TaskStatus neutral(String text) {
        return new TaskStatus(Text.literal(text).formatted(Formatting.GRAY));
    }

    private final Text text;

    TaskStatus(Text text) {
        this.text = text;
    }

    public Text getText() {
        return text;
    }
}

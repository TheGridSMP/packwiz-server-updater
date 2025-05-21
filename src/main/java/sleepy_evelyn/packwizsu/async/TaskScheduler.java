package sleepy_evelyn.packwizsu.async;

import sleepy_evelyn.packwizsu.util.HashedFileDownloader;
import sleepy_evelyn.packwizsu.util.PackTomlURLException;
import sleepy_evelyn.packwizsu.util.ProcessExitCodeException;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static sleepy_evelyn.packwizsu.PackWizSU.LOGGER;

public class TaskScheduler {

    public static final String BOOTSTRAP_TASK = "downloadBootstrap";
    public static final String UPDATE_PACKWIZ_TASK = "updatePackwiz";

    private static final LinkedList<AbstractAsyncTask> TASKS = new LinkedList<>();
    private static final Predicate<String> HAS_TASK = name -> TASKS.stream().anyMatch((task) -> task.isOf(name));

    public static boolean hasTask(String name) {
        return HAS_TASK.test(name);
    }

    public static void submit(String name, Supplier<AbstractAsyncTask> task) {
        if (hasTask(name))
            return;

        AbstractAsyncTask taskInstance = task.get();
        taskInstance.setName(name);

        TASKS.add(taskInstance);
    }

    public static void poll() {
        Iterator<AbstractAsyncTask> tasksIterator = TASKS.listIterator();
        Exception exception = null;
        TaskStatus status = null;

        while (tasksIterator.hasNext()) {
            AbstractAsyncTask task = tasksIterator.next();
            task.tick();

            if (!task.pollFinished()) continue;

            try {
                task.getFuture().join();

                if (task.isOf(UPDATE_PACKWIZ_TASK))
                    status = TaskStatus.UPDATE_FINISHED;
                else if (task.isOf(BOOTSTRAP_TASK))
                    status = TaskStatus.BOOTSTRAP_DOWNLOAD_FINISHED;
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                exception = e;

                if (cause instanceof InterruptedException)
                    status = TaskStatus.PROCESS_INTERRUPTED;
                else if (cause instanceof IOException)
                    status = TaskStatus.FILE_HANDLING_ERROR;

                if (task.isOf(UPDATE_PACKWIZ_TASK)) {
                    if (cause instanceof PackTomlURLException ptfe)
                        status = TaskStatus.error(ptfe.getMessage());
                    else if (cause instanceof ProcessExitCodeException pece)
                        status = TaskStatus.error(pece.getMessage());
                    else if (cause instanceof HashedFileDownloader.FailedHashMatchException fhme)
                        status = TaskStatus.error(fhme.getMessage());
                }

                if (status == null)
                    status = TaskStatus.UNKNOWN_FAIL;
            }

            task.sendMessage(status);

            if (exception != null)
                LOGGER.error("Encountered an error while polling command status", exception);

            tasksIterator.remove();
        }
    }
}

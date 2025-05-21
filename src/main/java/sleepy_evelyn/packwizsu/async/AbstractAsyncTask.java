package sleepy_evelyn.packwizsu.async;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractAsyncTask {
    private String name;
    private final CompletableFuture<Void> future;
    private final TickCounter ticker;

    public AbstractAsyncTask(CompletableFuture<Void> future, int pollTicks) {
        this.future = future;
        this.ticker = new TickCounter(pollTicks);
    }

    public void tick() {
        ticker.increment();
    }

    public boolean pollFinished() {
        return ticker.test() && future != null && future.isDone();
    }

    public void setName(String name) {
        this.name = name;
    }

    public CompletableFuture<Void> getFuture() {
        return future;
    }

    public boolean isOf(String name) { return this.name.equals(name); }

    public abstract void sendMessage(@Nullable Text text);

    public void sendMessage(@Nullable TaskStatus error) {
        if (error != null)
            this.sendMessage(error.getText());
    }
}
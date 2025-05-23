package sleepy_evelyn.packwizsu.async;

public class TickCounter {

    private final int tickThreshold;
    private final int maxValue;

    private int counter = 0;

    public TickCounter(int tickThreshold) {
        this(tickThreshold, Integer.MAX_VALUE);
    }

    public TickCounter(int tickThreshold, int maxValue) {
        this.tickThreshold = tickThreshold;
        this.maxValue = maxValue;
    }

    public boolean test() {
        if (counter < tickThreshold) return false;

        reset();
        return true;
    }

    public void increment() {
        if(counter >= maxValue || counter < 0) reset();
        else counter++;
    }

    public void reset() {
        counter = 0;
    }

    public int value() { return this.counter; }
}

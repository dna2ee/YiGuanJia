package seven.dna2ee.yiguanjia.driver;

public final class MonotonicClock {

    private static final long NS_PER_MS = 1_000_000;

    private MonotonicClock() {
    }

    public static long millis() {
        return System.nanoTime() / NS_PER_MS;
    }
}

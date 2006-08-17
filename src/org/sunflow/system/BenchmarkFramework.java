package org.sunflow.system;

/**
 * This class provides a very simple framework for running a BenchmarkTest
 * kernel several times and time the results.
 */
public class BenchmarkFramework {
    private Timer[] timers;
    private int timeLimit; // time limit in seconds

    public BenchmarkFramework(int iterations, int timeLimit) {
        this.timeLimit = timeLimit;
        timers = new Timer[iterations];
    }

    public void execute(BenchmarkTest test) {
        // clear previous results
        for (int i = 0; i < timers.length; i++)
            timers[i] = null;
        // loop for the specified number of iterations or until the time limit
        long startTime = System.nanoTime();
        for (int i = 0; i < timers.length && ((System.nanoTime() - startTime) / 1000000000) < timeLimit; i++) {
            UI.printInfo("[BCH] Running iteration %d", (i + 1));
            timers[i] = new Timer();
            test.kernelBegin();
            timers[i].start();
            test.kernelMain();
            timers[i].end();
            test.kernelEnd();
        }
        // report stats
        double avg = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int n = 0;
        for (Timer t : timers) {
            if (t == null)
                break;
            double s = t.seconds();
            min = Math.min(min, s);
            max = Math.max(max, s);
            avg += s;
            n++;
        }
        if (n == 0)
            return;
        avg /= n;
        double stdDev = 0;
        for (Timer t: timers) {
            if (t == null) break;
            double s = t.seconds();
            stdDev += (s - avg) * (s - avg); 
        }
        stdDev = Math.sqrt(stdDev / n);
        UI.printInfo("[BCH] Benchmark results:");
        UI.printInfo("[BCH]   * Iterations: %d", n);
        UI.printInfo("[BCH]   * Average:    %s", Timer.toString(avg));
        UI.printInfo("[BCH]   * Fastest:    %s", Timer.toString(min));
        UI.printInfo("[BCH]   * Longest:    %s", Timer.toString(max));
        UI.printInfo("[BCH]   * Deviation:  %s", Timer.toString(stdDev));
    }
}
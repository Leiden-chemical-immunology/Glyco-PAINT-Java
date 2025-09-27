package thread;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

// --- Watchdog utility ---
class WatchdogRunner {

    /**
     * Runs a task in a separate thread with a watchdog.
     *
     * The task is given time in slots (e.g. 60 seconds each).
     * After each slot, this method checks if the task is still running:
     *   - If finished → returns immediately.
     *   - If still running → prints a dot and extends another slot.
     *   - If still running after all slots → interrupts the task.
     *
     * @param task        the task to run
     * @param slotSeconds duration of each slot (seconds)
     * @param maxSlots    maximum number of slots before giving up
     * @return true if the task finished, false if interrupted
     */
    public static boolean runWithWatchdog(Runnable task, int slotSeconds, int maxSlots) {
        Thread t = new Thread(task, "WatchdogTask");
        t.start();

        for (int i = 0; i < maxSlots; i++) {
            try {
                t.join(slotSeconds * 10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            if (!t.isAlive()) {
                System.out.println("\nTask finished after " + (i + 1) + " slot(s).");
                return true;
            } else {
                System.out.print(".");   // heartbeat dot
                System.out.flush();
            }
        }

        System.err.println("\nTask did not finish in time. Interrupting...");
        t.interrupt();
        return false;
    }
}

// --- Simulated TrackMate task ---
class TrackMateTask implements Runnable {
    private final File nd2File;
    private final Path outputDir;

    public TrackMateTask(File nd2File, Path outputDir) {
        this.nd2File = nd2File;
        this.outputDir = outputDir;
    }

    @Override
    public void run() {
        System.out.println("TrackMate started for " + nd2File);
        try {
            // Simulate a long run (~2 minutes)
            Thread.sleep(125_000);
        } catch (InterruptedException e) {
            System.err.println("TrackMate interrupted");
            return;
        }
        System.out.println("TrackMate finished, results saved in " + outputDir);
    }
}


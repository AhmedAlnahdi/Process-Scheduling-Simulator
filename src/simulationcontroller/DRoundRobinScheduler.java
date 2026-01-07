package simulationcontroller;


public class DRoundRobinScheduler extends Scheduler {

    // This is used to check if the current process was null if it was we return 1 in getTimeQuant
    private Process currentProcess;

    // Inherited from Scheduler used for DRR quantum
    private long timeQuant;

    public DRoundRobinScheduler(Queue readyQ) {
        super(readyQ);
    }

    @Override
    protected Process selectNextProcess() {
        // Select the next process in readyQ (from parent Scheduler)
        // and store it so getTimeQuant() can include it in calculations.
        currentProcess = super.selectNextProcess();
        return currentProcess;
    }

    @Override
    public long getTimeQuant() {

        // If no process has been selected yet, return the minimum time quantum.
        if (currentProcess == null) {
            return 1;
        }

        /*
            We add +1 because in PrManager the running process is removed
            from the readyQ before getTimeQuant() is called.
            Therefore:
                - readyQ.size() does NOT include the running process
                - but Globals.SR DOES include the running process

            So to correctly compute n (the number of processes), we must add +1
            to include the currently running process.
         */
        int n = readyQ.size() + 1;

        // Calculating AR, Note: Globals.SR does include the current process so no need to add it manually
        double AR = (double) Globals.SR / n;

        // Quantum is rounded average, but must be at least 1.
        return (long) Math.max(1, Math.round(AR));
    }
}

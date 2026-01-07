package simulationcontroller;


public class PrManager {

    // Keep track of the CPU time
    private long internalClock;
    
    public Queue readyQ;
    
    public Queue holdQ1;
    
    public Queue holdQ2;
    
    private String schedulerType;
    
    private OtherKerServices otherServices;
    
    private int teamNumber;
    
    private Scheduler scheduler;
    /*
        Why track runningProcess:

        The simulator advances one CPU time unit at a time. During that time,
        new processes may arrive and enter the readyQ. Without tracking whether a
        process is currently executing, the system could incorrectly allow a newly
        arrived process to "interrupt" or affect the quantum of the running process.

        By keeping a runningProcess reference, we ensure:
        - Only one process occupies the CPU at any time.
        - New arrivals only enter the readyQ, never the CPU directly.
        - The running process completes its assigned quantum without interference.

        In short: runningProcess prevents premature admission to the CPU and
        preserves correct Round-Robin quantum behavior.
     */
    Process runningProcess = null;
    // To keep track of the remaining quantum of the current process in the CPU
    long remainingQuantum = 0;

    public PrManager(String schedulerType, OtherKerServices other, int teamNumber) {

        readyQ = new Queue();
        submitQ = new Queue();
        holdQ1 = new Queue();
        holdQ2 = new Queue();
        this.schedulerType = schedulerType;
        this.otherServices = other;
        this.teamNumber = teamNumber;
        scheduler = schedulerType.equalsIgnoreCase("StaticRR")
                ? new SRoundRobinScheduler(readyQ, teamNumber)
                : new DRoundRobinScheduler(readyQ);
    }

    /*
        Although the UML originally marked dispatch() as a private operation,
        the simulator’s architecture requires external components to trigger
        CPU dispatching each time unit. 

        Because of this, dispatch() is intentionally made public so that
        the controller or simulation loop can invoke it directly.
     */
    public void dispatch() {

        // 1) If nothing running -> pick one
        if (runningProcess == null) {
            if (readyQ.isEmpty()) {
                return;
            }
            /*
                rrtag is a scheduling timestamp used by BOTH StaticRR and DynamicRR
                to decide which process should run next.

                - When a process first arrives, rrtag = arrivalTime.
                - Whenever a process completes its quantum and returns to the ready queue,
                its rrtag is updated to the time when it finished that slice.
                (Processes that finish execution early are not reinserted, so they keep no tag.)

                By sorting the readyQ by rrtag, the scheduler always selects the process
                that has waited the longest (smallest rrtag value). This provides fairness
                across all Round Robin variants.
            */

            readyQ.sort("rrtag");

            runningProcess = scheduler.selectNextProcess();
            
            remainingQuantum = scheduler.getTimeQuant();
        }

        // 2) Execute 1 time unit
        runningProcess.executeFor(1);
        remainingQuantum--;

        // 3) If finished early
        if (runningProcess.isFinished()) {
            otherServices.deallocateMemory(runningProcess);
            otherServices.releaseDevices(runningProcess);
            // +1 because internalClock represents the start of this time unit,
            // and the process actually finishes at the end of it.
            runningProcess.setStats(internalClock + 1);

            Globals.finishedList.add(runningProcess);
            
            runningProcess = null;
            checkHoldQueues(); // resources are now free so we check processes in HoldQ's
            updateGlobals();
            return;   // quantum ended early
        }

        // 4) If quantum ended
        if (remainingQuantum == 0) {
            runningProcess.bumpRrTag(internalClock); // Update rrTag of current process
            readyQ.enqueue(runningProcess);
            runningProcess = null; // We set it null so other process in the readyQ can enter now
            updateGlobals();
        }
    }

    public void procArrivingRoutine(Process p) {

        int memCheck = otherServices.canAllocateMemory(p);
        int devCheck = otherServices.canReserveDevice(p);

        // 1) Reject if requirements exceed total capacity
        if (memCheck == -1 || devCheck == -1) {
            return;  // Immediate reject
        }

        // 2) HOLD if resources are insufficient but within limits
        if (memCheck == 0 || devCheck == 0) {
            putInHold(p);
            return;
        }

        // 3) Both memory and devices are available → admit process
        otherServices.allocateMemory(p);
        otherServices.allocateDevice(p);

        readyQ.enqueue(p);
        updateGlobals();
    }

    private void putInHold(Process p) {

        if (p.getPriority() == 1) {
            holdQ1.enqueue(p);
            holdQ1.sort("mem"); // Per project proposal
        } else {
            holdQ2.enqueue(p);
        }
    }

    // move processes from hold queues to ready if possible
    public void checkHoldQueues() {
        while (!holdQ1.isEmpty() && tryMoveProcess(holdQ1)) {
        }
        while (!holdQ2.isEmpty() && tryMoveProcess(holdQ2)) {
        }
    }

    private boolean tryMoveProcess(Queue queue) {
        Process p = queue.peek();
        if (p == null) {
            return false;
        }

        int memCheck = otherServices.canAllocateMemory(p);
        int devCheck = otherServices.canReserveDevice(p);

        if (memCheck == 1 && devCheck == 1) {
            queue.dequeue();

            readyQ.enqueue(p);
            otherServices.allocateMemory(p);
            otherServices.allocateDevice(p);
            updateGlobals();
            return true;
        }

        return false;
    }

    public void cpuTimeAdvance(long newTime) {
        if (newTime > internalClock) {
            internalClock = newTime;
        }
    }

    // time quantum
    public long getNextDecisionTime() {
        return scheduler.getTimeQuant();
    }

    private void updateGlobals() {
        Globals.SR = 0;
        for (Process x : readyQ.getAll()) {
            Globals.SR += x.getRemainingBurst();
        }
    }

    public long getClock() {
        return internalClock;
    }

}

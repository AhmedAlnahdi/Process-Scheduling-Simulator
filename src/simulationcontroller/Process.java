package simulationcontroller;


public class Process {

    // ====== Basic Attributes ======
    private int processId;
    private long arrivalTime;
    private long burstTime;
    private long remainingBurst;
    private long memoryRequired;
    private long rrTag;
    private int devicesRequired;
    private int priority;

    // ====== Statistics ======
    private long waitingTime;
    private long turnaroundTime;
    private long completionTime;

    // ====== Constructors ======
    public Process(int processId, long arrivalTime, long burstTime,
            long memoryRequired, int devicesRequired, int priority) {
        this.processId = processId;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingBurst = burstTime;
        this.memoryRequired = memoryRequired;
        this.devicesRequired = devicesRequired;
        this.priority = priority;
        rrTag = arrivalTime;
    }

    // ====== Primary Methods ======
    public void executeFor(long maxTime) {
        remainingBurst -= maxTime;
    }

    public void bumpRrTag(long clock){
        this.rrTag = clock;
    }
    // ====== Getters and Setters ======
    
    public long getRrTag() {
        return rrTag;
    }
    
    public int getProcessId() {
        return processId;
    }

    public long getRemainingBurst() {
        return remainingBurst;
    }

    public long getMemoryRequired() {
        return memoryRequired;
    }

    public int getDevicesRequired() {
        return devicesRequired;
    }

    public int getPriority() {
        return priority;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public long getBurstTime() {
        return burstTime;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public long getTurnaroundTime() {
        return turnaroundTime;
    }

    public long getCompletionTime() {
        return completionTime;
    }

   

    public void setStats(long finishTime) {
        this.completionTime = finishTime;
        this.turnaroundTime = (finishTime - arrivalTime);
        this.waitingTime = turnaroundTime-burstTime;
    }

    public boolean isFinished() {
        return remainingBurst <= 0;
    }

    @Override
    public String toString() {
        return "P" + processId;
    }

}

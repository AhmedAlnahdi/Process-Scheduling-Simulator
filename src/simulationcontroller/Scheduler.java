package simulationcontroller;

abstract class Scheduler {
    protected final Queue readyQ;
    
    private long timeQuant;
    protected Scheduler(Queue readyQ) {
        this.readyQ = readyQ;
    }

    protected Process selectNextProcess() {
        return readyQ.dequeue();
    }

    public long getTimeQuant() {
        return timeQuant;
    }
}
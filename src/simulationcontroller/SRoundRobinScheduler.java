package simulationcontroller;
 
public class SRoundRobinScheduler extends Scheduler {
   
    private long timeQuant;

    public SRoundRobinScheduler(Queue readyQ, int teamNumber) {
        super(readyQ);
        this.timeQuant = 10 + teamNumber;
    }

    @Override
    protected Process selectNextProcess() {
        return super.selectNextProcess();
    }


    @Override
    public long getTimeQuant() {
        return timeQuant;
    }
}

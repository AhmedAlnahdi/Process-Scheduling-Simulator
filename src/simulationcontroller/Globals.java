package simulationcontroller;



import java.util.ArrayList;
import java.util.List;

/*
    Global scheduling metrics shared across the system.
    - SR:  Sum of remaining burst times of all processes in the ready queue.
    AR (Average Remaining time) was originally meant to be stored globally,  
    but redesigned it to be computed locally inside DRoundRobinScheduler.
    So it prevents accidental global updates and keeps AR consistent with the 
    currently selected process and ready queue snapshot.
*/
public class Globals {
    public static long SR = 0;
    // This is used for statistics in SimulationController
    public static List<Process> finishedList = new ArrayList<>();
}

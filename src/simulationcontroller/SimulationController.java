package simulationcontroller;


import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SimulationController {

    // Per UML Attributes
    private long currentTime;
    
    private File inputFile;
    private File outputFile;
    
    private String currentInputFileName;
    
    private PrintWriter writer;

    // Kernel services
    private PrManager prManager;
    private OtherKerServices other;

    // Commands
    private List<String> commands;
    private int cmdIndex = 1;        // start after the C line
    private String nextCmd = null;

    // sysGen(): interpret C line
    private void sysGen(String initLine) {
        String[] parts = initLine.split(" ");

        long start = Long.parseLong(parts[1]);
        currentTime = start;

        long memory = Long.parseLong(parts[2].split("=")[1]);
        int devices = Integer.parseInt(parts[3].split("=")[1]);

        // Scheduling type
        String schedulerType = "";
        if (currentInputFileName.toLowerCase().contains("drr")) {
            schedulerType = "DynamicRR";
        } else if (currentInputFileName.toLowerCase().contains("srr")) {
            schedulerType = "StaticRR";
        }

        int teamNumber = 5;

        // Reset kernel & stats 
        other = new OtherKerServices(memory, devices);
        prManager = new PrManager(schedulerType, other, teamNumber);

        Globals.finishedList.clear();

        writer.print("CONFIG at " + start + ": ");
        writer.println("mem=" + memory + " devices=" + devices + " scheduler= " + schedulerType);
        writer.println();
    }

    // ------------ parseCmd: C or A or D ------------
    private void parseCmd(String line) {

        if (line.startsWith("C")) {
            sysGen(line);
        } else if (line.startsWith("A")) {
            String[] t = line.split(" ");

            int id = Integer.parseInt(t[2].split("=")[1]);
            int memReq = Integer.parseInt(t[3].split("=")[1]);
            int devReq = Integer.parseInt(t[4].split("=")[1]);
            int burst = Integer.parseInt(t[5].split("=")[1]);
            int prio = Integer.parseInt(t[6].split("=")[1]);

            Process p = new Process(id, currentTime, burst, memReq, devReq, prio);
            prManager.procArrivingRoutine(p);
        } else if (line.startsWith("D")) {
            displayState();
        }
    }

    private long getCmdTime(String line) {
        if (line == null) {
            return Long.MAX_VALUE;
        }

        line = line.trim();
        if (line.isEmpty()) {
            return Long.MAX_VALUE;
        }

        String[] parts = line.split(" ");
        if (parts.length < 2) {
            return Long.MAX_VALUE;
        }

        return Long.parseLong(parts[1]);
    }

    private void loadNextCmd() {
        nextCmd = null;

        while (cmdIndex < commands.size()) {

            String line = commands.get(cmdIndex).trim();
            cmdIndex++;

            if (!line.isEmpty()) {
                nextCmd = line;
                return;
            }
        }
    }

    // Show system state for D command
    private void displayState() {

        writer.println("-------------------------------------------------------");
        writer.println("System Status:                                         ");
        writer.println("-------------------------------------------------------");

        writer.printf("          Time: %.2f%n", (double) currentTime);
        writer.println("  Total Memory: " + other.getTotalMemory());
        writer.println(" Avail. Memory: " + other.getAvailableMemory());
        writer.println(" Total Devices: " + other.getTotalDevices());
        writer.println("Avail. Devices: " + other.getAvailableDevices());
        writer.println();

        writer.println("Jobs in Ready List                                      ");
        writer.println("--------------------------------------------------------");
        if (prManager.readyQ.isEmpty()) {
            writer.println("  EMPTY");
        } else {
            for (Process p : prManager.readyQ.getAll()) {
                writer.printf("Job ID %d , %.2f Cycles left to completion.%n",
                        p.getProcessId(), (double) p.getRemainingBurst());
            }
        }
        writer.println();

        writer.println("Jobs in Long Job List                                   ");
        writer.println("--------------------------------------------------------");
        writer.println("  EMPTY");
        writer.println();

        writer.println("Jobs in Hold List 1                                     ");
        writer.println("--------------------------------------------------------");
        if (prManager.holdQ1.isEmpty()) {
            writer.println("  EMPTY");
        } else {
            for (Process p : prManager.holdQ1.getAll()) {
                writer.printf("Job ID %d , %.2f Cycles left to completion.%n",
                        p.getProcessId(), (double) p.getRemainingBurst());
            }
        }
        writer.println();

        writer.println("Jobs in Hold List 2                                     ");
        writer.println("--------------------------------------------------------");
        if (prManager.holdQ2.isEmpty()) {
            writer.println("  EMPTY");
        } else {
            for (Process p : prManager.holdQ2.getAll()) {
                writer.printf("Job ID %d , %.2f Cycles left to completion.%n",
                        p.getProcessId(), (double) p.getRemainingBurst());
            }
        }
        writer.println();

     
        if (Globals.finishedList.isEmpty()) {
            writer.println("Finished Jobs (detailed)");
            writer.println("--------------------------------------------------------");
            writer.println("  Job    ArrivalTime     CompleteTime     TurnaroundTime    WaitingTime");
            writer.println("------------------------------------------------------------------------");
            writer.print("EMPTY");
        } else{
            displayFinalStatistics();
        }
        writer.println();
        writer.println();
    }

    // Final statistics
    private void displayFinalStatistics() {

        Globals.finishedList.sort(Comparator.comparingInt(Process::getProcessId));

        writer.println("Finished Jobs (detailed)");
        writer.println("--------------------------------------------------------");
        writer.println("  Job    ArrivalTime     CompleteTime     TurnaroundTime    WaitingTime");
        writer.println("------------------------------------------------------------------------");
        int finishedJobs = 0;

        for (Process p : Globals.finishedList) {

            finishedJobs++;
            writer.printf(
                    "  %-6d %-15.2f %-15.2f %-17.2f %.2f%n",
                    p.getProcessId(),
                    (double) p.getArrivalTime(),
                    (double) p.getCompletionTime(),
                    (double) p.getTurnaroundTime(),
                    (double) p.getWaitingTime()
            );
        }

        writer.println("Total Finished Jobs:             " + finishedJobs);
        writer.println();
    }

    public static void main(String[] args) {
        runAllSimulations();
    }

    public void startSimulation(String inputFileName, String outputFileName) { // read file into commands list

        try {
            this.commands = Files.readAllLines(Paths.get(inputFileName));
        } catch (IOException e) {
            System.out.println("File cannot be read");
            return;
        }
        
        this.currentInputFileName = inputFileName;
        
        try {
            outputFile = new File(outputFileName);
            writer = new PrintWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            System.out.println("Cannot open output file: " + outputFileName);
            return;
        }
        
        sysGen(commands.get(0));
        
        loadNextCmd();

        while (true) {

            // 1) Process commands whose timestamp == currentTime
            while (nextCmd != null && getCmdTime(nextCmd) == currentTime) {
                parseCmd(nextCmd);
                cmdIndex++;
                loadNextCmd();
            }

            // Termination condition
            if (nextCmd == null
                    && prManager.readyQ.isEmpty()
                    && prManager.holdQ1.isEmpty()
                    && prManager.holdQ2.isEmpty()
                    && prManager.runningProcess == null) {
                break;
            }

            // if the CPU is idle go to next command time
            if (prManager.runningProcess == null && prManager.readyQ.isEmpty()) {
                if (nextCmd != null) {
                    long nextT = getCmdTime(nextCmd);
                    currentTime = nextT;
                    prManager.cpuTimeAdvance(currentTime);
                    continue;
                }
            }

            // Determine quantum
            long q = prManager.getNextDecisionTime();

            // Run quantum
            for (long used = 0; used < q; used++) {

                prManager.dispatch();
                currentTime++;
                prManager.cpuTimeAdvance(currentTime);

                // During-quantum commands
                while (nextCmd != null && getCmdTime(nextCmd) == currentTime) {
                    parseCmd(nextCmd);
                    cmdIndex++;
                    loadNextCmd();
                }

                if (prManager.runningProcess == null) {
                    break;
                }
            }
        }

        // Show final stats
        
        writer.print("--- Simulation finished at time " + currentTime + " ---");
        writer.close();
    }

    public static void runAllSimulations() {
        File folder = new File("."); // current directory

        File[] inputs = folder.listFiles((dir, name) -> name.startsWith("input") && name.endsWith(".txt"));

        if (inputs == null || inputs.length == 0) {
            System.out.println("No input*.txt files found.");
            return;
        }

        for (File input : inputs) {
            String inputName = input.getName();
            System.out.println("Running simulation for inputFile: " + inputName);
            // Create output filename by replacing "input" with "output"
            String outputName = inputName.replace("input", "output");

            SimulationController sc = new SimulationController();

            sc.startSimulation(inputName, outputName);

        }
    }

}

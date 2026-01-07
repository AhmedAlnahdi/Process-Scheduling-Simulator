package simulationcontroller;


/*
    Why canAllocateMemory() and canReserveDevice() are separate checks:

    A process should never be allowed to reserve devices unless it can also be
    admitted into memory. In earlier versions, a process that failed the memory
    check (returning 0 = hold) could still reserve devices if they were available,
    which incorrectly allowed a non-admitted process to hold system resources.

    By checking memory and devices independently—and only allocating resources
    after both checks pass—we ensure that a process is admitted atomically:
    either all required resources are allocated, or none are.
*/
public class OtherKerServices {

    private final long TOTAL_MEMORY;
    private final int TOTAL_DEVICES;
    private long availableMemory;
    private int availableDevices;

    public OtherKerServices(long totalMemory, int totalDevices) {
        this.TOTAL_MEMORY = totalMemory;
        this.TOTAL_DEVICES = totalDevices;
        this.availableMemory = totalMemory;
        this.availableDevices = totalDevices;
    }

    public void allocateMemory(Process p) {
        availableMemory -= p.getMemoryRequired();
    }

    public void deallocateMemory(Process process) {
        availableMemory += process.getMemoryRequired();

        if (availableMemory > TOTAL_MEMORY) {
            availableMemory = TOTAL_MEMORY;
        }
    }

    /*
       -1 = exceeds total capacity (reject)
        0 = insufficient available capacity (hold)
        1 = allocated successfully (admit)
     */
    public int canAllocateMemory(Process process) {
        long required = process.getMemoryRequired();

        if (required > TOTAL_MEMORY) {
            return -1;  // Reject
        }
        if (required > availableMemory) {
            return 0;   // Hold
        }
        return 1;       // Admit
    }

    public void allocateDevice(Process p) {
        availableDevices -= p.getDevicesRequired();
    }

    
    public void releaseDevices(Process process) {
        availableDevices += process.getDevicesRequired();
        if (availableDevices > TOTAL_DEVICES) {
            availableDevices = TOTAL_DEVICES;
        }
    }

    /*
      -1 = exceeds total capacity (reject)
       0 = insufficient available capacity (hold)
       1 = allocated successfully (admit)
     */
    public int canReserveDevice(Process process) {
        int required = process.getDevicesRequired();
        if (required > TOTAL_DEVICES) {
            return -1;  // Reject
        }
        if (required > availableDevices) {
            return 0;   // Hold

        }
        return 1;       // Admit
    }

    public long getAvailableMemory() {
        return availableMemory;
    }

    public int getAvailableDevices() {
        return availableDevices;
    }

    public long getTotalMemory() {
        return TOTAL_MEMORY;
    }

    public int getTotalDevices() {
        return TOTAL_DEVICES;
    }

}

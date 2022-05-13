package simulator; 
import java.util.HashMap;
import java.util.Map;
/**
 * Simulation of an IO device. A device has an id and type name (e.g. 'printer'). Processes may make IO requests.
 * <p>
 * It is expected that an IO request is blocking i.e. the calling process will be removed from the CPU
 * while waiting for the request to complete.
 * 
 * @author Stephan Jamieson
 * @version 8/3/15
 */
public class IODevice {
        
    private int ID;
    private String name;
    private long freeTime;
    private Map<Integer, ProcessControlBlock> queue;    
    
    /**
     * Create a device with the given id and name (e.g. 'disk').
     */
    public IODevice(int ID, String name) { 
        this.ID = ID; 
        this.name = name;
        this.queue = new HashMap<Integer, ProcessControlBlock>();
    }
    
    /**
     * Obtain the device name.
     */
    public String getName() { return name; }
    
    /**
     * Obtain the device id.
     */
    public int getID() { return ID; }
    
    long getFreeTime() { return freeTime; }
    void setFreeTime(long systemTime) { freeTime = systemTime; }

    /**
     * Make an IO request of the given duration, calling the given interrupt handler 
     * when the request is complete.
     */
    public void requestIO(int duration, final ProcessControlBlock process, final InterruptHandler handler) {
        if (this.getFreeTime()<=Config.getSystemTimer().getSystemTime()) {
            this.setFreeTime(Config.getSystemTimer().getSystemTime()+duration);
        }
        else {
            this.setFreeTime(this.getFreeTime()+duration);
        }
        queue.put(process.getPID(), process);
        WakeUpEvent event = new WakeUpEvent(this.getFreeTime(), this.getID(), process.getPID(), 
            new EventHandler<WakeUpEvent>() {
                public void process(WakeUpEvent event) {
                    // WAKE_UP is never invoked by a user process. Any running user process must be suspended.
                    final ProcessControlBlock currentProcess = !Config.getCPU().isIdle() ? Config.getCPU().getCurrentProcess() : null;
                    if (currentProcess != null) {
                        assert(currentProcess.getState() == ProcessControlBlock.State.RUNNING);
                        currentProcess.setState(ProcessControlBlock.State.READY);
                    }
     
                    queue.remove(event.getProcessID());
                    assert(process.hasNextInstruction());
                    process.nextInstruction();
                    TRACE.INTERRUPT(InterruptHandler.WAKE_UP,  deviceToString(), process);
                    Config.getSimulationClock().logInterrupt();
                    handler.interrupt(InterruptHandler.WAKE_UP,  getID(), process);
                    TRACE.INTERRUPT_END();
                    
                    // Restore suspended process but only if it has not been replaced due to a scheduling decision.
                    if (currentProcess != null && currentProcess == Config.getCPU().getCurrentProcess()) {
                        currentProcess.setState(ProcessControlBlock.State.RUNNING);
                    }
                }                            
            });
        Config.getEventScheduler().schedule(event);
    }

    private String deviceToString() { return this.toString(); }
    
    /**
     * Obtain a string representation of this device of the form 'device(id=<id>)'.
     */
    public String toString() { return "device(id="+this.getID()+")"; }
}

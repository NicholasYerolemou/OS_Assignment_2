 package simulator; 
/**
 * An Execve event represents the creation of a program execution.
 * 
 * @author Stephan Jamieson
 * @version 8/3/15
 */
class ExecveEvent extends Event {

    private String progName;
    private Kernel kernel;
    private int priority;
    
    public ExecveEvent(long startTime, String progName, int priority, Kernel kernel) {
        super(startTime);
        this.priority=priority;
        this.progName=progName;
        this.kernel=kernel;
    }
        
    
    /**
     * Obtain the name of the program that must be run.
     */
    public String getProgramName() {
        return progName;
    }
    
    /**
     * Obtain the priority of the program that must be run.
     */
    int getPriority() { return priority; }
   
    public void process() {
        // EXECVE is never invoked by a user process. Any running user process must be suspended.
        final ProcessControlBlock currentProcess = !Config.getCPU().isIdle() ? Config.getCPU().getCurrentProcess() : null;
        if (currentProcess != null) {
            assert(currentProcess.getState() == ProcessControlBlock.State.RUNNING);
            currentProcess.setState(ProcessControlBlock.State.READY);
            
        }

        TRACE.SYSCALL(SystemCall.EXECVE, getProgramName());
        Config.getSimulationClock().logSystemCall();
        kernel.syscall(SystemCall.EXECVE, getProgramName(), getPriority());
        TRACE.SYSCALL_END();
        
        // Restore suspended process but only if it has not been replaced due to a scheduling decision.
        if (currentProcess != null && currentProcess == Config.getCPU().getCurrentProcess()) {
            currentProcess.setState(ProcessControlBlock.State.RUNNING);
        }
    }
    
    public String toString() { return "ExecveEvent("+this.getTime()+", "+this.getProgramName()+"["+this.getPriority()+"])"; }

}

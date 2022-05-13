package simulator; 
//
import simulator.Profiling;
/**
 * A CPU object stores the currently executing process, provides a method
 * for performing context switches, and records the 
 * number of context switches that have occurred during execution.
 * 
 * The CPU can execute in USER mode or SUPERVISOR mode. USER is the mode in which user programs are executed. SUPERVISOR is the mode in which kernel
 * actions (such as handling I/O requests and interrupts) are executed.
 * 
 * @author Stephan Jamieson
 * @version 8/3/15
 */
public class CPU  {

    /**
     * A CPU can execute in USER mode or SUPERVISOR mode.
     */
    public enum Mode { SUPERVISOR, USER };
    
    private ProcessControlBlock currentProcess;
    private Mode mode;
    private int contextSwitches;
    
    /**
     * Create CPU, setting contextSwitches to zero, mode to SUPERVISOR and currentProcess to null.
     */
    CPU() {
        this.contextSwitches = 0;
        this.mode = Mode.SUPERVISOR;
        this.currentProcess = null;
    }
    
    
    /**
     * Obtain the currently executing process.
     */
    public ProcessControlBlock getCurrentProcess() {
        assert(!isIdle());
        return currentProcess;
    }

    /**
     * Set to supervisor or user mode. 
     */
    private void setMode(final Mode mode) {
        this.mode = mode;
        assert(this.currentProcess!=null);
        Profiling.recordTransition(currentProcess.getPID(), this.mode);
    }
    
    /**
     * Obtain current mode.
     */
    public CPU.Mode getMode() { return this.mode; }
    
    /**
     * Exeucte the current process in user space until it terminates or blocks for IO.
     * <p>
     * The CPU will update the system timer to indicate the amout of user time spent processing.
     * <p>
     * The method returns the quantity of time used.
     * <p>
     * @return number of time units used.
     */
    int execute() {
        int units = -1;
        if (this.isIdle()) {
            units = 0;
        }
        else {
            this.setMode(Mode.USER);
            {
                final long currentTime = Config.getSimulationClock().getSystemTime();
                final long currentPID = this.getCurrentProcess().getPID();
                TRACE.PRINTF(32, "Time: %010d Simulator: execute process %d until CPU instruction complete.\n", currentTime, currentPID);
            }
            Instruction instr = getCurrentProcess().getInstruction();
            assert(instr instanceof CPUInstruction);
            units = ((CPUInstruction)instr).execute();
            Config.getSimulationClock().advanceUserTime(units);
            this.setMode(Mode.SUPERVISOR);
            next_instruction();
        }
        return units;
    }
    /**
     * Execute the current process in user space for the given number of time units.
     * <p>
     * If the current cpu burst can complete in the given time, then the CPU will execute 
     * the next instruction in the 'program'. This must be a system call (either I/O or terminate).
     * Either will cause this process to be switched out.
     * <p>
     * The CPU will update the system timer to indicate the amout of user time spent processing.
     * <p>
     * The method returns the quantity of unused time unit. A value greater than zero means that 
     * the current cpu burst was completed. A value of zero means the current cpu 
     * burst may or may not have completed.
     * 
     * @return number of unused time units.
     */
    int execute(int timeUnits) {
        int remainder = -1;
        if (this.isIdle()) {
            Config.getSimulationClock().advanceSystemTime(timeUnits);
            remainder = 0;
        }
        else {
            this.setMode(Mode.USER);
            {
                final long currentTime = Config.getSimulationClock().getSystemTime();
                final long currentPID = this.getCurrentProcess().getPID();
                TRACE.PRINTF(32, "Time: %010d Simulator: execute process %d for %d time units.\n", currentTime, currentPID, timeUnits);
            }
            final Instruction instr = getCurrentProcess().getInstruction();
            assert(instr instanceof CPUInstruction);
            remainder = ((CPUInstruction)instr).execute(timeUnits);
            
            if (remainder>=0) {
                // CPU burst completed.
                // Invoke following IO instruction.
                Config.getSimulationClock().advanceUserTime(timeUnits-remainder);
                this.setMode(Mode.SUPERVISOR);
                next_instruction();
            }
            else {
                remainder = 0;
                Config.getSimulationClock().advanceUserTime(timeUnits);
                this.setMode(Mode.SUPERVISOR);
            }
            
        }
        return remainder;
    }
        
    /**
     * Process I/O or termination.
     */
    private void next_instruction() {
        
        if (getCurrentProcess().hasNextInstruction()) {
            getCurrentProcess().nextInstruction();
            assert(getCurrentProcess().getInstruction() instanceof IOInstruction);
            IOInstruction ioInst = (IOInstruction)getCurrentProcess().getInstruction();
            TRACE.SYSCALL(SystemCall.IO_REQUEST, Config.getDevice(ioInst.getDeviceID()).toString(), ioInst.getDuration(), getCurrentProcess());
            Config.getSimulationClock().logSystemCall();
            Config.getKernel().syscall(SystemCall.IO_REQUEST, ioInst.getDeviceID(), Integer.valueOf(ioInst.getDuration()));
            TRACE.SYSCALL_END();

        }
        else {
            // Terminate process
            TRACE.SYSCALL(SystemCall.TERMINATE_PROCESS, getCurrentProcess());
            Config.getSimulationClock().logSystemCall();
            Config.getKernel().syscall(SystemCall.TERMINATE_PROCESS, getCurrentProcess().getPID());
            TRACE.SYSCALL_END();
        }
    }
    
    /**
     * Determine whether the CPU is idle (<code>getCurrentProcess()==null</code>).
     */
    public boolean isIdle() { return currentProcess==null; }
    
    private static String format(ProcessControlBlock process) {
        if (process==null) {
            return "{Idle}";
        }
        else {
            return process.toString();
        }
    }
  
    /**
     * Switch the current process out and the given process in. 
     * 
     * @return the previously executing process.
     */    
    public ProcessControlBlock contextSwitch(ProcessControlBlock process) {
        contextSwitches++;
        ProcessControlBlock out = currentProcess;
        currentProcess = process;
        TRACE.PRINTF(1, "Time: %010d Kernel: Context Switch %s, %s).\n", Config.getSimulationClock().getSystemTime(), format(out), format(process)); 
        Config.getSimulationClock().logContextSwitch();
        return out;
    }
    
    /**
     * Obtain the number of context switches.
     */
    public int getContextSwitches() { return contextSwitches; }
}

import simulator.Config;
import simulator.IODevice;
import simulator.Kernel;
import simulator.ProcessControlBlock;
//
import java.io.FileNotFoundException;
import java.io.IOException;
//
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An RRKernel implements Round Robin scheduling. 
 * 
 * Time on the CPU is allocated to user processes in 'slices'. During an execution slice the current process may still be interrupted by kernel activities
 * - such as I/O interrupt handling and new program loading. Thus a process is never guaranteed a full slice.  
 * 
 * @author Stephan Jamieson
 * @version 30/3/2022
 */
public class RRKernel implements Kernel {
    

    /**
     * Queue for processes available for execution ordered by arrival time.
     */
    private Deque<ProcessControlBlock> readyQueue;
    
    /**
     * Maximum slice of execution time available to a process.
     */
    private int SLICE_TIME;
        
    /**
     * Create an RRKernel. The first object in varargs must be a positive integer slice time in the form or a String or Integer object.
     */
    public RRKernel(Object... varargs) {
        assert(varargs.length==1);
        this.readyQueue = new ArrayDeque<ProcessControlBlock>();
        this.SLICE_TIME=Integer.valueOf(varargs[0].toString());
    }
    
    /**
     * Place a new process on the CPU. Either the current process has terminated, is now waiting for I/O, or has reached the end of its slice.
     */
    private ProcessControlBlock dispatch() {
            ProcessControlBlock oldProc;
            if (!readyQueue.isEmpty()) {
                ProcessControlBlock nextProc = readyQueue.removeFirst();
                oldProc = Config.getCPU().contextSwitch(nextProc);
                nextProc.setState(ProcessControlBlock.State.RUNNING);
                Config.getSystemTimer().scheduleInterrupt(SLICE_TIME, this, nextProc.getPID());
            }
            else {
                oldProc = Config.getCPU().contextSwitch(null);
            }
            return oldProc;
    }
            
    
    /**
     *  Invoke the system call with the given number (See SystemCall), providing zero or more arguments.
     */
    public int syscall(int number, Object... varargs) {
        int result = 0;
        switch (number) {
             case MAKE_DEVICE:
                {
                    IODevice device = new IODevice((Integer)varargs[0], (String)varargs[1]);
                    Config.addDevice(device);
                }
                break;
                
             case EXECVE: 
                {                    
                    ProcessControlBlock pcb = this.loadProgram((String)varargs[0]);
                    if (pcb!=null) {
                        // Loaded successfully.
                        pcb.setPriority((Integer)varargs[1]);
                        readyQueue.addLast(pcb);
                        if (Config.getCPU().isIdle()) { this.dispatch(); }
                    }
                    else {
                        result = -1;
                    }
                }
                break;
                
             case IO_REQUEST: 
                {
                    ProcessControlBlock ioRequester = Config.getCPU().getCurrentProcess();
                    Config.getSystemTimer().cancelInterrupt(Config.getCPU().getCurrentProcess().getPID());
                    IODevice device = Config.getDevice((Integer)varargs[0]);
                    device.requestIO((Integer)varargs[1], ioRequester, this);
                    ioRequester.setState(ProcessControlBlock.State.WAITING);
                    dispatch();
                }
                break;
                
             case TERMINATE_PROCESS:
                {
                    Config.getCPU().getCurrentProcess().setState(ProcessControlBlock.State.TERMINATED);
                    Config.getSystemTimer().cancelInterrupt(Config.getCPU().getCurrentProcess().getPID());    
                    dispatch();
                }
                break;
             default:
                result = -1;
        }
        return result;
    }
   
    /**
     * Invoke the interrupt handler (see InterruptHandler), providing the interrupt type and zero or more arguments.
     */    
    public void interrupt(int interruptType, Object... varargs){
        switch (interruptType) {            
            case TIME_OUT:
                {
                    int processID = (Integer)varargs[0];
                    assert(Config.getCPU().getCurrentProcess()!=null && processID==Config.getCPU().getCurrentProcess().getPID());
                    if (readyQueue.isEmpty()) {
                        // Give current process another slice.
                        Config.getSystemTimer().scheduleInterrupt(SLICE_TIME, this, processID);
    
                    }
                    else {
                        Config.getCPU().getCurrentProcess().setState(ProcessControlBlock.State.READY);
                        ProcessControlBlock oldProc = dispatch();
                        readyQueue.addLast(oldProc);
                    }
                }
                break;
            case WAKE_UP:
                {
                    ProcessControlBlock process = (ProcessControlBlock)varargs[1]; 
                    process.setState(ProcessControlBlock.State.READY);
                    readyQueue.addLast(process);
                    if (Config.getCPU().isIdle()) { 
                        this.dispatch(); 
                    }
                }
                break;
                
            default:
                throw new IllegalArgumentException("RoundRobinKernel:interrupt("+interruptType+"...): unknown type.");
        }
    }
    
    /**
     * Create a ProcessControlBlock for the program with the given file name. 
     * This method is a wrapper for handling I/O exceptions. Its purpose to make the syscall method 'cleaner' and easier to read.
     */
    private static ProcessControlBlock loadProgram(String filename) {
        try {
            return ProcessControlBlock.loadProgram(filename);
        }
        catch (FileNotFoundException fileExp) {
            throw new IllegalArgumentException("RoundRobinKernel: loadProgram(\""+filename+"\"): file not found.");
        }
        catch (IOException ioExp) {
            throw new IllegalArgumentException("RoundRobinKernel: loadProgram(\""+filename+"\"): IO error.");
        }
    }
    
    private void dumpReadyQueue() {
        System.out.println("Dumping ready queue");
        for(ProcessControlBlock pcb : readyQueue) {
             System.out.println(pcb);   
        }
    }
}

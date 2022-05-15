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

//find where the new PCB is created
//add the processes to ready queue
//check if one is shorter than current running program
//if yes. Interrupt process
//start from shortest in queue again

/**
 * An FCFSKernel implewhetherments First Come First Served Scheduling.
 * 
 * Processes are queued according to arrival time. Time on the CPU is only
 * relinquished when the current process terminates or blocks for I/O.
 * 
 * @author Nicholas Yerolemou based on work by Stephan Jamieson
 * 
 */
public class SJFKernel implements Kernel {

    /**
     * Queue for processes available for execution ordered by arrival time.
     */
    private Deque<ProcessControlBlock> readyQueue;

    /**
     * Create an SJFKernel. The kernel does not require any instantiation values,
     * so varargs is ignored. The formal parameter is retained to ensure
     * uniformity across different kernel types, allowing programs such as Simulate
     * to create kernels on the fly.
     */
    public SJFKernel(Object... varargs) {
        this.readyQueue = new ArrayDeque<ProcessControlBlock>();
    }

    /**
     * Place a new process on the CPU. Either the current process has terminated or
     * is now waiting for I/O.
     */
    private ProcessControlBlock dispatch() {
        ProcessControlBlock oldProc;
        if (!readyQueue.isEmpty()) {

            ProcessControlBlock shortestProc = readyQueue.removeFirst();
            int duration = shortestProc.getInstruction().getBurstRemaining();
            int size = readyQueue.size();

            for (int i = 1; i < size; i++)// loop through ready queue to find shortest job
            {
                ProcessControlBlock tempProc = readyQueue.removeFirst();
                if (tempProc.getInstruction().getBurstRemaining() < duration)// the new process is shorter
                {
                    duration = tempProc.getInstruction().getBurstRemaining();// new shortest duration is that of the
                    // current
                    // process
                    readyQueue.add(shortestProc);// add the old shortest process to the queue again
                    shortestProc = tempProc;
                } else // the new processes isnt the shortest re-add it to the queue
                {
                    readyQueue.add(tempProc);
                }
            }
            ProcessControlBlock nextProc = shortestProc;
            // readyQueue.removeFirst();

            oldProc = Config.getCPU().contextSwitch(nextProc);
            nextProc.setState(ProcessControlBlock.State.RUNNING);
        } else {
            oldProc = Config.getCPU().contextSwitch(null);
        }
        return oldProc;
    }

    /**
     * Invoke the system call with the given number (See SystemCall), providing zero
     * or more arguments.
     */
    public int syscall(int number, Object... varargs) {
        int result = 0;
        switch (number) {
            case MAKE_DEVICE: {
                IODevice device = new IODevice((Integer) varargs[0], (String) varargs[1]);
                Config.addDevice(device);
            }
                break;
            case EXECVE: {
                ProcessControlBlock pcb = this.loadProgram((String) varargs[0]);
                if (pcb != null) {
                    // Loaded successfully.
                    pcb.setPriority((Integer) varargs[1]);
                    readyQueue.addLast(pcb);
                    if (Config.getCPU().isIdle()) {
                        this.dispatch();
                    }
                } else {
                    // interupt the current process so it goes back to ready queue. then the
                    // scheduler will find the shortest process to run as normal
                    if (pcb.getInstruction().getBurstRemaining() < Config.getCPU().getCurrentProcess().getInstruction()
                            .getBurstRemaining())// if new process is shorter
                    {
                        this.interrupt(TIME_OUT, Config.getCPU().getCurrentProcess().getPID());
                    }

                    result = -1;
                }
            }
                break;
            case IO_REQUEST: {
                ProcessControlBlock ioRequester = Config.getCPU().getCurrentProcess();
                IODevice device = Config.getDevice((Integer) varargs[0]);
                device.requestIO((Integer) varargs[1], ioRequester, this);
                ioRequester.setState(ProcessControlBlock.State.WAITING);
                dispatch();
            }
                break;
            case TERMINATE_PROCESS: {
                Config.getCPU().getCurrentProcess().setState(ProcessControlBlock.State.TERMINATED);
                ProcessControlBlock process = dispatch();
                // process.setState(ProcessControlBlock.State.TERMINATED);
            }
                break;
            default:
                result = -1;
        }
        return result;
    }

    /**
     * Invoke the interrupt handler (see InterruptHandler), providing the interrupt
     * type and zero or more arguments.
     * The FCFS kernel only handles WAKE_UP events, not TIME_OUT events. The latter
     * will cause an IllegalArgumentException to be thrown.
     */
    public void interrupt(int interruptType, Object... varargs) {
        switch (interruptType) {
            case TIME_OUT:
                throw new IllegalArgumentException(
                        "FCFSKernel:interrupt(" + interruptType + "...): this kernel does not support timeouts.");
            case WAKE_UP:
                ProcessControlBlock process = (ProcessControlBlock) varargs[1];
                process.setState(ProcessControlBlock.State.READY);
                readyQueue.addLast(process);
                if (Config.getCPU().isIdle()) {
                    this.dispatch();
                }
                break;
            default:
                throw new IllegalArgumentException("FCFSKernel:interrupt(" + interruptType + "...): unknown type.");
        }
    }

    /**
     * Create a ProcessControlBlock for the program with the given file name.
     * This method is a wrapper for handling I/O exceptions. Its purpose to make the
     * syscall method 'cleaner' and easier to read.
     */
    private static ProcessControlBlock loadProgram(String filename) {
        try {
            return ProcessControlBlock.loadProgram(filename);
        } catch (FileNotFoundException fileExp) {
            throw new IllegalArgumentException("FCFSKernel: loadProgram(\"" + filename + "\"): file not found.");
        } catch (IOException ioExp) {
            throw new IllegalArgumentException("FCFSKernel: loadProgram(\"" + filename + "\"): IO error.");
        }
    }
}

package simulator;
import simulator.Config;
import simulator.CPUInstruction;
import simulator.Instruction;
import simulator.IOInstruction;
import simulator.Profiling;
import simulator.SystemTimer;
//
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
//
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Scanner;
/**
 * A ProcessControlBlock is used to represent and manage a process in the system.
 * 
 * @author Stephan Jamieson
 * @version 13/3/15
 */
public class ProcessControlBlock {

    /**
     * Possible process states.
     */
    public enum State { WAITING, READY, RUNNING, TERMINATED };

    private State state;
    private int ID;
    private int priority;
    private String programName;
    private Iterator<Instruction> instructions;
    private Instruction current;
    
    private boolean kernelInterrupt; 
    /** 
     * Create a ProcessControlBlock with the give PID, program name and list of program instructions.
     */
    private ProcessControlBlock(int ID, String programName, List<Instruction> instructions) {
        this.ID = ID;
        this.programName = programName;
        this.priority=0;
        this.instructions = instructions.iterator();
        assert(this.instructions.hasNext());
        this.current = this.instructions.next(); 
        this.state = State.READY;
        this.kernelInterrupt = false;
        Profiling.createProfile(this);
    }

    /** 
     * Obtain the process ID.
     */
    public int getPID() { return ID; }
    
    /**
     * Obtain the name of the program being processed.
     */
    public String getProgramName() { return programName; }
    
    /**
     * Obtain the process priority.
     */
    public int getPriority() { return this.priority; }
    
    /**
     * Set the priority of this process.
     */
    public int setPriority(int value) {
        final int old = getPriority();
        this.priority=value;
        return old;
    }
    
    /**
     * Obtain the program instruction that is currently being processed.
     */
    public Instruction getInstruction() { return current; }
    
    /**
     * Determine whether there is another program instruction to be executed.
     */
    public boolean hasNextInstruction() { return instructions.hasNext(); }
    
    /**
     * Advance processing to the next instruction.
     */
    void nextInstruction() { assert(this.hasNextInstruction()); current = instructions.next(); }
    
    /**
     * Obtain the current process state.
     */
    public State getState() { return state; }
        
    /**
     * Set the current process state.
     */
    public void setState(State state) { 
        this.state = state; 
        Profiling.recordTransition(this.getPID(), this.getState());
    }
    
    
    /**
     * Integer counter for allocating process IDs. It is assumed that a simulation never involves so many processes as to cause overflow.
     */
    private static int nextID = 1;
    
    /**
     * Obtain a new process ID. (Increment ID counter and return the previous value.)
     */
    private static int makeID() {
        nextID++;
        return nextID-1;
    }
    
    /**
     * Instantiate a ProcessControlBlock for the program with the given file name.
     */
    public static ProcessControlBlock loadProgram(String filename) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        List<Instruction> instructions = new ArrayList<Instruction>();
        
        String line = reader.readLine();
        while (line!=null) {
            if (line.trim().startsWith("#")) {
                // It's a comment, ignore.
            }
            else {
                Scanner scanner = new Scanner(line);
                String token;
                int duration;
                if (!scanner.hasNext()) {
                    throw new IOException("ProcessControlBlockImpl:loadProgram("+filename+"): illegal line in file. Missing token.");
                }
                else {
                    token = scanner.next();
                }
                if (!scanner.hasNextInt()) {
                    throw new IOException("ProcessControlBlockImpl:loadProgram("+filename+"): illegal line in file. Missing duration.");
                }
                else {
                    duration = scanner.nextInt();
                }
                if (token.equals("CPU")) {
                    instructions.add(new CPUInstruction(duration));
                }
                else if (token.equals("IO")) {
                    if (!scanner.hasNextInt()) {
                        throw new IOException("ProcessControlBlockImpl:loadProgram("+filename+"): illegal I/O line in file. Missing device ID.");
                    }
                    else {
                        int deviceID = scanner.nextInt();
                        instructions.add(new IOInstruction(duration, deviceID));
                    }
                }
                else {
                    throw new IOException("ProcessControlBlockImpl:loadProgram("+filename+"): illegal token in file.");
                }
            }
            line = reader.readLine();
        }
        reader.close();
        
        return new ProcessControlBlock(ProcessControlBlock.makeID(), filename, instructions);
    }
    
    /**
     * Obtain a String representation for the associated process in the form  'pid=<PID>, state=<State>, name="<Name>"'.
     */
    public String toString() {
        return String.format("process(pid=%d, state=%s, name=\"%s\")", this.getPID(), this.getState(), this.getProgramName());
    }
}

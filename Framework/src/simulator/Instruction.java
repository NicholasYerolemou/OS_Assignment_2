package simulator; 
/**
 * Representation of program instruction in abstract.
 * 
 * @author Stephan Jamieson
 * @version 12/3/2016
 */
public abstract class Instruction {

    private ProcessControlBlock parent;
    private int duration;
    
    /**
     * Create an instruction of the given duration for the given process.
     */
    Instruction(int duration) {
        this.parent=parent;
        this.duration = duration;
    }
        
    /**
     * Obtain this instruction's duration.
     */
    public int getDuration() { return duration; }
    
    /**
     * Obtain the amount of time required to complete this burst (I/O or CPU depending on precise type.)
     */
    public int getBurstRemaining() {
        return this.getDuration();
    }
}

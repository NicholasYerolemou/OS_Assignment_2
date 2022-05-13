package simulator;

import simulator.Config;
import simulator.SystemTimer;
import simulator.ProcessControlBlock;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The profiling class maintains a record of process states, their start and end times, and thus duration. In the case of the RUNNING state, a distinction
 * is further made between time spent in CPU supervisor mode and time spent in user mode.
 * 
 * Profiling is permanently enabled. When a process is created, a corresponding profile (record of states) is created too. The profile is maintained for the
 * lifetime of the process. 
 * 
 * The class provides a static method with which to write profiling data to a comma separated-values (CSV) file. The method can be used at any 
 * time, however, at completion of simulation makes most sense.
 *
 * @author Stephan Jamieson
 * @version 14/4/2022
 */
public class Profiling {

    private Profiling() {}
    
    private static Map<Integer, Profile> profiles = new HashMap<Integer, Profile>();
    //private static List<Interval> intervals = new ArrayList<Interval>();
    
    
    /**
     * Update the profile for the given process, recording a change of CPU mode.
     */
    static void recordTransition(final Integer PID, final CPU.Mode mode) {
        profiles.get(PID).recordTransition(Config.getSystemTimer().getSystemTime(), mode);
    }

    /**
     * Update the profile for the process with the given PID, recording a change in state.
     */
    static void recordTransition(final Integer PID, final ProcessControlBlock.State state) {
        profiles.get(PID).recordTransition(Config.getSystemTimer().getSystemTime(), state);        
    }

    /**
     * Obtain a profiler for the target process. It is assumed that the profile is created as part of process instantiation and thus 
     * the CPU is in SUPERVISOR mode.
     */
    static void createProfile(final ProcessControlBlock target) {
        /*DEBUG System.out.printf("createProfile(%s)\n", target.toString()); */
        final long currentTime = Config.getSystemTimer().getSystemTime();
        final Profile profile = new Profile(currentTime, target.getPID(), target.getProgramName(), target.getState(), CPU.Mode.SUPERVISOR);
        Profiling.profiles.put(profile.getPID(), profile);
    }
    
    
    /**
     * Write a CSV file for all processes containing process states, along with start and finish times.
     * Lines are of uniform length. The first line contains the names for the sequence of data values on each subsequent line: '
     * "PID, STATE, MODE, START TIME, END TIME, PROGRAM".
     */
    public static void writeCSV(final String filename) throws IOException {
        final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename));
        bufferedWriter.write(Interval.HEADER);
        bufferedWriter.newLine();
        for(Profile profile : profiles.values()) {
    
            for (Interval interval : profile.getIntervals()) {
                bufferedWriter.write(interval.toString());
                bufferedWriter.newLine();            
            }
        }
        bufferedWriter.close();
    }
    
    /**
     * An Interval represents a period during which a process is in a particular state (RUNNING, WAITING, ...) and mode (USER, SUPERVISOR).
     */
    private static class Interval {
        final int PID;
        final String programName;
        final ProcessControlBlock.State state;
        final CPU.Mode mode;
        final Long startTime;
        final Long endTime;
        
        /**
         * Create an Interval representing the period between the given transitions.
         */
        Interval(final int PID, final String programName, final Transition previous, final Transition current) {
            this(PID, programName, previous.state, previous.mode, previous.timeStamp, current.timeStamp);
        }
        
        /**
         * Create an Interval for the given data values.
         */
        Interval(final int PID, final String programName, final ProcessControlBlock.State state, final CPU.Mode mode, final Long startTime, final Long endTime) {
            this.PID = PID;
            this.programName = programName;
            this.state = state;
            this.mode = mode;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        /**
         * Obtain the Interval duration.
         */
        public long duration() { return endTime-startTime; }
        
        
        /**
         * Obtain a String representation of this interval in the form '<PID>, <Process state>, <CPU mode>, <Start time>, <End time>'. 
         */
        public String toString() {
            final String mode = (this.state == ProcessControlBlock.State.WAITING || this.state == ProcessControlBlock.State.READY
                || this.state == ProcessControlBlock.State.TERMINATED) ? "N/A" : this.mode.toString();
            final String endTime = this.endTime == null ? "-" : String.format("%010d", this.endTime.longValue());
            return String.format("%03d, %s, %s, %010d, %s, %s", this.PID, this.state, mode, this.startTime, endTime, this.programName); 
        }
        
        /**
         * String representing the data format produced by 'toString()'.
         */
        public final static String HEADER = "PID, STATE, MODE, START TIME, END TIME, PROGRAM";
    }
    
    /**
     * A Transition represents a change in a process state and/or CPU mode.
     */
    private static class Transition {
        private final Long timeStamp;
        private final ProcessControlBlock.State state;
        private final CPU.Mode mode;
        public Transition(final Long timeStamp, final ProcessControlBlock.State state, final CPU.Mode mode) {
            this.timeStamp = timeStamp;
            this.state = state;
            this.mode = mode;
        }
        
        /**
         * Two Transitions are equivalent if they represent the same process state and CPU mode.
         */
        public boolean equivalentTo(Transition other) {
            return this.state == other.state && this.mode == other.mode;
        }
        
        /**
         * Obtain a String representation of this Transition in the form '<Time stamp>, <Process state>, <CPU mode>'.
         */
        public String toString() {
            return String.format("%d, %s, %s", timeStamp, state, mode);
        }
    }
    
    
    /**
     * A Profile logs the state transitions for a given process.
     */
    static class Profile {

        private Transition previousTransition;
        private List<Interval> intervals;
        private final int PID;
        private final String programName;
        
        /**
         * Create a new profiler for the given target.
         * 
         * Requires target has initial state set.
         */
        Profile(final long timeStamp, final int PID, final String programName, final ProcessControlBlock.State state, CPU.Mode mode) {
            this.PID = PID;
            this.programName = programName;
            this.previousTransition = new Transition(timeStamp, state, mode);
            this.intervals = new ArrayList<Interval>();
            /*DEBUG System.out.printf("%d, %s\n", this.PID, this.previousTransition);*/

        }
    
        /**
         * Record that the target has transitioned from previousState to newState.
         */
        public void recordTransition(final long timeStamp, final ProcessControlBlock.State newState) {
            recordTransition(timeStamp, newState, previousTransition.mode);
        }  
        
        /**
         * Record that the target has transitioned into a different processor mode.
         */
        public void recordTransition(final long timeStamp, final CPU.Mode mode) {
            recordTransition(timeStamp, previousTransition.state, mode);
        }
        
        private void recordTransition(final long timeStamp, final ProcessControlBlock.State state, CPU.Mode mode) {
            final Transition currentTransition = new Transition(timeStamp, state, mode);
            Interval interval = new Interval(this.PID, this.programName, previousTransition, currentTransition);
            if (interval.duration()>0) {
                if (interval.state == ProcessControlBlock.State.READY && !intervals.isEmpty()) {
                    final Interval previous = intervals.get(intervals.size()-1);
                    if (previous.state == ProcessControlBlock.State.READY) {
                        intervals.remove(intervals.size()-1);
                        interval = new Interval(this.PID, this.programName, previous.state, previous.mode, previous.startTime, interval.endTime);   
                    }
                }
                this.intervals.add(interval);
            }
            /*DEBUG System.out.printf("%d, %s\n", this.PID, currentTransition); */
            previousTransition = currentTransition;
            
            if (state == ProcessControlBlock.State.TERMINATED) {
                final Transition terminal = new Transition(null, state, mode);
                this.intervals.add(new Interval(this.PID, this.programName, previousTransition, terminal));
            }
        }

        /**
         * Obtain the PID of the process associated with this profile.
         */
        public int getPID() {
            return this.PID;
        }
        
        /**
         * Obtain a list of the Intervals that the associated process has passed through.
         */
        public List<Interval> getIntervals() {
            return this.intervals;
        }
        
        /** 
         * Obtain the name of the program run by the process associated with this profile.
         */
        public String getProgramName() { return this.programName; }
    }
}

package simulator;
import java.util.HashMap;
import java.util.Map;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
//
import java.util.Scanner;
/**
 * The class manages the configuration and running of a simulation.
 * <p>
 * It collates the hardware components: clock, cpu, and I/O devices.
 * Classes such as concrete Kernels can access these components here. 
 * 
 * @author Stephan Jamieson
 * @version 13/3/16
 */
public class Config {

    
    private Config() {}
    
    private static SimulationClock clock;
    private static EventScheduler scheduler;
    private static CPU cpu;
    private static Kernel kernel;
    private static Map<Integer, IODevice> devices = new HashMap<Integer, IODevice>();    
    
    static SimulationClock getSimulationClock() { return Config.clock; }
    static EventScheduler getEventScheduler() { return Config.scheduler; }
    static Kernel getKernel() { return kernel;}


    /**
     * Obtain the SystemTimer used in the current configuration.
     */
    public static SystemTimer getSystemTimer() { return Config.getSimulationClock(); }
    
    /**
     * Obtain the CPU used in the current configuration.
     */
    public static CPU getCPU() { return Config.cpu; }
    
    /**
     * Add an IODevice to the configuration.
     */
    public static void addDevice(IODevice device) { devices.put(device.getID(), device); }
    
    /** 
     * Obtain the IODevice with the given ID.
     */
    public static IODevice getDevice(int ID) { 
        final IODevice device = devices.get(ID);
        if (device == null) {
            throw new ConfigurationBuildException(String.format("There is no I/O device with the id '%d'. Check the configuration file.", ID));
        }
        else {
            return device;
        }
    }
    
    /**
     * Create an initial simulation configuration that uses the given kernel, context switch cost and 
     * system call cost.
     */
    public static void init(Kernel kernel, int cSwitchCost, int sysCallCost) {
        Config.clock=new SimulationClock(sysCallCost, cSwitchCost);
        Config.scheduler=new EventScheduler();
        Config.cpu=new CPU();
        Config.kernel=kernel;
    }
    
    /** 
     * Run the configured simulation (the init() and buildConfiguration() methods must be called first).
     */
    public static void run() {
        Config.clock.setSystemTime(0);
        Config.scheduler.run();
    }
       
    /**
     * An instance of this class represents a failure due to an error in workload configuration i.e., an error in a configuration file or program file.
     */
    public static class ConfigurationBuildException extends Error {
        public ConfigurationBuildException(final String string) {
            super(string);
        }
        public ConfigurationBuildException(final String string, final Throwable throwable) {
            super(string, throwable);
        }
        
    }

    /**
     * Complete the simulation configuration by uploading the given config file.
     */
    public static void buildConfiguration(String filename) throws ConfigurationBuildException, FileNotFoundException, IOException {
        final String _config_error = String.format("Config file \"%s\":", filename);            
        final File config_file = new File(filename);
        final BufferedReader reader = new BufferedReader(new FileReader(config_file));
        
        
        String line = reader.readLine().trim();
        while (line!=null) {
            if (line.startsWith("#") || line.equals("")) {
                // It's a commment or blank line, ignore.
            }
            else if (line.startsWith("PROGRAM")) {
                final String _program_error = _config_error+String.format(" PROGRAM entry missing %s: \"%s\".", "%s", line);
                // Retrieve path to config file.
                final Scanner scanner = new Scanner(line);
                scanner.next(); // Consume "PROGRAM"
                
                
                if (!scanner.hasNextInt()) {
                    // Whoops, missing program start time.
                    final String message = String.format(_program_error, "start time");
                    throw new ConfigurationBuildException(message);                        
                }
                final int startTime = scanner.nextInt();
                if (!scanner.hasNextInt()) {
                    // Whoops, priority.
                    final String message = _program_error+String.format(_program_error, "priority");
                    throw new ConfigurationBuildException(message);                        
                }
                final int priority = scanner.nextInt();
                if (!scanner.hasNext()) {
                    // Whoops, missing program name.
                    final String message = String.format(_program_error, "program name");
                    throw new ConfigurationBuildException(message);                        

                }
                final File program_file = new File(config_file.getParent(), scanner.next());
                Config.scheduler.schedule(new ExecveEvent(startTime, program_file.getPath(), priority, Config.getKernel())); 
            }
            else if (line.startsWith("DEVICE")) {
                final String _device_error = _config_error+String.format(" DEVICE entry missing %s: \"%s\".", "%s", line);
                final Scanner scanner = new Scanner(line);
                scanner.next(); // Consume "DEVICE"
                if (!scanner.hasNextInt()) {
                    // Whoops, missing device ID
                    final String message = String.format(_device_error, "missing device ID");
                    throw new ConfigurationBuildException(message);
                }
                final int deviceID = scanner.nextInt();
                if (!scanner.hasNext()) {
                    // Whoops, missing device ID
                    final String message = String.format(_device_error, "missing device type");
                    throw new ConfigurationBuildException(message);
                }
                final String deviceType = scanner.next();
                TRACE.SYSCALL(SystemCall.MAKE_DEVICE, deviceID, deviceType);
                Config.getSimulationClock().logSystemCall();
                Config.kernel.syscall(SystemCall.MAKE_DEVICE, deviceID, deviceType);
                TRACE.SYSCALL_END();
            }
            else {
                    final String message = String.format(_config_error, String.format("Unrecognised token in configuration file: \"%s\".", line));
                    throw new ConfigurationBuildException(message);
            }
            line = reader.readLine();
        }
        reader.close();
    }       
}

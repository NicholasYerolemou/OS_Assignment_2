import simulator.Kernel;
import simulator.Config;
import simulator.Profiling;
import simulator.TRACE;
//
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Scanner;
/**
 * Main Simulator program.
 * 
 * The Simulator program is used to execute and report on scheduling activity produced for a suite of programs executed using a particular kernel.
 * 
 * The program requests a number of inputs:
 * 
 * 1. The name of a configuration file describing the I/O devices to be created and the programs to be run. (See the Config class documentation.)
 * 2. The name of a Kernel implementation. (The class name not the file name e.g. FCFSKernel, not FCFSKernel.class or FCFSKernel.java.)
 * 3. Any initialisation values required by the Kernel. (e.g. Slice time in the case of a Round Robin kernel.)
 * 4. The number of time units taken by a system call.
 * 5. The number of time units taken by a context switch.
 * 6. The trace level. (A number between 0 and 31 which determines how much information the program will print while running the simulation.)
 * 7. Optionally, a filename for writing out process execution profiles. (See the Profiling class documentation.)
 * 
 * To function correctly, the program requires that a Kernel implementation provide a constructor that accepts an array of Object as a
 * parameter (formally defined as 'Object... varargs'). Initialisation values are passed to the Kernel in this way.
 * 
 * Execution profiles are output in the form of a comma-separated-values file. Each line details a process state: process ID, state, CPU mode, start
 * time, end time, name of program that the process is executing.
 * 
 * @author Stephan Jamieson
 * @version 25/4/2022
 */
public class Simulate {

    private Simulate() {}
    
    public static void main(String[] args) {
        System.out.println("*** Simulator ***");
        final Scanner scanner = new Scanner(System.in);
        
        System.out.print("Configuration file name? ");
        final String configFileName = scanner.nextLine().trim();

        System.out.print("Kernel name? ");
        String kernelName = scanner.nextLine().trim();
        System.out.print("Enter kernel parameters (if any) as a comma-separated list: ");
        final String kernelParameters = scanner.nextLine().trim();

        System.out.print("Cost of system call? ");
        final int sysCallCost = scanner.nextInt();

        System.out.print("Cost of context switch: ");
        final int dispatchCost = scanner.nextInt();

        System.out.print("Trace level (0-31)? ");
        final int traceLevel = scanner.nextInt();
        TRACE.SET_TRACE_LEVEL(traceLevel);

        System.out.println("Instantiating kernel with supplied parameters...");
        final Kernel kernel;       
        try {
            if (kernelName.endsWith(".class") || kernelName.endsWith(".java")) {
                kernelName = kernelName.substring(kernelName.length()-5);
            }

            final Class<?> target = Simulate.class.getClassLoader().loadClass(kernelName); // , Simulate.class.getClassLoader()); 
            kernel = (Kernel)target.getConstructor(Object[].class).newInstance((Object)kernelParameters.split("\\s*,\\s*"));   
        }
        catch (ClassNotFoundException classNotFound) {
            System.out.println("Unable to load kernel class. Wrong name or not compiled?");
            return ;
        }
        catch (NoSuchMethodException noSuchConstruct) {
            System.out.println(noSuchConstruct);
            System.out.println("Unable to locate suitable constructor.\n");
            return;
        } 
        catch (Exception excep) {
            System.out.println("Unable to instantiate kernel");
            excep.printStackTrace();
            return;
        }
        
        System.out.println("Building configuration...");
        Config.init(kernel, dispatchCost, sysCallCost);
        try {
            Config.buildConfiguration(configFileName);
        }
        catch (Config.ConfigurationBuildException confBuildExcep) {
            System.out.println("An error occured attempting to process the configuration file.");
            System.out.println(confBuildExcep.getMessage());
            System.exit(-1);
        }
        catch (FileNotFoundException fnfExcep) {
            System.out.println("Unable to open configuration file. Not found.");
            System.exit(-1);
        }
        catch (IOException ioExcep) {
            System.out.println("An I/O error occured when processing the configuration file.");
            System.exit(-1);
        }
        
        System.out.println("Running simulation...");
        try {    
            Config.run();
            System.out.println("Done");
            System.out.println(Config.getSystemTimer());
            System.out.println("Context switches: "+Config.getCPU().getContextSwitches());
            System.out.printf("CPU utilization: %.2f\n", ((double)Config.getSystemTimer().getUserTime())/Config.getSystemTimer().getSystemTime()*100);
        }
        catch (Config.ConfigurationBuildException confBuildExcep) {
            System.out.println("An error occured attempting to run the simulation.");
            System.out.println(confBuildExcep.getMessage());
            System.exit(-1);
        }
        
        try {
            scanner.nextLine();
            System.out.println("Write execution profile to CSV? Enter a file name or press return: ");
            final String filename = scanner.nextLine().trim();
            if (!filename.equals("")) {
                Profiling.writeCSV(filename);
            }
        }
        catch (IOException ioExcep) {
            System.out.println("Unable to write CSV file.");
            ioExcep.printStackTrace();
            
        }
    }


    private static Kernel instantiateKernel(final String kernelName, final String kernelParameters) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Class<?> target = Simulate.class.getClassLoader().loadClass(kernelName); // , Simulate.class.getClassLoader());
        if (kernelParameters.equals("")) {
            return (Kernel)target.getConstructor().newInstance();
        }
        else {
            return (Kernel)target.getConstructor(String[].class).newInstance((Object)kernelParameters.split("\\s*,\\s*"));   
        }
    }
}
